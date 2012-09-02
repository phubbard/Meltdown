package net.phfactor.meltdown;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class ItemDisplayActivity extends Activity {

    private MeltdownApp app;
    private RestClient rclient;
    
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_display);
        
        // Create class-wide objects
		app = (MeltdownApp) this.getApplicationContext();
        rclient = new RestClient(this);

        // Pull out the item ID
        int post_id = getIntent().getExtras().getInt("post_id");

		// Off we go!
		displayItem(post_id);
    }

    protected void displayItem(int item_id)
    {
        RssItem item = app.findPostById(item_id);
        WebView wv = (WebView) findViewById(R.id.itemWebView);
        wv.loadData(item.html, "text/html", null);
    }
}
