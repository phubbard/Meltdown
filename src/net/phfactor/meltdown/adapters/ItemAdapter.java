package net.phfactor.meltdown.adapters;

import net.phfactor.meltdown.R;
import net.phfactor.meltdown.providers.ItemProvider;
import android.content.Context;
import android.database.Cursor;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

/*
 * Adapt a CP cursor of RssItems into a row into a fragment.
 */
public class ItemAdapter extends CursorAdapter
{

	static final String TAG = "MeltdownItemAdapter";
	static final int SNIPPET_LENGTH = 72;
	LayoutInflater inflater;

	// See http://stackoverflow.com/questions/12718831/update-item-height-when-modifying-text-size-in-listview
	String ZERO_WIDTH_SPACE = "\u200b";
	final String DOUBLE_BYTE_SPACE = "\u3000";	

	public ItemAdapter(Context context, Cursor c, int flags)
	{
		super(context, c, flags);
		
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		Log.d(TAG, "created");
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor)
	{
		Log.d(TAG, "bind");
		// Ignore favicon for now. TODO later.
		String title = cursor.getString(cursor.getColumnIndex(ItemProvider.C_TITLE));
		// FIXME resolve feed ID into feed name
		String feed = cursor.getString(cursor.getColumnIndex(ItemProvider.C_FEED_ID));		
		String excerpt = makeExcerpt(cursor.getString(cursor.getColumnIndex(ItemProvider.C_HTML)));
		Long tstamp = cursor.getLong(cursor.getColumnIndex(ItemProvider.C_CREATED_ON)) * 1000L;
		
		TextView tv = (TextView) view.findViewById(R.id.tvTitle);
		tv.setText(title);
		
		tv = (TextView) view.findViewById(R.id.tvBody);
		tv.setText(excerpt);
		
		tv = (TextView) view.findViewById(R.id.item_feed);
		tv.setText(feed);
		
		tv = (TextView) view.findViewById(R.id.item_timestamp);
		tv.setText(DateUtils.getRelativeTimeSpanString(tstamp));
	}

    private String makeExcerpt(String full_html)
    {
        String descr = removeTags(full_html);
        String rc = descr.substring(0, Math.min(descr.length(), SNIPPET_LENGTH)) + "...";
        return rc;
    }
    
    /**
     * Simple code to strip out <tag>s -- primitive way to sort of display HTML as
     * plain text.
     */
    public String removeTags(String str) 
    {
        str = str.replaceAll("<.*?>", " ");
        str = str.replaceAll("\\s+", " ");
        return str;
    }

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent)
	{
		// Android bug - parent mut be set to null or segfaults
		// http://stackoverflow.com/questions/10253506/expandablelistview-unsupportedoperationexception-addviewview-layoutparams-i
		View rc = inflater.inflate(R.layout.single_row, parent, false);
		return rc;
	}
	
}
