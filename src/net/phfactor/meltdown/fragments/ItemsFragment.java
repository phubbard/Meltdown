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
	
	private ItemAdapter adapter;
	private String group;
	private ListView listview;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		Log.d(TAG, "created");
		View view = inflater.inflate(R.layout.item_list, container); // todo version with false??
		this.listview = (ListView) view.findViewById(R.id.item_list);
		
		this.group = "unknown";
		
		if (savedInstanceState != null)
		{
			if (savedInstanceState.containsKey(GROUP_KEY))
			{
				Log.d(TAG, "Got group key for display");
				group = savedInstanceState.getString(GROUP_KEY);
			}
			else
				Log.w(TAG, "Missing group key!");
		}
		
		return view;
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		String selection_args[] = {};
		String selection = null;
		
		// TODO Add in group and or feed into query!
		if (!group.equals("unknown"))
		{
			selection_args[0] = this.group;
		}
		
		// FIXME
		this.adapter = new ItemAdapter(getActivity(), 
				getActivity().getContentResolver().query(ItemProvider.URI, null, selection, selection_args, 
						ItemProvider.SORT_ORDER), 0);
		listview.setAdapter(adapter);
	}
}
