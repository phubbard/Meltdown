package net.phfactor.meltdown;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.util.Log;

public class DataModel 
{
	static final String TAG = "MeltdownDM";
	
	public enum dataType {GROUPS, FEEDS, ITEMS};
	
	private ArrayList<HashMap<String, String>> groups_list;
	private ArrayList<HashMap<String, String>> feeds_list;
	private ArrayList<HashMap<String, String>> items_list;
	private HashMap<Integer, List<Integer>> groups_to_feeds;
	
	public DataModel()
	{
		Log.d(TAG, "Constructed");
	};
	
	public void storeGroupsPull(ArrayList<HashMap<String, String>> grp_data, HashMap<Integer, List<Integer>> gf_map)
	{
		Log.d(TAG, "Old group list had " + groups_list.size() + ", new has " + grp_data.size());
		groups_list = grp_data;		
		
		Log.d(TAG, "Old grp->feed map had" + groups_to_feeds.size() + ", new has " + gf_map.size());
		groups_to_feeds = gf_map;
	}
	
}
