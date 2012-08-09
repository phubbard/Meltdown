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
	static final String TAG = "MeltdownRG";
	
	public  String title;
	public  int id;
	public  List<Integer> feed_ids;
	
	public RssGroup(JSONObject data, MeltdownApp app)
	{
		try
		{
			this.title = data.getString("title");
			this.id = data.getInt("id");
			this.feed_ids = new ArrayList<Integer>();
			
		} catch (JSONException e) 
		{
			Log.e(TAG, "Unable to parse JSON feed!");			
			e.printStackTrace();
		}
	}	
	
	
}
