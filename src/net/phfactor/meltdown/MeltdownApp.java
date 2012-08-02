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
	
	// Parse array of string-encoded ints into List<Integer>
	// Library function for this somewhere? JSON breaks a lot of Java-isms.
	private List<Integer> gsToListInt(JSONArray id_array) throws JSONException
	{
		List<Integer> rc = new ArrayList<Integer>();
		
		for (int idx = 0; idx < id_array.length(); idx++)
		{
			rc.add(Integer.valueOf(id_array.getInt(idx)));
		}
		
		return rc;
	}
	// For JSONArray to ListView I cribbed from
	// http://p-xr.com/android-tutorial-how-to-parse-read-json-data-into-a-android-listview/
	
	/*!
	 *  Take the data returned from a groups fetch, parse and save into data model.
	 */
	public void saveGroupsData(String payload)
	{
		JSONArray group_list;		
		HashMap<String, List<Integer>> gf_map = new HashMap<String, List<Integer>>();
		HashMap<String, Integer> gi_map = new HashMap<String, Integer>();
		
		try 
		{
			JSONObject jsonPayload = new JSONObject(payload);
			
			// groups array first
			group_list = jsonPayload.getJSONArray("groups");

			for (int idx = 0; idx < group_list.length(); idx++)
			{
				String group_name = group_list.getJSONObject(idx).getString("title");
				int group_id = Integer.valueOf(group_list.getJSONObject(idx).getInt("id"));				
				List<Integer> id_list = gsToListInt(group_list.getJSONObject(idx).getJSONArray("feed_ids"));
				
				gf_map.put(group_name, id_list);
				gi_map.put(group_name, group_id);
			}
		} catch (JSONException e) 
		{
			e.printStackTrace();
			return;
		}
		
		data.storeGroupsPull(gf_map, gi_map);		
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
