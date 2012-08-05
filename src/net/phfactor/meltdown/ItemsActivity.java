package net.phfactor.meltdown;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.Toast;

public class ItemsActivity extends ListActivity 
{

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list);
		
		String grp_name = getIntent().getExtras().getString("title");
		Toast.makeText(this, grp_name, Toast.LENGTH_SHORT).show();
	}

}
