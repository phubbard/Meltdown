package net.phfactor.meltdown;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class GroupsActivity extends ListActivity 
{
	private MeltdownApp app;
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);

		RestClient rc = new RestClient(this);
		app = new MeltdownApp(this);

		// TODO run setup if login errors out?
		// Check for login, run prefs
		if (rc.haveSetup() == false)
		{
			startActivity(new Intent(this, SetupActivity.class));
			Toast.makeText(this, "Please configure a server", Toast.LENGTH_SHORT).show();
		}

		setContentView(R.layout.list);
		
		app.getGroups();
		app.getFeeds();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		getMenuInflater().inflate(R.menu.activity_groups, menu);
		return true;
	}

	private class getGroupsCB extends RestCallback
	{
		JSONArray group_list;
		ArrayList<HashMap<String, String>> myList = new ArrayList<HashMap<String, String>>();
		
		String TAG = "MeltdownGGCB";

		@Override
		public void handleData(String payload)
		{
			try {
				JSONObject jsonPayload = new JSONObject(payload);
				group_list = jsonPayload.getJSONArray("groups");
			} catch (JSONException e) {
				e.printStackTrace();
				return;
			}

			// For JSONArray to ListView I cribbed from
			// http://p-xr.com/android-tutorial-how-to-parse-read-json-data-into-a-android-listview/
			for (int idx = 0; idx < group_list.length(); idx++)
			{
				try {
					HashMap<String, String> map = new HashMap<String, String>();
					
					map.put("id", group_list.getJSONObject(idx).getString("id"));
					map.put("name", group_list.getJSONObject(idx).getString("title"));
					
					Log.d(TAG, map.toString());
					
					myList.add(map);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			
			ListAdapter adapter = new SimpleAdapter(GroupsActivity.this, myList, R.layout.row,
					new String[] {"name", "id"},
					new int[] { R.id.item_title, R.id.item_subtitle});

			setListAdapter(adapter);
			final ListView lv = getListView();
	        lv.setTextFilterEnabled(true);
	        lv.setOnItemClickListener(new OnItemClickListener()
	        {
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1,
						int position, long id) {
					HashMap<String, String> o = (HashMap<String, String>) lv.getItemAtPosition(position);
					Toast.makeText(GroupsActivity.this, o.get("name"), Toast.LENGTH_SHORT).show();
				}
	        });
		}
	}
}
