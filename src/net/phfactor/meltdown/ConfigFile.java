package net.phfactor.meltdown;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.app.AlarmManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

// Move the MD5 and prefs stuff here from App class; unrelated concerns
public class ConfigFile
{
	static final String TAG = "MeltdownConfig";
	
	private static final String P_URL = "serverUrl";
	private static final String P_TOKEN = "token";
	private static final String P_POST_URL = "postUrl";
	private static final String P_INTERVAL = "interval"; // Must match preference key
	private static final String P_DISABLE_SPARKS = "disable_sparks";

	private SharedPreferences prefs;
	private SharedPreferences.Editor editor;

	public ConfigFile(Context ctx)
	{
		this.prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
	}

	public void setConfig(String url, String email, String password)
	{
		editor = prefs.edit();
		editor.putString(P_URL, url);
		editor.putString(P_TOKEN, makeAuthToken(email, password));
		editor.commit();
	}

	protected void setUpdateInterval(Long new_interval)
	{
		editor = prefs.edit();
		editor.putLong(P_INTERVAL, new_interval);
		editor.commit();
	}
	
	protected Long getUpdateInterval()
	{
		String saved_val = prefs.getString(P_INTERVAL, null);
		if (saved_val == null)
			return AlarmManager.INTERVAL_FIFTEEN_MINUTES;
			
		return Long.parseLong(saved_val);
	}
	
	protected String makeAuthToken(String email, String pass)
	{
		String pre = String.format("%s:%s", email, pass);
		return md5(pre);
	}
	
	protected String getAPIUrl()
	{
		return getURL() + "/?api";
	}
		
	// Are the config values set, and are they correct?
	public Boolean haveConfigInfo()
	{
		if (prefs.getString(P_URL, null) == null)
			return false;
		if (prefs.getString(P_TOKEN, null) == null)
			return false;
		return true;
	}

	public boolean getDisableSparks() {
		return prefs.getBoolean(P_DISABLE_SPARKS, false);
	}

	public String getToken()
	{
		return prefs.getString(P_TOKEN, null);
	}
	
	public String getURL()
	{
		return prefs.getString(P_URL, null);
	}
	
	public String getUserPostURL()
	{
		return prefs.getString(P_POST_URL, null);
	}
	
	protected void setUserPostURL(String user_url)
	{
		editor = prefs.edit();
		editor.putString(P_POST_URL, user_url);
		editor.commit();
	}
	
	//! @see http://stackoverflow.com/questions/8700744/md5-with-android-and-php
	private static final String md5(final String s) 
	{
		try 
		{
			// Create MD5 Hash
			MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
			try 
			{
				digest.update(s.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) 
			{
				Log.e(TAG, "Error encoding into UTF-8!", e);
				e.printStackTrace();
			}
			byte messageDigest[] = digest.digest();

			// Create Hex String
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < messageDigest.length; i++) {
				String h = Integer.toHexString(0xFF & messageDigest[i]);
				while (h.length() < 2)
					h = "0" + h;
				hexString.append(h);
			}
			return hexString.toString();

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return "";
	}		
}
