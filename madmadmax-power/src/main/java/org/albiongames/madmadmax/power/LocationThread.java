package org.albiongames.madmadmax.power;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcel;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
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

    long mGpsTime = -100;
    long mGpsDistance = -100;

    Location mLastLocation = null;
    List<Location> mLastLocations = new LinkedList<>();

    boolean mMockRun = true;

    final static String COMPONENT = "LocationThread";

    public LocationThread(PowerService service)
    {
        super();

        mService = service;
    }

    @Override
    public void run()
    {
        if (Settings.getLong(Settings.KEY_MOCK_DATA) == Settings.MOCK_DATA_PLAY)
            runFromMock();
        else
            runNormal();
    }

    void runNormal()
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

    void runFromMock()
    {
        long sleepTime = 0;
        long lastLocationTime = 0;

        onStart();
        setStatus(STATUS_ON);

        FileInputStream stream = null;
        try
        {
            stream = new FileInputStream(mService.getFilesDir() + "/mock.dat");

            while (mMockRun)
            {
                int len = 0;
                int len1 = stream.read();
                int len2 = stream.read();

                len = (len2 << 8) + len1;

                if (len < 0)
                    break; // data finished?...

                byte[] data = new byte[len];
                stream.read(data);

                Parcel p = Parcel.obtain();
                p.unmarshall(data, 0, data.length);
                p.setDataPosition(0);

                Location l = Location.CREATOR.createFromParcel(p);

                long time = l.getTime();

                if (lastLocationTime == 0)
                {
                    sleepTime = 0;
                }
                else
                {
                    sleepTime = time - lastLocationTime;
                }

                if (sleepTime > 0)
                {
                    Tools.sleep(sleepTime);
                }

                l.setTime(System.currentTimeMillis());

                onLocationChanged(l);

                lastLocationTime = time;
            }
        }
        catch (IOException ex)
        {

        }
        finally
        {
            try
            {
                stream.close();
            }
            catch (IOException ex)
            {
            }
        }

        setStatus(STATUS_STOPPING);
        onStop();
        Tools.log("LocationThread: mock stop");
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

        long averageSpeedTime = Settings.getLong(Settings.KEY_AVERAGE_SPEED_TIME);
        Settings.setDouble(Settings.KEY_AVERAGE_SPEED, averageSpeed(averageSpeedTime));
    }

    protected void onStart()
    {
        if (!isMockPlay())
        {
            LocationManager locationManager = (LocationManager) mService.getSystemService(Context.LOCATION_SERVICE);


            if (locationManager == null)
                return;
        }

        mGpsTime = -100;
        mGpsDistance = -100;

        mLastUpdate = System.currentTimeMillis();
        mLastLocation = null;
        Settings.setDouble(Settings.KEY_AVERAGE_SPEED, 0.0);

        mService.getLogicStorage().put(new StorageEntry.MarkerStart());

        if (!isMockPlay())
        {
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
        }

        Settings.setDouble(Settings.KEY_LAST_INSTANT_SPEED, 0.0);
        Settings.setLong(Settings.KEY_LAST_GPS_UPDATE, 0);

        Settings.setLong(Settings.KEY_LOCATION_THREAD_STATUS, STATUS_STARTING);
        Settings.setLong(Settings.KEY_LOCATION_THREAD_LAST_QUALITY, -1);

        Tools.log("Location Thread started");

        if (Settings.getLong(Settings.KEY_MOCK_DATA) == Settings.MOCK_DATA_RECORD)
        {
            try
            {
                FileWriter writer = new FileWriter(mService.getFilesDir() + "/mock.dat");
                writer.close();
            }
            catch (IOException ex)
            {

            }
        }
    }

    protected void onStop()
    {
        if (mTimer != null)
        {
            mTimer.purge();
            mTimer = null;
        }

        if (!isMockPlay())
        {
            LocationManager locationManager = (LocationManager) mService.getSystemService(Context.LOCATION_SERVICE);
            try
            {
                locationManager.removeUpdates(this);

                mService.getLogicStorage().put(new StorageEntry.MarkerStop());
            } catch (SecurityException exception)
            {
                // cry out loud
            }
            mService = null;
        }

        Settings.setLong(Settings.KEY_LOCATION_THREAD_STATUS, STATUS_OFF);
        Settings.setLong(Settings.KEY_LOCATION_THREAD_LAST_QUALITY, -1);
    }

    public void graciousStop()
    {
        if (mLooper != null)
            mLooper.quit();
        else
            mMockRun = false;
    }

    /// Location Listener methods
    public void onLocationChanged(Location location)
    {
        // Called when a new location is found by the network location provider.
        if (location == null)
            return;

        long localTime = System.currentTimeMillis();
        location.setTime(localTime);

        if (Settings.getLong(Settings.KEY_MOCK_DATA) == Settings.MOCK_DATA_RECORD)
        {
            Parcel p = Parcel.obtain();
            location.writeToParcel(p, 0);
            final byte[] b = p.marshall();
            p.recycle();

            try
            {
                FileOutputStream output = new FileOutputStream(mService.getFilesDir() + "/mock.dat", true);
                int len = b.length;
                output.write(len & 0xFF);
                output.write((len >> 8) & 0xFF);

                output.write(b);
                output.close();
            }
            catch (IOException ex)
            {

            }
        }

        int satellites = -1;

        if (location.getExtras().containsKey("satellites"))
        {
            satellites = location.getExtras().getInt("satellites");

            if (satellites < Settings.getLong(Settings.KEY_MIN_SATELLITES))
            {
                mService.dump(COMPONENT, "rejecting location because of satellites: " + location.toString());
                Settings.setLong(Settings.KEY_LOCATION_THREAD_LAST_QUALITY, 0);
                return;
            }
        }

        float acc = -1;
        if (location.hasAccuracy())
        {
            acc = location.getAccuracy();

            if (acc > Settings.getLong(Settings.KEY_MIN_ACCURACY))
            {
                mService.dump(COMPONENT, "rejecting location because of accuracy: " + location.toString());
                Settings.setLong(Settings.KEY_LOCATION_THREAD_LAST_QUALITY, 0);
                return;
            }
        }

        Settings.setLong(Settings.KEY_LOCATION_THREAD_LAST_QUALITY, 1);

        Tools.log("Got location: " + location.toString());

        double lat = location.getLatitude();
        double lon = location.getLongitude();
        float speed = location.getSpeed();

        Settings.setDouble(Settings.KEY_LAST_INSTANT_SPEED, speed);
        Settings.setLong(Settings.KEY_LAST_GPS_UPDATE, localTime);

        double localDistance = 0;
        if (mLastLocation != null && speed > 0.001)
        {
            localDistance = location.distanceTo(mLastLocation);
        }
        mLastLocation = location;

        if (localDistance < 0.01)
        {
            return;
        }

        StorageEntry.Location location1 = new StorageEntry.Location(localTime, lat, lon, acc, speed, localDistance, satellites);

        mService.getLogicStorage().put(location1);
        mLastUpdate = localTime;

        addLocation(location);


        long averageSpeedTime = Settings.getLong(Settings.KEY_AVERAGE_SPEED_TIME);
        Settings.setDouble(Settings.KEY_AVERAGE_SPEED, averageSpeed(averageSpeedTime));
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
    }

    public void onStatusChanged(String provider, int status, Bundle extras)
    {
//        int satellites = extras.getInt("satellites");

//        Tools.log("LocationTread::onStatusChanged: "+ provider + ": " + Integer.toString(status) + ": satellites = " + Integer.toString(satellites));
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

    public synchronized float averageSpeed(long duration)
    {
        if (mLastLocations.isEmpty())
            return 0.0f;

        float speed = 0;
        long now = System.currentTimeMillis(); //mLastLocations.get(mLastLocations.size()-1).getTime();//
        long minTime = now - duration;
//        System.out.println("now is: " + Long.toString(now) + ", minTime: " + Long.toString(minTime));

        LinkedList<Long> xs = new LinkedList<>();
        LinkedList<Float> ys = new LinkedList<>();
        boolean haveBorder = false;

        Location prevLocation = null;

        String dump = "";

        for (int i = mLastLocations.size() - 1; i >= 0; --i)
        {
            Location l = mLastLocations.get(i);

//            System.out.println("position: " + Integer.toString(i) + ", location: " + l.toString());

            long x = l.getTime() - minTime;
            float y = l.getSpeed();

            dump += Long.toString(x) + ", " + Float.toString(y) + "; ";

            if (prevLocation == null)
            {
                xs.add(duration);
                ys.add(y);
            }

            if (x < 0)
            {
//                System.out.println("x < minTime");
                if (haveBorder)
                {
//                    System.out.println("not processing");
                    break;
                }
                else
                {
//                    System.out.println("set as border");
                    haveBorder = true;
                }

                if (prevLocation == null)
                    return l.getSpeed();

                float df = prevLocation.getSpeed() - y;
                long dt = (prevLocation.getTime() - minTime) - x;

                long mt = -x;

                float s = y + (mt * df) / dt;

                x = 0;
                y = s;
            }

            xs.add(0, x);
            ys.add(0, y);

            prevLocation = l;
        }

        Tools.log(dump);

        if (!haveBorder)
        {
//            System.out.println("adding x: " + Long.toString(minTime));
//            System.out.println("adding y: " + Float.toString(0.0f));

            xs.add(0, 0L);
            ys.add(0, 0.0f);
        }

        float totalSquare = 0.0f;
        for (int i = 1; i < xs.size(); ++i)
        {
            long x0 = xs.get(i - 1);
            float y0 = ys.get(i - 1);
            long x1 = xs.get(i);
            float y1 = ys.get(i);

//            System.out.println("iteration: " + Integer.toString(i) + ", x0: " + Long.toString(x0) + ", y0: " + Float.toString(y0) + ", x1: " + Long.toString(x1) + ", y1: " + Float.toString(y1));

            float yMin = Math.min(y0, y1);
            float yMax = Math.max(y0, y1);

            float square = yMin * (x1 - x0) + ((yMax - yMin) * (x1 - x0)) / 2;

            totalSquare += square;
        }

        speed = totalSquare / duration;

        return speed;
    }

    boolean isMockPlay()
    {
        return Settings.getLong(Settings.KEY_MOCK_DATA) == Settings.MOCK_DATA_PLAY;
    }
}
