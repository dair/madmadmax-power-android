package org.albiongames.madmadmax.power;

import android.util.DebugUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by dair on 31/03/16.
 */
public class NetworkingThread extends GenericThread
{
    PowerService mService = null;
    boolean mWorking = false;
    static long mLastParamsRequest = 0;
    static long mLastNetworkInteraction = 0;

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

    NetworkingThread(PowerService service)
    {
        mService = service;
    }

    @Override
    protected void periodicTask()
    {
        if (mService == null || mService.getStatus() != PowerService.STATUS_ON)
            return;

        if (mWorking)
            return;

        String deviceId = Settings.getString(Settings.KEY_DEVICE_ID);
        if (deviceId == null || deviceId.isEmpty())
            return;

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

        mWorking = true;
        while (true)
        {
            StorageEntry.Base entry = mService.getNetworkStorage().get();
            if (entry == null)
                break;
            JSONObject object = entry.toJsonObject();
            try
            {
                object.put("id", deviceId);
            } catch (JSONException ex)
            {
            }

            Request request = new Request("POST", pUrl(), object.toString());
            Response response = null;
            try
            {
                response = one(request);

                object = response.getObject();
                if (object == null)
                    break;

                int code = object.getInt("code");

                if (code == 1) // success
                {
                    mService.getNetworkStorage().remove();
                    Settings.setLong(Settings.KEY_LATEST_SUCCESS_CONNECTION, System.currentTimeMillis());
                }
                else
                {
                    throw new Exception("Code validation failed");
                }
//                Tools.log("NetworkThread: Logic: " + Integer.toString(mService.getLogicStorage().size()) + ", Network: " +
//                        Integer.toString(mService.getNetworkStorage().size()));
            }
            catch (Exception ex)
            {
                Tools.log("Networking exception: " + ex.toString());
                Settings.setLong(Settings.KEY_LATEST_FAILED_CONNECTION, System.currentTimeMillis());
                break; // try again later
            }
        }
        mWorking = false;
    }

    static String addParamUpdate(String s)
    {
        String ret = s;
        try
        {
            JSONObject object = new JSONObject(s);
            long lastCommandId = Settings.getLong(Settings.KEY_LAST_COMMAND_ID);
            object.put("c", lastCommandId);

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

        DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
        wr.write(bytes);
        wr.flush();

        int status = connection.getResponseCode();

        if (status == HttpURLConnection.HTTP_OK)
        {
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder responseString = new StringBuilder(); // or StringBuffer if not Java 5+
            String line;
            while((line = rd.readLine()) != null) {
                responseString.append(line);
                responseString.append('\r');
            }
            rd.close();

            JSONObject object = new JSONObject(responseString.toString());

            if (object.has("params"))
            {
                JSONObject params = object.getJSONObject("params");
                Settings.networkUpdate(params);
                object.remove("params");
                mLastParamsRequest = System.currentTimeMillis();
            }

            Response response = new Response(object);
            wr.close();

            return response;
        }
        else
        {
            throw new Exception(connection.getResponseMessage());
        }
    }

    @Override
    protected void onStop()
    {
        if (!mWorking)
            periodicTask();
        while (mWorking)
        {
            try
            {
                Thread.sleep(200);
            }
            catch (InterruptedException ex)
            {
            }
        }
    }
}

