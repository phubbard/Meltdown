package net.phfactor.meltdown;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class ItemsActivity extends ListActivity 
{
	static final String TAG = "MeltdownILA";
	private MeltdownApp app;
	private String group_name;
	private int group_id;
	private int last_pos;
	ArrayList<HashMap<String, String>> item_ids;	

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		// See http://stackoverflow.com/questions/820398/android-change-custom-title-view-at-run-time
		setContentView(R.layout.items);
		super.onCreate(savedInstanceState);

		app = (MeltdownApp) this.getApplicationContext();
				
		group_name = getIntent().getExtras().getString("title");
		group_id = getIntent().getExtras().getInt("group_id");

		last_pos = 0;
		getActionBar().setTitle(group_name);
		final ListView lv = getListView();		

		item_ids = app.getAllItemsForGroup(group_id);
		
		// TODO Refresh on drag down
		lv.setOverscrollHeader(getWallpaper());
		
		ListAdapter ladapt = new SimpleAdapter(this, item_ids, R.layout.itemrow,
				new String[] {"title", "author"},
				new int[] {R.id.item_title, R.id.item_subtitle});
        lv.setTextFilterEnabled(true);
		
        Log.d(TAG, ladapt.getCount() + " items for " + group_name);
        
		setListAdapter(ladapt);
        lv.setOnItemClickListener(new OnItemClickListener()
        {
        	@Override
        	public void onItemClick(AdapterView<?> arg0, View view, int pos, long id)
        	{
        		viewPost(pos);
        	}
        });		
	}
	
	private void removeItem(int post_id)
	{
		for (int idx = 0; idx < item_ids.size(); idx++)
		{
			int cur_id = Integer.parseInt(item_ids.get(idx).get("id"));
			if (cur_id == post_id)
			{
				item_ids.remove(idx);
				return;
			}
		}		
	}
	
	private void viewPost(int position)
	{
		HashMap<String, String> o = (HashMap<String, String>) getListView().getItemAtPosition(position);
		//Log.d(TAG, o.toString());
		
		RssItem item = app.findPostById(Integer.parseInt(o.get("id")));
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
			if (item_ids.size() == 0)
			{
				finish();
				return;
			}
			
			int pos = (last_pos + 1) % item_ids.size();
			Log.d(TAG, "Next item " + pos + " of " + item_ids.size());
			viewPost(pos);
		}
		else
			Log.d(TAG, "Item display cancelled, leaving unread");
	}
	

}
