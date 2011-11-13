package net.fabene.butone;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;


import android.app.Service;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

public class PostService extends Service {

	private LocationManager lm;
	private MyLocationListener ll;
	private Handler handler;
	Camera camera;
	Button buttonClick;
	String myTag = "";
	String username = "";
	final String postURL = "http://butoneextended.appspot.com/upload";
	float lastLon = 0, lastLat = 0;

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		if (intent.getExtras() != null) {
			myTag = intent.getStringExtra("tag");
			username = intent.getStringExtra("username");
		}
		if (myTag.equals("")) {
			Toast.makeText(getApplicationContext(), "You have not assigned a tag to this ButOne.", Toast.LENGTH_SHORT).show();
			stopSelf();
		}
		else {
			// ---use the LocationManager class to obtain GPS locations---
			
			lm = (LocationManager) getSystemService(LOCATION_SERVICE);
			handler = new Handler(Looper.getMainLooper());
			// this location listener waits for the current location to change,
			//then makes a post in a new thread
			ll = new MyLocationListener(myTag);
			Criteria criteria = new Criteria();
			criteria.setAccuracy(Criteria.ACCURACY_COARSE);
			lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 200, ll);
			//lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1, 200, ll);

			//lm.requestLocationUpdates( 1, 200, criteria, ll, Looper.getMainLooper());

			Log.d("Ben", "Making Toast");
			Toast.makeText(getApplicationContext(), "Posting " + myTag + " tag...", Toast.LENGTH_SHORT).show();
		}
	}
	
	@Override
	public void onCreate() {
	}

	//You can't make normal toasts from within non-UI threads
	//so this is a work-around. The handler knows where the main
	//thread is with which to use Toast.
	public void showMessage(final String message) {
		handler.post(new Runnable() {
			public void run() {
				Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
			}
		});
	}
	
	public void postData(final String tagName) {
		new Thread(new Runnable() {
			public void run() {
				// Create a new HttpClient and Post Header
				DefaultHttpClient httpclient = new DefaultHttpClient();
				HttpPost httppost = new HttpPost(postURL);

				try {
					// Add your data
					
					List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(5);
					nameValuePairs.add(new BasicNameValuePair("username", username));
					nameValuePairs.add(new BasicNameValuePair("tag", tagName));
					nameValuePairs.add(new BasicNameValuePair("lat", "" + lastLat));
					nameValuePairs.add(new BasicNameValuePair("lon", "" + lastLon));
					Log.d("info", Variables.getInstance().getURL());
					nameValuePairs.add(new BasicNameValuePair("imgUrl",  Variables.getInstance().getURL()));
					
					httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

					// Execute HTTP Post Request
					HttpResponse response = httpclient.execute(httppost);
					Log.d("Ben", "Sent POST, got " + response.getStatusLine());
					
					showMessage(tagName + " posted by " + username + ".");
				} catch (ClientProtocolException e) {
					showMessage("Post failed.");
					Log.e("Ben", "" + e);
				} catch (IOException e) {
					showMessage("Post failed.");
					Log.e("Ben", "" + e);
				}
				lm.removeUpdates(ll);
				stopSelf();
			}
		}).start();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	private class MyLocationListener implements LocationListener {
		
		boolean bFoundLocation = false;
		
		private class MyRunnable implements Runnable {

			String tagName;
			
			MyRunnable(String tagName) {
				this.tagName = tagName;
			}
			
			//this is called if no current location is found. 
			//and it uses some stored location
			public void run() {
				Criteria criteria = new Criteria();
				criteria.setAccuracy(Criteria.ACCURACY_FINE);
				Location locM = lm.getLastKnownLocation(lm.getBestProvider(criteria, false));
				if( locM == null){
					criteria.setAccuracy(Criteria.ACCURACY_COARSE);
					locM = lm.getLastKnownLocation(lm.getBestProvider(criteria, false));	
				}
				if( locM == null){
					criteria.setAccuracy(Criteria.ACCURACY_MEDIUM);
					locM = lm.getLastKnownLocation(lm.getBestProvider(criteria, false));	
				}
				
				if (locM != null) {
					lastLat = (float) locM.getLatitude();
					lastLon = (float) locM.getLongitude();
				}
				else{
					lastLat = 0.0f;
					lastLon = 0.0f;
				}
				Toast.makeText(getApplicationContext(), "Couldn't get a GPS fix. Posting from "+lastLat+","+ lastLon+ " instead.", Toast.LENGTH_SHORT).show();

				mHandler.removeCallbacks(quitTrying);
				
				postData(tagName);
				Log.d("info", postURL);
			}
			
		}
		
		
		private Handler mHandler = new Handler();
		private MyRunnable quitTrying = new MyRunnable(myTag);
		private String tagName;
		
		MyLocationListener(String tagName) {
			//If we don't receive a new gps location in 15 seconds, quit trying
			mHandler.postDelayed(quitTrying, 30000);
			this.tagName = tagName;
		}
		
		public void onLocationChanged(Location location) {
			if (!bFoundLocation) {
				bFoundLocation = true;
				Criteria criteria = new Criteria();
				criteria.setAccuracy(Criteria.ACCURACY_COARSE);
	
				Location locM = lm.getLastKnownLocation(lm.getBestProvider(criteria, false));
				if (locM != null) {
					lastLat = (float) locM.getLatitude();
					lastLon = (float) locM.getLongitude();
				}
				mHandler.removeCallbacks(quitTrying);
				postData(tagName);
			}
		}

		public void onProviderDisabled(String provider) {}
		public void onProviderEnabled(String provider) {}
		public void onStatusChanged(String provider, int status, Bundle extras) {}

	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

}
