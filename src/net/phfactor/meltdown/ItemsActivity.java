package net.phfactor.meltdown;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TwoLineListItem;

public class ItemsActivity extends ListActivity 
{
	static final String TAG = "MeltdownILA";
    /**
     * Custom list adapter that fits our rss data into the list.
     */
    private RSSListAdapter mAdapter;
	
    // Take this many chars from the front of the description.
    public static final int SNIPPET_LENGTH = 87;

    private MeltdownApp app;
	private String group_name;
	private int group_id;
	private int last_pos;
	
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

		last_pos = 0;
		getActionBar().setTitle(group_name);
		final ListView lv = getListView();		
        // The above layout contains a list id "android:list"
        // which ListActivity adopts as its list -- we can
        // access it with getListView().

		// TODO start a progress dialog when this goes to SQLite data - might be slow
		items = app.getAllItemsForGroup(group_id);
		
		// TODO Refresh on drag down
		lv.setOverscrollHeader(getWallpaper());
		
        // Install our custom RSSListAdapter.
        mAdapter = new RSSListAdapter(this, items);
        getListView().setAdapter(mAdapter);

/*        
		ListAdapter ladapt = new SimpleAdapter(this, item_ids, R.layout.itemrow,
				new String[] {"title", "author"},
				new int[] {R.id.item_title, R.id.item_subtitle});
        lv.setTextFilterEnabled(true);
		
        Log.d(TAG, ladapt.getCount() + " items for " + group_name);
        
		setListAdapter(ladapt);
*/		
        lv.setOnItemClickListener(new OnItemClickListener()
        {
        	@Override
        	public void onItemClick(AdapterView<?> arg0, View view, int pos, long id)
        	{
        		viewPost(pos);
        	}
        });		
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
        public View getView(int position, View convertView, ViewGroup parent) {
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
            String descr = item.html;
            descr = removeTags(descr);
            String final_descr = descr.substring(0, Math.min(descr.length(), SNIPPET_LENGTH)) + "...";
            view.getText2().setText(final_descr);
            return view;
        }

    }

    /**
     * Simple code to strip out <tag>s -- primitive way to sortof display HTML as
     * plain text.
     */
    public String removeTags(String str) 
    {
        str = str.replaceAll("<.*?>", " ");
        str = str.replaceAll("\\s+", " ");
        return str;
    }

    /**
     * Resets the output UI -- list and status text empty.
     */
    public void resetUI() 
    {
        // Reset the list to be empty.
        List<RssItem> items = new ArrayList<RssItem>();
        mAdapter = new RSSListAdapter(this, items);
        getListView().setAdapter(mAdapter);
    }

	
	private void removeItem(int post_id)
	{
		for (int idx = 0; idx < items.size(); idx++)
		{
			int cur_id = items.get(idx).id;
			if (cur_id == post_id)
			{
				items.remove(idx);
				return;
			}
		}		
	}
	
	private void viewPost(int position)
	{
		RssItem item = (RssItem) getListView().getItemAtPosition(position);
		
		Intent intent = new Intent(ItemsActivity.this, ItemDisplayActivity.class);
		Bundle bundle = new Bundle();
		bundle.putInt("post_id", item.id);
		intent.putExtras(bundle);
		
		last_pos = position;
		// http://developer.android.com/training/basics/intents/result.html
		startActivityForResult(intent, item.id);						
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		if (resultCode == RESULT_OK)
		{
			Log.d(TAG, "Item #" + requestCode + " displayed and marked as read");
			removeItem(requestCode);
			
			// Out of posts?
			if (items.size() == 0)
			{
				finish();
				return;
			}
			
			int pos = (last_pos + 1) % items.size();
			Log.d(TAG, "Next item " + pos + " of " + items.size());
			viewPost(pos);
		}
		else
			Log.d(TAG, "Item display cancelled, leaving unread");
	}
	

}
