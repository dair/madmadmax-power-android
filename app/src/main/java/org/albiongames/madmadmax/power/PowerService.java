package org.albiongames.madmadmax.power;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class PowerService extends Service
{
    static PowerService mInstance = null;

    public static PowerService instance()
    {
        return mInstance;
    }

    public static final int STATUS_OFF = 0;
    public static final int STATUS_ON = 1;
    public static final int STATUS_STARTING = 2;
    public static final int STATUS_CLOSING = 3;

    private int mStatus = STATUS_OFF;

    public int getStatus()
    {
        return mStatus;
    }

    private final LocalBinder mBinder = new LocalBinder();

    BluetoothThread mBluetoothThread = null;
    NetworkingThread mNetworkingThread = null;
    LocationThread mLocationThread = null;
    LogicThread mLogicThread = null;

    List<StorageEntry.Base> mPositions = new LinkedList<>();
//    Map<Integer, GenericThread> mThreads = new HashMap<Integer, GenericThread>();

    Activity mActivity = null;

    Storage mLogicStorage = null;
    Storage mNetworkStorage = null;

    Error mError = null;

    public class LocalBinder extends Binder
    {
        PowerService getService()
        {
            // Return this instance of LocalService so clients can call public methods
            return PowerService.this;
        }
    }

    public PowerService()
    {
        Tools.log("Service: ctor");
    }

    void setActivity(Activity activity)
    {
        mActivity = activity;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Tools.log("Service: onStartCommand");

        if (mStatus == STATUS_OFF)
        {
            mInstance = this;

            mStatus = STATUS_STARTING;
            try
            {
                mLogicStorage = new Storage(getFilesDir() + "/logic");
                mNetworkStorage = new Storage(getFilesDir() + "/network");
            } catch (IOException ex)
            {
                mError = new Error(ex.getLocalizedMessage());
            }

            mNetworkingThread = new NetworkingThread(this);
            mNetworkingThread.start();
            mLocationThread = new LocationThread(this);
            mLocationThread.start();
            mLogicThread = new LogicThread(this);
            mLogicThread.start();

            mStatus = STATUS_ON;
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        Tools.log("Service: onDestroy");
        super.onDestroy();
    }

    public Error getError()
    {
        return mError;
    }

    public Storage getLogicStorage()
    {
        return mLogicStorage;
    }

    public Storage getNetworkStorage()
    {
        return mNetworkStorage;
    }

    protected void iGraciousStop()
    {
        mStatus = STATUS_CLOSING;

        mNetworkingThread.graciousStop();
        mLogicThread.graciousStop();
        mLocationThread.graciousStop();

        while (mNetworkingThread.isAlive())
        {
            Tools.sleep(100);
        }

        mStatus = STATUS_OFF;
        stopSelf();
    }

    public static void graciousStop()
    {
        new AsyncTask()
        {
            @Override
            protected Object doInBackground(Object[] params)
            {
                mInstance.iGraciousStop();
                return null;
            }
        }.execute();
    }

    public List<StorageEntry.Base> getPositions()
    {
        return mPositions;
    }

    public int getStatusThreadStatus(StatusThread t)
    {
        if (t == null)
            return StatusThread.STATUS_OFF;
        return t.getStatus();
    }

    public int getNetworkThreadStatus()
    {
        return getStatusThreadStatus(mNetworkingThread);
    }

    public int getLocationThreadStatus()
    {
        return getStatusThreadStatus(mLocationThread);
    }

    public int getLogicThreadStatus()
    {
        return getStatusThreadStatus(mLogicThread);
    }
}
