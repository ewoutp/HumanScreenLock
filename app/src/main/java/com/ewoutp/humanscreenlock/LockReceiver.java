package com.ewoutp.humanscreenlock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Automatically start our lock service on boot.
 * 
 * @author ewout
 * 
 */
public class LockReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// Trigger the sensor watch service
		Intent myIntent = new Intent(context, LockService.class);
		myIntent.putExtra("action", "" + intent.getAction()); 
		context.startService(myIntent);
	}

}
