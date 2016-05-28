package org.albiongames.madmadmax.power;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by dair on 31/03/16.
 */
public class LocationThread extends GenericThread implements LocationListener
{
    private PowerService mService = null;
    private long mLastUpdate = 0;

    public LocationThread(PowerService service)
    {
        super();

        mService = service;
    }

    @Override
    protected void periodicTask()
    {
        long now = System.currentTimeMillis();
        if (now - mLastUpdate > Settings.getLong(Settings.KEY_GPS_IDLE_INTERVAL))
        {
            mService.getStorageThread().addEntry(new StorageEntry.MarkerStart());
            mLastUpdate = now;
        }
    }

    @Override
    protected void onStart()
    {
        LocationManager locationManager = (LocationManager) mService.getSystemService(Context.LOCATION_SERVICE);

        if (locationManager == null)
            return;

        try
        {
            mLastUpdate = System.currentTimeMillis();

            mService.getStorageThread().addEntry(new StorageEntry.MarkerStart());
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, Settings.getLong(Settings.KEY_MIN_GPS_TIME), Settings.getLong(Settings.KEY_MIN_GPS_DISTANCE), this);
        }
        catch (SecurityException exceptiion)
        {
            // cry out loud
        }
    }

    @Override
    protected void onStop()
    {
        LocationManager locationManager = (LocationManager) mService.getSystemService(Context.LOCATION_SERVICE);
        try
        {
            locationManager.removeUpdates(this);

            mService.getStorageThread().addEntry(new StorageEntry.MarkerStop());
        }
        catch (SecurityException exceptiion)
        {
            // cry out loud
        }
        mService = null;
    }

    /// Location Listener methods
    public void onLocationChanged(Location location)
    {
        // Called when a new location is found by the network location provider.
        if (location == null)
            return;

        double lat = location.getLatitude();
        double lon = location.getLongitude();
        float speed = location.getSpeed();
        long time = location.getTime();

        mService.getStorageThread().addEntry(new StorageEntry.Location(time, lat, lon, speed));
        mLastUpdate = time;
    }

    public void onStatusChanged(String provider, int status, Bundle extras)
    {
        mService.getStorageThread().addEntry(new StorageEntry.Info("onStatusChanged: " + provider + ": " + Integer.toString(status)));
    }

    public void onProviderEnabled(String provider)
    {
        mService.getStorageThread().addEntry(new StorageEntry.Info("onProviderEnabled: " + provider));
    }

    public void onProviderDisabled(String provider)
    {
        mService.getStorageThread().addEntry(new StorageEntry.Info("onProviderDisabled: " + provider));
    }
}
