package net.phfactor.meltdown;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.util.Log;

// Class to handle feed favicons
public class Favicon
{
	static final String TAG = "MeltdownFavicon";
	
	public int id;
	public Drawable icon;
	
	public Favicon(String payload)
	{
		try 
		{
			JSONObject jdata = new JSONObject(payload);
			this.id = jdata.getInt("id");
			save(jdata.getString("data"));
			
		} catch (JSONException je)
		{
			Log.e(TAG, "Error parsing icon data!");
			return;
		}
	}

	public Favicon()
	{
		icon = null;
		id = -1;
	}
	
	// Adapted from 
	// http://stackoverflow.com/questions/10586953/base64-image-and-html-fromhtml-android
	public void save(String encoded_icon)
	{
		// Strings have a prefix that confuses the parser 'image/png;base64,' that we need to strip off
		// Ref http://stackoverflow.com/questions/11388018/phonegap-plugin-to-convert-base64-string-to-a-png-image-in-android/11388019#11388019
		String justData = encoded_icon.substring(encoded_icon.indexOf(",") + 1);
		
		try
		{
			byte[] data = Base64.decode(justData, Base64.DEFAULT);
			Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
			this.icon = new BitmapDrawable(bitmap);
		}
		catch (IllegalArgumentException iae)
		{
			Log.e(TAG, "Error parsing favicon, drat");
		}
	}
}
