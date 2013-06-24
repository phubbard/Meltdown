package net.phfactor.meltdown;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

/* Simple representation of a group of RSS feeds, basically a composite struct
 * wrapped up for easier syntax.
 */

public class RssGroup 
{
	static final String TAG = "MeltdownRssGroup";
	
	public String title;
	public int id;
	public List<Integer> feed_ids;
	Boolean synthetic;
	
	// This is derived from walking the data from the server
	public List<RssItem> items;
	
	public RssGroup(JSONObject data, MeltdownApp app)
	{
		try
		{
			this.title = data.getString("title");
			this.id = data.getInt("id");
			// Is this a 'synthetic' group, i.e. only existing on Meltdown?
			this.synthetic = (this.id >= MeltdownApp.ORPHAN_ID);
			this.feed_ids = new ArrayList<Integer>();
			this.items = new ArrayList<RssItem>();
		} catch (JSONException e) 
		{
			Log.e(TAG, "Unable to parse JSON feed!");
			e.printStackTrace();
		}
	}

	public RssGroup(String title, int id)
	{
		this.title = title;
		this.id = id;
		this.synthetic = (this.id >= MeltdownApp.ORPHAN_ID);		
		this.feed_ids = new ArrayList<Integer>();
		this.items = new ArrayList<RssItem>(); 
	}

	public void clearItems()
	{
		this.items = new ArrayList<RssItem>();
	}
}
