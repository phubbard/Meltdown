package net.phfactor.meltdown;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

/*!
 * @file RestClient.java
 * @author Paul Hubbard
 * @brief REST/HTTP code for Meltdown
 * @see http://feedafever.com/api
 */

public class RestClient 
{
	static final String TAG = "MeltdownRestClient";

	private static final String P_URL = "serverUrl";
	private static final String P_EMAIL = "email";
	private static final String P_PASS = "pass";
	private static final String P_LAST_FETCH = "last_ts";
	private final SharedPreferences prefs;
	private SharedPreferences.Editor editor;
	
	private Context ctx;
	private String auth_token;
	private String base_url;
	protected RestCallback callback;
	
	public String last_result;
	
	public RestClient(Context context)
	{
		ctx = context;
		prefs = ctx.getSharedPreferences(TAG, Context.MODE_PRIVATE);
		
		doSetup();
	}

	// *****************************************************************************
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

	public void setConfig(String url, String email, String password)
	{
		editor = prefs.edit();
		editor.putString(P_URL, url);
		editor.putString(P_EMAIL, email);
		editor.putString(P_PASS, password);
		editor.commit();
		
		doSetup();
	}
	
	protected String getURL()
	{
		return prefs.getString(P_URL, null);
	}
	
	protected String getEmail()
	{
		return prefs.getString(P_EMAIL, null);
	}
	
	protected String getPass()
	{
		return prefs.getString(P_PASS, null);
	}
	
	protected String getAPIUrl()
	{
		return getURL() + "/?api";
	}
		
	private void updateTimestamp()
	{
		editor = prefs.edit();
		editor.putLong(P_LAST_FETCH, System.currentTimeMillis() / 1000L);
		editor.commit();
	}
	
	public long getLastFetchTime()
	{
		return prefs.getLong(P_LAST_FETCH, 0L);
	}
	
	private void doSetup()
	{
		base_url = getAPIUrl();
		auth_token = makeAuthToken();
		
		last_result = "";
	}
	
	public Boolean haveSetup()
	{
		if (prefs.getString(P_URL, null) == null)
			return false;
		if (prefs.getString(P_EMAIL, null) == null)
			return false;
		if (prefs.getString(P_PASS, null) == null)
			return false;
		
		return true;
	}
	
	protected String makeAuthToken()
	{
		String pre = String.format("%s:%s", getEmail(), getPass());
		return md5(pre);
	}
	
	// This took forever to get working. Change with great caution if at all.
	protected HttpPost addAuth(HttpPost post_request) throws UnsupportedEncodingException
	{
		post_request.setHeader("Content-Type", "application/x-www-form-urlencoded");
		StringEntity payload;
		payload = new StringEntity(String.format("api_key=%s", auth_token), "UTF-8");
		post_request.setEntity(payload);	
		return post_request;
	}
	
	public Boolean checkAuth(String payload)
	{
		JSONObject jsonObj;
		
		try 
		{
			jsonObj = new JSONObject(payload);

			if (jsonObj.getInt("auth") == 1)
				return true;

		} catch (JSONException e) 
		{
			e.printStackTrace();
		}	
		return false;
	}

	public void tryLogin(RestCallback cb_hook)
	{
		grabURL(getAPIUrl(), cb_hook);
	}
	
	public void fetchGroups(RestCallback cb_hook)
	{
		String url = String.format(getAPIUrl() + "&groups");
		grabURL(url, cb_hook);
		updateTimestamp();
	}
	
	// Async http code from Greg's InBoxActivity.java in the Smile project - nice work. Extended
	// a bit for a callback interface to return data.
	public void grabURL(String url, RestCallback cb_hook) 
	{
		callback = cb_hook;
		GrabURL gurl = new GrabURL();
		gurl.execute(url);
	}
	
	private class GrabURL extends AsyncTask<String, Void, Void> 
	{
		private HttpClient client;	
		private String content;
		private String Error = null;
		private ProgressDialog Dialog = new ProgressDialog(ctx);
		
		protected void onPreExecute() 
		{
			client = new DefaultHttpClient();
			content = "";
			
			Dialog.setIndeterminate(true);
			Dialog.setMessage("Fetching data...");
			Dialog.show();
		}

		protected Void doInBackground(String... urls) 
		{
			try 
			{
				Log.d(TAG, "Fetching " + urls[0]);
				HttpPost post = new HttpPost(urls[0]);
				
				// Add the auth token to the request
				post = addAuth(post);

				ResponseHandler<String> responseHandler = new BasicResponseHandler();
				content = client.execute(post, responseHandler);
				Log.d(TAG, "Got back: " + content);
				
			} catch (ClientProtocolException e) 
			{
				Error = "Prot Err: " + e.getMessage();
				cancel(true);
			}
			catch (UnknownHostException e) 
			{
				Error = "UnknownHostErr: " + e.getMessage();
				cancel(true);          
			}
			catch (IOException e) 
			{
				Error = "IOxErr: " + e.getMessage();
				cancel(true);
			}
			catch (Exception e) 
			{
				Error = "Exception. "+e.getMessage();
				cancel(true);
			}

			return null;
		}

		@Override
		protected void onCancelled() 
		{
			super.onCancelled();

			Dialog.dismiss();			
		}

		protected void onPostExecute(Void unused) 
		{
			Dialog.dismiss();

			if (Error != null) 
				Toast.makeText(ctx, Error, Toast.LENGTH_LONG).show();
			else
				callback.handleData(content);
		}  
	}		
}
