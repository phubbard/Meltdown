package net.phfactor.meltdown;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class SetupPostURL extends Activity implements OnClickListener 
{
	ConfigFile cfg;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setup_post_url);
		
        ActionBar bar = getActionBar();
		bar.setDisplayHomeAsUpEnabled(true);
        bar.setTitle(getString(R.string.title_activity_setup_post_url));
        
		cfg = new ConfigFile(this);
		TextView tv = (TextView) findViewById(R.id.spURL);
		String url = cfg.getUserPostURL();
		if (url != null)
			tv.setText(url);
		
		Button btn = (Button) findViewById(R.id.btnSavePost);
		btn.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) 
	{
		// Pull values out of view and save
		EditText tv = (EditText) findViewById(R.id.spURL);

		String url = tv.getText().toString();
		cfg.setUserPostURL(url);
		finish();
	}

}
