package org.albiongames.madmadmax.power;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by dair on 31/03/16.
 */
public class NetworkingThread extends GenericThread
{
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

    public interface Listener
    {
        void onNetworkSuccess(Request request, Response response);
        void onNetworkError(Request request, Error error);
    }

    public static class QueueItem
    {
        private Request mRequest;
        private Listener mListener;
        public QueueItem(Request request, Listener listener)
        {
            mRequest = request;
            mListener = listener;
        }

        public Request getRequest()
        {
            return mRequest;
        }

        public Listener getListener()
        {
            return mListener;
        }
    }

    private Queue<QueueItem> mQueue = new LinkedBlockingQueue<>();

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

    public void addRequest(Request request, Listener listener)
    {
        mQueue.add(new QueueItem(request, listener));
    }

    @Override
    protected void periodicTask()
    {
        while (!mQueue.isEmpty())
        {
            QueueItem item = mQueue.poll();
            if (item == null)
                break;
            one(item.getRequest(), item.getListener());
        }
    }

    public static void one(Request request, Listener listener)
    {
        HttpURLConnection connection = null;
        try
        {
            URL url = new URL(request.getUrl());
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod(request.getMethod());
            connection.setReadTimeout(3000);
            connection.setConnectTimeout(5000);
            if (request.getBody() != null &&
                    request.getBody().length() > 0)
            {
                connection.setRequestProperty("Content-Type", "application/json");
                byte[] bytes = request.getBody().getBytes("UTF-8");
                int len = bytes.length;
//                connection.setRequestProperty("Content-Length", Integer.toString(len));
                connection.setFixedLengthStreamingMode(len);
                connection.setUseCaches(false);
                connection.setDoOutput(true);
                connection.setDoInput(true);

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

                    Response response = new Response(object);
                    if (listener != null)
                    {
                        listener.onNetworkSuccess(request, response);
                    }
                }
                else
                {
                    Error error = new Error(connection.getResponseMessage());
                    if (listener != null)
                    {
                        listener.onNetworkError(request, error);
                    }
                }

                wr.close();
            }
        }
        catch (Exception ex)
        {
            Tools.log(ex.toString());
            Error error = new Error(ex.getLocalizedMessage());
            listener.onNetworkError(request, error);
        }
    }
}

