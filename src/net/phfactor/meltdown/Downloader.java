package net.phfactor.meltdown;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/*!
 * The Downloader is run like a cron job, via the AlarmService, and 
 * does all of the downloading of groups, feeds and items.
 * 
 */
public class Downloader extends IntentService 
{
	private final String TAG = "MeltdownDownloader";
	private MeltdownApp mapp;
	private RestClient xcvr;
	private Long tzero, tend;
	
	/*
	 * We use local broadcasts for the (asynchronously running) IntentService to tell the GroupsActivity 
	 * and ItemsActivity that we're updating the data, and if they're running to update their respective 
	 * list views and progress dialogs. Simple, fast, efficient; I quite like it.
	 * 
	 * TODO Collapse these down to action + extras in human-readable format
	 * TODO Move these to strings.xml for localization.
	 */
	static final String ACTION_UPDATE_STARTING = "updateStart";
	static final String ACTION_UPDATING_GROUPS = "Updating group";
	static final String ACTION_UPDATING_FEEDS = "Updating feeds";
	static final String ACTION_UPDATING_ITEMS = "Updating items";
	static final String ACTION_UPDATING_ICONS = "Updating favicons";
	static final String ACTION_DISK_READ = "Reloading cache from disk";
	static final String ACTION_UPDATING_CACHE = "Updating disk cache";
	static final String ACTION_UPDATE_DONE = "updateDone";
	
	public Downloader(String name) 
	{
		super(name);
	}

	public Downloader()
	{
		super("Downloader");
	}
	
	// See http://www.intertech.com/Blog/Post/Using-LocalBroadcastManager-in-Service-to-Activity-Communications.aspx
	private void sendLocalBroadcast(String action)
	{
		Intent intent = new Intent(action);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}
	
	@Override
	protected void onHandleIntent(Intent intent) 
	{
		mapp = (MeltdownApp) getApplication();
		ConfigFile config = new ConfigFile(getApplicationContext());
		xcvr = new RestClient(config.getToken(), config.getAPIUrl());

		if (mapp.isNetDown())
			return;
		
		if (!mapp.isAppConfigured())
		{
			Log.w(TAG, "Setup incomplete, cannot download");
			return;
		}
		
		tzero = System.currentTimeMillis();
		Log.i(TAG, mapp.getAppVersion() + " beginning update...");
		sendLocalBroadcast(ACTION_UPDATE_STARTING);
		
		Log.i(TAG, "Getting groups...");
		sendLocalBroadcast(ACTION_UPDATING_GROUPS);
		mapp.saveGroupsData(xcvr.fetchGroups());
		
		Log.i(TAG, "Getting feeds....");
		sendLocalBroadcast(ACTION_UPDATING_FEEDS);
		mapp.saveFeedsData(xcvr.fetchFeeds());

		// Favicons commented out until I build a new listview for items and use them there. Code works though.
//		Log.i(TAG, "Getting icons...");
//		sendLocalBroadcast(ACTION_UPDATING_ICONS);
//		mapp.saveFavicons(xcvr.fetchFavicons());
		
		Log.i(TAG, "Building indices...");
		mapp.updateFeedIndices();
		
		Boolean first_run = (mapp.getRunCount() == 0);
		if (first_run)
		{
			Log.i(TAG, "Loading from disk and fetching items...");
			sendLocalBroadcast(ACTION_DISK_READ);
		}
		else
		{
			Log.i(TAG, "Now fetching items...");
			sendLocalBroadcast(ACTION_UPDATING_ITEMS);
		}
		mapp.syncUnreadPosts(first_run);
		
		Log.i(TAG, "Culling on-disk storage...");
		sendLocalBroadcast(ACTION_UPDATING_CACHE);
		mapp.sweepDiskCache();		
	
		mapp.sortGroupsByName();
		mapp.sortItemsByDate();
		mapp.incrementRunCount();
		
		tend = System.currentTimeMillis();
		Double elapsed = (tend - tzero) / 1000.0;
		Log.i(TAG, "Service complete, " + elapsed + " seconds elapsed, " + mapp.totalUnreadItems() + " items."); 
		sendLocalBroadcast(ACTION_UPDATE_DONE);
	}
}
