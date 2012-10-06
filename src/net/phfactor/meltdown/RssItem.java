package net.phfactor.meltdown;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

public class RssItem
{
	static final String TAG = "MeltdownRI";
	
	public Boolean is_read;
	public String title;
	public String url;
	private String html;
	public String excerpt;
	public int id;
	public Long created_on_time;
	public int feed_id;
	public Boolean is_saved;
	public String author;
	private Context ctx;
	
    // Take this many chars from the front of the description.
    public static final int SNIPPET_LENGTH = 87;

    public RssItem(Context context, int post_id)
    {
    	ctx = context;
		try
		{
			JSONObject jdata = new JSONObject(loadFromFile(post_id));
			parseFromJson(jdata);
		} catch (JSONException je)
		{
			Log.e(TAG, "Error parsing feed item", je);
		}			
    }
	
	public RssItem()
	{		
		ctx = null;
	}
	
	public RssItem(JSONObject data)
	{
		parseFromJson(data);
	}
	
	private void parseFromJson(JSONObject data)
	{
		try
		{
			// Bools are returned as ints from Fever, so convert
			this.is_read = (data.getInt("is_read") == 1);
			this.title = data.getString("title");
			this.url = data.getString("url");
			this.html = data.getString("html");
			this.excerpt = makeExcerpt(data.getString("html"));
			this.id = data.getInt("id");
			this.created_on_time = data.getLong("created_on_time");
			this.feed_id = data.getInt("feed_id");
			this.is_saved = (data.getInt("is_saved") == 1);
			this.author = data.getString("author");
		} catch (JSONException je)
		{
			Log.e(TAG, "Error parsing feed item", je);
			Log.e(TAG, data.toString());
		}
		
	}
	// Save the entire thing to a json-encoded string. Hide the JSON, since we may switch.
	public byte[] serialize()
	{
		JSONObject rc = new JSONObject();
		try
		{
			String tmp_str = "0";
			if (is_read)
				tmp_str = "1";
			rc.put("is_read", tmp_str);
			rc.put("title", title);
			rc.put("url", url);
			rc.put("html", html);
			rc.put("id", Integer.toString(id));
			rc.put("created_on_time", Long.toString(created_on_time));
			rc.put("feed_id", Integer.toString(feed_id));
			
			if (is_saved)
				tmp_str = "1";
			else
				tmp_str = "0";
			rc.put("is_saved", tmp_str);
			rc.put("author", author);			
		} catch (JSONException je)
		{
			Log.e(TAG,  "Error serializing RssItem");
			return null;			
		}
		return rc.toString().getBytes();
	}	
	
	public void dropHTML()
	{
		this.html = "Need to lazy load me";
	}
	
	public void deleteDiskFile()
	{
		Log.d(TAG, "Removing " + filename());
		ctx.deleteFile(filename());
	}
	
	// Lazy-load the data from file.
	public String getHTML(Context context)
	{
		ctx = context;
		try
		{
			JSONObject jdata = new JSONObject(loadFromFile(id));
			parseFromJson(jdata);
		} catch (JSONException je)
		{
			Log.e(TAG, "Error parsing feed item", je);
			return("Error loading HTML from disk.\n\n" + excerpt);
		}			
		return html;
	}
	
    /**
     * Simple code to strip out <tag>s -- primitive way to sort of display HTML as
     * plain text.
     */
    public String removeTags(String str) 
    {
        str = str.replaceAll("<.*?>", " ");
        str = str.replaceAll("\\s+", " ");
        return str;
    }
	
    private String filename()
    {
    	return String.format("%d.post", id);
    }
    
    private String filename(int post_id)
    {
    	return String.format("%d.post", post_id);
    }
    
    /*
     * Excerpts are used in the 2-line view that lists items for reading. Precompute them to avoid the
     * penalty time of loading the HTML from disk.
     */
    private String makeExcerpt(String full_html)
    {
        String descr = removeTags(full_html);
        String rc = descr.substring(0, Math.min(descr.length(), SNIPPET_LENGTH)) + "...";
        return rc;
    }
    
	// Plan B. http://developer.android.com/guide/topics/data/data-storage.html
	protected void saveToFile(Context context)
	{
		ctx = context;
		try 
		{
			FileOutputStream fos = ctx.openFileOutput(filename(), Context.MODE_PRIVATE);
			fos.write(serialize());
			fos.close();
		}
		catch (FileNotFoundException fe)
		{
			Log.e(TAG, "File error", fe);
		} catch (IOException e) 
		{
			Log.e(TAG, "File error on item " + id, e);
		}
	}
	
	protected String loadFromFile(int post_id)
	{
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		
		try 
		{
			FileInputStream fos = ctx.openFileInput(filename(post_id));
			int r_char = fos.read();
			
			while (r_char > -1)
			{
				buffer.write(r_char);
				r_char = fos.read();
			}
			fos.close();
		}			
		catch (FileNotFoundException fe)
		{
			Log.e(TAG, "File error", fe);
		} catch (IOException e) 
		{
			Log.e(TAG, "File error on item " + id, e);
		}
		
		return buffer.toString();
	}
}
