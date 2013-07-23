package com.plugin.gcm.icons;

import android.graphics.Bitmap;

public abstract class IconClient {
	protected final Bitmap defaultIcon;
	
	public IconClient(Bitmap defaultIcon) {
		this.defaultIcon = defaultIcon;
	}
	
	abstract public void haveIcon(Bitmap bitmap);
}
