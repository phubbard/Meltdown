package net.phfactor.meltdown;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class SetupActivity extends Activity implements OnClickListener 
{
	private RestClient rc;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.setup);
		Button testBtn = (Button) findViewById(R.id.sbTest);
		testBtn.setOnClickListener(this);
		
		rc = new RestClient(this);
		if (rc.haveSetup())
		{
			TextView tv = (TextView) findViewById(R.id.sServerUrl);
			tv.setText(rc.getURL());
			tv = (TextView) findViewById(R.id.sEmail);
			tv.setText(rc.getEmail());
			tv = (TextView) findViewById(R.id.sPass);
			tv.setText(rc.getPass()); 
		}
	}

	private class SetupDH extends RestCallback 
	{
		@Override
		public void handleData(String payload)
		{
			if (rc.checkAuth(payload))
			{
				Toast.makeText(SetupActivity.this, "Logged in OK!", Toast.LENGTH_SHORT).show();
				SetupActivity.this.finish();
			}
			else
				Toast.makeText(SetupActivity.this, "Login error, please correct data", Toast.LENGTH_LONG).show();
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
		
		rc.setConfig(url, email, password);

		SetupDH dh = new SetupDH();
		
		rc.tryLogin(dh);
	}
}
