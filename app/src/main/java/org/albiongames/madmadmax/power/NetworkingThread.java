package org.albiongames.madmadmax.power;

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
        private String mBody;
        public Response(String body)
        {
            mBody = body;
        }

        public String getBody()
        {
            return mBody;
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

    public static final String BASE_URL="http://192.168.100.100:3000";

    public static String authUrl()
    {
        return BASE_URL + "/device/auth";
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
            if (request.getBody() != null &&
                    request.getBody().length() > 0)
            {
                connection.setRequestProperty("Content-Type", "application/json");
                byte[] bytes = request.getBody().getBytes("UTF-8");
                int len = bytes.length;
                connection.setRequestProperty("Content-Length", Integer.toString(len));
                connection.setUseCaches(false);
                connection.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                wr.write(bytes);
                wr.close();

                InputStream is = connection.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                StringBuilder responseString = new StringBuilder(); // or StringBuffer if not Java 5+
                String line;
                while((line = rd.readLine()) != null) {
                    responseString.append(line);
                    responseString.append('\r');
                }
                rd.close();

                int status = connection.getResponseCode();
                if (status == HttpURLConnection.HTTP_OK)
                {
                    Response response = new Response(responseString.toString());
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
            }
        }
        catch (Exception ex)
        {
            Error error = new Error(ex.getLocalizedMessage());
            listener.onNetworkError(request, error);
        }
    }
}

