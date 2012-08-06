package net.phfactor.meltdown;

import android.app.ListActivity;
import android.os.Bundle;
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
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list);
		
		app = (MeltdownApp) this.getApplicationContext();
		
		String grp_name = getIntent().getExtras().getString("title");
		int group_id = getIntent().getExtras().getInt("group_id");
		
		final ListView lv = getListView();
		
		// TODO Refresh on drag down
		lv.setOverscrollHeader(getWallpaper());
		
		ListAdapter ladapt = new SimpleAdapter(this, app.getALItems(group_id), R.layout.itemrow,
				new String[] {"Title"},
				new int[] {R.id.item_title});
		setListAdapter(ladapt);
	}

}
