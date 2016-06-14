package org.albiongames.madmadmax.power;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by dair on 31/03/16.
 */
public class LocationThread extends Thread implements LocationListener
{
    private PowerService mService = null;
    private long mLastUpdate = 0;
    private Looper mLooper = null;

    public LocationThread(PowerService service)
    {
        super();

        mService = service;
    }

    @Override
    public void run()
    {
        Looper.prepare();
        mLooper = Looper.myLooper();

        onStart();

        Looper.loop();

        onStop();
        mLooper = null;
    }

//    @Override
//    protected void periodicTask()
//    {
//        if (mService == null || mService.getStatus() != PowerService.STATUS_ON)
//            return;
//
//        long now = System.currentTimeMillis();
//        if (now - mLastUpdate > Settings.getLong(Settings.KEY_GPS_IDLE_INTERVAL))
//        {
//            mService.getLogicStorage().put(new StorageEntry.Marker("ping"));
//            mLastUpdate = now;
//        }
//    }

    protected void onStart()
    {
        LocationManager locationManager = (LocationManager) mService.getSystemService(Context.LOCATION_SERVICE);

        if (locationManager == null)
            return;

        try
        {
            mLastUpdate = System.currentTimeMillis();

            mService.getLogicStorage().put(new StorageEntry.MarkerStart());

            long gpsTime = Settings.getLong(Settings.KEY_MIN_GPS_TIME);
            long gpsDistance = Settings.getLong(Settings.KEY_MIN_GPS_DISTANCE);

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, gpsTime, gpsDistance, this);

            Tools.log("Location Thread started");
        }
        catch (SecurityException exceptiion)
        {
            // cry out loud
        }
    }

    protected void onStop()
    {
        LocationManager locationManager = (LocationManager) mService.getSystemService(Context.LOCATION_SERVICE);
        try
        {
            locationManager.removeUpdates(this);

            mService.getLogicStorage().put(new StorageEntry.MarkerStop());
        }
        catch (SecurityException exception)
        {
            // cry out loud
        }
        mService = null;
    }

    public void graciousStop()
    {
        if (mLooper != null)
            mLooper.quit();
    }

    /// Location Listener methods
    public void onLocationChanged(Location location)
    {
        // Called when a new location is found by the network location provider.
        if (location == null)
            return;
        int satellites = location.getExtras().getInt("satellites");

        Tools.log("Got location: " + location.toString());

        if (satellites < Settings.getLong(Settings.KEY_MIN_SATELLITES))
            return;

        double lat = location.getLatitude();
        double lon = location.getLongitude();
        float speed = location.getSpeed();
        long time = location.getTime();

        mService.getLogicStorage().put(new StorageEntry.Location(time, lat, lon, speed, satellites));
        mLastUpdate = time;
    }

    public void onStatusChanged(String provider, int status, Bundle extras)
    {
//        int satellites = extras.getInt("satellites");

//        mService.getLogicStorage().put(new StorageEntry.Info("onStatusChanged: " + provider + ": " + Integer.toString(status) + ": satellites = " + Integer.toString(satellites)));
    }

    public void onProviderEnabled(String provider)
    {
//        mService.getLogicStorage().put(new StorageEntry.Info("onProviderEnabled: " + provider));
    }

    public void onProviderDisabled(String provider)
    {
//        mService.getLogicStorage().put(new StorageEntry.Info("onProviderDisabled: " + provider));
    }
}
