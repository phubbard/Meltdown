package net.phfactor.meltdown;

import java.io.File;
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
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;

public class MeltdownApp extends Application implements OnSharedPreferenceChangeListener
{
	static final String TAG = "MeltdownApp";

	static final String DATABASE = "items.db";
	static final String TABLE = "items";
	static final String C_ID = "_id";
	static final String C_HTML = "html";

	static final String FIRST_RUN = "first_run";

	static final int ORPHAN_ID = 8675309;
	static final int SPARKS_ID = 8675310;

	private List<RssGroup> groups;
	private List<RssFeed> feeds;
	private List<Favicon> icons;

	private long last_refresh_time;
	private int run_count;
	
	private PendingIntent pendingIntent;

	private RestClient xcvr;
	private ConfigFile configFile;

	public MeltdownApp()
	{
		super();
	}
	
	protected Boolean isNetDown()
	{
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isConnected())
			return false;
		
		return true;		    	
	}

	/* Sweep disk files and remove any not present in the in-memory items array
	 * 
	 */
	protected void sweepDiskCache()
	{
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
				}
			}
		}
	}

	public String getAppVersion()
	{
		PackageInfo pinfo;
		try {
			pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
		} catch (NameNotFoundException e)
		{
			e.printStackTrace();
			return "unknown";
		}
		return "Version " + pinfo.versionName + " build " + pinfo.versionCode;
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
		} catch (NullPointerException ne)
		{
			Log.e(TAG, "Error parsing unread IDs - offline?");
		}
		return rc;
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

	protected RssFeed findFeedById(int feed_id)
	{
		for (int idx =0; idx < feeds.size(); idx++)
		{
			if (feeds.get(idx).id == feed_id)
				return feeds.get(idx);
		}
		return null;
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

	RssGroup findGroupForItem(RssItem item)
	{
		if (item == null)
			return null;

		RssFeed feed = findFeedById(item.feed_id);
		if (feed == null)
			return null;

		if (feed.groups.size() == 0)
		{
			Log.e(TAG, "Feed " + feed.title + " has no group!");
			return null;
		}
		else if (feed.groups.size() > 1)
			Log.d(TAG, "Feed " + feed.title + " has " + feed.groups.size() + " group(s)");

		return findGroupById(feed.groups.get(0));
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

	// How many cache files are present? Just used by about page.
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

	// How many feeds?
	protected int getFeedCount()
	{
		return feeds.size();
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

	protected long get_last_refresh_time()
	{
		return last_refresh_time;
	}

	public int totalUnreadItems()
	{
		int rc = 0;
		for (int idx = 0; idx < groups.size(); idx++)
			rc += groupUnreadItems(groups.get(idx));
		return rc;
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

	public int groupUnreadItems(RssGroup group)
	{
		int count = 0;
		for (int idx = 0; idx < group.items.size(); idx++)
			if (group.items.get(idx).is_read == false)
				count++;
		return count;
	}

	public int groupUnreadItems(int group_id)
	{
		RssGroup group = findGroupById(group_id);
		if (group == null)
			return 0;

		return groupUnreadItems(group);
	}

	// Is the app setup and verified?
	public Boolean isAppConfigured()
	{
		return configFile.haveConfigInfo();
	}
	
	public Boolean checkLogin()
	{
		// Need to recreate the RestClient, as it caches the API URL.
		xcvr = new RestClient(configFile.getToken(), configFile.getAPIUrl());

		return xcvr.verifyLogin();
		
	}

	// Call the users' post hook
	protected void callUserURL(String url)
	{
		xcvr.callUserURL(url);
	}

	// Iterate over a group, and mark all of the items in it as read. One API call and then local cleanup.
	protected void markGroupRead(int group_id)
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
		sweepReadFromGroup(grp);
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

	// Mark an item/post as read, both locally and on the server.
	public synchronized void markItemRead(int item_id)
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

	protected int getRunCount()
	{
		return run_count;
	}
	
	protected void incrementRunCount()
	{
		run_count++;
	}
	
	// See http://developer.android.com/training/efficient-downloads/redundant_redundant.html
	private void enableHttpResponseCache() 
	{
		try 
		{
			long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
			File httpCacheDir = new File(getCacheDir(), "http");
			Class.forName("android.net.http.HttpResponseCache")
			.getMethod("install", File.class, long.class)
			.invoke(null, httpCacheDir, httpCacheSize);
		} catch (Exception httpResponseCacheNotAvailable) 
		{
			Log.w(TAG, "HTTP response cache is unavailable.");
		}
	}
	
	@Override
	public void onCreate()
	{
		super.onCreate();

		Log.i(TAG, "App created, initializing");
		last_refresh_time = 0L;
		run_count = 0;
		
		this.feeds = new ArrayList<RssFeed>();
		this.groups = new ArrayList<RssGroup>();
		this.icons = new ArrayList<Favicon>();
		
		enableHttpResponseCache();

		configFile = new ConfigFile(getApplicationContext());
		xcvr = new RestClient(configFile.getToken(), configFile.getAPIUrl());
		
		// Setup callback hook for changes in update interval and such.
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		prefs.registerOnSharedPreferenceChangeListener(this);
		
		RssGroup orphans = new RssGroup("Orphaned feeds", ORPHAN_ID);
		this.groups.add(orphans);
		RssGroup sparks = new RssGroup("Sparks", SPARKS_ID);
		this.groups.add(sparks);

		startUpdates();
		Log.i(TAG, "App init completed.");
	}

	/*
	 * feeds groups and unread item ids are returned as CSV strings; common code to parse same.
	 */
	private List<Integer> parseStrArray(String array_str)
	{
		String[] nums = array_str.split(",");
		List<Integer> rc = new ArrayList<Integer>();

		for (int idx = 0; idx < nums.length; idx++)
		{
			try {
				rc.add(Integer.valueOf(nums[idx]));
			}
			catch (NumberFormatException nfe)
			{
				Log.e(TAG, "Parse error, ignoring");
			}
		}
		return rc;
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
	}

	// Parse the results of the get-the-feeds API call into our simple array.
	// Note - overwrites the old without checking!
	protected void saveFeedsData(String payload)
	{
		JSONArray jfeeds;

		if (payload == null)
			return;

		// Erase the old feeds array and rebuild it with the servers' list
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

	// Feeds groups associate groups with the feeds they contain, so you can populate a group with feeds
	// and thence actual items.
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

	// Take the data returned from a groups fetch, parse and save into data model.
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
					// Log.d(TAG, "Merging updating group info for " + newGroup.title);
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

	/*
	 * Save a story (aka item) into our data structures and cache file. Needs to lookup which
	 * group should hold the item, but we do that only once, and might look it up via group many
	 * times, so sorting it an ingress seems reasonable.
	 */
	private void saveRssItem(RssItem item)
	{
		RssGroup group = findGroupForItem(item);
		if (group == null)
		{
			RssFeed feed = findFeedById(item.feed_id);
			if (feed == null)
			{
				Log.d(TAG, "Orphan item! No group found for post ID " + item.id + " " + item.title);
				group = findGroupById(ORPHAN_ID);
			}
			else if (feed.is_spark)
			{
				if (!configFile.getDisableSparks())
				{
					Log.d(TAG, "Spark found for post ID " + item.id + " " + item.title);
					group = findGroupById(SPARKS_ID);
				}
				else
					Log.d(TAG, "Sparks disabled, not saving item " + item.id);
			}
		}

		if(group != null)
			group.items.add(item);
	}

	/*
	 * I tried using the favicons as ActionBar headers, but there's scaling needed and I wasn't impressed. The
	 * code is idle for now, but I think next I'll try a tweaked listview of items, with the favicon center left
	 * on each story. Might look good and provide a visual cue as to source/author.
	 */
	public void saveFavicons(String payload)
	{
		JSONArray jitems;
		Favicon this_item;

		if (payload == null)
			return;

		try
		{
			JSONObject jdata = new JSONObject(payload);

			jitems = jdata.getJSONArray("favicons");
			if (jitems.length() == 0)
			{
				Log.i(TAG, "No icons found!");
				return;
			}

			for (int idx = 0; idx < jitems.length(); idx++)
			{
				// Constructor does all the hard work for us.
				this_item = new Favicon(jitems.getString(idx));
				icons.add(this_item);
			}
		} catch (JSONException e)
		{
			e.printStackTrace();
		}

		// Now link feeds with icons
		for (int idx = 0; idx < icons.size(); idx++)
		{
			RssFeed feed = findFeedById(icons.get(idx).id);
			if (feed != null)
				feed.icon = icons.get(idx);
		}
	}

	// We display the groups in alphabetical order. Seems a sensible default.
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

	/*
	 * Start the Downloader, and add it to  (approximate) via alarm service. We use
	 * inexact to save battery life; the fetches can be off but that's fine.
	 */
	protected void startUpdates()
	{
		Intent sIntent = new Intent(this, Downloader.class);
		startService(sIntent);
		
		Log.i(TAG, "Setting up periodic updates...");
		Context ctx = getApplicationContext();
		Intent svc_intent = new Intent(ctx, Downloader.class);
		pendingIntent = PendingIntent.getService(ctx, 0, svc_intent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
		am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(),
				configFile.getUpdateInterval(), pendingIntent);
		
		Log.d(TAG, "Download service scheduled for periodic execution every "
				+ DateUtils.formatElapsedTime(configFile.getUpdateInterval() / 1000L));
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		Log.i(TAG, "Preferences changed, restarting Downloader with new interval");
    	Context ctx = getApplicationContext();

    	if (pendingIntent != null)
    		pendingIntent.cancel();
    	
    	ctx.stopService(new Intent(ctx, Downloader.class));
    	startUpdates();		
	}

	protected void sweepReadFromGroup(RssGroup group)
	{
		if (group == null)
			return;

		List<RssItem> trimmedList = new ArrayList<RssItem>();
		for (int idx = 0; idx < group.items.size(); idx++)
		{
			RssItem item = group.items.get(idx);
			if (item.is_read)
				item.deleteDiskFile();
			else
				trimmedList.add(item);
		}

		group.items = trimmedList;
	}

	/*
	 * Iterate over the items, remove any that are marked read. GC, called by Downloader
	 * and when exiting a group view. Mark and sweep GC, very basic.
	 */
	protected void sweepReadItems()
	{
		for (int gidx = 0; gidx < groups.size(); gidx++)
			sweepReadFromGroup(groups.get(gidx));
	}

	/* The unread_items_ids returns an N-length array of items that are unread on the server. We have to:
	 * parse the list
	 * match what we have locally
	 * fetch the new items in blocks of 50 or less.
	 * cull local items not on the list
	 * 
	 * This routine is the heart of Meltdown's synchronization with the server.
	 */
	protected void syncUnreadPosts(Boolean reload_from_disk)
	{
		List<Integer> serverItemIDs = fetchUnreadItemsIDs();

		Log.d(TAG, serverItemIDs.size() + " unread items found on server.");
		if (serverItemIDs.size() == 0)
			return;

		if (reload_from_disk)
			reloadItemsFromDisk(serverItemIDs);
		else
		{
			Log.i(TAG, "Sweeping out stale posts...");
			for (int idx = 0; idx < groups.size(); idx++)
			{
				RssGroup grp = groups.get(idx);

				// This pass removes local items that are gone from the server - read elsewhere, most likely.
				for (int i = 0; i < grp.items.size(); i++)
				{
					int post_id = grp.items.get(i).id;
					if (!serverItemIDs.contains(post_id))
						findPostById(post_id).is_read = true;
				}

				// Now they're marked as read, remove from memory and disk
				sweepReadFromGroup(grp);
			}
		}

		// Now we're looking for posts on the server we *don't* have yet.
		List<Integer> itemsToGet = new ArrayList<Integer>();

		for (int idx = 0; idx < serverItemIDs.size(); idx++)
		{
			if (findPostById(serverItemIDs.get(idx)) == null)
				itemsToGet.add(serverItemIDs.get(idx));
		}

		Log.i(TAG, itemsToGet.size() + " items to retrieve");
		if (itemsToGet.size() == 0)
			return;

		// Could use size of newItems for progress dialog. Someday. Maybe a 'persistent activity' notification? TODO.
		final int PER_FETCH = 50;
		final int num_fetches = (int) Math.ceil(itemsToGet.size() / PER_FETCH) + 1;
		for (int idx = 0; idx < num_fetches; idx++)
		{
			int left_index = idx * PER_FETCH;
			int right_index = Math.min((idx + 1) * PER_FETCH, itemsToGet.size());
			Log.d(TAG, "On run " + idx + " pulling from " + left_index + " to " + (right_index - 1) + " of " + itemsToGet.size());

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
			// FIXME is this my crashing bug?
//			if(feed.is_spark)
//			{
//				Log.e(TAG, "Feed " + feed.title + " is a spark.");
//				// don't add sparks to any groups
//				continue;
//			}
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

}
