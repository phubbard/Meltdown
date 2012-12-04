package net.phfactor.meltdown;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.ConditionVariable;
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
	
	private String auth_token;
	private String api_url;
	
	public String last_result;
	
	protected Boolean login_ok;
	ConditionVariable condv;
	
	public RestClient(String token, String url)
	{
		auth_token = token;
		api_url = url;
	}
	
	// callback from login checker, saves result and unlocks condition variable
	protected void setLoginResult(Boolean result)
	{
		login_ok = result;
		condv.open();
	}
	
	// Verify that our credentials are correct by opening the API URL. If they are, 
	// we'll get 'auth:1' in the result. We also check for the min API version (3).
	public Boolean verifyLogin()
	{
		login_ok = false;
		condv = new ConditionVariable();

		class ltask extends AsyncTask<Void, Void, Void>
		{
			protected Void doInBackground(Void... params)
			{
				setLoginResult(checkAuth());
				return null;
			}
		}
		new ltask().execute();
		
		if (!condv.block(10000L))
			Log.w(TAG, "Timed out on login check!");
		return login_ok;
	}
	
	// Validate that the auth token is correct and that we have at least API level 3.
	private Boolean checkAuth()
	{
		String payload = syncGetUrl(api_url);
		JSONObject jsonObj;
		
		if (payload == null)
			return false;
		
		try 
		{
			jsonObj = new JSONObject(payload);

			if (jsonObj.getInt("auth") == 1)
				if (jsonObj.getInt("api_version") >= 3)
					return true;

		} catch (JSONException e) 
		{
			e.printStackTrace();
		}	
		return false;
	}

	// Sync methods - only called from the IntentService, so OK to block.
	public String fetchGroups()
	{
		String url = String.format(api_url + "&groups");
		return syncGetUrl(url);
	}
	
	public String fetchFeeds()
	{
		String url = String.format(api_url + "&feeds");
		return syncGetUrl(url);
	}
	
	public String fetchFavicons()
	{
		String url = String.format(api_url + "&favicons");
		return syncGetUrl(url);
	}
	
	public String fetchUnreadList()
	{
		String url = String.format(api_url + "&unread_item_ids");
		return syncGetUrl(url);	
	}
	
	private String makeItemListURL(List<Integer> ids)
	{
		String idstr = api_url + "&items&with_ids=";
		for (int idx = 0; idx < ids.size(); idx++)
			idstr += String.format("%d,", ids.get(idx));
		
		// Remove trailing comma
		return (idstr.substring(0, idstr.length() - 1));
	}
	
	public String fetchListOfItems(List<Integer> ids)
	{
		if (ids.size() == 0)
			return null;
		
		Log.d(TAG, "Fetching " + ids.size() + " items from server");
		String url = makeItemListURL(ids);
		return (syncGetUrl(url));
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
	
	// Asynchronously mark a post as saved
	public void markItemSaved(int item_id)
	{
		final String vars = String.format("mark=item&as=saved&id=%d", item_id);
		
		class mTask extends AsyncTask<Void, Void, Void> {

			@Override
			protected Void doInBackground(Void... params) 			
			{
				syncPostUrl(vars);
				return null;
			}
		}
		
		new mTask().execute();		
	}
	
	// Call the users' hook URL, async. Ignore response and/or errors.
	protected void callUserURL(final String user_url)
	{
		class mTask extends AsyncTask<Void, Void, Void> {

			@Override
			protected Void doInBackground(Void... params)
			{
				try 
				{
					HttpClient client = AndroidHttpClient.newInstance("Meltdown");
					HttpGet get = new HttpGet(user_url);
					client.execute(get);
					AndroidHttpClient fcc = (AndroidHttpClient) client;
					fcc.close();
					return null;
				} catch (ClientProtocolException e)
				{
				} catch (IOException e)
				{
				}
				return null;
			}			
		}
		
		new mTask().execute();
	}
	
	// Asynchronously mark a post as read.
	/*
	 *  The post args have to be in the body. 
	 *  POST:	api_key=blah&mark=item&as=read&id=57163
	 */
	public void markItemRead(int post_id)
	{
		final String vars = String.format("mark=item&as=read&id=%d", post_id);
		
		class mTask extends AsyncTask<Void, Void, Void> {

			@Override
			protected Void doInBackground(Void... params) 			
			{
				syncPostUrl(vars);
				return null;
			}
		}
		new mTask().execute();		
	}

	/* Mark a group as read. Note that, because are required to include your last-fetch timestamp
	 * in the request, some items may escape being marked as read. Sort of a race condition.
	 */
	public void markGroupRead(int group_id, long last_pull_timestamp)
	{
		final String vars = String.format("mark=group&as=read&id=%d&before=%d", group_id, last_pull_timestamp);
		class mTask extends AsyncTask<Void, Void, Void> {

			@Override
			protected Void doInBackground(Void... params) 			
			{
				syncPostUrl(vars);
				return null;
			}
		}
		
		new mTask().execute();		
	}
	
	/*
	 *  Specialization of syncGetUrl that puts variables into payload, as seems to be required.
	 *  Variables string must be url-encoded e.g. 'mark=as&id=1234' *without* leading ampersand.
	 */
	public String syncPostUrl(String variables)
	{
		HttpClient client;	
		String content = "";
		String Error = null;
		
		try 
		{
			client = AndroidHttpClient.newInstance("Meltdown");
			HttpPost post = new HttpPost(api_url);
			
			// Log.d(TAG, "URL: " + api_url + " vars: " + variables);
			 
			// Tell Apache we'll take gzip; should compress really well.
			AndroidHttpClient.modifyRequestToAcceptGzipResponse(post);
			
			// Add the auth token to the request
			post.setHeader("Content-Type", "application/x-www-form-urlencoded");
			StringEntity payload;
			String full_post_vars = String.format("api_key=%s&%s", auth_token, variables);
			payload = new StringEntity(full_post_vars, "UTF-8");
			//Log.d(TAG, "Payload: " + full_post_vars);
			post.setEntity(payload);				
	
			//Log.d(TAG, "executing post...");
			HttpResponse response = client.execute(post);
			
			//Log.d(TAG, "parsing response");
			InputStream istr = AndroidHttpClient.getUngzippedContent(response.getEntity());
			content = convertStreamToString(istr);
			
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
			Error = "General exception: "+e.getMessage() + " " + e.toString();
		}
		
		if (Error != null)
			Log.e(TAG, Error);
		
		return null;		
	}
	
	// This took forever to get working. Change with great caution if at all. Adds the mandatory
	// access token to the HTTP header.
	protected HttpPost addAuth(HttpPost post_request) throws UnsupportedEncodingException
	{
		post_request.setHeader("Content-Type", "application/x-www-form-urlencoded");
		StringEntity payload;
		payload = new StringEntity(String.format("api_key=%s", auth_token), "UTF-8");
		post_request.setEntity(payload);	
		return post_request;
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
	
			//Log.d(TAG, "executing post...");
			HttpResponse response = client.execute(post);
			
			//Log.d(TAG, "parsing response");
			InputStream istr = AndroidHttpClient.getUngzippedContent(response.getEntity());
			content = convertStreamToString(istr);
			
//			ResponseHandler<String> responseHandler = new BasicResponseHandler();
//			content = client.execute(post, responseHandler);
//			Log.d("DATA for  " + url, content);
			
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
			Error = "General exception: "+e.getMessage() + " " + e.toString();
		}
		
		if (Error != null)
			Log.e(TAG, Error);
		
		return null;		
	}
}
