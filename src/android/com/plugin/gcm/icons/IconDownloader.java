package com.plugin.gcm.icons;

import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.http.AndroidHttpClient;
import android.util.Log;

public class IconDownloader {
	final private static String TAG = "IconDownloader";

	static Bitmap downloadBitmap(String url) {
	    final AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
	    final HttpGet getRequest = new HttpGet(url);

	    try {
	        HttpResponse response = client.execute(getRequest);
	        final int statusCode = response.getStatusLine().getStatusCode();
	        if (HttpStatus.SC_OK != statusCode) { 
	            Log.w(TAG, "Error " + statusCode + " while retrieving bitmap from " + url); 
	            return null;
	        }
	        
	        final HttpEntity entity = response.getEntity();
	        if (null != entity) {
	            InputStream inputStream = null;
	            try {
	                inputStream = entity.getContent(); 
	                return BitmapFactory.decodeStream(inputStream);
	            } finally {
	                if (null != inputStream) {
	                    inputStream.close();  
	                }
	                entity.consumeContent();
	            }
	        }
	    } catch (Exception e) {
	        getRequest.abort();
	        Log.w(TAG, "Error while retrieving bitmap from " + url + ": " + e.toString());
	    } finally {
	        if (null != client) {
	            client.close();
	        }
	    }
	    return null;
	}
}
