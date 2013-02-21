package net.phfactor.meltdown;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class AboutActivity extends Activity implements OnClickListener 
{
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        fillInFields();
    }
    
    private void fillInFields()
    {        
        MeltdownApp mapp = (MeltdownApp) getApplication();
        TextView tv = (TextView) findViewById(R.id.tvAppVersion);
        tv.setText(mapp.getAppVersion());
    
        // Parse the URLs, make 'em clickable. It's only polite.
        tv = (TextView) findViewById(R.id.tvVanity);
        tv.setText(Html.fromHtml(getString(R.string.vanityBlurb)));        
        tv.setClickable(true);
        tv.setOnClickListener(this);
        
        String disp_string = "";
        ConfigFile conf = new ConfigFile(this);
        
        disp_string += "Server URL: " + conf.getURL();
        
        disp_string += "\nLast refresh " + DateUtils.getRelativeTimeSpanString(1000L * mapp.get_last_refresh_time());
        if (mapp.isNetDown())
        	disp_string += " - network down.";
        else
        	disp_string += " - network up.";

        disp_string += "\n" + mapp.totalUnreadItems() + " unread items in " + (mapp.getUnreadGroups().size()) + " groups";
        disp_string += " and " + mapp.getFeedCount() + " feeds";
        disp_string += "\n" + mapp.getFileCount() + " cached posts on disk";
        
        if (conf.getUpdateInterval() > 0L)
        	disp_string += "\nUpdate interval: " + DateUtils.formatElapsedTime(conf.getUpdateInterval() / 1000L);
        else
        	disp_string += "\nUpdates: manual only";
        
        tv = (TextView) findViewById(R.id.tvVerbiage);
        tv.setText(disp_string);
    }


	@Override
	public void onClick(View v)
	{
		// This is a minor sin - send every click to Github project page. 
		// TODO FIXME you schlub!
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse("http://github.com/phubbard/Meltdown"));
		startActivity(intent);				
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case android.R.id.home:
			finish();
			return true;
		}
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.activity_about, menu);
		return true;
	}	
}
