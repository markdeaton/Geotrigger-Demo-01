package com.esri.apl.geotrigger.demo.geotrigger_1;

import org.apache.http.Header;
import org.apache.http.StatusLine;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ToggleButton;

import com.esri.android.geotriggers.GeotriggerBroadcastReceiver;
import com.esri.android.geotriggers.GeotriggerBroadcastReceiver.DeviceReadyListener;
import com.esri.android.geotriggers.GeotriggerBroadcastReceiver.LocationUpdateListener;
import com.esri.android.geotriggers.GeotriggerService;
import com.esri.core.portal.ApiRequestListener;
import com.esri.core.portal.PortalDevice.DeviceCredentials;
import com.esri.core.tasks.ags.geotrigger.DeviceTask;
import com.esri.core.tasks.ags.geotrigger.TriggerTask;

public class Tracker extends Activity implements LocationUpdateListener, DeviceReadyListener {

	private String TAG; // How LogCat entries will be prefixed
	private final String APP_TAG = /*"yelp"*/"med_gt_2013_01";
	
	private ToggleButton tbTracking;
    private GeotriggerBroadcastReceiver mGeotriggerBroadcastReceiver;
    private TriggerTask mCreateTriggersTask;
    private Handler mHandler = new Handler();
    
    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(mGeotriggerBroadcastReceiver, GeotriggerBroadcastReceiver.getDefaultIntentFilter());
        GeotriggerService.requestOnDemandUpdate(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mGeotriggerBroadcastReceiver);
    }


    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tracker);
		TAG = getString(R.string.LOG_TAG);		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		mGeotriggerBroadcastReceiver = new GeotriggerBroadcastReceiver();
		tbTracking = (ToggleButton)findViewById(R.id.tbTracking);
		tbTracking.setOnCheckedChangeListener(onTrackerStatusChange);
		
		// Make sure tracking profile is set according to initial state of toggle button
		onTrackerStatusChange.onCheckedChanged(tbTracking, tbTracking.isChecked());
	}

	OnCheckedChangeListener onTrackerStatusChange = new OnCheckedChangeListener() {
		
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			String sAppId = getString(R.string.APP_ID);
			String sSenderId = getString(R.string.GCM_SENDER_ID);
			
			if (isChecked) {
				// Start tracking
		        GeotriggerService.init(Tracker.this, sAppId, sSenderId, GeotriggerService.TRACKING_PROFILE_FINE);
			}
			else {
				// Stop tracking
		        GeotriggerService.init(Tracker.this, sAppId, sSenderId, GeotriggerService.TRACKING_PROFILE_OFF);
			}
		}
	};
	
/*	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.tracker, menu);
		return true;
	}*/

	@Override
	public void onLocationUpdate(Location arg0, boolean arg1) {
		Log.d(TAG, "Location update: " + arg0.getLongitude() + ", " + arg0.getLatitude());
	}

	@Override
	public void onDeviceReady(DeviceCredentials credentials, boolean arg1) {
        // Create a device task and add the "yelp" tag to the device
        DeviceTask dt = new DeviceTask(credentials, this, mHandler);
        dt.setTags(new String[]{APP_TAG}, new ApiRequestListener() {
            @Override
            public void onSuccess(JSONObject json, Header[] headers) {
                Log.d(TAG, "Device tag set successfully: " + APP_TAG);
            }

            @Override
            public void onFailure(Exception e) {
            	Log.e(TAG, "Error setting device tag: " + e.getMessage());
            }

            @Override
            public void onComplete(JSONObject json, Header[] headers, StatusLine status) {
            	Log.d(TAG, "DeviceReady onComplete");
            }
        });

        // Create the trigger(s)
        if (mCreateTriggersTask == null) createGeotriggers(credentials);
    }

	private void createGeotriggers(DeviceCredentials credentials) {
		mCreateTriggersTask = new TriggerTask(credentials, this, mHandler);

/*		createGeotrigger("med-vm-leave", "Veterans Memorial leave", "Leave: Veterans Memorial", "leave",
				"""http://maps.esri.com/apl4/GeotriggerTest?id=exit_cactus", 
				34.059349, -117.195609, 100, 1, sAppTag);*/
		createGeotrigger("med-vm-enter", "Veterans Memorial enter", "enter: Veterans Memorial", "enter",
				"", 34.059229, -117.195622, 350, 20, APP_TAG);
	}

    /** Create a single geotrigger */
    private void createGeotrigger(String id, String name, String text, String direction, String url,
            double latitude, double longitude, double distance, int times, String tag) {
        if (mCreateTriggersTask != null) {
            Log.d(TAG, "Creating " + direction + " trigger around " + name);
            JSONObject params = new JSONObject();
            try {
                JSONObject point = new JSONObject();
                point.put("latitude", latitude);
                point.put("longitude", longitude);
                point.put("distance", distance);

                JSONObject condition = new JSONObject();
                condition.put("geo", point);
                condition.put("direction", direction);

                JSONObject message = new JSONObject();
                message.put("text", text);
                message.put("url", url);

                JSONObject action = new JSONObject();
                action.put("message", message);

                params.put("condition", condition);
                params.put("action", action);
                params.put("triggerId", id);
                params.put("times", times);
                params.put("setTags", tag);

            } catch (JSONException e) {
                Log.e(TAG, "Error creating trigger/create post body", e);
            }
            mCreateTriggersTask.post("create", params, new ApiRequestListener() {
                @Override
                public void onSuccess(JSONObject json, Header[] headers) {
                    Log.d(TAG, "Trigger Created! " + json.toString());
                }

                @Override
                public void onFailure(Exception e) {
                }

                @Override
                public void onComplete(JSONObject json, Header[] headers, StatusLine status) {
                }
            });
        } else {
            Log.e(TAG, "Device is not ready! You must wait for onDeviceReady before making API calls");
        }
    }
}
