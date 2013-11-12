package net.phfactor.meltdown.fragments;

import net.phfactor.meltdown.R;
import net.phfactor.meltdown.RssItem;
import net.phfactor.meltdown.activities.ItemDetailActivity;
import net.phfactor.meltdown.activities.ItemListActivity;
import net.phfactor.meltdown.providers.ItemProvider;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

/**
 * A fragment representing a single Item detail screen. This fragment is either
 * contained in a {@link ItemListActivity} in two-pane mode (on tablets) or a
 * {@link ItemDetailActivity} on handsets.
 */
public class ItemDetailFragment extends Fragment
{
	static final String TAG = "MeltdownItemDetailFragment";
	/**
	 * The fragment argument representing the item ID that this fragment
	 * represents.
	 */
	public static final String ARG_ITEM_ID = "item_id";

	private RssItem item;
	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public ItemDetailFragment()
	{
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		item = new RssItem();
		
		if (getArguments().containsKey(ARG_ITEM_ID))
		{
			// Load item from ContentProvider 
			// TODO, preferably via a Loader
			String selection = ItemProvider.C_FEVER_ID + "=?";
			String args[] = {getArguments().getString(ARG_ITEM_ID)};
			ContentResolver mcr = getActivity().getContentResolver();
			Cursor cursor = mcr.query(ItemProvider.URI, null, selection, args, null);
			if ((cursor != null) && cursor.moveToNext())
			{
				item = new RssItem(cursor);
			}
			else
				Log.e(TAG, "Unable to locate item record for ID " + getArguments().getString(ARG_ITEM_ID));
			
			// Load the dummy content specified by the fragment
			// arguments. In a real-world scenario, use a Loader
			// to load content from a content provider.
			//mItem = DummyContent.ITEM_MAP.get(getArguments().getString(ARG_ITEM_ID));
		}
		else
			Log.e(TAG, "fragment started without item ID argument!");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View rootView = inflater.inflate(R.layout.activity_item_display, container, false);

		if (item != null)
		{
			// This is the lower-center text field - age of post
	        TextView tv = (TextView) rootView.findViewById(R.id.itmFeedTitle);
	        tv.setText(DateUtils.getRelativeTimeSpanString(item.created_on_time * 1000L));
			((TextView) rootView.findViewById(R.id.itmItemTitle)).setText(item.title);
			((WebView) rootView.findViewById(R.id.itemWebView)).loadDataWithBaseURL(null, item.html, 
					"text/html", "UTF-8", null);
		}
		return rootView;
	}
}
