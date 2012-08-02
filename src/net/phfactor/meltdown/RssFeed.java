package net.phfactor.meltdown;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

/*
 * Simple representation of an RSS feed.
 */
public class RssFeed 
{
	static final String TAG = "MeltdownRF";
	
	public  String site_url;
	public  String url;
	public  String title;
	public  int id;
	public  int favicon_id;
	public  Boolean is_spark;
	public  long last_updated_on_time;
	
	public RssFeed(JSONObject feed)
	{
		try
		{
			this.site_url = feed.getString("site_url");
			this.url = feed.getString("url");
			this.title = feed.getString("title");
			this.id = feed.getInt("id");
			this.favicon_id = feed.getInt("favicon_id");
			this.is_spark = feed.getBoolean("is_spark");
			this.last_updated_on_time = feed.getLong("last_updated_on_time");			
			
		}catch (JSONException je)
		{
			Log.e(TAG, "Unable to parse JSON feed!");			
		}
	}
}
