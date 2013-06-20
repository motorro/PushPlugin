package com.plugin.gcm;

import java.util.List;

import com.google.android.gcm.GCMBaseIntentService;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

@SuppressLint("NewApi")
public class GCMIntentService extends GCMBaseIntentService {

	public static final int NOTIFICATION_ID = 237;
	private static final String TAG = "GCMIntentService";
	
	public GCMIntentService() {
		super("GCMIntentService");
	}

	@Override
	public void onRegistered(Context context, String regId) {

		Log.v(TAG, "onRegistered: "+ regId);

		JSONObject json;

		try
		{
			json = new JSONObject().put("event", "registered");
			json.put("regid", regId);

			Log.v(TAG, "onRegistered: " + json.toString());

			// Send this JSON data to the JavaScript application above EVENT should be set to the msg type
			// In this case this is the registration ID
			PushPlugin.sendJavascript( json );

		}
		catch( JSONException e)
		{
			// No message to the user is sent, JSON failed
			Log.e(TAG, "onRegistered: JSON exception");
		}
	}

	@Override
	public void onUnregistered(Context context, String regId) {
		Log.d(TAG, "onUnregistered - regId: " + regId);
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
		Log.d(TAG, "onMessage - context: " + context);

		// Extract the payload from the message
		Bundle extras = intent.getExtras();
		if (extras != null)
		{
			boolean	foreground = this.isInForeground();

			extras.putBoolean("foreground", foreground);

			if (foreground)
				PushPlugin.sendExtras(extras);
			else
				createNotification(context, extras);
		}
	}

	public void createNotification(Context context, Bundle extras)
	{
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		String appName = getAppName(this);

		Intent notificationIntent = new Intent(this, PushHandlerActivity.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notificationIntent.putExtra("pushBundle", extras);

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);		

		NotificationCompat.Builder mBuilder = 
			new NotificationCompat.Builder(context)
				.setPriority(0)
				.setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)
				.setWhen(System.currentTimeMillis())
				.setContentTitle(appName)
				.setContentIntent(contentIntent);
		
		String ticker = extras.getString("ticker");
		if (null == ticker || ticker.equals("")) {
			ticker = appName;
		} 
		mBuilder.setTicker(ticker);

		String message = extras.getString("message");
		if (null == message || message.equals("")) {
			message = "<missing message content>";
		} 
		mBuilder.setContentText(message);

		String msgcnt = extras.getString("msgcnt");
		if (null != msgcnt) {
			mBuilder.setNumber(Integer.parseInt(msgcnt));
		}

		setIcons(mBuilder, context, extras);
		//		mBuilder.setSmallIcon(getSmallIcon(context, extras));
		mBuilder.setSound(getRingtoneUri(context, extras));
		
		mNotificationManager.notify((String) appName, NOTIFICATION_ID, mBuilder.build());
	}
	
	/**
	 * Gets sound passed in message or default ringtone
	 * @param context
	 * @param extras
	 * @return
	 */
	private Uri getRingtoneUri(Context context, Bundle extras) {
		Uri result = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		
		String soundName = extras.getString("sound");
		if (null == soundName || soundName.equals("")) {
			return result;
		}

		Resources res = context.getResources();
		int soundId = res.getIdentifier(soundName, "raw", context.getPackageName());
		if (0 == soundId) {
			return result;
		}
		
		result = Uri.parse("android.resource://" + context.getPackageName() + "/" + soundId);
		return result;
	}

	/**
	 * Sets custom icons if passed in message
	 * @param builder
	 * @param context
	 * @param extras
	 * @return
	 */
	private NotificationCompat.Builder setIcons(NotificationCompat.Builder builder, Context context, Bundle extras) {
		Resources res = context.getResources();
		int smallIcon = context.getApplicationInfo().icon;
		Bitmap largeIcon = (((BitmapDrawable)res.getDrawable(smallIcon)).getBitmap());
		
		String iconName = extras.getString("icon");
		if (null != iconName && false == iconName.equals("")) {
			int smallIconId = res.getIdentifier(iconName, "drawable", context.getPackageName());
			int largeIconId = res.getIdentifier(iconName + "_large", "drawable", context.getPackageName());
			
			if (0 != smallIconId) {
				smallIcon = smallIconId;
			}
			
			if (0 != largeIconId) {
				largeIcon = (((BitmapDrawable)res.getDrawable(largeIconId)).getBitmap());
			}
		}
		
		return builder.setSmallIcon(smallIcon).setLargeIcon(largeIcon);
	}

	public static void cancelNotification(Context context)
	{
		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel((String)getAppName(context), NOTIFICATION_ID);	
	}
	
	private static String getAppName(Context context)
	{
		CharSequence appName = 
				context
					.getPackageManager()
					.getApplicationLabel(context.getApplicationInfo());
		
		return (String)appName;
	}
	
	public boolean isInForeground()
	{
		ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningTaskInfo> services = activityManager
				.getRunningTasks(Integer.MAX_VALUE);

		if (services.get(0).topActivity.getPackageName().toString().equalsIgnoreCase(getApplicationContext().getPackageName().toString()))
			return true;

		return false;
	}	

	@Override
	public void onError(Context context, String errorId) {
		Log.e(TAG, "onError - errorId: " + errorId);
	}

}
