package net.phfactor.meltdown;

import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class RssItem 
{
	static final String TAG = "MeltdownRI";
	
	public Boolean is_read;
	public String title;
	public String url;
	public String html;
	public int id;
	public long created_on_time;
	public int feed_id;
	public Boolean is_saved;
	public String author;
	
	public RssItem()
	{
		
	}
	
	public RssItem(JSONObject data)
	{
		try
		{
			this.is_read = (data.getInt("is_read") == 1);
			this.title = data.getString("title");
			this.url = data.getString("url");
			this.html = data.getString("html");
			this.id = data.getInt("id");
			this.created_on_time = data.getLong("created_on_time");
			this.feed_id = data.getInt("feed_id");
			this.is_saved = (data.getInt("is_saved") == 1);
			this.author = data.getString("author");
		} catch (JSONException je)
		{
			Log.e(TAG, "Error parsing feed item", je);
		}
	}
	
	public HashMap<String, String> getHashMap()
	{
		HashMap<String, String> rc = new HashMap<String, String>();
		rc.put("is_read", is_read.toString());
		rc.put("title", title);
		rc.put("url", url);
		rc.put("html", html);
		rc.put("id", Integer.toString(id));
		rc.put("created_on_time", Long.toString(created_on_time));
		rc.put("feed_id", Integer.toString(feed_id));
		rc.put("is_saved", is_saved.toString());
		rc.put("author", author);
		return rc;
	}
}
