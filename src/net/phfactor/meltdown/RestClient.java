package net.phfactor.meltdown;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/*!
 * @file RestClient.java
 * @author Paul Hubbard
 * @brief REST/HTTP code for Meltdown
 * @see http://feedafever.com/api
 */

public class RestClient 
{
	static final String TAG = "MeltdownRestClient";

	private Context ctx;
	private String auth_token;
	private String base_url;
	SharedPreferences prefs;

	public RestClient(Context context)
	{
		ctx = context;
		prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		
		auth_token = makeAuthToken();
		base_url = getAPIUrl();
	}

	// *****************************************************************************
	//! @see http://stackoverflow.com/questions/8700744/md5-with-android-and-php
	public static final String md5(final String s) 
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

	protected String makeAuthToken()
	{
		String pre = String.format("%s:%s", 
				prefs.getString("prefUsername", "user@example.com"),
				prefs.getString("prefPassword", "password"));
		return md5(pre);
	}
	
	protected String getAPIUrl()
	{
		return prefs.getString("prefServerUrl", "http://example.com/fever/") + "/?api";
	}
	
	//! @see http://stackoverflow.com/questions/8119225/httppost-from-android-to-asp-net-page
	protected Boolean tryLogin()
	{
		HttpResponse response;
		JSONObject jsonObj;
		
		String req_url = base_url + "&groups";
		HttpClient client = new DefaultHttpClient();
		HttpPost poster = new HttpPost(req_url);
		HttpParams params = new BasicHttpParams();
		params.setParameter("api_key", auth_token);
		poster.setParams(params);
		try {
			response = client.execute(poster);
			try {
				jsonObj = new JSONObject(response.getEntity().toString());
				
				if (jsonObj.getInt("auth") == 1)
					return true;
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (ClientProtocolException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
}
