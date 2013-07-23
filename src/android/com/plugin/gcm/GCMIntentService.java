package com.plugin.gcm;

import java.util.List;

import com.google.android.gcm.GCMBaseIntentService;
import com.plugin.gcm.icons.IconClient;
import com.plugin.gcm.icons.IconDownloadTask;

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
import android.support.v4.app.NotificationCompat.Builder;
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
			boolean	foreground = PushPlugin.isInForeground();

			extras.putBoolean("foreground", foreground);

			if (foreground)
				PushPlugin.sendExtras(extras);
			else
				createNotification(context, extras);
		}
	}

	public void createNotification(Context context, Bundle extras)
	{
		final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		final String appName = getAppName(this);

		final Intent notificationIntent = new Intent(this, PushHandlerActivity.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notificationIntent.putExtra("pushBundle", extras);

		final PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);		

		final NotificationCompat.Builder mBuilder = 
			new NotificationCompat.Builder(context)
				.setPriority(0)
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

		final String msgcnt = extras.getString("msgcnt");
		if (null != msgcnt) {
			mBuilder.setNumber(Integer.parseInt(msgcnt));
		}

		setFeedback(mBuilder, context, extras);
		setIcons(mBuilder, context, extras, new IIconBuilderClient() {
			@Override
			public void onIconsSet(Builder builder) {
				mNotificationManager.notify((String) appName, NOTIFICATION_ID, builder.build());
			}
		});
	}
	
	/**
	 * Sets notification feedback according
	 * 'sound' or 'soundname' parameters define a local sound resource 
	 * 'default' as a value of 'sound' or 'soundname' makes default notification sound
	 * If either of parameters are not found - notification is silent (icon only)
	 * @param builder
	 * @param context
	 * @param extras
	 * @return
	 */
	private NotificationCompat.Builder setFeedback(final NotificationCompat.Builder builder, final Context context, final Bundle extras) {
		// Check both sound properties
		// If sound is not set - do not vibrate and flash also
		String soundName = extras.getString("sound");
		if (null == soundName) {
			soundName = extras.getString("soundname");
		}
		if (null == soundName || soundName.equals("")) {
			// No sound - no other feedback type
			return builder;
		}
		
		// Set default vibrate and flash patterns as soon as we have a non-null value for sound
		// If "default" passed as a sound name - set default sound properties
		if (soundName.equals("default")) {
			builder.setDefaults(Notification.DEFAULT_ALL);
			return builder;
		} else {
			builder.setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE);
		}
		
		// Get the sound resource
		
		// USe default sound as a fallback
		Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		
		final Resources res = context.getResources();
		final int soundId = res.getIdentifier(soundName, "raw", context.getPackageName());
		if (0 != soundId) {
			soundUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + soundId);
		}
		return builder.setSound(soundUri);
	}
	
	/**
	 * Sets custom icons if passed in message
	 * @param builder
	 * @param context
	 * @param extras
	 * @return
	 */
	private void setIcons(final NotificationCompat.Builder builder, final Context context, final Bundle extras, final IIconBuilderClient client) {
		final Resources res = context.getResources();
		int smallIcon = context.getApplicationInfo().icon;
		
		final int largeHeight = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
		final int largeWidth  = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
		
		Bitmap largeIcon = (((BitmapDrawable)res.getDrawable(smallIcon)).getBitmap());
		
		// Get small icon and hardcoded large substitute
		final String iconName = extras.getString("icon");
		if (null != iconName && false == iconName.equals("")) {
			final int smallIconId = res.getIdentifier(iconName, "drawable", context.getPackageName());
			final int largeIconId = res.getIdentifier(iconName + "_large", "drawable", context.getPackageName());
			
			if (0 != smallIconId) {
				smallIcon = smallIconId;
			}
			
			if (0 != largeIconId) {
				largeIcon = (((BitmapDrawable)res.getDrawable(largeIconId)).getBitmap());
			}
		}
		
		// Set small icon right away
		builder.setSmallIcon(smallIcon);
		
		// Get large icon URL if provided
		final String largeIconUrl = extras.getString("largeIconUrl");
		if (null == largeIconUrl || largeIconUrl.equals("")) {
			// No icon URL set - return evaluated icons
			builder.setLargeIcon(Bitmap.createScaledBitmap(largeIcon, largeWidth, largeHeight, true));
			client.onIconsSet(builder);
			return;
		}
		
		// Try to download large icon
		IconDownloadTask task = new IconDownloadTask(new IconClient(largeIcon) {
			public void haveIcon(Bitmap bitmap) {
				Bitmap icon = defaultIcon;
				if (null != bitmap) {
					icon = bitmap;
				}
				builder.setLargeIcon(Bitmap.createScaledBitmap(icon, largeWidth, largeHeight, true));
				client.onIconsSet(builder);
			}
		});
		task.execute(largeIconUrl);
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
	
	@Override
	public void onError(Context context, String errorId) {
		Log.e(TAG, "onError - errorId: " + errorId);
	}

}
