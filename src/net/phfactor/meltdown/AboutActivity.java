package net.phfactor.meltdown;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
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
        MeltdownApp mapp = (MeltdownApp) getApplication();
        TextView tv = (TextView) findViewById(R.id.tvAppVersion);
        tv.setText(mapp.getAppVersion());
    
        // Parse the URLs, make 'em clickable. It's only polite.
        tv = (TextView) findViewById(R.id.tvVanity);
        tv.setText(Html.fromHtml(getString(R.string.vanityBlurb)));
        tv.setClickable(true);

        String disp_string = "";
        ConfigFile conf = new ConfigFile(this);
        
        disp_string += "Server URL: " + conf.getURL();
        disp_string += "\nLast refresh " + DateUtils.getRelativeTimeSpanString(1000L * mapp.getLast_refresh_time());
        disp_string += "\n" + mapp.getNumItems() + " unread items in " + (mapp.getUnreadGroups().size()) + " groups";
        disp_string += "\n" + mapp.getFileCount() + " cached posts on disk";
        
        tv = (TextView) findViewById(R.id.tvVerbiage);
        tv.setText(disp_string);
    }
}
