package org.albiongames.madmadmax.power;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;

import java.util.LinkedList;
import java.util.List;
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
    Timer mTimer = null;

    long mGpsTime = 0;
    long mGpsDistance = 0;

    List<Float> mLastSpeed = new LinkedList<>();

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

    private synchronized void askRequests()
    {
        long newGpsTime = Settings.getLong(Settings.KEY_MIN_GPS_TIME);
        long newGpsDistance = Settings.getLong(Settings.KEY_MIN_GPS_DISTANCE);

        if (newGpsTime != mGpsTime || newGpsDistance != mGpsDistance)
        {
            LocationManager locationManager = (LocationManager) mService.getSystemService(Context.LOCATION_SERVICE);

            if (locationManager == null)
                return;

            try
            {
                locationManager.removeUpdates(this);

                mGpsTime = newGpsTime;
                mGpsDistance = newGpsDistance;

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, mGpsTime, mGpsDistance, this);
            }
            catch (SecurityException ex)
            {

            }
        }

    }

    protected void onStart()
    {
        LocationManager locationManager = (LocationManager) mService.getSystemService(Context.LOCATION_SERVICE);

        if (locationManager == null)
            return;

        mLastUpdate = System.currentTimeMillis();

        mService.getLogicStorage().put(new StorageEntry.MarkerStart());

        askRequests();

        mTimer = new Timer("updates");
        mTimer.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                askRequests();
            }
        }, 1000, 1000);

        Tools.log("Location Thread started");
    }

    protected void onStop()
    {
        mTimer.purge();
        mTimer = null;

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
        float acc = location.getAccuracy();

        if (acc > Settings.getLong(Settings.KEY_MIN_ACCURACY))
            return;

        double lat = location.getLatitude();
        double lon = location.getLongitude();
        float speed = location.getSpeed();
        long time = location.getTime();

        StorageEntry.Location location1 = new StorageEntry.Location(time, lat, lon, acc, speed, satellites);
        mService.getPositions().add(location1);

        mService.getLogicStorage().put(location1);
        mLastUpdate = time;

        mLastSpeed.add(0, speed);
        while (mLastSpeed.size() > Settings.getLong(Settings.KEY_AVERAGE_SPEED_COUNT))
        {
            mLastSpeed.remove(mLastSpeed.size() - 1);
        }
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

    public float averageSpeed()
    {
        float speed = 0;
        int count = 0;
        long maxCount = Settings.getLong(Settings.KEY_AVERAGE_SPEED_COUNT);
        for (Float f: mLastSpeed)
        {
            ++count;
            if (count > maxCount)
                break;

            speed = speed + f;
        }
        speed = speed / maxCount;
        return speed;
    }
}
