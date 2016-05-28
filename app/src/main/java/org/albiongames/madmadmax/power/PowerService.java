package org.albiongames.madmadmax.power;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.util.HashMap;
import java.util.Map;

public class PowerService extends Service
{
    public static final int THREAD_BLUETOOTH = 0;
    public static final int THREAD_LOCATION = 1;
    public static final int THREAD_NETWORKING = 2;
    public static final int THREAD_STORAGE = 3;

    public static final int STATUS_OFF = 0;
    public static final int STATUS_ON = 1;
    public static final int STATUS_STARTING = 2;
    public static final int STATUS_CLOSING = 3;

    private int mStatus = STATUS_OFF;

    private final LocalBinder mBinder = new LocalBinder();

    BluetoothThread mBluetoothThread = null;
    NetworkingThread mNetworkingThread = null;
    LocationThread mLocationThread = null;
    StorageThread mStorageThread = null;

    Map<Integer, GenericThread> mThreads = new HashMap<Integer, GenericThread>();

    Activity mActivity = null;

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
        mNetworkingThread = new NetworkingThread();
        mLocationThread = new LocationThread(this);
        mBluetoothThread = new BluetoothThread();
        mStorageThread = new StorageThread(this);

        mThreads.put(THREAD_BLUETOOTH, mBluetoothThread);
        mThreads.put(THREAD_LOCATION, mLocationThread);
        mThreads.put(THREAD_NETWORKING, mNetworkingThread);
        mThreads.put(THREAD_STORAGE, mStorageThread);

        mStorageThread.start();
        mLocationThread.start();
        mNetworkingThread.start();
        mBluetoothThread.start();

        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mLocationThread.graciousStop();
        mNetworkingThread.graciousStop();
        mBluetoothThread.graciousStop();
        mStorageThread.graciousStop();

        mThreads.clear();

        mStorageThread = null;
        mLocationThread = null;
        mNetworkingThread = null;
        mBluetoothThread = null;
    }

    public StorageThread getStorageThread()
    {
        return mStorageThread;
    }
}
