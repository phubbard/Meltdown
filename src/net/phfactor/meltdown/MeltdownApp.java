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
	
	private List<RssGroup> groups;
	private List<RssFeed> feeds;
		
	private int max_read_id;	
	private long last_refresh_time;
	
	private RestClient xcvr;
	private SharedPreferences prefs;
	private SharedPreference`s.Editor editor;

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
		this.items = new ArrayList<RssItem>();
		
		prefs = getSharedPreferences(TAG, Context.MODE_PRIVATE);
		xcvr = new RestClient(this);		
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
		return items.size();
	}
	
	protected long getLast_refresh_time()
	{
		return last_refresh_time;
	}
	
	// Simple helper - if items is empty, we are waiting for something. Used by main page as a way
	// to keep or dismiss the progress dialog.
	protected Boolean waiting_for_data()
	{
		return (items.size() == 0);
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
		for (int idx = 0; idx < items.size(); idx++)
		{
			if (items.get(idx).id == post_id)
				return items.get(idx);
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

		return group.items;
	}
	
	public int unreadItemCount(int group_id)
	{
		
	}
	// Unread items count for a given group ID
	public int unreadItemCount(int group_id)
	{
		List<Integer> plist = itemsIDsForGroup(group_id);
		int rc = 0;
		for (int idx = 0; idx < plist.size(); idx++)
		{
			if (findPostById(plist.get(idx)) != null)
			{
				if (findPostById(plist.get(idx)).is_read)
					continue;
				rc++;
			}
		}
		
		return rc;
	}
	
	// Return items, with HTML only lazy-loaded at the last minute by the caller.
	public List<RssItem> getItemsForGroup(int group_id)
	{
		ArrayList<RssItem> rc = new ArrayList<RssItem>();
		RssGroup grp = findGroupById(group_id);
		if (grp == null)
		{
			Log.e(TAG, "Unable to locate group id " + group_id);
			return null;
		}

		for (int idx = 0; idx < grp.items.size(); idx++)
		{
			RssItem item = findPostById(grp.items.get(idx));
			if (item != null)
				if (item.is_read == false)
					rc.add(findPostById(grp.items.get(idx)));
		}

		Log.d(TAG, rc.size() + " items for group ID " + group_id + " " + grp.title);
		return rc;
	}
	
	public List<RssGroup> getUnreadGroups()
	{
		ArrayList<RssGroup> rc = new ArrayList<RssGroup>();
		for (int idx = 0; idx < groups.size(); idx++)
		{
			RssGroup thisgrp = groups.get(idx);
			if (thisgrp.items.size() > 0)
				rc.add(thisgrp);
		}
		return rc;
	}
	
	// Returns all groups, with or without items in them
	public List<RssGroup> getGroups()
	{
		return this.groups;
	}
	
	// After a fetch and GC, build item indices for each group
	protected void updateGroupIndices()
	{
		Log.d(TAG, "Updating group counts and indices....");
		Long tzero = System.currentTimeMillis();
		
		// Clear the old
		for (int idx = 0; idx < groups.size(); idx++)
			groups.get(idx).items.clear();
		
		for (int idx = 0; idx < items.size(); idx++)
		{
			int current_if_id = items.get(idx).feed_id;
			// Find the group holding this items' feed. May be in more than one group!
			for (int grp_idx = 0; grp_idx < groups.size(); grp_idx++)
			{
				RssGroup cur_grp = groups.get(grp_idx);
				if (cur_grp.feed_ids.contains(current_if_id))
				{
					cur_grp.items.add(items.get(idx).id);
				}
			}			
		}
		Long tend = System.currentTimeMillis();
		Log.d(TAG, "Group counts updated in " + (tend - tzero) + " msec");
	}
	
	public int getTotalUnread()
	{
		return items.size();
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
			for (int grp_idx = 0; grp_idx < groups.size(); grp_idx++)
			{
				if (groups.get(grp_idx).id == grp_id)
					groups.get(grp_idx).feed_ids = parseStrArray(cur_grp.getString("feed_ids"));
			}
		}
	}

	/*!
	 *  Take the data returned from a groups fetch, parse and save into data model.
	 */
	protected void saveGroupsData(String payload)
	{
		JSONArray jgroups;		
		RssGroup this_grp;
		
		if (payload == null)
			return;
		
		this.groups = new ArrayList<RssGroup>();

		try
		{
			JSONObject jsonPayload = new JSONObject(payload);			
			jgroups = jsonPayload.getJSONArray("groups");
			for (int idx = 0; idx < jgroups.length(); idx++)
			{
				this_grp = new RssGroup(jgroups.getJSONObject(idx), this);				
				groups.add(this_grp);
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
	
	private void clearAllItems()
	{
		for (int idx = 0; idx < groups.size(); idx++)
			groups.get(idx).clearItems();
	}
	
	private Boolean havePostById(int item_id)
	{
		for (int idx = 0; idx < groups.size(); idx++)
		{
			RssGroup grp = groups.get
		}
	}
	
	protected void doServerSync(Boolean reload_from_disk)
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
				for (int i = 0; i < grp.items.size(); i++)
				{
					int item_id = grp.items.get(i).id;
					if (!unreadItems.contains(item_id))
					{
						grp.items.remove(grp.items.get(i));
					}
				}
			}
		}
		
		// So stale posts are removed, now pull from server any we don't have locally
		List<Integer> itemsToGet = new ArrayList<Integer>();
		for (int idx = 0; idx < unreadItems.size(); idx++)
		{
			if havePostById(unreadItems.get(idx))
				itemsToGet.add(unreadItems.get(idx));
		}
		
	}
	
	
	/* The unread_items_ids returns an N-length array of items that are unread on the server. We have to:
	 * parse the list
	 * match what we have locally
	 * fetch the new items in blocks of 50 or less.
	 * cull local items not on the list
	 */
	protected void gimmeANameFool(Boolean first_run)
	{
		ArrayList<RssItem> new_items = new ArrayList<RssItem>();
				
		// If necessary, reload any as-yet-unread-but-downloaded items from disk
		if (first_run)
				reloadItemsFromDisk(newItems);				
		else // Run N
		{
			Log.i(TAG, "Sweeping out stale local posts, starting count " + items.size());
			// Sweep out any posts we have locally that aren't unread on the server.
			for (int idx = 0; idx < items.size(); idx++)
			{
				if (newItems.contains(items.get(idx).id))
				{
					// Copy it over
					new_items.add(items.get(idx));
				}
			}
			items.clear();
			items.addAll(new_items);
			Log.i(TAG, "Sweep done, count now " + items.size());			
		}

		
		// Remove any on server list that we have locally before we run the fetches to avoid excess traffic.
		List<Integer> unseenItems = new ArrayList<Integer>();
		
		for (int idx = 0; idx < newItems.size(); idx++)
		{
			if (findPostById(newItems.get(idx)) == null)
				unseenItems.add(newItems.get(idx));
		}
		
		Log.d(TAG, unseenItems.size() + " left to fetch.");
		
		// Could use size of newItems for progress dialog. Someday. Maybe a 'persistent activity' notification? TODO.
		final int PER_FETCH = 50;
		final int num_fetches = (int) Math.ceil(unseenItems.size() / PER_FETCH) + 1;
		for (int idx = 0; idx < num_fetches; idx++)
		{
			int left_index = idx * PER_FETCH;
			int right_index = Math.min((idx + 1) * PER_FETCH, unseenItems.size());
			Log.d(TAG, "On run " + idx + " pulling from " + left_index + " to " + right_index + " of " + unseenItems.size());
			
			List<Integer> ids = unseenItems.subList(left_index, right_index);
			String payload = xcvr.fetchListOfItems(ids);
			saveItemsData(payload);
		}		
	}
	
	// Save RSS items parsed from payload, return number saved.
	public int saveItemsData(String payload)
	{
		JSONArray jitems;
		RssItem this_item;
		int old_size = items.size();
		
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
				
				items.add(this_item);
			}
			Log.i(TAG, items.size() - old_size +" items added, " + items.size()	+ " total, ");
			return jitems.length();
		} catch (JSONException e) 
		{
			e.printStackTrace();
		}			
		return 0;		
	}
	
	// Save an RssItem to group or groups that contain it
	private int saveItem(RssItem item)
	{
		for ()
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
				items.add(item);
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
		Long tzero = System.currentTimeMillis();
		Log.d(TAG, "Starting sweep of read items...");
		int item_count = 0;

		for (int idx = 0; idx < items.size(); idx++)
		{
			RssItem item = items.get(idx);
			if (item.is_read)
			{
				item_count++;
				item.deleteDiskFile();
				
				// Is this valid? To remove an entry as we iterate over it?
				items.remove(item);
			}
		}		
		Log.d(TAG, item_count + " items removed in " + (System.currentTimeMillis() - tzero) + " msec, " + items.size() + " remaining");
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
			@Override
			public int compare(RssGroup r1, RssGroup r2) {
				return String.CASE_INSENSITIVE_ORDER.compare(r1.title, r2.title);
			}
		});		
	}
	
	// Newest first - reversed order by comparing 2 to 1
	// http://stackoverflow.com/questions/5894818/how-to-sort-arraylistlong-in-java-in-decreasing-order
	protected void sortItemsByDate()
	{
		Collections.sort(items, new Comparator<RssItem>() {
			@Override
			public int compare(RssItem r1, RssItem r2) {
				return r2.created_on_time.compareTo(r1.created_on_time);
			}
		});
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
	
		Log.d(TAG, "Starting to mark thread '" + title + "' in group " + grp.title + " as read");
		for (int idx = 0; idx < items.size(); idx++)
		{
			int feed_id = items.get(idx).feed_id;
			if (grp.feed_ids.contains(feed_id))
			{
				if (title.equals(items.get(idx).title))
				{
					markItemRead(items.get(idx).id, group_id);
					rm_count++;
				}
			}
		}
		
		sweepReadItems();
		
		Log.d(TAG, "Completed marking group as read, " + rm_count + " removed.");		
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
		
		for (int idx = 0; idx < items.size(); idx++)
		{
			int feed_id = items.get(idx).feed_id;
			if (grp.feed_ids.contains(feed_id))
			{
				RssItem item = findPostById(items.get(idx).id);
				if (item != null)
					item.is_read = true;
			}
		}
		
		sweepReadItems();
		
		Log.d(TAG, "Completed marking group as read");
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
 