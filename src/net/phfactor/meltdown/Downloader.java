package net.phfactor.meltdown;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/*!
 * The Downloader is run like a cron job, via the AlarmService, and 
 * does all of the downloading of groups, feeds and items.
 * 
 * TODO: Make run interval a preference
 */
public class Downloader extends IntentService 
{
	private final String TAG = "MeltdownDownloader";
	private MeltdownApp mapp;
	private RestClient xcvr;
	private Long tzero, tend;
	
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
	// Local-only async updates to any listening activities. Very handy.
	private void sendLocalBroadcast(String action)
	{
		Intent intent = new Intent(action);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}
	
	@Override
	protected void onHandleIntent(Intent intent) 
	{
		Boolean first_run;
		Bundle bundle = intent.getExtras();
		if (bundle == null)
			first_run = false;
		else
			first_run = bundle.getBoolean(MeltdownApp.FIRST_RUN, false);
		
		mapp = (MeltdownApp) getApplication();
		ConfigFile config = new ConfigFile(getApplicationContext());
		xcvr = new RestClient(config.getToken(), config.getAPIUrl());

		if (!mapp.isAppConfigured())
		{
			Log.w(TAG, "Setup incomplete, cannot download");
			return;
		}
		
		tzero = System.currentTimeMillis();
		Log.i(TAG, mapp.getAppVersion() + " beginning update...");
		mapp.download_start();
		sendLocalBroadcast(ACTION_UPDATE_STARTING);
		
		Log.i(TAG, "Getting groups...");
		sendLocalBroadcast(ACTION_UPDATING_GROUPS);
		mapp.saveGroupsData(xcvr.fetchGroups());
		
		Log.i(TAG, "Getting feeds....");
		sendLocalBroadcast(ACTION_UPDATING_FEEDS);
		mapp.saveFeedsData(xcvr.fetchFeeds());

//		Log.i(TAG, "Getting icons...");
//		sendLocalBroadcast(ACTION_UPDATING_ICONS);
//		mapp.saveFavicons(xcvr.fetchFavicons());
		
		Log.i(TAG, "Building indices...");
		mapp.updateFeedIndices();
		
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
		mapp.cullItemFiles();		
	
		Log.i(TAG, "Sorting...");
		mapp.sortGroupsByName();
		mapp.sortItemsByDate();
		
		mapp.download_complete();
		
		tend = System.currentTimeMillis();
		Double elapsed = (tend - tzero) / 1000.0;
		Log.i(TAG, "Service complete, " + elapsed + " seconds elapsed, " + mapp.getNumItems() + " items."); 
		sendLocalBroadcast(ACTION_UPDATE_DONE);
	}
}
