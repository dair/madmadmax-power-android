package org.albiongames.madmadmax.power.service;

import static org.albiongames.madmadmax.power.network.NetworkTools.*;

import android.net.TrafficStats;

import org.albiongames.madmadmax.power.data_storage.Settings;
import org.albiongames.madmadmax.power.Tools;
import org.albiongames.madmadmax.power.data_storage.Storage;
import org.albiongames.madmadmax.power.data_storage.StorageEntry;
import org.albiongames.madmadmax.power.data_storage.Upgrades;
import org.albiongames.madmadmax.power.network.NetworkTools;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPOutputStream;

/**
 * Created by dair on 31/03/16.
 */
public class NetworkingThread extends StatusThread
{
    PowerService mService = null;
    boolean mWorking = false;

    String mDeviceId = null;

    long mErrorSleep = 500;



    NetworkingThread(PowerService service, Settings settings)
    {
        super(settings);
        mService = service;
    }

    boolean checkFinishMarker(JSONObject object)
    {
        if (object == null)
            return false;

        try
        {
            if (object.has("type") && object.getString("type").equals("marker") &&
                    object.has("tag") && object.getString("tag").equals("stop"))
                return true;
        }
        catch (JSONException ex)
        {

        }

        return false;
    }

    boolean processOneItem(StorageEntry.Base entry)
    {
        boolean ret = false;

        JSONObject object = entry.toJsonObject();
        try
        {
            object.put("id", mDeviceId);
        }
        catch (JSONException ex)
        {
        }

        Request request = new Request("POST", pUrl(settings), object.toString());

        Response response = null;

        try
        {
            response = one(request, ZIP_AUTO,getSettings());

            JSONObject responseObject = response.getObject();
            if (responseObject == null)
                throw new Exception("response object is null");

            int code = responseObject.getInt("code");

            if (code == 1) // success
            {
                mService.getNetworkStorage().remove();
                getSettings().setLong(Settings.KEY_LATEST_SUCCESS_CONNECTION, System.currentTimeMillis());

                if (getStatus() == STATUS_STOPPING && checkFinishMarker(object))
                {
                    setStatus(STATUS_OFF);
                }

                ret = true;
            }
            else
            {
                throw new Exception("Code validation failed");
            }
        }
        catch (Exception ex)
        {
            Tools.log("Networking exception: " + ex.toString());
            getSettings().setLong(Settings.KEY_LATEST_FAILED_CONNECTION, System.currentTimeMillis());
            ret = false;
        }

        return ret; // try again later
    }

    long mRxBytes = -1;
    long mTxBytes = -1;

    @Override
    public void run()
    {
        Tools.log("NetworkingThread: start");
        int mErrorCountOnExit = 0;

        setRx(0);
        setTx(0);

        super.run();

        mDeviceId = getSettings().getString(Settings.KEY_DEVICE_ID);
        if (mDeviceId == null || mDeviceId.isEmpty())
            return;

        int uid = android.os.Process.myUid();
        mRxBytes = TrafficStats.getUidTxBytes(uid);
        mTxBytes = TrafficStats.getUidRxBytes(uid);
        getSettings().setLong(Settings.KEY_RX_BYTES, 0);
        getSettings().setLong(Settings.KEY_TX_BYTES, 0);

        setStatus(STATUS_ON);

        while (getStatus() != STATUS_OFF)
        {
            if (mService.getNetworkStorage().isEmpty())
            {
                long now = System.currentTimeMillis();
                if (now - getLastNetworkInteraction() > getSettings().getLong(Settings.KEY_GPS_IDLE_INTERVAL))
                {
                    StorageEntry.Marker marker = new StorageEntry.Marker("ping");
                    mService.getNetworkStorage().put(marker);
                    setLastNetworkInteraction(now);
                }
            }

            int number = 0;

            if (mService.getNetworkStorage().isEmpty())
            {
                // no data
                if (getStatus() == STATUS_STOPPING)
                    break;
                Tools.sleep(500);
            }
            else
            {
                StorageEntry.Base entry = mService.getNetworkStorage().get();
                if (entry == null)
                {
                    mService.getNetworkStorage().remove(); // wtf?
                }
                else if (entry.isTypeOf(StorageEntry.TYPE_LOCATION))
                {
                    mService.getLocationStorage().put(entry);
                    mService.getNetworkStorage().remove();
                }
                else if (entry.isTypeOf(StorageEntry.TYPE_INFO))
                {
                    mService.getInfoStorage().put(entry);
                    mService.getNetworkStorage().remove();
                }
                else
                {
                    boolean result = processOneItem(entry);

                    if (!result) {
                        // network error
                        if (getStatus() == STATUS_STOPPING) {
                            mErrorSleep = 500;
                            if (mErrorCountOnExit > 3)
                                break;
                            else
                                ++mErrorCountOnExit;
                        }

                        Tools.sleep(mErrorSleep);
                        if (mErrorSleep < 5000)
                            mErrorSleep += 500;
                    } else {

                        long rxBytes = TrafficStats.getUidTxBytes(uid);
                        long txBytes = TrafficStats.getUidRxBytes(uid);
                        long rx = rxBytes - mRxBytes;
                        long tx = txBytes - mTxBytes;

                        long storedRx = getSettings().getLong(Settings.KEY_RX_BYTES);
                        long storedTx = getSettings().getLong(Settings.KEY_TX_BYTES);

                        storedRx += rx;
                        storedTx += tx;

                        getSettings().setLong(Settings.KEY_RX_BYTES, storedRx);
                        getSettings().setLong(Settings.KEY_TX_BYTES, storedTx);

                        // successfully sent a packet to the server
                        mErrorCountOnExit = 0;
                        mErrorSleep = 500;

                        // on success let's see how much we have left
                        if (getStatus() == STATUS_STOPPING) {
                            if (mService.getNetworkStorage().size() > 10) // whoa, that's too much, stop now
                            {
                                break;
                            }
                        }
                    }
                }
            }

            synchronized (mService.getLocationStorage())
            {
                if (mService.getLocationStorage().size() > getSettings().getLong(Settings.KEY_LOCATION_PACKAGE_SIZE))
                {
                    flushQueue(mService.getLocationStorage());
                }
                
                if (mService.getLocationStorage().size() > getSettings().getLong(Settings.KEY_LOCATION_PACKAGE_SIZE))
                {
                    flushQueue(mService.getLocationStorage());
                }
            }
        }

        // on exit, flush locationqueue
        flushQueue(mService.getLocationStorage());
        flushQueue(mService.getInfoStorage());

        setStatus(STATUS_OFF);

        Tools.log("NetworkingThread: stop");
    }



    void flushQueue(Storage storage)
    {
        StorageEntry.Bundle bundle = new StorageEntry.Bundle();
        while (!storage.isEmpty())
        {
            StorageEntry.Base entry = storage.get();
            bundle.add(entry);
            storage.remove();
        }

        if (bundle.size() > 0)
            mService.getNetworkStorage().put(bundle);

    }





    public void graciousStop()
    {
        if (getStatus() == STATUS_ON)
            setStatus(STATUS_STOPPING);
    }


}

