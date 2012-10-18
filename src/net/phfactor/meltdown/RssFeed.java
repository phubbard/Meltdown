package net.phfactor.meltdown;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

/*
 * Simple representation of an RSS feed.
 */
public class RssFeed 
{
	static final String TAG = "MeltdownRssFeed";
	
	public  String site_url;
	public  String url;
	public  String title;
	public  int id;
	public  int favicon_id;
	public  Boolean is_spark;
	public  long last_updated_on_time;
	
	// Derived reverse index
	public List<Integer> groups;
	
	public RssFeed(JSONObject feed)
	{
		try
		{
			//Log.d(TAG, feed.toString());
			
			this.site_url = feed.getString("site_url");
			this.url = feed.getString("url");
			this.title = feed.getString("title");
			this.id = feed.getInt("id");
			this.favicon_id = feed.getInt("favicon_id");
			this.is_spark = (feed.getInt("is_spark") == 0);
			this.last_updated_on_time = feed.getLong("last_updated_on_time");			
			this.groups = new ArrayList<Integer>();
			
		}catch (JSONException je)
		{
			Log.e(TAG, "Unable to parse JSON feed!", je);			
		}
	}
}
