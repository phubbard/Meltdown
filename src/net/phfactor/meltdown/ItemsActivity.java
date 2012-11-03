package net.phfactor.meltdown;

import java.util.List;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.TwoLineListItem;

// Display a list of RSS items from a selected group.
public class ItemsActivity extends ListActivity 
{
	static final String TAG = "MeltdownItemsActivity";
    /**
     * Custom list adapter that fits our rss data into the list.
     */
    private RSSListAdapter mAdapter;
	
    private MeltdownApp app;
	private String group_name;
	private mBroadcastCatcher catcher;
	private IntentFilter ifilter;
	private int group_id;
	private int last_item_id;
		
	// Cache - list of item IDs we are to display
	List<RssItem> items;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		// See http://stackoverflow.com/questions/820398/android-change-custom-title-view-at-run-time
		setContentView(R.layout.items);
		super.onCreate(savedInstanceState);

		app = (MeltdownApp) this.getApplicationContext();
				
		// We are started with group name and ID in the Intent; pull 'em out for display and retrieval
		group_name = getIntent().getExtras().getString("title");
		group_id = getIntent().getExtras().getInt("group_id");

		last_item_id = 0;
		ActionBar bar = getActionBar();
		bar.setTitle(group_name);
		bar.setDisplayHomeAsUpEnabled(true);		

		// Hook into data-updated broadcasts from Downloader
		catcher = new mBroadcastCatcher();
		ifilter = new IntentFilter();
		ifilter.addAction(Downloader.ACTION_UPDATING_GROUPS);
		ifilter.addAction(Downloader.ACTION_UPDATING_FEEDS);
		ifilter.addAction(Downloader.ACTION_UPDATING_ITEMS);
				
		final ListView lv = getListView();		

		// Click loads and item, long click currently shows debug info. Heh.
        lv.setOnItemClickListener(new OnItemClickListener()
        {
        	@Override
        	public void onItemClick(AdapterView<?> arg0, View view, int pos, long id)
        	{
        		viewPost(pos);
        	}
        });
        
        // TOOD Popup a choice dialog - remove thread, mark as read/saved, load in browser....
        lv.setOnItemLongClickListener(new OnItemLongClickListener()
		{

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View view,
					int pos, long id)
			{
				RssItem item = (RssItem) getListView().getItemAtPosition(pos);

				if (item == null)
					return true;
				
				String itemText = String.format("ID: %d read: %b", item.id, item.is_read);
				Toast.makeText(ItemsActivity.this, itemText, Toast.LENGTH_LONG).show();
				/* Commented out - really should have a confirmation dialog
				int rm_ct = app.markGroupThreadRead(group_id, item.title);
				reloadItemsAndView();
				Toast.makeText(ItemsActivity.this, rm_ct + " item(s) removed", Toast.LENGTH_LONG).show();
				*/
				return true;
			}
		});
        
		// Setup and populate the listview
        // Install our custom RSSListAdapter.		
		items = app.getItemsForGroup(group_id);
		if (items == null)
			finish();
		
        mAdapter = new RSSListAdapter(this, items);
        getListView().setAdapter(mAdapter);    	
        
        reloadItemsAndView();
	}
	
    // Catch updates from the Service, update the data adapter. Needs more work.
    // See http://www.intertech.com/Blog/Post/Using-LocalBroadcastManager-in-Service-to-Activity-Communications.aspx
    private class mBroadcastCatcher extends BroadcastReceiver
    {
		@Override
		public void onReceive(Context context, Intent intent)
		{
			Log.d(TAG, "Got a local broadcast, action: " + intent.getAction());
			mAdapter.notifyDataSetChanged();
		}
    }

    private int figureNextPos()
    {
		for (int idx = 0; idx < items.size(); idx++)
			if (items.get(idx).id == last_item_id)
				return ((idx + 1) % items.size());
		
		// Just in case.
		return 0;
    }
    /**
     * ArrayAdapter encapsulates a java.util.List of T, for presentation in a
     * ListView. This subclass specializes it to hold RssItems and display
     * their title/description data in a TwoLineListItem.
     */
    private class RSSListAdapter extends ArrayAdapter<RssItem> 
    {
        private LayoutInflater mInflater;

        public RSSListAdapter(Context context, List<RssItem> objects) 
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
            if (convertView == null) {
                view = (TwoLineListItem) mInflater.inflate(android.R.layout.simple_list_item_2,
                        null);
            } else {
                view = (TwoLineListItem) convertView;
            }

            RssItem item = this.getItem(position);

            // Set the item title and description into the view.
            // This example does not render real HTML, so as a hack to make
            // the description look better, we strip out the
            // tags and take just the first SNIPPET_LENGTH chars.
            view.getText1().setText(item.title);
            view.getText2().setText(item.excerpt);
            
            return view;
        }
    }

    /*
     * Reload items and the list view adapter - used when contents change.
     */
    private void reloadItemsAndView()
    {
		items = app.getItemsForGroup(group_id);            	
        mAdapter.notifyDataSetChanged();		
    }

	/* Open a post/item, and note if the user hits back or next to get out. If back, we don't
	 * mark it as read.
	 */
	private void viewPost(int position)
	{
		RssItem item = (RssItem) getListView().getItemAtPosition(position);
		
		if (item == null)
		{
			finish();
			return;
		}
		
		Intent intent = new Intent(ItemsActivity.this, ItemDisplayActivity.class);
		Bundle bundle = new Bundle();
		bundle.putInt("post_id", item.id);
		intent.putExtras(bundle);
		
		last_item_id = item.id;
		// http://developer.android.com/training/basics/intents/result.html
		startActivityForResult(intent, item.id);						
	}

	/* Want to make sure that user wants to mark an entire group as read - 
	 * this is harder than you'd expect, see
	 * http://stackoverflow.com/questions/2478517/how-to-display-a-yes-no-dialog-box-in-android
	 */
	private void showARDialog()
	{
		// TODO Move the threshold to preferences
		if (app.groupUnreadItems(group_id) < 10)
		{
			app.markGroupRead(group_id);
			finish();
			return;			
		}
		
		DialogInterface.OnClickListener dcl = new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				switch(which)
				{
				case DialogInterface.BUTTON_POSITIVE:
					app.markGroupRead(group_id);
					finish();
					return;
				case DialogInterface.BUTTON_NEGATIVE:
					return;
				}
			}
		};
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(String.format("Do you want to mark %d items as read in group %s?", 
				app.groupUnreadItems(group_id), group_name));
		builder.setPositiveButton("Yes", dcl);
		builder.setNegativeButton("No", dcl);
		builder.show();
	}
	
	// Item viewer returns RESULT_OK if the user hit next, in which case we mark it as read and move to next.
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		if (resultCode == RESULT_OK)
		{
			Log.d(TAG, "Item #" + requestCode + " displayed and marked as read");
			// Out of posts?
			if (app.groupUnreadItems(group_id) == 0)
			{
				finish();
				return;
			}
			
			int pos = figureNextPos();
			Log.d(TAG, "Next item " + pos + " of " + items.size());
			viewPost(pos);
		}
		else
			Log.d(TAG, "Item display cancelled, leaving unread");
	}
	
	protected void onPause()
	{
		super.onPause();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(catcher);
		
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		
		LocalBroadcastManager.getInstance(this).registerReceiver(catcher, ifilter);
		
		// Just in case...
		mAdapter.notifyDataSetChanged();
	}
		
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch (item.getItemId())
		{
		case android.R.id.home:
			finish();
			return true;
			
		case R.id.itemMGDr:
			// Mark entire group as read - confirm first
			showARDialog();
			return true;
		}
		
		return false;
	}
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
    {
    	MenuInflater infl = getMenuInflater();
    	infl.inflate(R.menu.activity_group, menu);
    	return true;
	}

}
