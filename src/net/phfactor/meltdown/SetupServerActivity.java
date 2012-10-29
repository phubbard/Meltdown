package net.phfactor.meltdown;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class SetupServerActivity extends Activity implements OnClickListener 
{
	private MeltdownApp mapp;
	private ConfigFile auth;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.setup_server);
		Button testBtn = (Button) findViewById(R.id.sbTest);
		testBtn.setOnClickListener(this);
		
		mapp = (MeltdownApp) getApplication();
		auth = new ConfigFile(this);
		
		if (mapp.isAppConfigured())
		{
			TextView tv = (TextView) findViewById(R.id.sServerUrl);
			tv.setText(auth.getURL());
		}
	}
	
	@Override
	public void onClick(View v) 
	{
		// Pull values out of view and save
		String url, email, password;
		TextView tv = (TextView) findViewById(R.id.sServerUrl);
		url = tv.getText().toString();
		tv = (TextView) findViewById(R.id.sEmail);
		email = tv.getText().toString();
		tv = (TextView) findViewById(R.id.sPass);
		password = tv.getText().toString();
		
		auth.setConfig(url, email, password);
		
		if (mapp.isAppConfigured())
		{
			Toast.makeText(SetupServerActivity.this, "Logged in OK!", Toast.LENGTH_SHORT).show();
			mapp.startUpdates();
			SetupServerActivity.this.finish();			
		}
		else
			Toast.makeText(SetupServerActivity.this, "Login error, please correct data", Toast.LENGTH_LONG).show();			
	}
}
