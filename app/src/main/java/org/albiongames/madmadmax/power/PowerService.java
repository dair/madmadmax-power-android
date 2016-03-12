package org.albiongames.madmadmax.power;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class PowerService extends Service
{
    public static final int STATUS_OFF = 0;
    public static final int STATUS_ON = 1;
    public static final int STATUS_STARTING = 2;
    public static final int STATUS_CLOSING = 3;

    private int mStatus = STATUS_OFF;

    private final LocalBinder mBinder = new LocalBinder();

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

    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        // TODO
        return START_STICKY;
    }
}