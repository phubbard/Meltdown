package net.phfactor.meltdown.adapters;

import net.phfactor.meltdown.R;
import net.phfactor.meltdown.RssItem;
import android.content.Context;
import android.database.Cursor;
import android.text.format.DateUtils;
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
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor)
	{
		// Look up item in database
		RssItem item = new RssItem(cursor);

		// Ignore favicon for now. TODO later.
				
		TextView tv = (TextView) view.findViewById(R.id.tvTitle);
		tv.setText(item.title);
		
		tv = (TextView) view.findViewById(R.id.tvBody);
		tv.setText(item.excerpt);
		
		tv = (TextView) view.findViewById(R.id.item_feed);		
		// FIXME resolve feed ID into feed name
		// TODO Set AB title to feed
		tv.setText("TBD"); 
		
		tv = (TextView) view.findViewById(R.id.item_timestamp);
		tv.setText(DateUtils.getRelativeTimeSpanString(item.created_on_time * 1000L));
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent)
	{
		// Android bug - segfaults
		// http://stackoverflow.com/questions/10253506/expandablelistview-unsupportedoperationexception-addviewview-layoutparams-i
		View rc = inflater.inflate(R.layout.single_row, parent, false);
		return rc;
	}
	
}
