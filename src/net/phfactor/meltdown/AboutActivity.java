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
        
        disp_string += getString(R.string.vanityBlurb);
        disp_string += "\n\nStats and information\nServer URL: " + mapp.getURL();
        disp_string += "\nLast refresh: " + DateUtils.getRelativeTimeSpanString(1000L * mapp.getLast_refresh_time());
        disp_string += "\nLast fetch: " + DateUtils.getRelativeTimeSpanString(1000L * mapp.getLastFetchTime());
        disp_string += "\n" + mapp.getNumItems() + " unread items";
        disp_string += "\nMax item ID: " + mapp.getMaxItemID();
        disp_string += "\n" + (mapp.getUnreadGroups().size()) + " groups with unread items";
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
		
		return pinfo.versionName + " build " + pinfo.versionCode;
	}
    
}
