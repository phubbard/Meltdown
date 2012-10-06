package net.phfactor.meltdown;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ShareActionProvider;
import android.widget.TextView;

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
        if (rss_item == null)
        {
        	finish();
        	return;
        }
        
        getActionBar().setTitle(rss_item.title);
        
        // Lookup feed name and display it between the buttons
        TextView tv = (TextView) findViewById(R.id.itmFeedTitle);

        // Feed title - currently in footer
        RssFeed rgrp = app.findFeedById(rss_item.feed_id);
        tv.setText(rgrp.title);

        // New top-of-screen title - experimental
        tv = (TextView) findViewById(R.id.itmItemTitle);
        tv.setText(rss_item.title);     
        tv.setBackgroundColor(Color.LTGRAY);

        // TODO Change action bar icon to feeds' favicon
        WebView wv = (WebView) findViewById(R.id.itemWebView);
        
        // Note that the most-basic load from file inserts garbage characters-
        //wv.loadData(rss_item.getHTML(getApplicationContext()), "text/html", "UTF-8");
        
        // See http://stackoverflow.com/questions/3150400/html-list-tag-not-working-in-android-textview-what-can-i-do
        // This works. Workaround.
        wv.loadDataWithBaseURL(null, rss_item.getHTML(getApplicationContext()), "text/html", "utf-8", null);
    }
    
    // See http://android-developers.blogspot.com/2012/02/share-with-intents.html
    private Intent createShareIntent()
    {
    	Intent shareIntent = new Intent(Intent.ACTION_SEND);
    	shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
    	shareIntent.setType("text/plain");
    	shareIntent.putExtra(Intent.EXTRA_SUBJECT, rss_item.title);
    	
    	String shareBody = String.format("%s\n\n -- Shared from Meltdown RSS Reader", rss_item.url);
    	shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
    	return shareIntent;
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
    {
    	MenuInflater infl = getMenuInflater();
    	infl.inflate(R.menu.activity_item, menu);
    	
        // Get the menu item.
        MenuItem menuItem = menu.findItem(R.id.itemShare);
        // Get the provider and hold onto it to set/change the share intent.
        ShareActionProvider mShareActionProvider = (ShareActionProvider) menuItem.getActionProvider();

        // Attach an intent to this ShareActionProvider.  You can update this at any time,
        // like when the user selects a new piece of data they might like to share.
        mShareActionProvider.setShareIntent(createShareIntent());
    	
    	return true;
	}
    
	private void nextItem()
	{
		// Mark-as-read is async/background task
		app.markItemRead(cur_post, MeltdownApp.GROUP_UNKNOWN);
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
			
		case R.id.itemSave:
			app.markItemSaved(rss_item.id);
			nextItem();
			return true;
		}
		return false;
	}
}
