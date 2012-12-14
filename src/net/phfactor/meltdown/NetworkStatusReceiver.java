package net.phfactor.meltdown;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

// Track network availability; just sets a variable in the application object. Keep it maximally simple here.
public class NetworkStatusReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent)
	{
		Boolean net_down = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
		MeltdownApp mapp = (MeltdownApp) context.getApplicationContext();
		mapp.setNetStatus(net_down);
	}
}
