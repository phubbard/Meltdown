package net.phfactor.meltdown.fragments;

import net.phfactor.meltdown.R;
import net.phfactor.meltdown.adapters.ItemAdapter;
import net.phfactor.meltdown.providers.ItemProvider;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class ItemsFragment extends Fragment
{
	static final String TAG = "MeltdownItemsFragment";

	public static final String GROUP_KEY = "group";
	public static final String FEED_KEY = "feed";
	
	private ItemAdapter adapter;
	private String group;
	private String feed;
	private ListView listview;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		Log.d(TAG, "created");
		View view = inflater.inflate(R.layout.item_list, container); // todo version with false??
		this.listview = (ListView) view.findViewById(R.id.item_list);
		
		this.group = "unknown";
		this.feed = "unknown";
		
		if (savedInstanceState != null)
		{
			if (savedInstanceState.containsKey(GROUP_KEY))
			{
				Log.d(TAG, "Got group key for display");
				group = savedInstanceState.getString(GROUP_KEY);
			}
			
			if (savedInstanceState.containsKey(FEED_KEY))
			{
				Log.d(TAG, "got feed key for display");
				feed = savedInstanceState.getString(FEED_KEY);
			}
		}
		
		return view;
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		
		// TODO Add in group and or feed into query!
		String selection_args = this.group;
		
		// FIXME
		this.adapter = new ItemAdapter(getActivity(), 
				getActivity().getContentResolver().query(ItemProvider.URI, null, ItemProvider.C_FEED_ID, 
						null, ItemProvider.SORT_ORDER), 0);
		listview.setAdapter(adapter);
	}
}
