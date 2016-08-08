package org.albiongames.madmadmax.power.service;

/**
 * Created by dair on 23/06/16.
 */
public abstract class StatusThread extends Thread
{
    public static final int STATUS_OFF = 0;
    public static final int STATUS_ON = 1;
    public static final int STATUS_STARTING = 2;
    public static final int STATUS_STOPPING = 3;

    private int mStatus = STATUS_OFF;
    private long mLastStatusChangeTime = 0;

    protected void setStatus(int newStatus)
    {
        if (mStatus != newStatus)
        {
            mStatus = newStatus;
            mLastStatusChangeTime = System.currentTimeMillis();
        }
    }

    public int getStatus()
    {
        return mStatus;
    }

    public long getLastStatusChangeTime()
    {
        return mLastStatusChangeTime;
    }
}
