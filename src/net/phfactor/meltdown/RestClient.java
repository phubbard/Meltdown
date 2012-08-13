package net.phfactor.meltdown;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.http.AndroidHttpClient;
import android.util.Log;

/*!
 * @file RestClient.java
 * @author Paul Hubbard
 * @brief REST/HTTP code for Meltdown
 * @see http://feedafever.com/api
 * 
 * Done Add gzip support http://stackoverflow.com/questions/1573391/android-http-communication-should-use-accept-encoding-gzip
 * TODO Add progress indicator http://stackoverflow.com/questions/3028306/download-a-file-with-android-and-showing-the-progress-in-a-progressdialog/3028660#3028660
 * Note its getEntity that does the actual fetch, interesting. http://stackoverflow.com/questions/6751241/httpentity-getcontent-progress-indicator
 * 
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
	
	public Boolean checkAuth()
	{
		String payload = syncGetUrl(getAPIUrl());
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

	public Boolean tryLogin()
	{
		return checkAuth();
	}
	
	public String fetchGroups()
	{
		String url = String.format(getAPIUrl() + "&groups");
		String content = syncGetUrl(url);
		if (content != null)
			updateTimestamp();
		
		return content;
	}
	
	public String fetchFeeds()
	{
		String url = String.format(getAPIUrl() + "&feeds");
		return(syncGetUrl(url));
	}
	
	public String fetchSomeFeeds(int max_read_id)
	{
		// TODO
		/* As per API, request a chunk of feed items. Chunksize depends on the server and
		 * number of unread in the queue. The output of this is the content, which is then fed
		 * into the parser. How do I cleanly detect end of items? And max item number?
		 */
		String url = String.format("%s&items&max_id=%d", getAPIUrl(), max_read_id);
		return syncGetUrl(url);
	}
	
    /*!
     * @brief To convert the InputStream to String we use the BufferedReader.readLine()
     * method. We iterate until the BufferedReader return null which means
     * there's no more data to read. Each line will appended to a StringBuilder
     * and returned as String.
     */
	private static String convertStreamToString(InputStream is) 
	{
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
 
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
	
	// Blocking fetch w/authentication added
	public String syncGetUrl(String url)
	{
		HttpClient client;	
		String content = "";
		String Error = null;
		
		try 
		{
			client = AndroidHttpClient.newInstance("Meltdown");
			HttpPost post = new HttpPost(url);
			
			// Tell Apache we'll take gzip; should compress really well.
			AndroidHttpClient.modifyRequestToAcceptGzipResponse(post);
			
			// Add the auth token to the request
			post = addAuth(post);
	
			HttpResponse response = client.execute(post);
			InputStream istr = AndroidHttpClient.getUngzippedContent(response.getEntity());
			content = convertStreamToString(istr);
			
//			ResponseHandler<String> responseHandler = new BasicResponseHandler();
//			content = client.execute(post, responseHandler);
			Log.d("DATA for  " + url, content);
			
			AndroidHttpClient fcc = (AndroidHttpClient) client;
			fcc.close();
			
			return content;
		} catch (ClientProtocolException e) 
		{
			Error = "Prot Err: " + e.getMessage();
		}
		catch (UnknownHostException e) 
		{
			Error = "UnknownHostErr: " + e.getMessage();
		}
		catch (IOException e) 
		{
			Error = "IOxErr: " + e.getMessage();
		}
		catch (Exception e) 
		{
			Error = "Exception. "+e.getMessage();
		}
		
		if (Error != null)
			Log.e(TAG, Error);
		
		return null;		
	}
}
