package net.phfactor.meltdown;

import android.app.IntentService;
import android.content.Intent;
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
	
	public Downloader(String name) 
	{
		super(name);
	}

	public Downloader()
	{
		super("Downloader");
	}
	
	@Override
	protected void onHandleIntent(Intent intent) 
	{
		mapp = (MeltdownApp) getApplication();
		xcvr = new RestClient(mapp);

		tzero = System.currentTimeMillis();
		Log.i(TAG, "Beginning download");
		
		if (!mapp.haveSetup())
		{
			Log.w(TAG, "Setup incomplete, cannot download");
			return;
		}
		
		mapp.download_start();
		
		Log.i(TAG, "Getting groups...");
		mapp.saveGroupsData(xcvr.fetchGroups());
		Log.i(TAG, "Getting feeds....");
		mapp.saveFeedsData(xcvr.fetchFeeds());
		
		if (intent.getExtras().getBoolean("first_run"))		
			mapp.reloadItemsFromDisk();
		
		Log.i(TAG, "Now fetching items...");
		while (mapp.saveItemsData(xcvr.fetchSomeItems(mapp.getMax_read_id())) >= 0) 
			{};
		
		Log.i(TAG, "Culling on-disk storage...");
		mapp.cullItemFiles();
		
		Log.i(TAG, "Cullng in-memory storage...");
		mapp.sweepReadItems();
		
		mapp.download_complete();
		tend = System.currentTimeMillis();
		Double elapsed = (tend - tzero) / 1000.0;
		Log.i(TAG, "Service complete, " + elapsed + " seconds elapsed, "
		+ mapp.getNumItems() + " items."); 
	}

}
