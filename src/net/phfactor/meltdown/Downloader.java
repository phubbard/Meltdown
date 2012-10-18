package net.phfactor.meltdown;

import android.app.IntentService;
import android.content.Intent;
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
	static final String ACTION_UPDATED_GROUPS = "Updated group";
	static final String ACTION_UPDATED_FEEDS = "Updated feeds";
	static final String ACTION_UPDATED_ITEMS = "Updated items";
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
		mapp = (MeltdownApp) getApplication();
		xcvr = new RestClient(mapp);

		if (!mapp.haveSetup())
		{
			Log.w(TAG, "Setup incomplete, cannot download");
			return;
		}
		
		tzero = System.currentTimeMillis();
		Log.i(TAG, "Beginning update...");
		mapp.download_start();
		sendLocalBroadcast(ACTION_UPDATE_STARTING);
		
		Log.i(TAG, "Getting groups...");
		mapp.saveGroupsData(xcvr.fetchGroups());
		sendLocalBroadcast(ACTION_UPDATED_GROUPS);
		
		Log.i(TAG, "Getting feeds....");
		mapp.saveFeedsData(xcvr.fetchFeeds());
		sendLocalBroadcast(ACTION_UPDATED_FEEDS);
		
		Log.i(TAG, "Now fetching items...");
		mapp.gimmeANameFool(intent.getExtras().getBoolean(MeltdownApp.FIRST_RUN));
		Log.i(TAG, "Culling on-disk storage...");
		mapp.cullItemFiles();
		
		Log.i(TAG, "Creating index");
		mapp.updateGroupIndices();
		sendLocalBroadcast(ACTION_UPDATED_ITEMS);
	
		Log.i(TAG, "Sorting...");
		mapp.sortGroupsByName();
		mapp.sortItemsByDate();
		
		mapp.download_complete();
		tend = System.currentTimeMillis();
		Double elapsed = (tend - tzero) / 1000.0;
		Log.i(TAG, "Service complete, " + elapsed + " seconds elapsed, "
		+ mapp.getNumItems() + " items."); 
		sendLocalBroadcast(ACTION_UPDATE_DONE);
	}
}
