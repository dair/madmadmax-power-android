package org.albiongames.madmadmax.power;

import android.net.TrafficStats;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by dair on 31/03/16.
 */
public class NetworkingThread extends StatusThread
{
    PowerService mService = null;
    boolean mWorking = false;
    static long mLastParamsRequest = 0;
    static long mLastNetworkInteraction = 0;

    String mDeviceId = null;

    long mErrorSleep = 500;

    public static class Request
    {
        private String mMethod;
        private String mUrl;
        private String mBody;

        public Request(final String method, final String url, JSONObject object)
        {
            mMethod = method;
            mUrl = url;
            mBody = object.toString();
        }

        public Request(final String method, final String url, final String body)
        {
            mMethod = method;
            mUrl = url;
            mBody = body;
        }

        public String getMethod()
        {
            return mMethod;
        }

        public String getUrl()
        {
            return mUrl;
        }

        public String getBody()
        {
            return mBody;
        }
    }

    public static class Response
    {
        private JSONObject mObject = null;
        public Response(JSONObject object)
        {
            mObject = object;
        }

        public final JSONObject getObject()
        {
            return mObject;
        }
    }

    public static String baseUrl()
    {
        String storedUrl = Settings.getString(Settings.KEY_SERVER_URL);
        if (storedUrl == null)
            return null;
        if (!storedUrl.startsWith("http://") &&
            !storedUrl.startsWith("https://"))
        {
            storedUrl = "http://" + storedUrl;
        }

        while (storedUrl.endsWith("/"))
        {
            storedUrl = storedUrl.substring(0, storedUrl.length()-1);
        }

        return storedUrl;
    }

    public static String authUrl()
    {
        return baseUrl() + "/device/reg";
    }

    public static String pUrl()
    {
        return baseUrl() + "/device/p";
    }

    public static String fuelUrl()
    {
        return baseUrl() + "/device/fuel";
    }

    public static String repairUrl()
    {
        return baseUrl() + "/device/repair";
    }

    NetworkingThread(PowerService service)
    {
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

        Request request = new Request("POST", pUrl(), object.toString());

        Response response = null;

        try
        {
            response = one(request);

            JSONObject responseObject = response.getObject();
            if (responseObject == null)
                throw new Exception("response object is null");

            int code = responseObject.getInt("code");

            if (code == 1) // success
            {
                mService.getNetworkStorage().remove();
                Settings.setLong(Settings.KEY_LATEST_SUCCESS_CONNECTION, System.currentTimeMillis());

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
            Settings.setLong(Settings.KEY_LATEST_FAILED_CONNECTION, System.currentTimeMillis());
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

        super.run();

        mDeviceId = Settings.getString(Settings.KEY_DEVICE_ID);
        if (mDeviceId == null || mDeviceId.isEmpty())
            return;

        int uid = android.os.Process.myUid();
        mRxBytes = TrafficStats.getUidTxBytes(uid);
        mTxBytes = TrafficStats.getUidRxBytes(uid);
        Settings.setLong(Settings.KEY_RX_BYTES, 0);
        Settings.setLong(Settings.KEY_TX_BYTES, 0);

        setStatus(STATUS_ON);

        while (getStatus() != STATUS_OFF)
        {
            if (mService.getNetworkStorage().isEmpty())
            {
                long now = System.currentTimeMillis();
                if (now - mLastNetworkInteraction > Settings.getLong(Settings.KEY_GPS_IDLE_INTERVAL))
                {
                    StorageEntry.Marker marker = new StorageEntry.Marker("ping");
                    mService.getNetworkStorage().put(marker);
                    mLastNetworkInteraction = now;
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

                        long storedRx = Settings.getLong(Settings.KEY_RX_BYTES);
                        long storedTx = Settings.getLong(Settings.KEY_TX_BYTES);

                        storedRx += rx;
                        storedTx += tx;

                        Settings.setLong(Settings.KEY_RX_BYTES, storedRx);
                        Settings.setLong(Settings.KEY_TX_BYTES, storedTx);

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
                if (mService.getLocationStorage().size() > Settings.getLong(Settings.KEY_LOCATION_PACKAGE_SIZE))
                {
                    StorageEntry.Bundle bundle = new StorageEntry.Bundle();
                    while (!mService.getLocationStorage().isEmpty())
                    {
                        StorageEntry.Base entry = mService.getLocationStorage().get();
                        bundle.add(entry);
                        mService.getLocationStorage().remove();
                    }

                    mService.getNetworkStorage().put(bundle);
                }
            }
        }

        setStatus(STATUS_OFF);

        Tools.log("NetworkingThread: stop");
    }

    static String addParamUpdate(String s)
    {
        String ret = s;
        try
        {
            JSONObject object = new JSONObject(s);
            long lastCommandId = Settings.getLong(Settings.KEY_LAST_COMMAND_ID);
            object.put("c", lastCommandId);

            long lastUpgradeTime = Settings.getLong(Settings.KEY_LAST_UPGRADE_TIME);
            object.put("u", lastUpgradeTime);

            ret = object.toString();
        }
        catch (JSONException ex)
        {
            // cry
        }

        return ret;
    }

    public static synchronized Response one(Request request) throws Exception
    {
        HttpURLConnection connection = null;
        URL url = new URL(request.getUrl());
        connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod(request.getMethod());
        connection.setReadTimeout((int)Settings.getLong(Settings.KEY_NETWORK_TIMEOUT));
        connection.setConnectTimeout((int)Settings.getLong(Settings.KEY_NETWORK_TIMEOUT));

        connection.setRequestProperty("Content-Type", "application/json");
        String bodyString = request.getBody();
        if (bodyString == null || bodyString.isEmpty())
            bodyString = "{}";

        if (System.currentTimeMillis() - mLastParamsRequest >= Settings.getLong(Settings.KEY_PARAM_UPDATE))
        {
            bodyString = addParamUpdate(bodyString);
        }

        byte[] bytes = bodyString.getBytes("UTF-8");
        int len = bytes.length;
//                connection.setRequestProperty("Content-Length", Integer.toString(len));
        connection.setFixedLengthStreamingMode(len);
        connection.setUseCaches(false);
        connection.setDoOutput(true);
        connection.setDoInput(true);

        connection.connect();

//        Tools.log("cathcing eof: 1");

        DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
        wr.write(bytes);
        wr.flush();

//        Tools.log("cathcing eof: 2");
        int status = 442;

        try
        {
            status = connection.getResponseCode();
        }
        catch (EOFException ex)
        {
            // server closed connection??
        }

//        Tools.log("cathcing eof: 2.5");
        if (status == HttpURLConnection.HTTP_OK)
        {
//            Tools.log("cathcing eof: 3");
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder responseString = new StringBuilder(); // or StringBuffer if not Java 5+
            String line;
            while ((line = rd.readLine()) != null)
            {
                responseString.append(line);
                responseString.append('\r');
            }
//            Tools.log("cathcing eof: 4");
            rd.close();
//            Tools.log("cathcing eof: 5");

            JSONObject object = new JSONObject(responseString.toString());

            if (object.has("params"))
            {
                JSONObject params = object.getJSONObject("params");
                Settings.networkUpdate(params);
                object.remove("params");
                mLastParamsRequest = System.currentTimeMillis();
            }

            if (object.has("upgrades"))
            {
                JSONObject params = object.getJSONObject("upgrades");

                long storeTime = Settings.getLong(Settings.KEY_LAST_UPGRADE_TIME);

                if (params.has("time"))
                {
                    storeTime = params.getLong("time");
                    params.remove("time");
                }

                if (PowerService.instance() != null)
                {
                    Upgrades.upgradesFromNetwork(params);
                }
                Settings.setLong(Settings.KEY_LAST_UPGRADE_TIME, storeTime);



                object.remove("upgrades");
            }
//            Tools.log("cathcing eof: 6");

            Response response = new Response(object);
            wr.close();

//            Tools.log("cathcing eof: 7");

            return response;
        }
        else
        {
            throw new Exception(connection.getResponseMessage());
        }
    }

    public void graciousStop()
    {
        if (getStatus() == STATUS_ON)
            setStatus(STATUS_STOPPING);
    }

}

