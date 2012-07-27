package net.phfactor.meltdown;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class GroupsActivity extends Activity 
{
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);

		RestClient rc = new RestClient(this);

		// Check for login, run prefs
		if (rc.haveSetup() == false)
		{
			startActivity(new Intent(this, SetupActivity.class));
			Toast.makeText(this, "Please configure a server", Toast.LENGTH_SHORT).show();
		}

		setContentView(R.layout.activity_groups);
		rc.fetchGroups(new getGroupsCB());
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
			
			ListView lv = (ListView) findViewById(R.id.gListView);
			lv.setAdapter(new ListAdapter() 
			{

				@Override
				public int getCount() 
				{
					return group_list.length();
				}

				@Override
				public Object getItem(int position) 
				{
					try {
						return group_list.get(position).toString();
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return "error";
				}

				@Override
				public long getItemId(int position) 
				{
					// TODO Auto-generated method stub
					return 0;
				}

				@Override
				public int getItemViewType(int position) {
					// TODO Auto-generated method stub
					return 0;
				}

				@Override
				public View getView(int position, View convertView,
						ViewGroup parent) {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public int getViewTypeCount() {
					// TODO Auto-generated method stub
					return 1;
				}

				@Override
				public boolean hasStableIds() {
					// TODO Auto-generated method stub
					return false;
				}

				@Override
				public boolean isEmpty() 
				{
					return (group_list.length() == 0);
				}

				@Override
				public void registerDataSetObserver(
						DataSetObserver observer) {
					// TODO Auto-generated method stub

				}

				@Override
				public void unregisterDataSetObserver(
						DataSetObserver observer) {
					// TODO Auto-generated method stub

				}

				@Override
				public boolean areAllItemsEnabled() {
					// TODO Auto-generated method stub
					return false;
				}

				@Override
				public boolean isEnabled(int position) {
					// TODO Auto-generated method stub
					return false;
				}

			});
		}
	}
}
