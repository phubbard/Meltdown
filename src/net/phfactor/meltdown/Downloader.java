package net.phfactor.meltdown;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

/*!
 * The Downloader is run like a cron job, via the AlarmService, and 
 * does all of the downloading of groups, feeds and items.
 * 
 * TODO: Mark-and-sweep GC of the items files in my next class to write.
 * TODO: Make run interval a preference
 */
public class Downloader extends IntentService 
{
	private final String TAG = "MeltdownDownloader";
	private MeltdownApp mapp;
	private RestClient xcvr;
	
	public Downloader(String name) 
	{
		super(name);
		doSetup();
	}

	public Downloader()
	{
		super("Downloader");
		doSetup();
	}
	
	private void doSetup()
	{
		mapp = (MeltdownApp) getApplication();
		xcvr = new RestClient(mapp);
		Log.d(TAG, "created OK");		
	}
	
	@Override
	protected void onHandleIntent(Intent intent) 
	{
		Long tzero = System.currentTimeMillis();
		Log.i(TAG, "Beginning download");
		
		if (mapp == null)
			doSetup();
		
		if (!mapp.haveSetup())
		{
			Log.w(TAG, "Setup incomplete, cannot download");
			return;
		}
		
		mapp.updateInProgress = true;
		mapp.clearAllData();
		
		Log.i(TAG, "Getting groups...");
		mapp.saveGroupsData(xcvr.fetchGroups());
		Log.i(TAG, "Getting feeds....");
		mapp.saveFeedsData(xcvr.fetchFeeds());
		Log.i(TAG, "Now fetching items...");
		while (mapp.saveItemsData(xcvr.fetchSomeItems(mapp.getMax_read_id())) > 0)
			Log.d(TAG, "Pulling another chunk, max now " + mapp.getMax_read_id());
		
		mapp.updateInProgress = false;
		Long tend = System.currentTimeMillis();
		Double elapsed = (tend - tzero) / 1000.0;
		Log.i(TAG, "Download complete, " + elapsed + " seconds elapsed."); 
	}

}
