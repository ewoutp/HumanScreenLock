package com.ewoutp.humanscreenlock;

import android.os.Handler;
import android.os.Message;
import android.view.View;

public class RefreshTimer extends Handler {
	
	private static final int MSG_START = 1;
	private static final int MSG_STOP = 2;
	private static final int MSG_UPDATE = 3;
	
	private final View view;
	private final int refreshPeriod;
	private long nextUpdate;
	
	public RefreshTimer(View view, int refreshPeriod) {
		this.view = view;
		this.refreshPeriod = refreshPeriod;
	}
		
	public void start() {
		nextUpdate = System.currentTimeMillis() + refreshPeriod;
		sendEmptyMessage(MSG_START);
	}
	
	public void stop() {
		sendEmptyMessage(MSG_STOP);
	}
	
	@Override
	public void handleMessage(Message msg) {
		super.handleMessage(msg);
		
		switch (msg.what) {
		case MSG_START:
			sendEmptyMessage(MSG_UPDATE);
			break;
		case MSG_STOP:
			removeMessages(MSG_UPDATE);
			break;
		case MSG_UPDATE:
			long time = System.currentTimeMillis();
			if (time >= nextUpdate) {
				view.invalidate();
				nextUpdate = time + refreshPeriod;
			}
			sendEmptyMessageDelayed(MSG_UPDATE, refreshPeriod / 10);
		}		
	}
}
