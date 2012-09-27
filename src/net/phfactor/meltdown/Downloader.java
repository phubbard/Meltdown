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

		if (!mapp.haveSetup())
		{
			Log.w(TAG, "Setup incomplete, cannot download");
			return;
		}
		
		tzero = System.currentTimeMillis();
		Log.i(TAG, "Beginning update...");
		mapp.download_start();
		
		Log.i(TAG, "Getting groups...");
		mapp.saveGroupsData(xcvr.fetchGroups());
		
		Log.i(TAG, "Getting feeds....");
		mapp.saveFeedsData(xcvr.fetchFeeds());
		
		Log.i(TAG, "Now fetching items...");
//		while (mapp.saveItemsData(xcvr.fetchSomeItems(mapp.getMaxItemID())) >= 0) 
//			{};
		mapp.gimmeANameFool(intent.getExtras().getBoolean(MeltdownApp.FIRST_RUN));

		Log.i(TAG, "Culling on-disk storage...");
		mapp.cullItemFiles();
		
//		Log.i(TAG, "Culling in-memory storage...");
//		mapp.sweepReadItems();
		
		Log.i(TAG, "Creating index");
		mapp.updateGroupIndices();
	
		mapp.download_complete();
		tend = System.currentTimeMillis();
		Double elapsed = (tend - tzero) / 1000.0;
		Log.i(TAG, "Service complete, " + elapsed + " seconds elapsed, "
		+ mapp.getNumItems() + " items."); 
	}

}
