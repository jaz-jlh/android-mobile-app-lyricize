package com.dodrio.finalproject;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.io.FileOutputStream;

/**
 * Created by Spivey on 11/17/2016.
 */

public class GPSService extends Service {
    LocationManager locationManager;
    LocationListener locationListener;

    public int singleInstanceFlag = 0;

    public GPSService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i("GPSService", "Service Binded");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Toast.makeText(this, "Service Started", Toast.LENGTH_SHORT).show();
        Log.i("GPSService", "Service onStart");
        singleInstanceFlag = 0;
        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                Log.d("GPSService", "location changed");
                if(singleInstanceFlag == 0) {
                    singleInstanceFlag = 1;
                    doServiceWork(location);
                }

            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {
                Log.d("GPSService", "provider enabled");
            }

            public void onProviderDisabled(String provider) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
                Log.d("GPSService", "provider disabled");
            }
        };

        // Register the listener with the Location Manager to receive location updates
        if(PackageManager.PERMISSION_GRANTED==checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } else {
            //requestPermissions(LOCATION_PERMS, LOCATION_REQUEST);
            Log.d("Intent Example", "Security Exception");
        }
        /*try{
            Log.d("GPSService", "getting location...");
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        } catch (SecurityException e) {
            Log.d("GPSService", "Security Exception " + e.getMessage());
        }*/

        return super.onStartCommand(intent,flags,startId);
    }

    @Override
    public void onCreate() {
        Log.i("GPSService", "Service onCreate");

    }

    @Override
    public void onDestroy() {
        //Toast.makeText(this, "Service Stopped", Toast.LENGTH_SHORT).show();
        Log.i("GPSService", "Service onDestroy");
        _shutdownService();

    }

    private void doServiceWork(Location location) {
        //Toast.makeText(this, "Location: " + location.toString(), Toast.LENGTH_LONG).show();
        String string = location.toString();
        string = string.substring(13,22) + ", " + string.substring(23,33);
        Intent intent = new Intent();
        intent.setAction("GPS");
        intent.putExtra("coordinates",string);
        Log.d("doServiceWork","Sending these coordinates: " + string);
        this.sendBroadcast(intent);
        _shutdownService();
    }

    private void _shutdownService() {
        try{
            locationManager.removeUpdates(locationListener);
        } catch (SecurityException e) {
            Log.d("GPSService", "Security Exception" + e.getMessage());
        }
        Log.i("GPSService", "Timer stopped...");
    }


}
