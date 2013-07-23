package com.plugin.gcm.icons;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

public class IconDownloadTask extends AsyncTask<String, Void, Bitmap> {
	private  IconClient client;
	final private static String TAG = "IconDownloadTask";
	
	public IconDownloadTask(IconClient client) {
		this.client = client;
	}
	
	@Override
	protected Bitmap doInBackground(String... params) {
		final String url = params[0];
		Log.i(TAG, "Downloading image: " + url);
		return IconDownloader.downloadBitmap(url);
	}
	
	@Override 
	protected void onPostExecute(Bitmap bitmap) {
		if (isCancelled()) {
			Log.w(TAG, "Image download was cancelled!");
			bitmap = null;
		}
		if (null != client) {
			client.haveIcon(bitmap);
		}
	}
}
