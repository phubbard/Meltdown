package net.phfactor.meltdown;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Application;
import android.util.Log;

public class MeltdownApp extends Application 
{	
	static final String TAG = "MeltdownApp";

	private List<RssGroup> groups;
	private List<RssFeed> feeds;
	private long last_refresh_time;
	
	private RestClient xcvr;


	public MeltdownApp()
	{
		groups = new ArrayList<RssGroup>();
		feeds = new ArrayList<RssFeed>();
		last_refresh_time = 0L;
	}

	// Parse array of string-encoded ints into List<Integer>
	// Library function for this somewhere? JSON breaks a lot of Java-isms.
	public List<Integer> gsToListInt(JSONArray id_array) throws JSONException
	{
		List<Integer> rc = new ArrayList<Integer>();
		
		for (int idx = 0; idx < id_array.length(); idx++)
			rc.add(Integer.valueOf(id_array.getInt(idx)));
		return rc;
	}
	
	// For JSONArray to ListView I cribbed from
	// http://p-xr.com/android-tutorial-how-to-parse-read-json-data-into-a-android-listview/
	
	/*!
	 *  Take the data returned from a groups fetch, parse and save into data model.
	 */
	public void saveGroupsData(String payload)
	{
		JSONArray jgroups;		

		try
		{
			JSONObject jsonPayload = new JSONObject(payload);			
			jgroups = jsonPayload.getJSONArray("groups");
			for (int idx = 0; idx < jgroups.length(); idx++)
				groups.add(new RssGroup(jgroups.getJSONObject(idx), this));
			
		} catch (JSONException e) 
		{
			e.printStackTrace();
		}
	}

	public void saveFeedsData(String payload)
	{
		JSONArray jfeeds;
		
		try
		{
			JSONObject jpayload = new JSONObject(payload);
			jfeeds = jpayload.getJSONArray("feeds");
			this.last_refresh_time = jpayload.getLong("last_refreshed_on_time");
			for (int idx = 0; idx < jfeeds.length(); idx++)
				feeds.add(new RssFeed(jfeeds.getJSONObject(idx)));
			
		} catch (JSONException e) 
		{
			e.printStackTrace();
		}
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
