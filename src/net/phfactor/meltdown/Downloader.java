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
	private Long tzero, tend;
	
	public Downloader(String name) 
	{
		super(name);
	}

	public Downloader()
	{
		super("Downloader");
	}
	
	private void doSetup()
	{
		mapp = (MeltdownApp) getApplication();
		xcvr = new RestClient(mapp);
		Log.d(TAG, "created OK");		
	}
	
	private void logProgress()
	{
		int num_pulled = mapp.getMax_read_id();
		int num_extant = mapp.getMax_id_on_server(); 
		Double pct_complete = ((num_extant - num_pulled) / num_extant) * 100.0;
		Double rate = (num_pulled / ((System.currentTimeMillis() - tzero) * 1000.0));
		Log.d(TAG, "At " + mapp.getMax_read_id() + " of " + mapp.getMax_id_on_server());
		Log.d(TAG, String.format("%3.2f percent complete at %3.2f per second", pct_complete, rate));
	}
	
	
	@Override
	protected void onHandleIntent(Intent intent) 
	{
		tzero = System.currentTimeMillis();
		Log.i(TAG, "Beginning download");
		
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
			logProgress();
		
		mapp.updateInProgress = false;
		tend = System.currentTimeMillis();
		Double elapsed = (tend - tzero) / 1000.0;
		Log.i(TAG, "Feed download complete, " + elapsed + " seconds elapsed to get " + mapp.getNumItems() + " items."); 
	}

}
