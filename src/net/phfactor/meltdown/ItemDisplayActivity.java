package net.phfactor.meltdown;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;

public class ItemDisplayActivity extends Activity 
{
    private MeltdownApp app;
    private int cur_post;
    private RssItem rss_item;
    
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_display);
        
        // Create class-wide objects
		app = (MeltdownApp) this.getApplicationContext();

        // Pull out the item ID
        cur_post = getIntent().getExtras().getInt("post_id");

        // Setup buttons
        Button nextBtn = (Button) findViewById(R.id.itmBtnNext);
        Button openBtn = (Button) findViewById(R.id.itmBtnOpen);
        
        nextBtn.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v)
        	{
        		nextItem();
        	}
        });
        
        openBtn.setOnClickListener(new View.OnClickListener() {        	
			public void onClick(View v) 
			{
				loadItem();
			}
		});
        
		// Off we go!
		displayItem();
    }

    protected void displayItem()
    {
        rss_item = app.findPostById(cur_post);
        getActionBar().setTitle(rss_item.title);
        WebView wv = (WebView) findViewById(R.id.itemWebView);
        wv.loadData(rss_item.html, "text/html", null);
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
    {
    	MenuInflater infl = getMenuInflater();
    	infl.inflate(R.menu.activity_item_display, menu);
    	return true;
	}
    
	private void nextItem()
	{
		// Mark-as-read is async/background task
		app.markItemRead(cur_post);
		setResult(RESULT_OK);
		finish();
	}
	
	private void loadItem()
	{
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(rss_item.url));
		startActivity(intent);		
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch (item.getItemId())
		{
		case R.id.itemNextArticle:
			nextItem();
			return true;
			
		case R.id.menu_load_page:
			loadItem();
			return true;
		}
		return false;
	}
}
