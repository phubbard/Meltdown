package net.phfactor.meltdown;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.ConditionVariable;
import android.util.Log;

/*!
 * @file RestClient.java
 * @author Paul Hubbard
 * @brief REST/HTTP code for Meltdown
 * @see http://feedafever.com/api
 * 
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
		String payload = syncGetUrl(api_url, null);
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
		return syncSetVariables("groups");
	}
	
	public String fetchFeeds()
	{
		return syncSetVariables("feeds");
	}
	
	public String fetchFavicons()
	{
		return syncSetVariables("favicons");
	}
	
	public String fetchUnreadList()
	{
		return syncSetVariables("unread_item_ids");
	}
	
	// Construct an http variable list with a list of item ids. Max length is 50 as per Fever API.
	private String makeItemVarList(List<Integer> ids)
	{
		String idstr = "items&with_ids=";
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
		String vars = makeItemVarList(ids);
		return (syncSetVariables(vars));
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
		final String vars = String.format(Locale.ENGLISH, "mark=item&as=saved&id=%d", item_id);
		
		class mTask extends AsyncTask<Void, Void, Void> 
		{
			@Override
			protected Void doInBackground(Void... params) 			
			{
				syncSetVariables(vars);
				return null;
			}
		}
		
		new mTask().execute();		
	}
	
	// Call the users' hook URL, async. Ignore response and/or errors.
	protected void callUserURL(final String user_url)
	{
		class mTask extends AsyncTask<Void, Void, Void> 
		{
			@Override
			protected Void doInBackground(Void... params)
			{
				try 
				{
					HttpURLConnection connection;
					URL uurl = new URL(user_url);
					connection = (HttpURLConnection) uurl.openConnection();
					connection.connect();
					connection.disconnect();
//					
//					HttpClient client = AndroidHttpClient.newInstance("Meltdown");
//					HttpGet get = new HttpGet(user_url);
//					client.execute(get);
//					AndroidHttpClient fcc = (AndroidHttpClient) client;
//					fcc.close();
					return null;
				} catch (MalformedURLException e)
				{
					Log.e(TAG, "User URL badly formed, cannot invoke", e);
					
				} catch (IOException e)
				{
					Log.e(TAG, "Error on user hook call", e);
				}
				return null;
			}			
		}
		
		new mTask().execute();
	}
	
	// Asynchronously mark a post as read.
	public void markItemRead(int post_id)
	{
		final String vars = String.format(Locale.ENGLISH, "mark=item&as=read&id=%d", post_id);
		
		class mTask extends AsyncTask<Void, Void, Void> 
		{
			@Override
			protected Void doInBackground(Void... params) 			
			{
				syncSetVariables(vars);
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
				syncSetVariables(vars);
				return null;
			}
		}
		
		new mTask().execute();		
	}
	
	// Some calls just call the base API with a list of parameters - make it easy to do so.
	public String syncSetVariables(String vars)
	{
		return syncGetUrl(api_url, vars);
	}

	/*
	 *  New dev 1/13/13, working on SSL support and in RTFM the first thing to do is to switch
	 *  from AndroidHttpClient to HttpURLConnection:
	 *  http://android-developers.blogspot.com/2011/09/androids-http-clients.html
	 *  
	 *  and
	 *  http://developer.android.com/reference/java/net/HttpURLConnection.html
	 *  
	 *   So let's start there, replace the current methods and swap the API. Once that's done
	 *   I can tackle http/s support.
	 */
	public String syncGetUrl(String url_string, String variables)
	{
		HttpURLConnection connection;
		
		if (variables != null)
			url_string += "&" + variables;
		
		Log.d(TAG, url_string);
		
		try
		{
			URL mURL = new URL(url_string);
			connection = (HttpURLConnection) mURL.openConnection();
						
			/*
			 * The auth method in Fever is peculiar; you have to use POST, even for GET calls,
			 * and the API key cannot be sent as a header. I'd call that a bug, pure and simple.
			 */			
			connection.setDoOutput(true);
			connection.setReadTimeout(15000); // TODO Make this a pref or adaptive? 
			connection.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
			connection.setRequestMethod("POST");
			
			connection.connect();
			connection.getOutputStream().write(("api_key=" + auth_token).getBytes());
		}
		catch (MalformedURLException me)
		{
			Log.e(TAG, "Bad server URL '" + url_string + "'");
			return null;
		}
		catch (IOException ie)
		{
			Log.e(TAG, "IO error on transfer: " + ie.getMessage());
			return null;
		}
		
		// OK, should be connected at this point, try and read the response.
		// TODO Here is where we could check headers for Last-Modified - see
		// http://developer.android.com/training/efficient-downloads/redundant_redundant.html
		try
		{
			InputStream in = new BufferedInputStream(connection.getInputStream());
			return convertStreamToString(in);
		} catch (IOException e)
		{
			Log.e(TAG, "IO error on transfer: " + e.getMessage());			
		}
		finally
		{
			connection.disconnect();
		}
		
		return null;
	}
}
