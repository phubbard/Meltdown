package net.phfactor.meltdown;

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
	static final String TAG = "MeltdownLA";
	private MeltdownApp app;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		// See http://stackoverflow.com/questions/820398/android-change-custom-title-view-at-run-time
		setContentView(R.layout.items);
		super.onCreate(savedInstanceState);

		String grp_name = getIntent().getExtras().getString("title");
		int group_id = getIntent().getExtras().getInt("group_id");

		getActionBar().setTitle(grp_name);
		
		app = (MeltdownApp) this.getApplicationContext();
				
		final ListView lv = getListView();		
		
		// TODO Refresh on drag down
		lv.setOverscrollHeader(getWallpaper());
		
		ListAdapter ladapt = new SimpleAdapter(this, app.getAllItemsForGroup(group_id), R.layout.itemrow,
				new String[] {"title", "author"},
				new int[] {R.id.item_title, R.id.item_subtitle});
        lv.setTextFilterEnabled(true);
		
		setListAdapter(ladapt);
        lv.setOnItemClickListener(new OnItemClickListener()
        {
        	@Override
        	public void onItemClick(AdapterView<?> arg0, View view, int pos, long id)
        	{
				HashMap<String, String> o = (HashMap<String, String>) lv.getItemAtPosition(pos);
				Log.d(TAG, o.toString());
				
				RssItem item = app.findPostById(Integer.parseInt(o.get("id")));
				Intent intent = new Intent(ItemsActivity.this, ItemDisplayActivity.class);
				Bundle bundle = new Bundle();
				bundle.putInt("post_id", item.id);
				intent.putExtras(bundle);
				startActivity(intent);				
        	}
        });
	}

}
