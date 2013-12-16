package com.ewoutp.humanscreenlock;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;

public class MainActivity extends ActionBarActivity {

	private GraphView graphView;
	private RefreshTimer refreshTimer;
	private float lastX;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		graphView = (GraphView) findViewById(R.id.graphView);
		refreshTimer = new RefreshTimer(graphView, 1000 / 20);

		Intent myIntent = new Intent(this, LockService.class);
		this.startService(myIntent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_main_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		refreshTimer.start();
		buildChart();
	}

	@Override
	protected void onPause() {
		super.onPause();
		refreshTimer.stop();
	}

	private void buildChart() {
		graphView.invalidate();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			lastX = event.getX();
			break;
		case MotionEvent.ACTION_UP:
			float currentX = event.getX();
			if (currentX < lastX)
				buildChart();
			break;
		}
		return super.onTouchEvent(event);
	}
}
