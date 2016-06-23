package org.albiongames.madmadmax.power;

import java.lang.Thread;

/**
 * Created by dair on 31/03/16.
 */
public abstract class GenericThread extends StatusThread
{
    private long mTimeout = 1000;

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

        setStatus(STATUS_STARTING);
        onStart();
        setStatus(STATUS_ON);

        Tools.log("Thread: startRunning");

        while (getStatus() == STATUS_ON)
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
        setStatus(STATUS_OFF);
    }

    public void graciousStop()
    {
        setStatus(STATUS_STOPPING);
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
