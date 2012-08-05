package net.phfactor.meltdown;

// For JSONArray to ListView I cribbed from
// http://p-xr.com/android-tutorial-how-to-parse-read-json-data-into-a-android-listview/


import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.widget.Toast;

public class GroupsActivity extends ListActivity 
{
	private MeltdownApp app;
	
	private ProgressDialog pd;
	private RestClient rc;
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);

		rc = new RestClient(this);
		app = new MeltdownApp(this);

		// TODO run setup if login errors out?
		// Check for login, run prefs
		if (rc.haveSetup() == false)
		{
			startActivity(new Intent(this, SetupActivity.class));
			Toast.makeText(this, "Please configure a server", Toast.LENGTH_SHORT).show();
		}

		setContentView(R.layout.list);
		
		pd = new ProgressDialog(this);
		
		// TODO proportional!
		pd.setIndeterminate(true);
		pd.setMessage("Fetching data...");
		pd.show();

		class GGTask extends AsyncTask<Void, Void, Void> {
			protected Void doInBackground(Void... args) {
				app.saveGroupsData(rc.fetchGroups());
				app.saveFeedsData(rc.fetchFeeds());
				return null;
			}
			@Override
			protected void onPostExecute(Void arg) {
				pd.dismiss();
			}
		}

		new GGTask().execute();
	}
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		getMenuInflater().inflate(R.menu.activity_groups, menu);
		return true;
	}
}
