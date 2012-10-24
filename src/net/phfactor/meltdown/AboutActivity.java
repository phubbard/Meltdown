package net.phfactor.meltdown;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.widget.TextView;

public class AboutActivity extends Activity {

    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        fillInFields();
    }

    private void fillInFields()
    {        
        TextView tv = (TextView) findViewById(R.id.tvAppVersion);
        tv.setText(getAppVersion());
    
        String disp_string = "";
        MeltdownApp mapp = (MeltdownApp) getApplication();
        ConfigFile auth = new ConfigFile(this);
        
        // TODO Formatting and clickable URLs
        disp_string += getString(R.string.vanityBlurb);
        disp_string += "\n\nServer URL: " + auth.getURL();
        disp_string += "\nLast refresh " + DateUtils.getRelativeTimeSpanString(1000L * mapp.getLast_refresh_time());
        disp_string += "\n" + mapp.getNumItems() + " unread items in " + (mapp.getUnreadGroups().size()) + " groups";
        disp_string += "\n" + mapp.getFileCount() + " cached posts on disk";
        
        tv = (TextView) findViewById(R.id.tvVerbiage);
        tv.setText(disp_string);
    }
    
	public String getAppVersion()
	{
		PackageInfo pinfo;
		try {
			pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
		} catch (NameNotFoundException e) 
		{
			e.printStackTrace();
			return "unknown";
		}
		return "Version " + pinfo.versionName + " build " + pinfo.versionCode;
	}
}
