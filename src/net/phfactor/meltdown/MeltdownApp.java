package net.phfactor.meltdown;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Application;
import android.content.Context;
import android.util.Log;

public class MeltdownApp extends Application 
{	
	static final String TAG = "MeltdownApp";

	private List<RssGroup> groups;
	private List<RssFeed> feeds;
	private List<RssItem> items;
	private int max_read_id;
	private int max_fetched_id;
	private int max_id_on_server;
	private long last_refresh_time;
	private RestClient xcvr;

	@Override
	public void onCreate()
	{
		super.onCreate();

		groups = new ArrayList<RssGroup>();
		feeds = new ArrayList<RssFeed>();
		items = new ArrayList<RssItem>();
		max_read_id = 0;
		max_fetched_id = 0;
		max_id_on_server = 0;
		last_refresh_time = 0L;
		xcvr = new RestClient(getApplicationContext());		
	}
	
	public MeltdownApp(Context ctx)
	{
	}

	public MeltdownApp()
	{
	}
	
	public int getMaxFetchedId()
	{
		return max_fetched_id;
	}
	
	
	// TODO Replace by sqlite query once DB-backed
	protected RssGroup findGroupById(int grp_id)
	{
		for (int idx = 0; idx < groups.size(); idx++)
		{
			if (groups.get(idx).id == grp_id)
				return groups.get(idx);
		}
		return null;
	}
	
	// TODO Replace by sqlite query once DB-backed
	protected RssGroup findGroupByName(String name)
	{
		for (int idx = 0; idx < groups.size(); idx++)
		{
			if (groups.get(idx).title.equals(name))
				return groups.get(idx);
		}
		return null;
	}

	private synchronized int removePost(int post_id)
	{
		Log.d(TAG, items.size() + " before delete");
		for (int idx = 0; idx < items.size(); idx++)
		{
			if (items.get(idx).id == post_id)
			{
				Log.d(TAG, "Removing post id " + idx);
				items.remove(idx);
				Log.d(TAG, items.size() + " after delete");
				return 0;
			}
		}
		return 1;		
	}
	
	// TODO Replace by sqlite query
	protected RssItem findPostById(int post_id)
	{
		for (int idx = 0; idx < items.size(); idx++)
		{
			if (items.get(idx).id == post_id)
				return items.get(idx);
		}
		return null;
	}
	
	// Unread items for a given group
	public List<Integer> itemsForGroup(int group_id)
	{
		ArrayList<Integer> rc = new ArrayList<Integer>();
		
		RssGroup group = findGroupById(group_id);
		if (group == null)
			return rc;
		
		for (int idx = 0; idx < group.feed_ids.size(); idx++)
		{
			for (int item_idx = 0; item_idx < items.size(); item_idx++)
			{
				if (items.get(item_idx).feed_id == group.feed_ids.get(idx))
					rc.add(items.get(item_idx).id);
			}
		}
		return rc;
	}
	// Unread items count for a given group ID
	public int unreadItemCount(int group_id)
	{
		return itemsForGroup(group_id).size();
	}
	
	public List<RssItem> getAllItemsForGroup(int group_id)
	{
		ArrayList<RssItem> rc = new ArrayList<RssItem>();
		RssGroup grp = findGroupById(group_id);
		if (grp == null)
		{
			Log.e(TAG, "Unable to locate group id " + group_id);
			return null;
		}
		
		for (int cur_feed = 0; cur_feed < grp.feed_ids.size(); cur_feed++)
		{
			for (int cur_item = 0; cur_item < items.size(); cur_item++)
			{
				RssItem current_item = items.get(cur_item);
				if (current_item.feed_id == grp.feed_ids.get(cur_feed))				
				{
					if (rc.contains(current_item))
						continue;
					
					rc.add(current_item);
				}
			}
		}
		Log.d(TAG, rc.size() + " items for group ID " + group_id);
		return rc;
	}
	
	/* The feeds_groups data is a bit different. Separate json array, and the 
	 * encoding differs. The arrays are CSV instead of JSON, so we have to create 
	 * a specialized parser for them.
	 * TODO Feedback to developer on this API
	 */
	public List<Integer> gsToListInt(String id_str) throws JSONException
	{
		List<Integer> rc = new ArrayList<Integer>();
		
		String[] nums = id_str.split(",");
		
		for (int idx = 0; idx < nums.length; idx++)
			rc.add(Integer.valueOf(nums[idx]));
		return rc;
	}

	private void saveFeedsGroupsData(JSONObject payload) throws JSONException
	{
		JSONArray fg_arry = payload.getJSONArray("feeds_groups");
		for (int idx = 0; idx < fg_arry.length(); idx++)
		{
			JSONObject cur_grp = fg_arry.getJSONObject(idx);
			int grp_id = cur_grp.getInt("group_id");
			for (int grp_idx = 0; grp_idx < groups.size(); grp_idx++)
			{
				if (groups.get(grp_idx).id == grp_id)
					groups.get(grp_idx).feed_ids = gsToListInt(cur_grp.getString("feed_ids"));
			}
		}
	}
	
	/* ***********************************
	 * REST callback methods
	 */
	
	/*!
	 *  Take the data returned from a groups fetch, parse and save into data model.
	 */
	protected void saveGroupsData(String payload)
	{
		JSONArray jgroups;		
		RssGroup this_grp;
		
		if (payload == null)
			return;
		
		try
		{
			JSONObject jsonPayload = new JSONObject(payload);			
			jgroups = jsonPayload.getJSONArray("groups");
			for (int idx = 0; idx < jgroups.length(); idx++)
			{
				this_grp = new RssGroup(jgroups.getJSONObject(idx), this);				
				groups.add(this_grp);
			}			
			saveFeedsGroupsData(jsonPayload);
			
		} catch (JSONException e) 
		{
			e.printStackTrace();
		}		
		
		Log.i(TAG, groups.size() + " groups found");
	}

	protected void saveFeedsData(String payload)
	{
		JSONArray jfeeds;
		
		if (payload == null)
			return;
		
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
		Log.d(TAG, feeds.size() + " feeds found");
	}

	public List<RssGroup> getGroups()
	{
		return this.groups;
	}
	
	// Save RSS items parsed from payload, return number saved.
	public int saveItemsData(String payload)
	{
		JSONArray jitems;
		RssItem this_item;
		int old_size = items.size();
		
		if (payload == null)
			return 0;
		
		try
		{
			JSONObject jdata = new JSONObject(payload);
			
			jitems = jdata.getJSONArray("items");
			// Check ending condition(s)
			if (jitems.length() == 0)
			{
				Log.i(TAG, "No more items in feed!");
				return 0;
			}
			
			this.max_id_on_server = jdata.getInt("total_items");
			this.last_refresh_time = jdata.getLong("last_refreshed_on_time");
			for (int idx = 0; idx < jitems.length(); idx++)
			{
				this_item = new RssItem(jitems.getJSONObject(idx));
				this.max_read_id = Math.max(this.max_read_id, this_item.id);
				
				if (!items.contains(this_item))
					
					items.add(this_item);
			}
			Log.i(TAG, items.size() - old_size +" items added, " + items.size() + " total");
			return jitems.length();
		} catch (JSONException e) 
		{
			e.printStackTrace();
		}	
		return 0;		
	}

	public void markItemRead(int item_id)
	{
		removePost(item_id);
		xcvr.markItemRead(item_id);
	}
}
