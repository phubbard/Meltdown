package net.phfactor.meltdown;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;

public class ItemDisplayActivity extends Activity 
{
    private MeltdownApp app;
    private int cur_post;
    
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_display);
        
        // Create class-wide objects
		app = (MeltdownApp) this.getApplicationContext();

        // Pull out the item ID
        cur_post = getIntent().getExtras().getInt("post_id");

		// Off we go!
		displayItem();
    }

    protected void displayItem()
    {
        RssItem item = app.findPostById(cur_post);
        WebView wv = (WebView) findViewById(R.id.itemWebView);
        wv.loadData(item.html, "text/html", null);
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
    {
    	MenuInflater infl = getMenuInflater();
    	infl.inflate(R.menu.activity_item_display, menu);
    	return true;
	}
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch (item.getItemId())
		{
		case R.id.itemNextArticle:
			// Mark-as-read is async/background task
			app.markItemRead(cur_post);
			finish();
			return true;
		}
		return false;
	}
}
