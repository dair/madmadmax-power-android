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
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by dair on 31/03/16.
 */
public class LocationThread extends StatusThread implements LocationListener
{
    private PowerService mService = null;
    private long mLastUpdate = 0;
    private Looper mLooper = null;
    Timer mTimer = null;

    long mGpsTime = 0;
    long mGpsDistance = 0;

    Location mLastLocation = null;
    List<Location> mLastLocations = new LinkedList<>();

    public LocationThread(PowerService service)
    {
        super();

        mService = service;
    }

    @Override
    public void run()
    {
        setStatus(STATUS_STARTING);
        Tools.log("LocationThread: start");
        Looper.prepare();
        mLooper = Looper.myLooper();

        onStart();

        setStatus(STATUS_ON);
        Looper.loop();

        setStatus(STATUS_STOPPING);
        onStop();
        mLooper = null;
        Tools.log("LocationThread: stop");
        setStatus(STATUS_OFF);
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
        mLastLocation = null;
        Settings.setDouble(Settings.KEY_AVERAGE_SPEED, 0.0);

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

        Settings.setDouble(Settings.KEY_LAST_INSTANT_SPEED, 0.0);
        Settings.setLong(Settings.KEY_LAST_GPS_UPDATE, 0);

        Settings.setLong(Settings.KEY_LOCATION_THREAD_STATUS, STATUS_STARTING);
        Settings.setLong(Settings.KEY_LOCATION_THREAD_SATELLITES, 0);

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

        Settings.setLong(Settings.KEY_LOCATION_THREAD_STATUS, STATUS_OFF);
        Settings.setLong(Settings.KEY_LOCATION_THREAD_SATELLITES, 0);
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
        int satellites = -1;

        if (location.getExtras().containsKey("satellites"))
        {
            satellites = location.getExtras().getInt("satellites");
            Settings.setLong(Settings.KEY_LOCATION_THREAD_SATELLITES, satellites);

            if (satellites < Settings.getLong(Settings.KEY_MIN_SATELLITES))
                return;
        }

        float acc = -1;
        if (location.hasAccuracy())
        {
            acc = location.getAccuracy();

            if (acc > Settings.getLong(Settings.KEY_MIN_ACCURACY))
                return;
        }

        Tools.log("Got location: " + location.toString());

        double lat = location.getLatitude();
        double lon = location.getLongitude();
        float speed = location.getSpeed();
        long time = location.getTime();

        Settings.setDouble(Settings.KEY_LAST_INSTANT_SPEED, speed);
        Settings.setLong(Settings.KEY_LAST_GPS_UPDATE, time);

        double localDistance = 0;
        if (mLastLocation != null && speed > 0.001)
        {
            localDistance = location.distanceTo(mLastLocation);
        }
        mLastLocation = location;

        StorageEntry.Location location1 = new StorageEntry.Location(time, lat, lon, acc, speed, localDistance, satellites);
        mService.getPositions().add(location1);

        mService.getLogicStorage().put(location1);
        mLastUpdate = time;

        addLocation(location);

        Settings.setLong(Settings.KEY_LOCATION_THREAD_STATUS, STATUS_ON);

    }

    void addLocation(Location location)
    {
        mLastLocations.add(location);

        long now = System.currentTimeMillis();
        long minTime = now - Settings.getLong(Settings.KEY_AVERAGE_SPEED_TIME);
        boolean haveBorder = false;

        int i = mLastLocations.size() - 1;
        for (; i >= 0; --i)
        {
            Location l = mLastLocations.get(i);
            if (l.getTime() < minTime)
            {
                if (haveBorder)
                    break;
                else
                    haveBorder = true; // we should have ONE value less than our border
            }
        }

        while (i > 0)
        {
            mLastLocations.remove(0);
            --i;
        }

        Settings.setDouble(Settings.KEY_AVERAGE_SPEED, averageSpeed());
    }

    public void onStatusChanged(String provider, int status, Bundle extras)
    {
        int satellites = extras.getInt("satellites");

        Settings.setLong(Settings.KEY_LOCATION_THREAD_SATELLITES, satellites);

        Tools.log("LocationTread::onStatusChanged: "+ provider + ": " + Integer.toString(status) + ": satellites = " + Integer.toString(satellites));
    }

    public void onProviderEnabled(String provider)
    {
        Tools.log("LocationThread::onProviderEnabled: " + provider);
        Settings.setLong(Settings.KEY_LOCATION_THREAD_STATUS, STATUS_ON);
    }

    public void onProviderDisabled(String provider)
    {
        Tools.log("LocationThread::onProviderDisabled: " + provider);
        Settings.setLong(Settings.KEY_LOCATION_THREAD_STATUS, STATUS_OFF);
    }

    public synchronized float averageSpeed()
    {
        if (mLastLocations.isEmpty())
            return 0.0f;

        float speed = 0;
        long now = System.currentTimeMillis();
        long duration = Settings.getLong(Settings.KEY_AVERAGE_SPEED_TIME);
        long minTime = now - duration;

        LinkedList<Long> xs = new LinkedList<>();
        LinkedList<Float> ys = new LinkedList<>();
        boolean haveBorder = false;

        Location prevLocation = null;
        for (int i = mLastLocations.size() - 1; i >= 0; --i)
        {
            Location l = mLastLocations.get(i);

            long x = l.getTime();
            float y = l.getSpeed();

            if (x < minTime)
            {
                if (haveBorder)
                    break;
                else
                {
                    if (prevLocation == null)
                        return l.getSpeed();

                    float df = prevLocation.getSpeed() - y;
                    long dt = prevLocation.getTime() - x;

                    long mt = minTime - x;

                    float s = (mt * df) / dt;


                    x = minTime;
                    y = s;

                    haveBorder = true;
                }
            }

            xs.add(0, x);
            ys.add(0, y);

            prevLocation = l;
        }

        if (!haveBorder)
        {
            xs.add(0, minTime);
            ys.add(0, 0.0f);
        }

        float totalSquare = 0.0f;
        for (int i = 1; i < xs.size(); ++i)
        {
            long x0 = xs.get(i - 1);
            float y0 = ys.get(i - 1);
            long x1 = xs.get(i);
            float y1 = ys.get(i);

            float yMin = Math.min(y0, y1);
            float yMax = Math.max(y0, y1);

            float square = yMin * (y1 - y0) + ((yMax - yMin) * (y1 - y0)) / 2;

            totalSquare += square;
        }

        speed = totalSquare / duration;

        return speed;
    }
}
