package net.phfactor.meltdown;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.widget.Toast;

public class GroupsActivity extends Activity 
{
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);

        RestClient rc = new RestClient(this);
        
        // Check for login, run prefs
        if (rc.haveSetup() == false)
        {
        	startActivity(new Intent(this, SetupActivity.class));
        	Toast.makeText(this, "Please configure a server", Toast.LENGTH_SHORT).show();
        }
        
        // try login
        setContentView(R.layout.activity_groups);
        
        if (rc.tryLogin())
        	Toast.makeText(this, "Logged in OK!", Toast.LENGTH_LONG).show();
        else
        {
        	Toast.makeText(this, "Error on login. Please check preferences.", Toast.LENGTH_LONG).show();
        	startActivity(new Intent(this, SetupActivity.class));
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        getMenuInflater().inflate(R.menu.activity_groups, menu);
        return true;
    }
}
