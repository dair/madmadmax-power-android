package org.albiongames.madmadmax.power;

import java.lang.Thread;

/**
 * Created by dair on 31/03/16.
 */
public abstract class GenericThread extends Thread
{
    private long mTimeout = 1000;

    public static final int STATUS_OFF = 0;
    public static final int STATUS_ON = 1;
    public static final int STATUS_STARTING = 2;
    public static final int STATUS_STOPPING = 3;

    private int mStatus = STATUS_OFF;

    protected abstract void periodicTask();

    protected void onStart()
    {
        // do nothing, reimplement in children
    }

    protected void onStop()
    {
        // do nothing, reimplement in children
    }

    @Override
    public void run()
    {
        super.run();

        mStatus = STATUS_STARTING;
        onStart();
        mStatus = STATUS_ON;

        Tools.log("Thread: startRunning");

        while (mStatus == STATUS_ON)
        {
            long timeStart = System.currentTimeMillis();
            periodicTask();
            long timeStop = System.currentTimeMillis();
            long duration = timeStop - timeStart;
            long sleepTime = mTimeout - duration;
            if (sleepTime > 0)
            {
                try
                {
                    sleep(sleepTime);
                }
                catch (InterruptedException ex)
                {
                    // interruped
                }
            }
        }

        onStop();

        Tools.log("Thread: interrupted");
        mStatus = STATUS_OFF;
    }

    public void graciousStop()
    {
        mStatus = STATUS_STOPPING;
        try
        {
            join();
        }
        catch (InterruptedException ex)
        {
            // what should I do here?..
        }
    }

    public void setTimeout(long t)
    {
        mTimeout = t;
    }
}
