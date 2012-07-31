package net.phfactor.meltdown;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Application;
import android.util.Log;

public class MeltdownApp extends Application 
{	
	static final String TAG = "MeltdownApp";


	private DataModel data;
	private RestClient xcvr;


	public MeltdownApp()
	{
		data = new DataModel();
	}

	private List<Integer> jaToLI(JSONArray json_array) throws JSONException
	{
		List<Integer> rc = new ArrayList<Integer>();
		for (int idx = 0; idx < json_array.length(); idx++)
		{
			rc.add(json_array.getInt(idx));
		}
		return rc;
	}
	
	/*!
	 *  Take the data returned from a groups fetch, parse and save into data model.
	 */
	public void saveGroupsData(String payload)
	{
		JSONArray group_list;
		JSONArray fg_list;
		
		ArrayList<HashMap<String, String>> myList = new ArrayList<HashMap<String, String>>();
		HashMap<Integer, List<Integer>> gf_map = new HashMap<Integer, List<Integer>>();
		try 
		{
			JSONObject jsonPayload = new JSONObject(payload);
			
			// groups array first
			group_list = jsonPayload.getJSONArray("groups");

			// For JSONArray to ListView I cribbed from
			// http://p-xr.com/android-tutorial-how-to-parse-read-json-data-into-a-android-listview/
			for (int idx = 0; idx < group_list.length(); idx++)
			{
					HashMap<String, Integer> map = new HashMap<String, Integer>();
					
					map.put("title", value)
					map.put("id", group_list.getJSONObject(idx).get("id"));
					map.put("title", group_list.getJSONObject(idx).getString("title"));
					Log.d(TAG, map.toString());
	
					myList.add(map);
			}
	
			// Now feeds_groups - map group ID onto list of feed IDs
			fg_list = jsonPayload.getJSONArray("feeds_groups");
			for (int idx = 0; idx < fg_list.length(); idx++)
			{
				int key = fg_list.getJSONObject(idx).getInt("id");
				JSONArray json_array = fg_list.getJSONObject(idx).getJSONArray("feed_ids");
				gf_map.put(key, jaToLI(json_array));
			}
		} catch (JSONException e) 
		{
			e.printStackTrace();
			return;
		}
		
		data.storeGroupsPull(myList, gf_map);		
	}

	public void saveFeedsData(String payload)
	{
		// TODO
	}

	public void saveItemsData(String payload)
	{
		// TODO
	}

	public void markItemRead(int item_id)
	{
		Log.d(TAG, "Firing task to mark item " + item_id + " as read.");
		RestCallback no_op = new RestCallback();
		String url = String.format("%s&mark=item&as=read&id=%d", xcvr.getAPIUrl(), item_id);
		xcvr.grabURL(url, no_op);
	}
}
