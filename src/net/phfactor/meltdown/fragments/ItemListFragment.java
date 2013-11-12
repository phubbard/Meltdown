package net.phfactor.meltdown.fragments;

import net.phfactor.meltdown.MeltdownApp;
import net.phfactor.meltdown.RssGroup;
import net.phfactor.meltdown.RssItem;
import net.phfactor.meltdown.adapters.ItemAdapter;
import net.phfactor.meltdown.providers.ItemProvider;
import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

/**
 * A list fragment representing a list of Items. This fragment also supports
 * tablet devices by allowing list items to be given an 'activated' state upon
 * selection. This helps indicate which item is currently being viewed in a
 * {@link ItemDetailFragment}.
 * <p>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class ItemListFragment extends ListFragment
{
	static final String TAG = "MeltdownItemListFragment";
	public static final String GROUP_KEY = "group";
	
	private String group;
	
	
	/**
	 * The serialization (saved instance state) Bundle key representing the
	 * activated item position. Only used on tablets.
	 */
	private static final String STATE_ACTIVATED_POSITION = "activated_position";

	/**
	 * The fragment's current callback object, which is notified of list item
	 * clicks.
	 */
	private Callbacks mCallbacks = sIVCallbacks;

	/**
	 * The current activated item position. Only used on tablets.
	 */
	private int mActivatedPosition = ListView.INVALID_POSITION;

	/**
	 * A callback interface that all activities containing this fragment must
	 * implement. This mechanism allows activities to be notified of item
	 * selections.
	 */
	public interface Callbacks
	{
		/**
		 * Callback for when an item has been selected.
		 */
		public void onItemSelected(String id);
	}

	/**
	 * A dummy implementation of the {@link Callbacks} interface that does
	 * nothing. Used only when this fragment is not attached to an activity.
	 */
	private static Callbacks sDummyCallbacks = new Callbacks()
	{
		@Override
		public void onItemSelected(String id)
		{
		}
	};

	private static Callbacks sIVCallbacks = new Callbacks()
	{
		@Override
		public void onItemSelected(String id)
		{
			Log.d(TAG, "Item " + id + " selected");
		}		
	};
	
	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public ItemListFragment()
	{
	}

	private String[] getFeedArray(String group_name)
	{
		MeltdownApp app = (MeltdownApp) getActivity().getApplication();
		RssGroup group = app.findGroupByName(group_name);
		if (group != null)
			return group.getFeedIDs();
		
		Log.w(TAG, "Could not find group " + group_name);
		return null;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		Log.d(TAG, "created");
		super.onCreate(savedInstanceState);
		
		if (savedInstanceState != null)
		{
			if (savedInstanceState.containsKey(GROUP_KEY))
			{
				group = savedInstanceState.getString(GROUP_KEY);
				Log.d(TAG, "Got group key for display: " + group);				
			}
			else
			{
				Log.w(TAG, "Missing group name!");
				group = "unknown";
			}
		}
		else
		{
			Log.w(TAG, "Missing group name !");
			group = "unknown";			
		}
		
		ContentResolver mcr = getActivity().getContentResolver();
		// Add feed IDs into query
		String where_clause = ItemProvider.C_FEED_ID + "=?";		
		String[] params = getFeedArray(group);
		
		Cursor cursor;
		
		if (params != null)
			cursor = mcr.query(ItemProvider.URI, null, where_clause, params, ItemProvider.SORT_ORDER);
		else
			cursor = mcr.query(ItemProvider.URI, null, null, null, ItemProvider.SORT_ORDER);
		
		ItemAdapter adapter = new ItemAdapter(getActivity(), cursor, 0);
		setListAdapter(adapter);
		Log.d(TAG, "adapter done");
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		// Restore the previously serialized activated item position.
		if (savedInstanceState != null
				&& savedInstanceState.containsKey(STATE_ACTIVATED_POSITION))
		{
			setActivatedPosition(savedInstanceState
					.getInt(STATE_ACTIVATED_POSITION));
		}
	}

	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		
		// Activities containing this fragment must implement its callbacks.
		if (!(activity instanceof Callbacks))
		{
			throw new IllegalStateException(
					"Activity must implement fragment's callbacks.");
		}

		mCallbacks = (Callbacks) activity;
	}

	@Override
	public void onDetach()
	{
		super.onDetach();

		// Reset the active callbacks interface to the dummy implementation.
		mCallbacks = sDummyCallbacks;
	}

	@Override
	public void onListItemClick(ListView listView, View view, int position,	long id)
	{
		super.onListItemClick(listView, view, position, id);

		// The listview is a cursor adapter, so this seems to be the (awkward) way to 
		// look up the item based on cursor position.
		Cursor cursor = (Cursor) listView.getItemAtPosition(position);
		RssItem item = new RssItem(cursor);

		// Notify the active callbacks interface (the activity, if the
		// fragment is attached to one) that an item has been selected.
		mCallbacks.onItemSelected(String.format("%d", item.id));
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		if (mActivatedPosition != ListView.INVALID_POSITION)
		{
			// Serialize and persist the activated item position.
			outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
		}
	}

	/**
	 * Turns on activate-on-click mode. When this mode is on, list items will be
	 * given the 'activated' state when touched.
	 */
	public void setActivateOnItemClick(boolean activateOnItemClick)
	{
		// When setting CHOICE_MODE_SINGLE, ListView will automatically
		// give items the 'activated' state when touched.
		getListView().setChoiceMode(
				activateOnItemClick ? ListView.CHOICE_MODE_SINGLE
						: ListView.CHOICE_MODE_NONE);
	}

	private void setActivatedPosition(int position)
	{
		if (position == ListView.INVALID_POSITION)
		{
			getListView().setItemChecked(mActivatedPosition, false);
		} else
		{
			getListView().setItemChecked(position, true);
		}

		mActivatedPosition = position;
	}
}
