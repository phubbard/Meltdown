package net.phfactor.meltdown;

import java.util.List;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.TwoLineListItem;

public class GroupsActivity extends ListActivity 
{
	static final String TAG = "MeltdownGA";
    /**
     * Custom list adapter that fits our rss data into the list.
     */
    private GroupListAdapter mAdapter;
	private dBroadcastCatcher catcher;
	private MeltdownApp app;
	ProgressDialog pd;
	
	private int last_pos;
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list);
		getActionBar().setSubtitle("Starting up...");
		last_pos = 0;
		
		catcher = new dBroadcastCatcher();
		IntentFilter ifilter = new IntentFilter();
		ifilter.addAction(Downloader.ACTION_UPDATING_GROUPS);
		ifilter.addAction(Downloader.ACTION_UPDATING_FEEDS);
		ifilter.addAction(Downloader.ACTION_UPDATING_ITEMS);
		ifilter.addAction(Downloader.ACTION_UPDATING_ICONS);
		ifilter.addAction(Downloader.ACTION_DISK_READ);
		ifilter.addAction(Downloader.ACTION_UPDATING_CACHE);
		ifilter.addAction(Downloader.ACTION_UPDATE_STARTING);
		ifilter.addAction(Downloader.ACTION_UPDATE_DONE);
		LocalBroadcastManager.getInstance(this).registerReceiver(catcher, ifilter);
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		Log.d(TAG, "Groups just got paused, doing GC sweep of items");
		app.sweepReadItems();
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		app = (MeltdownApp) getApplication();
		
		// Run setup if login errors out
		if (app.isAppConfigured() == false)
		{
			startActivity(new Intent(this, SetupServerActivity.class));
			Toast.makeText(this, "Please configure your server", Toast.LENGTH_SHORT).show();
			return;
		}
		
		app.sweepReadItems();
		doRefresh();
	}

	public void doRefresh()
	{
		getActionBar().setSubtitle(app.totalUnreadItems() + " to read");
		mAdapter = new GroupListAdapter(GroupsActivity.this, app.getUnreadGroups());
		setListAdapter(mAdapter);
        final ListView lv = getListView();
        
        // FIXME As in the ItemsActivity, figure out the next group to read and position _there_
        if (last_pos > 0)
        	last_pos = Math.min(last_pos, lv.getCount());
        lv.setSelection(last_pos);

        lv.setOnItemClickListener(new OnItemClickListener()
        {
        	@Override
        	public void onItemClick(AdapterView<?> arg0, View view, int pos, long id)
        	{
				RssGroup group = (RssGroup) lv.getItemAtPosition(pos);
				
				Intent intent = new Intent(GroupsActivity.this, ItemsActivity.class);
				Bundle bundle = new Bundle();
				bundle.putString("title", group.title);
				bundle.putInt("group_id", group.id);
				intent.putExtras(bundle);
				// TODO Startactivity for result - get callback when done
				// TODO listview.setSelection to go back to correct display location
				// http://stackoverflow.com/questions/2628741/listview-and-scroll
				last_pos = pos;
				startActivity(intent);
        	}		        	
        });
	}
	
	/**
     * ArrayAdapter encapsulates a java.util.List of T, for presentation in a
     * ListView. This subclass specializes it to hold RssItems and display
     * their title/description data in a TwoLineListItem.
     */
    private class GroupListAdapter extends ArrayAdapter<RssGroup> 
    {
        private LayoutInflater mInflater;

        public GroupListAdapter(Context context, List<RssGroup> objects) 
        {
            super(context, 0, objects);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        /**
         * This is called to render a particular item for the on screen list.
         * Uses an off-the-shelf TwoLineListItem view, which contains text1 and
         * text2 TextViews. We pull data from the RssItem and set it into the
         * view. The convertView is the view from a previous getView(), so
         * we can re-use it.
         * 
         * @see ArrayAdapter#getView
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) 
        {
            TwoLineListItem view;

            // Here view may be passed in for re-use, or we make a new one.
            if (convertView == null) 
            {
                view = (TwoLineListItem) mInflater.inflate(android.R.layout.simple_list_item_2,
                        null);
            } else 
            {
                view = (TwoLineListItem) convertView;
            }

            // FIXME race condition
            RssGroup grp = app.getUnreadGroups().get(position);

            // Set the item title and description into the view.
            view.getText1().setText(grp.title);
            int unread_count = app.groupUnreadItems(grp.id);
            String descr = "";
            if (unread_count == 0)
            	descr = " -- No unread items --";
            else if (unread_count == 1)
            	descr = "One unread item";
            else
            	descr = String.format("%d unread items", unread_count);
            view.getText2().setText(descr);
            
            // Simulate zebra-striping - a touch of class. Maybe. Need to consider themes.
            //if (position % 2 == 0)
            //	view.setBackgroundColor(Color.LTGRAY);
                        
            return view;
        }
    }
	
    // Catch updates from the Service, update the data adapter. Needs more work.
    // See http://www.intertech.com/Blog/Post/Using-LocalBroadcastManager-in-Service-to-Activity-Communications.aspx
    private class dBroadcastCatcher extends BroadcastReceiver
    {
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (intent.getAction().equals(Downloader.ACTION_UPDATE_STARTING))
			{
				pd = new ProgressDialog(GroupsActivity.this);
				pd.setTitle("Sync in progress");
				pd.show();
			}
			else if (intent.getAction().equals(Downloader.ACTION_UPDATE_DONE))
			{
				doRefresh();
				if (pd != null)
					pd.dismiss();
			}
			else if (pd != null)
				pd.setMessage(intent.getAction());
		}
    }
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch (item.getItemId())
		{
		case R.id.menu_refresh:
			Log.d(TAG, "Refreshing...");
			startService(new Intent(this, Downloader.class));
			doRefresh();
			return true;
		case R.id.menu_settings:
			Log.d(TAG, "Settings selecected");
			startActivity(new Intent(this, SetupServerActivity.class));
			return true;
		case R.id.menu_about:
			startActivity(new Intent(this, AboutActivity.class));
			return true;
		}
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		getMenuInflater().inflate(R.menu.activity_groups, menu);
		return true;
	}
}
