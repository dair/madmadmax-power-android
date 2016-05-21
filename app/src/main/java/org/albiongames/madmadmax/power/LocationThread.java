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

/**
 * Created by dair on 31/03/16.
 */
public class LocationThread extends GenericThread implements LocationListener
{
    private Service mService = null;

    public LocationThread(Service service)
    {
        super();

        mService = service;
    }

    @Override
    protected void periodicTask()
    {

    }

    @Override
    protected void onStart()
    {
        LocationManager locationManager = (LocationManager) mService.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null)
            return;

        try
        {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
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
        }
        catch (SecurityException exceptiion)
        {
            // cry out loud
        }
    }

    /// Location Listener methods
    public void onLocationChanged(Location location)
    {
        // Called when a new location is found by the network location provider.

    }

    public void onStatusChanged(String provider, int status, Bundle extras)
    {

    }

    public void onProviderEnabled(String provider)
    {

    }

    public void onProviderDisabled(String provider)
    {

    }
}
