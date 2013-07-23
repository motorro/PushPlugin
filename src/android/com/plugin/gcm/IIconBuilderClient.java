package com.plugin.gcm;

import android.support.v4.app.NotificationCompat;

public interface IIconBuilderClient {
	void onIconsSet(NotificationCompat.Builder builder);
}
