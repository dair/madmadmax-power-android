package org.albiongames.madmadmax.power;

import java.lang.Thread;

/**
 * Created by dair on 31/03/16.
 */
public class GenericThread extends Thread
{
    private long mTimeout = 1000;

    protected void periodicTask()
    {
        // do nothing
    }

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

        while (!isInterrupted())
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
    }

    public void graciousStop()
    {
        onStop();
        interrupt();
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
