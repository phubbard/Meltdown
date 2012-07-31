package net.phfactor.meltdown;

import java.util.HashMap;
import java.util.List;

import android.util.Log;

public class DataModel 
{
	static final String TAG = "MeltdownDM";
	
	public enum dataType {GROUPS, FEEDS, ITEMS};
	
	// Map group name to array of numeric feed ids
	private HashMap<String, List<Integer>> gf_map;
	
	// Map feed ids to item ids
	private HashMap<Integer, List<Integer>> fi_map;
	
	public DataModel()
	{
		gf_map = new HashMap<String, List<Integer>>();
		fi_map = new HashMap<Integer, List<Integer>>();
		
		Log.d(TAG, "Constructed");
	};
	
	public void storeGroupsPull(HashMap<String, List<Integer>> ngf_map)
	{
		Log.d(TAG, "Old group list had " + gf_map.size() + ", new has " + ngf_map.size());
		gf_map = ngf_map;
	}
	
	public void storeFeedsPull(HashMap<Integer, List<Integer>> nfi_map)
	{
		Log.d(TAG, "Old fi_map had " + fi_map.size() + ", new has " + nfi_map.size());
		fi_map = nfi_map;
	}
}
