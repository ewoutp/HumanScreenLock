package com.ewoutp.humanscreenlock;

import java.util.ArrayList;
import java.util.List;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * Service that monitors sensors to keep the screen alive.
 * 
 * @author ewout
 * 
 */
public class LockService extends Service implements SensorEventListener, OnSharedPreferenceChangeListener {

	private static final String logTag = "HumanScreenLock";
	private static final String showNotificationsPrefKey = "show_notifications";
	private static final int LOCK_NOTIFY_ID = 1;
	public static final float forceThreshold = 0.07f;
	private SensorManager sensorMgr;
	private PowerManager powerMgr;
	private PowerManager.WakeLock wakeLock;
	private Sensor accSensor;
	private boolean listening;
	private CountDownTimer releaseLockTimer;
	private NotificationManager notificationMgr;
	private Notification notification;
	private KeyguardManager keyguardMgr;

	static final int listLength = 60;
	static final List<Double> listX = new ArrayList<Double>(listLength);
	static final List<Double> listY = new ArrayList<Double>(listLength);
	static final List<Double> listZ = new ArrayList<Double>(listLength);
	static final List<Double> listF = new ArrayList<Double>(listLength);

	/**
	 * We do not allow binding.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/**
	 * This service has been created. Setup references.
	 */
	@Override
	public void onCreate() {
		super.onCreate();

		powerMgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
		sensorMgr = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		notificationMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		keyguardMgr = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
		accSensor = sensorMgr.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

		LockReceiver recv = new LockReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		registerReceiver(recv, filter);
		Log.d(logTag, "receiver registered");

		// Listen for preferences changes
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
	}

	/**
	 * startService has been called for this service.
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if ((intent != null) && (intent.getStringExtra("action") == Intent.ACTION_SCREEN_OFF)) {
			// Screen is turned on, release lock and stop listening
			releaseLock();
			stopListening();
		} else {
			if (isScreenOn()) {
				startListening();
			}
		}
		return super.onStartCommand(intent, flags, startId);
	}

	/**
	 * This service is about to destroy.
	 */
	@Override
	public void onDestroy() {
		// Stop listening for preferences changes
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.unregisterOnSharedPreferenceChangeListener(this);
		
		// Stop listening
		stopListening();
		super.onDestroy();
	}

	/**
	 * Sensor accuracy has changed. We ignore this.
	 */
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	/**
	 * Sensor value has changed
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {
		double x = event.values[0];
		double y = event.values[1];
		double z = event.values[2];
		double force = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2)
				+ Math.pow(z, 2));

		if (listX.size() == listLength)
			listX.remove(0);
		listX.add(x);
		if (listY.size() == listLength)
			listY.remove(0);
		listY.add(y);
		if (listZ.size() == listLength)
			listZ.remove(0);
		listZ.add(z);
		if (listF.size() == listLength)
			listF.remove(0);
		listF.add(force);

		boolean isTrigger = (force >= forceThreshold);
		if (isScreenOn()) {
			// The screen is on, so acquire lock when triggers
			if (isTrigger && !isKeyguardLocked()) {
				acquireLock();
			}
		} else {
			// The screen is off, stop listening for events now
			stopListening();
		}
	}

	/**
	 * Start listening for sensor events.
	 */
	private synchronized void startListening() {
		if (listening)
			return;

		if (accSensor != null) {
			// Register a listener
			Log.d(logTag, "Start listening to sensor");
			sensorMgr.registerListener(this, accSensor,
					SensorManager.SENSOR_DELAY_UI);
			listening = true;
		}
	}

	/**
	 * Stop listening for sensor events.
	 */
	private synchronized void stopListening() {
		if (listening) {
			// Unregister me as listener
			Log.d(logTag, "Stop listening to sensor");
			sensorMgr.unregisterListener(this);
			listening = false;
		}
	}

	/**
	 * Acquire a screenlock (if not already locked)
	 */
	private synchronized void acquireLock() {
		startReleaseLockTimer();

		// Acquire the lock (if needed)
		if (wakeLock == null) {
			Log.d(logTag, "Acquire wake lock");
			wakeLock = powerMgr.newWakeLock(
					PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "my tag");
			wakeLock.acquire();
		}

		// Set notification (if needed)
		showNotification();
	}

	/**
	 * Should notifications be shown?
	 */
	private boolean showNotifications() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		return prefs.getBoolean(showNotificationsPrefKey, true);
	}

	/**
	 * Show the lock is on notification.
	 */
	private synchronized void showNotification() {
		if ((notification == null) && showNotifications()) {
			Log.d(logTag, "notify of wake lock");
			PendingIntent pIntent = PendingIntent.getActivity(this, 0, 
					new Intent(this, MainActivity.class), Intent.FLAG_ACTIVITY_NEW_TASK);
			notification = new NotificationCompat.Builder(this)
					.setSmallIcon(R.drawable.ic_stat_name)
					.setContentTitle(this.getString(R.string.notLockOnContentTitle))
					.setContentText(this.getString(R.string.notLockOnContentText))
					.setOngoing(true)
					.setContentIntent(pIntent)
					.build();
			notificationMgr.notify(LOCK_NOTIFY_ID, notification);
		}
	}

	private synchronized void hideNotification() {
		if (notification != null) {
			Log.d(logTag, "Cancel notification");
			notificationMgr.cancel(LOCK_NOTIFY_ID);
			notification = null;
		}
	}

	/**
	 * Release a screenlock (if locked)
	 */
	private synchronized void releaseLock() {
		stopReleaseLockTimer();
		if (wakeLock != null) {
			Log.d(logTag, "Release wake lock");
			wakeLock.release();
			wakeLock = null;
		}
		hideNotification();
	}

	/**
	 * Start/restart the reset-timer
	 */
	private synchronized void startReleaseLockTimer() {
		stopReleaseLockTimer();
		if (releaseLockTimer == null) {
			releaseLockTimer = new CountDownTimer(1000, 2000) {
				public void onTick(long x) {
					// Ignore
				}

				public void onFinish() {
					releaseLockTimer = null;
					releaseLock();
				}
			};
			releaseLockTimer.start();
		}
	}

	/**
	 * Stop the release-lock timer
	 */
	private synchronized void stopReleaseLockTimer() {
		if (releaseLockTimer != null) {
			releaseLockTimer.cancel();
			releaseLockTimer = null;
		}
	}

	/**
	 * Is the screen powered on?
	 */
	private boolean isScreenOn() {
		PowerManager powerMgr = this.powerMgr;
		return (powerMgr == null) || powerMgr.isScreenOn();
	}

	/**
	 * Is the keyguard locked? This will always return false on devices with SDK
	 * less than 16.
	 * 
	 * @return
	 */
	private boolean isKeyguardLocked() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
			return false;
		return keyguardMgr.isKeyguardLocked();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (showNotificationsPrefKey.equals(key)) {
			if (!showNotifications()) {
				hideNotification();
			}
		}
	}
}
