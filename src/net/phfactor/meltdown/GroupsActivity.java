package net.phfactor.meltdown;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.widget.Toast;

public class GroupsActivity extends Activity 
{
	private SharedPreferences prefs;
	
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // Check for login, run prefs        
        if (haveSetup() == true)
        {
        	startActivity(new Intent(this, PreferencesActivity.class));
        	Toast.makeText(this, "Please configure a server", Toast.LENGTH_SHORT).show();
        }
        
        // try login
        setContentView(R.layout.activity_groups);
        
        RestClient rc = new RestClient(this);
        if (rc.tryLogin())
        	Toast.makeText(this, "Logged in OK!", Toast.LENGTH_LONG).show();
        else
        {
        	Toast.makeText(this, "Error on login. Please check preferences.", Toast.LENGTH_LONG).show();
        	startActivity(new Intent(this, PreferencesActivity.class));
        }
    }

    private Boolean haveSetup()
    {
    	if (prefs.getString("prefUsername", null) == null)
    		return false;
    	
    	if (prefs.getString("prefServerUrl", null) == null)
    		return false;
    	
    	if (prefs.getString("prefPassword", null) == null)
    		return false;
    	
    	return true;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_groups, menu);
        return true;
    }

    
}
