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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
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
	
	private MeltdownApp mapp;
	private String auth_token;
	
	public String last_result;
	
	public RestClient(MeltdownApp g_app)
	{
		mapp = g_app;
		auth_token = g_app.getToken();
	}
	
	public Boolean checkAuth()
	{
		String payload = syncGetUrl(mapp.getAPIUrl());
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

	public String fetchGroups()
	{
		String url = String.format(mapp.getAPIUrl() + "&groups");
		String content = syncGetUrl(url);
		if (content != null)
			mapp.updateTimestamp();
		
		return content;
	}
	
	public String fetchFeeds()
	{
		String url = String.format(mapp.getAPIUrl() + "&feeds");
		return(syncGetUrl(url));
	}
	
	public String fetchUnreadList()
	{
		String url = String.format(mapp.getAPIUrl() + "&unread_item_ids");
		return(syncGetUrl(url));	
	}
	
	// TODO Write Me!
	private String makeItemListURL(List<Integer> ids)
	{
		String idstr = mapp.getAPIUrl() + "&items&with_ids=";
		for (int idx = 0; idx < ids.size(); idx++)
			idstr += String.format("%d,", ids.get(idx));
		
		// Remove trailing comma
		return (idstr.substring(0, idstr.length() - 1));
	}
	
	public String fetchListOfItems(List<Integer> ids)
	{
		if (ids.size() == 0)
			return null;
		
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
			HttpPost post = new HttpPost(mapp.getAPIUrl());
			
			//Log.d(TAG, "URL: " + mapp.getAPIUrl() + " vars: " + variables);
			 
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
	
	// This took forever to get working. Change with great caution if at all.
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
