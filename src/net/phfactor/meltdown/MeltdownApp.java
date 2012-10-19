package net.phfactor.meltdown;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

public class MeltdownApp extends Application 
{	
	static final String TAG = "MeltdownApp";

	static final String DATABASE = "items.db";
	static final String TABLE = "items";
	static final String C_ID = "_id";
	static final String C_HTML = "html";

	private static final String P_URL = "serverUrl";
	private static final String P_TOKEN = "token";
	private static final String P_LAST_FETCH = "last_ts";
	
	static final String FIRST_RUN = "first_run";
	static final int GROUP_UNKNOWN = -1;
	
	static final int ORPHAN_ID = 8675309;
	
	private List<RssGroup> groups;
	private List<RssFeed> feeds;
		
	private int max_read_id;	
	private long last_refresh_time;
	
	private RestClient xcvr;
	private SharedPreferences prefs;
	private SharedPreferences.Editor editor;

	public Boolean updateInProgress;

	public MeltdownApp()
	{
		super();
	}
		
	@Override
	public void onCreate()
	{
		super.onCreate();

		Log.i(TAG, "App created, initializing");
		max_read_id = 0;
		last_refresh_time = 0L;
		updateInProgress = false;		
	
		this.feeds = new ArrayList<RssFeed>();
		this.groups = new ArrayList<RssGroup>();
		
		prefs = getSharedPreferences(TAG, Context.MODE_PRIVATE);
		xcvr = new RestClient(this);		
		
		RssGroup orphans = new RssGroup("Orphaned feeds", ORPHAN_ID);		
		this.groups.add(orphans);
		
		startUpdates();
		
		Log.i(TAG, "App init completed.");
	}
	
	/*
	 * Start the Downloader, and add it to twice-hourly (approximate) via alarm service. We use
	 * inexact to save battery life; the fetches can be off but that's fine.
	 */
	protected void startUpdates()
	{
		// Tell downloader that this is the first run, so it should reload the items from disk
		Intent sIntent = new Intent(this, Downloader.class);
		sIntent.putExtra(FIRST_RUN, true);
		startService(sIntent);
		
		Log.i(TAG, "Setting up periodic updates...");
		
    	Context ctx = getApplicationContext();
		Intent svc_intent = new Intent(ctx, Downloader.class);
		svc_intent.putExtra("first_run", false);
		PendingIntent pending_intent = PendingIntent.getService(ctx, 0, svc_intent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
		am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), 
				AlarmManager.INTERVAL_FIFTEEN_MINUTES, pending_intent);

		Log.d(TAG, "Score update service scheduled for execution");		
	}
	
	public int getNumItems()
	{
		int rc = 0;
		for (int idx = 0; idx < groups.size(); idx++)
			rc += groups.get(idx).items.size();
		return rc;
	}
	
	protected long getLast_refresh_time()
	{
		return last_refresh_time;
	}
	
	/*
	 * Max item ID is used as part of the fetch calls, where we we ask for 'new since N'.
	 */
	protected int getMaxItemID() 
	{
		return max_read_id;
	}

	/*
	 *  Reverse index methods - find group, feed or post by numeric ID.
	 */
	protected RssGroup findGroupById(int grp_id)
	{
		for (int idx = 0; idx < groups.size(); idx++)
		{
			if (groups.get(idx).id == grp_id)
				return groups.get(idx);
		}
		return null;
	}
	
	protected RssFeed findFeedById(int feed_id)
	{
		for (int idx =0; idx < feeds.size(); idx++)
		{
			if (feeds.get(idx).id == feed_id)
				return feeds.get(idx);
		}
		return null;
	}
	
	protected RssItem findPostById(int post_id)
	{
		for (int grp_idx = 0; grp_idx < groups.size(); grp_idx++)
		{
			RssGroup group = groups.get(grp_idx);
			for (int idx = 0; idx < group.items.size(); idx++)
			{
				if (group.items.get(idx).id == post_id)
					return group.items.get(idx);
			}
		}
		return null;
	}
	
	// Similar - locate a group object by title string
	protected RssGroup findGroupByName(String name)
	{
		for (int idx = 0; idx < groups.size(); idx++)
		{
			if (groups.get(idx).title.equals(name))
				return groups.get(idx);
		}
		return null;
	}
	
	// Reverse index - search all items for those with a feed ID in the current groups' feed list
	public List<Integer> itemsIDsForGroup(int group_id)
	{
		RssGroup group = findGroupById(group_id);
		if (group == null)
			return null;

		List<Integer>rc = new ArrayList<Integer>();
		for (int idx = 0; idx < group.items.size(); idx++)
		{
			rc.add(group.items.get(idx).id);
		}
		return rc;
	}
	
	public int unreadItemCount(int group_id)
	{
		RssGroup group = findGroupById(group_id);
		if (group == null)
			return 0;

		return groupUnreadItems(group);
	}

	public int groupUnreadItems(RssGroup group)
	{
		// TODO / FIXME - check each item for read status??
		int count = 0;
		for (int idx = 0; idx < group.items.size(); idx++)
			if (group.items.get(idx).is_read == false)
				count++;
		return count;		
	}
	
	// Return items, with HTML only lazy-loaded at the last minute by the caller.
	public List<RssItem> getItemsForGroup(int group_id)
	{
		RssGroup grp = findGroupById(group_id);
		if (grp == null)
		{
			Log.e(TAG, "Unable to locate group id " + group_id);
			return null;
		}
		return grp.items;
	}
	
	public List<RssGroup> getUnreadGroups()
	{
		ArrayList<RssGroup> rc = new ArrayList<RssGroup>();
		for (int idx = 0; idx < groups.size(); idx++)
		{
			RssGroup thisgrp = groups.get(idx);
			if (groupUnreadItems(thisgrp) > 0)
				rc.add(thisgrp);
		}
		return rc;
	}
		
	/* ***********************************
	 * REST callback methods
	 */
	
	// Feeds groups associate groups with the feeds they contain.
	private void saveFeedsGroupsData(JSONObject payload) throws JSONException
	{
		JSONArray fg_arry = payload.getJSONArray("feeds_groups");
		for (int idx = 0; idx < fg_arry.length(); idx++)
		{
			JSONObject cur_grp = fg_arry.getJSONObject(idx);
			int grp_id = cur_grp.getInt("group_id");
			RssGroup grp = findGroupById(grp_id);
			if (grp != null)
				grp.feed_ids = parseStrArray(cur_grp.getString("feed_ids"));
			else
				Log.e(TAG, "Unable to find group ID " + grp_id);
		}
	}

	/*!
	 *  Take the data returned from a groups fetch, parse and save into data model.
	 */
	protected void saveGroupsData(String payload)
	{
		JSONArray jgroups;		
		RssGroup newGroup;
		
		if (payload == null)
			return;
		
		try
		{
			JSONObject jsonPayload = new JSONObject(payload);			
			jgroups = jsonPayload.getJSONArray("groups");
			for (int idx = 0; idx < jgroups.length(); idx++)
			{
				newGroup = new RssGroup(jgroups.getJSONObject(idx), this);
				
				// Merge with existing if post-initialization
				RssGroup oldGroup = findGroupById(newGroup.id);
				if (oldGroup != null)
				{
					Log.d(TAG, "Merging updating group info for " + newGroup.title);
					if (!newGroup.title.equals(oldGroup.title))
					{
						Log.d(TAG, "Title changed, now " + newGroup.title);
						oldGroup.title = newGroup.title;
					}
					
					if (!oldGroup.feed_ids.containsAll(newGroup.feed_ids))
					{
						Log.d(TAG, "Feed IDs have changed");
						oldGroup.feed_ids = newGroup.feed_ids;
					}
				}
				else
				{
					// Log.d(TAG, "Doing initial groups allocation for " + newGroup.title);					
					groups.add(newGroup);
				}
			}			
			saveFeedsGroupsData(jsonPayload);
			
		} catch (JSONException e) 
		{
			e.printStackTrace();
		}		
		
		Log.i(TAG, groups.size() + " groups found");
	}

	protected void saveFeedsData(String payload)
	{
		JSONArray jfeeds;
		
		if (payload == null)
			return;
		
		this.feeds = new ArrayList<RssFeed>();
		
		try
		{
			JSONObject jpayload = new JSONObject(payload);
			jfeeds = jpayload.getJSONArray("feeds");
			this.last_refresh_time = jpayload.getLong("last_refreshed_on_time");
			for (int idx = 0; idx < jfeeds.length(); idx++)
				feeds.add(new RssFeed(jfeeds.getJSONObject(idx)));
			
		} catch (JSONException e) 
		{
			e.printStackTrace();
		}
		Log.d(TAG, feeds.size() + " feeds found");
	}

	/*
	 * feeds groups and unread item ids are returned as CSV strings; common code to parse same.
	 */
	private List<Integer> parseStrArray(String array_str)
	{
		String[] nums = array_str.split(",");
		List<Integer> rc = new ArrayList<Integer>();
		
		for (int idx = 0; idx < nums.length; idx++)
			rc.add(Integer.valueOf(nums[idx]));
		return rc;		
	}
	
	//As with the parseFeedsGroups, this is retured as a comma-delimited string we have to parse.
	private List<Integer> fetchUnreadItemsIDs()
	{
		List<Integer> rc= new ArrayList<Integer>();
		JSONObject jdata;
		try
		{
			jdata = new JSONObject(xcvr.fetchUnreadList());
			String array_str = jdata.getString("unread_item_ids");
			return parseStrArray(array_str); 
		} catch (JSONException je)
		{
			Log.e(TAG, "Error parsing list of unread item IDs");
		}
		return rc;
	}
	
	/* The unread_items_ids returns an N-length array of items that are unread on the server. We have to:
	 * parse the list
	 * match what we have locally
	 * fetch the new items in blocks of 50 or less.
	 * cull local items not on the list
	 */	
	protected void syncUnreadPosts(Boolean reload_from_disk)
	{
		List<Integer> unreadItems = fetchUnreadItemsIDs();
		
		Log.d(TAG, unreadItems.size() + " unread items found on server.");	
		if (unreadItems.size() == 0)
			return;
		
		if (reload_from_disk)
			reloadItemsFromDisk(unreadItems);
		else
		{
			Log.i(TAG, "Sweeping out stale posts...");
			for (int idx = 0; idx < groups.size(); idx++)
			{
				RssGroup grp = groups.get(idx);
				int old_size = grp.items.size();
				for (int i = 0; i < grp.items.size(); i++)
				{
					int item_id = grp.items.get(i).id;
					if (!unreadItems.contains(item_id))
					{
						// FIXME copy instead of remove?
						grp.items.remove(grp.items.get(i));
					}
				}
				
				int new_size = grp.items.size();
				Log.d(TAG, new_size - old_size + " items removed from " + grp.title + ", " + new_size + " remain");
			}
		}

		Log.i(TAG, "Checking for posts I need to pull...");
		// So stale posts are removed, now pull from server any we don't have locally
		List<Integer> itemsToGet = new ArrayList<Integer>();
		for (int idx = 0; idx < unreadItems.size(); idx++)
		{
			if (findPostById(unreadItems.get(idx)) == null)
				itemsToGet.add(unreadItems.get(idx));
		}
		
		Log.i(TAG, itemsToGet.size() + " items to retrieve");
		// Could use size of newItems for progress dialog. Someday. Maybe a 'persistent activity' notification? TODO.
		final int PER_FETCH = 50;
		final int num_fetches = (int) Math.ceil(itemsToGet.size() / PER_FETCH) + 1;
		for (int idx = 0; idx < num_fetches; idx++)
		{
			int left_index = idx * PER_FETCH;
			int right_index = Math.min((idx + 1) * PER_FETCH, itemsToGet.size());
			Log.d(TAG, "On run " + idx + " pulling from " + left_index + " to " + right_index + " of " + itemsToGet.size());
			
			List<Integer> ids = itemsToGet.subList(left_index, right_index);
			String payload = xcvr.fetchListOfItems(ids);
			saveItemsData(payload);
		}		
		
	}
		
	/*
	 * We have group->feed and item->feed mappings, need to make feed->group
	 */
	protected void updateFeedIndices()
	{
		for (int idx = 0; idx < feeds.size(); idx++)
		{
			RssFeed feed = feeds.get(idx);
			RssGroup group = findGroupHoldingFeed(feed.id);
			if (group == null)
			{
				Log.e(TAG, "No group found for feed " + feed.title);
				continue;
			}
			// Skip duplicates
			if (feed.groups.contains(group.id))
				continue;
			
			feed.groups.add(group.id);
		}
	}
	
	RssGroup findGroupHoldingFeed(int feed_id)
	{
		for (int idx = 0; idx < groups.size(); idx++)
		{
			if (groups.get(idx).feed_ids.contains(feed_id))
				return groups.get(idx);
		}
		return null;
	}
	
	RssGroup findGroupForItem(RssItem item)
	{
		if (item == null)
			return null;
		
		RssFeed feed = findFeedById(item.feed_id);
		// FIXME
		if (feed.groups.size() == 0)
		{
			Log.e(TAG, "Feed " + feed.title + " has no group!");
			return null;
		}
		else if (feed.groups.size() > 1)
			Log.d(TAG, "Feed " + feed.title + " has " + feed.groups.size() + " group(s)");
		
		return findGroupById(feed.groups.get(0));
	}
	
	// Save RSS items parsed from payload, return number saved.
	public int saveItemsData(String payload)
	{
		JSONArray jitems;
		RssItem this_item;
		
		if (payload == null)
			return 0;
		
		Context ctx = getApplicationContext();
		
		try
		{
			JSONObject jdata = new JSONObject(payload);
			
			jitems = jdata.getJSONArray("items");
			// Check ending condition(s)
			if (jitems.length() == 0)
			{
				Log.i(TAG, "No more items in feed!");
				return -1;
			}
			
			this.last_refresh_time = jdata.getLong("last_refreshed_on_time");
			for (int idx = 0; idx < jitems.length(); idx++)
			{
				this_item = new RssItem(jitems.getJSONObject(idx));
				// Save it to disk
				this_item.saveToFile(ctx);
				
				// Remove the HTML for later loading if needed
				this_item.dropHTML();

				saveRssItem(this_item);
			}
			return jitems.length();
		} catch (JSONException e) 
		{
			e.printStackTrace();
		}			
		return 0;		
	}
	
	private void saveRssItem(RssItem item)
	{
		RssGroup group = findGroupForItem(item);
		if (group == null)
		{
			Log.d(TAG, "Orphan item! No group found for post ID " + item.id + " " + item.title);
			group = findGroupById(ORPHAN_ID);
		}
		group.items.add(item);
	}
	
	
	// At startup, load all of the items from disk, and reset max_ids before starting download from server.
	// Called by the Downloader, as this takes minutes for 10k items on a nexus7.
	private void reloadItemsFromDisk(List<Integer> new_items)
	{
		Log.d(TAG, "Starting load of data from disk");
		Long tzero = System.currentTimeMillis();
		Long last_printf = System.currentTimeMillis();
		Long notif_delta = 2000L;
		
		String[] files = fileList();
		int fcount = 0;
		Context ctx = getApplicationContext();
		
		for (int idx = 0; idx < files.length; idx++)
		{
			// Polish - display an update every couple seconds
			if ((System.currentTimeMillis() - last_printf) >= notif_delta)
			{
				last_printf = System.currentTimeMillis();
				Log.d(TAG, " - at " + idx + " of " + files.length + " files");
			}
			
			int post_id = filenameToInt(files[idx]);
			
			// Must be a valid ID and as-yet-unread
			if ((post_id > 0) && (new_items.contains(post_id)))
			{
				RssItem item = new RssItem(ctx, post_id);
				saveRssItem(item);
				fcount++;
			}
			else if (post_id > 0)
				deleteFile(files[idx]);
				
		}
		Log.d(TAG, files.length + " files processed, " + fcount + " loaded in " + 
		((System.currentTimeMillis() - tzero) / 1000L) + " seconds");
		Log.d(TAG, "Max read ID is now " + max_read_id);
	}
	
	/*
	 * Iterate over the items, remove any that are marked read. GC, called by Downloader 
	 * and when exiting a group view. Mark and sweep GC, very basic.
	 */
	protected void sweepReadItems()
	{
		for (int gidx = 0; gidx < groups.size(); gidx++)
		{
			RssGroup group = groups.get(gidx);
			for (int idx = 0; idx < group.items.size(); idx++)
			{
				RssItem item = group.items.get(idx);
				if (item.is_read)
				{
					item.deleteDiskFile();
					group.items.remove(item);
				}
			}
			//Log.d(TAG, group.items.size() + " items left in " + group.title + " after read sweep");
		}
	}
	
	protected Boolean verifyLogin()
	{
		// FIXME Async login check
		return haveSetup();
	}
	
	// See http://stackoverflow.com/questions/5815423/sorting-arraylist-in-android-in-alphabetical-order-case-insensitive
	protected void sortGroupsByName()
	{
		Collections.sort(groups, new Comparator<RssGroup>() {
			public int compare(RssGroup r1, RssGroup r2) {
				return String.CASE_INSENSITIVE_ORDER.compare(r1.title, r2.title);
			}
		});		
	}
	
	// Newest first - reversed order by comparing 2 to 1
	// http://stackoverflow.com/questions/5894818/how-to-sort-arraylistlong-in-java-in-decreasing-order
	protected void sortItemsByDate()
	{
		for (int idx = 0; idx < groups.size(); idx++)
		{
			Collections.sort(groups.get(idx).items, new Comparator<RssItem>() {
				public int compare(RssItem r1, RssItem r2) {
					return r2.created_on_time.compareTo(r1.created_on_time);
				}
			});
		}
	}
	
	// Mark an item/post as read, both locally and on the server.
	public synchronized void markItemRead(int item_id, int group_id)
	{
		// Get the server update running in the background
		xcvr.markItemRead(item_id);

		RssItem item = findPostById(item_id);
		if (item != null)
			item.is_read = true;			
	}
	
	public synchronized void markItemSaved(int item_id)
	{
		xcvr.markItemSaved(item_id);
		
		RssItem item = findPostById(item_id);
		if (item != null)
			item.is_saved = true;					
	}
	
	// Mark some thread read in a group matching a given title.
	// Use case is long-running threads in RSS feeds from WatchUSeek and similar.
	protected int markGroupThreadRead(int group_id, String title)
	{
		int rm_count = 0;
		RssGroup grp = findGroupById(group_id);
		if (grp == null)
		{
			Log.e(TAG, "Unable to locate group " + group_id);
			return 0;
		}
	
		for (int idx = 0; idx < grp.items.size(); idx++)
		{
			RssItem item = grp.items.get(idx);
			if (item.title.equals(title))
			{
				item.is_read = true;
				rm_count++;
			}
		}
		sweepReadItems();
		return rm_count;
	}
	
	// Iterate over a group, and mark all of the items in it as read. One API call and then local cleanup.
	protected synchronized void markGroupRead(int group_id)
	{
		RssGroup grp = findGroupById(group_id);
		if (grp == null)
		{
			Log.e(TAG, "Unable to locate group " + group_id);
			return;
		}
	
		Log.d(TAG, "Starting to mark group " + grp.title + " as read");
		xcvr.markGroupRead(group_id, last_refresh_time);
	
		for (int idx = 0; idx < grp.items.size(); idx++)
		{
			grp.items.get(idx).is_read = true;
		}
		sweepReadItems();
	}

	/*
	 * Given an items' filename in the format "%d.post", return the %d
	 */
	private int filenameToInt(String filename)
	{
		if (filename == null)
			return -1;
		
		String delims = "[.]";
		String[] tokens = filename.split(delims);
		if (tokens.length == 2)
		{
			try
			{
				return (Integer.parseInt(tokens[0]));				
			} catch (NumberFormatException nfe)
			{
				return -1;
			}
		}
		
		Log.e(TAG, "Unable to parse " + filename);
		return -1;
	}
	
	// How many cache files present?
	protected int getFileCount()
	{
		int count = 0;
		String[] filenames = fileList();
		for (int idx = 0; idx < filenames.length; idx++)
		{
			if(filenameToInt(filenames[idx]) > 0)
				count++;
		}
		return count;
	}
	
	/* Sweep disk files and remove any not present in the in-memory items array
	 * 
	 */
	protected void cullItemFiles()
	{
		Log.d(TAG, "Starting cull of on-disk files");
		Long ftzero = System.currentTimeMillis();
		int rm_ct = 0;
		
		String[] filenames = fileList();
		for (int idx = 0; idx < filenames.length; idx++)
		{
			int fn_idx = filenameToInt(filenames[idx]);
			if (fn_idx > 0)
			{
				if (findPostById(fn_idx) == null)
				{
					if (!deleteFile(filenames[idx]))
						Log.e(TAG, " error removing " + filenames[idx]);
					else
						rm_ct++;
				}
			}
			else
				Log.d(TAG, "Skipping non-post file " + filenames[idx]);
		}
		
		Long ftend = System.currentTimeMillis();
		Log.d(TAG, "Cull of " + filenames.length + " completed in " + (ftend - ftzero) / 1000L + " seconds, " +
		rm_ct + " removed.");
	}
	
	// Called by Downloader - this cues the creation of old-vs-new lists for post-download GC
	protected void download_start()
	{
		this.updateInProgress = true;
	}
	
	protected void download_complete()
	{		
		this.updateInProgress = false;
	}
	
	// Prefs currently used for storing and retrieving server/email/password
	// *****************************************************************************
	//! @see http://stackoverflow.com/questions/8700744/md5-with-android-and-php
	// Used for creating the dev token
	private static final String md5(final String s) 
	{
		try 
		{
			// Create MD5 Hash
			MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
			try 
			{
				digest.update(s.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) 
			{
				Log.e(TAG, "Error encoding into UTF-8!", e);
				e.printStackTrace();
			}
			byte messageDigest[] = digest.digest();

			// Create Hex String
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < messageDigest.length; i++) {
				String h = Integer.toHexString(0xFF & messageDigest[i]);
				while (h.length() < 2)
					h = "0" + h;
				hexString.append(h);
			}
			return hexString.toString();

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return "";
	}	
	
	public void setConfig(String url, String email, String password)
	{
		editor = prefs.edit();
		editor.putString(P_URL, url);
		editor.putString(P_TOKEN, makeAuthToken(email, password));
		editor.commit();
	}
	
	protected String getURL()
	{
		return prefs.getString(P_URL, null);
	}
	
	protected String getToken()
	{
		return prefs.getString(P_TOKEN, null);
	}
	
	protected String getAPIUrl()
	{
		return getURL() + "/?api";
	}
		
	protected void updateTimestamp()
	{
		editor = prefs.edit();
		editor.putLong(P_LAST_FETCH, System.currentTimeMillis() / 1000L);
		editor.commit();
	}
	
	public long getLastFetchTime()
	{
		return prefs.getLong(P_LAST_FETCH, 0L);
	}
	
	public Boolean haveSetup()
	{
		if (prefs.getString(P_URL, null) == null)
			return false;
		if (prefs.getString(P_TOKEN, null) == null)
			return false;
		
		return true;
	}
	
	protected String makeAuthToken(String email, String pass)
	{
		String pre = String.format("%s:%s", email, pass);
		return md5(pre);
	}
	
}
 