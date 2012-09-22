package net.phfactor.meltdown;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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
	private static final String P_EMAIL = "email";
	private static final String P_PASS = "pass";
	private static final String P_LAST_FETCH = "last_ts";
	
	private List<RssGroup> groups;
	private List<RssFeed> feeds;
	private List<RssItem> items;
		
	private int max_read_id;
	private int max_fetched_id;
	private int max_id_on_server;
	@SuppressWarnings("unused")
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
		max_fetched_id = 0;
		max_id_on_server = 0;
		last_refresh_time = 0L;
		updateInProgress = false;		
	
		// Reload indices from disk before we start downloading
		clearAllData();
		
		prefs = getSharedPreferences(TAG, Context.MODE_PRIVATE);
		xcvr = new RestClient(this);		
		startUpdates();
		
		Log.i(TAG, "App init completed.");
	}
	
	protected synchronized void clearAllData() 
	{
		this.feeds = new ArrayList<RssFeed>();
		this.groups = new ArrayList<RssGroup>();
		this.items = new ArrayList<RssItem>();
	}
		
	/*
	 * Start the Downloader, and add it to twice-hourly (approximate) via alarm service. We use
	 * inexact to save battery life; the fetches can be off but that's fine.
	 */
	protected void startUpdates()
	{
		// Tell downloader that this is the first run, so it should reload the items from disk
		Intent sIntent = new Intent(this, Downloader.class);
		sIntent.putExtra("first_run", true);
		startService(sIntent);
		
		Log.i(TAG, "Setting up periodic updates...");
		
    	Context ctx = getApplicationContext();
		Intent svc_intent = new Intent(ctx, Downloader.class);
		svc_intent.putExtra("first_run", false);
		PendingIntent pending_intent = PendingIntent.getService(ctx, 0, svc_intent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
		am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), 
				AlarmManager.INTERVAL_HALF_HOUR, pending_intent);

		Log.d(TAG, "Score update service scheduled for execution");		
	}
	
	public int getNumItems()
	{
		return items.size();
	}
	
	// Simple helper - if items is empty, we are waiting for something. Used by main page as a way
	// to keep or dismiss the progress dialog.
	protected Boolean waiting_for_data()
	{
		return (items.size() == 0);
	}
	
	protected int getMax_read_id() 
	{
		return max_read_id;
	}

	public int getMaxFetchedId()
	{
		return max_fetched_id;
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
	public List<Integer> itemsForGroup(int group_id)
	{
		ArrayList<Integer> rc = new ArrayList<Integer>();
		
		RssGroup group = findGroupById(group_id);
		if (group == null)
			return rc;
		
		for (int item_idx = 0; item_idx < items.size(); item_idx++)
		{
			int items_feed_id = items.get(item_idx).feed_id;
			if (group.feed_ids.contains(items_feed_id))
			{
				if (items.get(item_idx).is_read == false)
					rc.add(items.get(item_idx).id);
			}
		}
		
		Log.d(TAG, rc.size() + " items found for " + group.title);
		return rc;
	}
	
	// Unread items count for a given group ID
	public int unreadItemCount(int group_id)
	{
		return itemsForGroup(group_id).size();
	}
	
	// Return items, with HTML only loaded at the last minute.
	public List<RssItem> getFullItemsForGroup(int group_id)
	{
		ArrayList<RssItem> rc = new ArrayList<RssItem>();
		RssGroup grp = findGroupById(group_id);
		if (grp == null)
		{
			Log.e(TAG, "Unable to locate group id " + group_id);
			return null;
		}

		for (int item_idx = 0; item_idx < items.size(); item_idx++)
		{
			int items_feed_id = items.get(item_idx).feed_id;
			if (grp.feed_ids.contains(items_feed_id))
			{
				rc.add(items.get(item_idx));
			}
		}
		Log.d(TAG, rc.size() + " items for group ID " + group_id);
		return rc;
	}
	
	protected int getMax_id_on_server() {
		return max_id_on_server;
	}

	/* The feeds_groups data is a bit different. Separate json array, and the 
	 * encoding differs. The arrays are CSV instead of JSON, so we have to create 
	 * a specialized parser for them.
	 * TODO Feedback to developer on this API
	 */
	private List<Integer> gsToListInt(String id_str) throws JSONException
	{
		List<Integer> rc = new ArrayList<Integer>();
		
		String[] nums = id_str.split(",");
		
		for (int idx = 0; idx < nums.length; idx++)
			rc.add(Integer.valueOf(nums[idx]));
		return rc;
	}

	public List<RssGroup> getGroups()
	{
		return this.groups;
	}
	
	// Used by top-level landing screen, returns a list of groups that have unread items in them.
	// TODO index over items instead of groups - O(n) instead of n^2
	public List<RssGroup> slowGetGroups()
	{
		List<RssGroup> rc = new ArrayList<RssGroup>();
		
		// Start simple and stupid!
		for (int group_idx = 0; group_idx < groups.size(); group_idx++)
		{
			if (unreadItemCount(groups.get(group_idx).id) > 0)
				rc.add(groups.get(group_idx));
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
			for (int grp_idx = 0; grp_idx < groups.size(); grp_idx++)
			{
				if (groups.get(grp_idx).id == grp_id)
					groups.get(grp_idx).feed_ids = gsToListInt(cur_grp.getString("feed_ids"));
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
			
			this.max_id_on_server = jdata.getInt("total_items");
			this.last_refresh_time = jdata.getLong("last_refreshed_on_time");
			for (int idx = 0; idx < jitems.length(); idx++)
			{
				this_item = new RssItem(jitems.getJSONObject(idx));
				setMaxLocal(this_item);

				// Skip over items that've been read already
				if (this_item.is_read)
					continue;

				
				// Save it to disk
				this_item.saveToFile(ctx);
				
				// Remove the HTML for later loading if needed
				this_item.dropHTML();
				
				items.add(this_item);
			}
			Log.i(TAG, items.size() - old_size +" items added, " + jitems.length() + " in payload, "
			+ items.size() + " total, " + getMax_read_id() + " max ID");
			return jitems.length();
		} catch (JSONException e) 
		{
			e.printStackTrace();
		}	
		
		return 0;		
	}
	
	// At startup, load all of the items from disk, and reset max_ids before starting download from server.
	// Called by the Downloader, as this takes minutes for 10k items on a nexus7.
	protected void reloadItemsFromDisk()
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
			if (post_id > 0)
			{
				RssItem item = new RssItem(ctx, post_id);
				items.add(item);
				setMaxLocal(item);
				fcount++;
			}
		}
		Log.d(TAG, files.length + " files processed, " + fcount + " loaded in " + 
		((System.currentTimeMillis() - tzero) / 1000L) + " seconds");
		Log.d(TAG, "Max read ID is now " + max_read_id);
	}
	
	
	private void setMaxLocal(RssItem item)
	{
		max_read_id = Math.max(max_read_id, item.id);
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
				items.remove(idx);
			}
		}		
		Log.d(TAG, item_count + " items removed in " + (System.currentTimeMillis() - tzero) + " msec, " + items.size() + " remaining");
	}
	
	
	protected Boolean verifyLogin()
	{
		// FIXME Async login check
		return haveSetup();
	}
	
	// Mark an item/post as read, both locally and on the server.
	public synchronized void markItemRead(int item_id)
	{
		// Get the server update running in the background
		xcvr.markItemRead(item_id);

		RssItem item = findPostById(item_id);
		if (item != null)
		{
			item.is_read = true;
		}
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
					markItemRead(items.get(idx).id);
					rm_count++;
				}
			}
		}
		
		sweepReadItems();
		
		Log.d(TAG, "Completed marking group as read, " + rm_count + " removed.");		
		return rm_count;
	}
	
	// Iterate over a group, and mark all of the items in it as read.
	protected synchronized void markGroupRead(int group_id)
	{
		RssGroup grp = findGroupById(group_id);
		if (grp == null)
		{
			Log.e(TAG, "Unable to locate group " + group_id);
			return;
		}
	
		Log.d(TAG, "Starting to mark group " + grp.title + " as read");
		for (int idx = 0; idx < items.size(); idx++)
		{
			int feed_id = items.get(idx).feed_id;
			if (grp.feed_ids.contains(feed_id))
				markItemRead(items.get(idx).id);
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
		editor.putString(P_EMAIL, email);
		editor.putString(P_PASS, password);
		editor.commit();
	}
	
	protected String getURL()
	{
		return prefs.getString(P_URL, null);
	}
	
	protected String getEmail()
	{
		return prefs.getString(P_EMAIL, null);
	}
	
	private String getPass()
	{
		return prefs.getString(P_PASS, null);
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
		if (prefs.getString(P_EMAIL, null) == null)
			return false;
		if (prefs.getString(P_PASS, null) == null)
			return false;
		
		return true;
	}
	
	protected String makeAuthToken()
	{
		String pre = String.format("%s:%s", getEmail(), getPass());
		return md5(pre);
	}
	
}
 