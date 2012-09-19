package net.phfactor.meltdown;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.PatternMatcher;
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
	
	private List<Integer> new_items;
	
	private int max_read_id;
	private int max_fetched_id;
	private int max_id_on_server;
	private long last_refresh_time;
	
	private DbHelper dbHelper;	
	private RestClient xcvr;

	public Boolean updateInProgress;

	private SharedPreferences prefs;
	private SharedPreferences.Editor editor;

	public MeltdownApp()
	{
		super();
	}
		
	@Override
	public void onCreate()
	{
		super.onCreate();

		Log.i(TAG, "App created, initializing");
		clearAllData();

		max_read_id = 0;
		max_fetched_id = 0;
		max_id_on_server = 0;
		last_refresh_time = 0L;
		updateInProgress = false;
		
		prefs = getSharedPreferences(TAG, Context.MODE_PRIVATE);
		xcvr = new RestClient(this);		
		this.dbHelper = new DbHelper(getApplicationContext(), 2);
		startUpdates();
		
		Log.i(TAG, "App init completed.");
	}
	
	private void startUpdates()
	{
		startService(new Intent(this, Downloader.class));
		
		Log.i(TAG, "Setting up periodic updates...");
		
    	Context ctx = getApplicationContext();
		Intent svc_intent = new Intent(ctx, Downloader.class);
		PendingIntent pending_intent = PendingIntent.getService(ctx, 0, svc_intent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
		am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), 
				AlarmManager.INTERVAL_HALF_HOUR, pending_intent);

		Log.d(TAG, "Score update service scheduled for execution");		
	}
	
	// Helper method - used by landing page to display progress bar
	public int getProgress()
	{
		if (!updateInProgress)
			return 100;
		
		// FIXME Make sure these variables are being written by RestClient and/or methods here!!
		int numerator = (max_read_id - max_fetched_id);
		int denominator = (max_id_on_server - max_fetched_id);
		
		if (denominator > 0)
			return (int) (numerator / denominator);
		else
			return 0;
	}
	
	public int getNumItems()
	{
		return items.size();
	}
	
	@Override
	public void onLowMemory() 
	{
		super.onLowMemory();
		Log.w(TAG, "Low memory triggered!");
	}

	@Override
	public void onTrimMemory(int level) 
	{
		super.onTrimMemory(level);
		Log.w(TAG, "Trim memory called!");
	}

	// Simple helper - if items is empty, we are waiting for something
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
	
	
	// TODO Replace by sqlite query once DB-backed
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
	
	// TODO Replace by sqlite query once DB-backed
	protected RssGroup findGroupByName(String name)
	{
		for (int idx = 0; idx < groups.size(); idx++)
		{
			if (groups.get(idx).title.equals(name))
				return groups.get(idx);
		}
		return null;
	}
	
	// TODO Replace by sqlite query
	protected RssItem findPostById(int post_id)
	{
		for (int idx = 0; idx < items.size(); idx++)
		{
			if (items.get(idx).id == post_id)
				return items.get(idx);
		}
		return null;
	}
	
	// Unread items for a given group - crap n2 algorithm
	public List<Integer> olditemsForGroup(int group_id)
	{
		ArrayList<Integer> rc = new ArrayList<Integer>();
		
		RssGroup group = findGroupById(group_id);
		if (group == null)
			return rc;
		
		Log.d(TAG, group.feed_ids.size() + " feeds to check in group " + group.title);
		for (int idx = 0; idx < group.feed_ids.size(); idx++)
		{
			Log.d(TAG, items.size() + " items to check");
			for (int item_idx = 0; item_idx < items.size(); item_idx++)
			{
				if (items.get(item_idx).feed_id == group.feed_ids.get(idx))
					rc.add(items.get(item_idx).id);
			}
		}
		
		Log.d(TAG, rc.size() + " items found for " + group.title);
		return rc;
	}
	
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
	
	public List<RssItem> getAllItemsForGroup(int group_id)
	{
		ArrayList<RssItem> rc = new ArrayList<RssItem>();
		RssGroup grp = findGroupById(group_id);
		if (grp == null)
		{
			Log.e(TAG, "Unable to locate group id " + group_id);
			return null;
		}
		
		for (int cur_feed = 0; cur_feed < grp.feed_ids.size(); cur_feed++)
		{
			for (int cur_item = 0; cur_item < items.size(); cur_item++)
			{
				RssItem current_item = items.get(cur_item);
				if (current_item.feed_id == grp.feed_ids.get(cur_feed))				
				{
					// Grab the HTML from disk
					current_item.html = loadFromFile(current_item.id);
					rc.add(current_item);
				}
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

	public List<RssGroup> getGroups()
	{
		Log.d(TAG, "returning groups " + this.groups.size());
		return this.groups;
	}
	
	
	/* ***********************************
	 * REST callback methods
	 */
	
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
				this.max_read_id = Math.max(this.max_read_id, this_item.id);

				// Skip over items that've been read already
				if (this_item.is_read)
					continue;
				
				// Save ID to new-items list for post-DL cleanup
				new_items.add(this_item.id);
				
				// Save off the bulky HTML to SQLite
				saveToFile(this_item.id, this_item.html);
				
				// This should trigger GC of the now-orphaned HTML
				this_item.html = "";
				
				items.add(this_item);
			}
			Log.i(TAG, items.size() - old_size +" items added, " + items.size() + " total");
			return jitems.length();
		} catch (JSONException e) 
		{
			e.printStackTrace();
		}	
		
		return 0;		
	}

	private synchronized int removePost(int post_id)
	{
		Log.d(TAG, items.size() + " before delete");
		for (int idx = 0; idx < items.size(); idx++)
		{
			if (items.get(idx).id == post_id)
			{
				Log.d(TAG, "Removing post id " + idx);
				items.remove(idx);
				Log.d(TAG, items.size() + " after delete");
				return 0;
			}
		}
		return 1;		
	}
	
	public synchronized void markItemRead(int item_id)
	{
		removePost(item_id);
		xcvr.markItemRead(item_id);
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
			return (Integer.parseInt(tokens[0]));
		
		Log.e(TAG, "Unable to parse " + filename);
		return -1;
	}
	
	private void cullItemFiles()
	{
		Log.d(TAG, "Starting cull of on-disk files");
		Long ftzero = System.currentTimeMillis();
		
		int new_count = 0, old_count = 0;
		
		PatternMatcher pm = new PatternMatcher("*.post", PatternMatcher.PATTERN_SIMPLE_GLOB);
		String[] filenames = fileList();
		for (int idx = 0; idx < filenames.length; idx++)
		{
			if (pm.match(filenames[idx]))
			{
				Log.d(TAG, "checking " + filenames[idx]);
				if (new_items.contains(filenameToInt(filenames[idx])))
				{
					Log.d(TAG, " - that one is new, leaving");
					new_count++;
				}
				else
				{
					old_count++;
					if (deleteFile(filenames[idx]))
						Log.d(TAG, " removed successfully");
					else
						Log.e(TAG, " error removing " + filenames[idx]);
				}
			}
		}
		
		Long ftend = System.currentTimeMillis();
		Log.d(TAG, "Cull completed in " + ftend / 1000L + " seconds");
		Log.d(TAG, "New: " + new_count + " old: " + old_count + " ref: " + new_items.size());
		if (new_items.size() != new_count)
			Log.e(TAG, "CULL COUNT MISMATCH!!!");
	}
	
	// Called by Downloader - this cues the creation of old-vs-new lists for post-download GC
	protected void download_start()
	{
		this.updateInProgress = true;
		this.new_items = new ArrayList<Integer>();
	}
	
	protected void download_complete()
	{
		cullItemFiles();
		
		this.updateInProgress = false;
	}
	
	protected synchronized void clearAllData() 
	{
		// FIXME sync items files and delete outdated and read items		
		this.feeds = new ArrayList<RssFeed>();
		this.groups = new ArrayList<RssGroup>();
		this.items = new ArrayList<RssItem>();
	}
	
	// PLan B. http://developer.android.com/guide/topics/data/data-storage.html
	private void saveToFile(int id, String html)
	{
		String filename = String.format("%d.post", id);
		
		try 
		{
			FileOutputStream fos = openFileOutput(filename, Context.MODE_PRIVATE);
			fos.write(html.getBytes());
			fos.close();
		}
		catch (FileNotFoundException fe)
		{
			Log.e(TAG, "File error", fe);
		} catch (IOException e) 
		{
			Log.e(TAG, "File error on item " + id, e);
		}
	}
	
	private String loadFromFile(int id)
	{
		String filename = String.format("%d.post", id);
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		
		try 
		{
			FileInputStream fos = openFileInput(filename);
			int r_char = fos.read();
			
			while (r_char > -1)
			{
				buffer.write(r_char);
				r_char = fos.read();
			}
			fos.close();
		}			
		catch (FileNotFoundException fe)
		{
			Log.e(TAG, "File error", fe);
		} catch (IOException e) 
		{
			Log.e(TAG, "File error on item " + id, e);
		}
		
		return buffer.toString();
	}
	
	private void saveItem(int id, String html)
	{
		SQLiteDatabase db = this.dbHelper.getWritableDatabase();

		try
		{
			ContentValues data = new ContentValues();
			data.put(C_ID, id);
			data.put(C_HTML, html);
			db.insertWithOnConflict(TABLE, null, data, SQLiteDatabase.CONFLICT_REPLACE);
			Log.d(TAG, "Record " + id + " saved OK");
		}
		finally
		{
			db.close();
		}
	}
	
	private String getItem(int id)
	{
		SQLiteDatabase db = this.dbHelper.getReadableDatabase();
		Cursor cursor;
		String with_stmt = String.format("%s='%d'", C_ID, id);
		String rc = "";
		try
		{
			cursor = db.query(TABLE, null, with_stmt, null, null, null, null);
			if (cursor.moveToNext())
			{
				rc = cursor.getString(cursor.getColumnIndex(C_HTML));
			}
			else
				Log.e(TAG, "Missing DB record for ID " + id);
			
			
			cursor.close();
			db.close();
			return rc;			
		}
		catch (SQLException e) 
		{
			Log.e(TAG, "Got an sqlite error on app database!", e);
			db.close();
		}
		
		return rc;
	}
	
	// Now idle - DB for storing items. Too slow on writes; might be fixable
	private class DbHelper extends SQLiteOpenHelper
	{		
		public DbHelper(Context context, int version_number)
		{
			super(context, DATABASE, null, version_number);			
		}
		
		@Override
		public void onCreate(SQLiteDatabase db)
		{
			try 
			{
				db.execSQL("create table " + TABLE + " ( "+ C_ID +" int primary key, "
						+ C_HTML + " text)");
			} catch (SQLException e) 
			{
				Log.e(TAG, "Unable to create database, fatal error", e);
				e.printStackTrace();
			}
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
			try {
				db.execSQL("drop table " + TABLE);
				Log.w(TAG, "Item database erased");
				this.onCreate(db);
			} catch (SQLException e)
			{
				Log.e(TAG, "Unable to upgrade database, fatal error", e);
				e.printStackTrace();
			}
		}				
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
 