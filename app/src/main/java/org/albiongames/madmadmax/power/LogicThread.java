package org.albiongames.madmadmax.power;

/**
 * Created by dair on 12/06/16.
 */
public class LogicThread extends GenericThread
{
    PowerService mService = null;

    LogicThread(PowerService service)
    {
        mService = service;
    }

    @Override
    protected void periodicTask()
    {
        if (mService == null || mService.getStatus() != PowerService.STATUS_ON)
            return;

        while (true)
        {
            StorageEntry.Base entry = mService.getLogicStorage().get();
            if (entry == null)
                break;

            mService.getNetworkStorage().put(entry);
            mService.getLogicStorage().remove();

            Tools.log("LogicThread: Logic: " + Integer.toString(mService.getLogicStorage().size()) + ", Network: " +
                Integer.toString(mService.getNetworkStorage().size()));
        }
    }

}
