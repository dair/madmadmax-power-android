package org.albiongames.madmadmax.power.network;

import org.albiongames.madmadmax.power.data_storage.Settings;
import org.albiongames.madmadmax.power.data_storage.Upgrades;
import org.albiongames.madmadmax.power.service.PowerService;
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
 * This class contains static implementations from NetworkThread class.
 * <p/>
 * Created by Dmitry.Subbotenko on 08.08.2016.
 */
public class NetworkTools {

  static long mLastParamsRequest = 0;
  static long mLastNetworkInteraction = 0;

  static long mTrafficRx;
  static long mTrafficTx;

  public final static int ZIP_AUTO = 0;
  public final static int ZIP_YES = 1;
  public final static int ZIP_NO = 2;


  public static class Request {
    private String mMethod;
    private String mUrl;
    private String mBody;

    public Request(final String method, final String url, JSONObject object) {
      mMethod = method;
      mUrl = url;
      mBody = object.toString();
    }

    public Request(final String method, final String url, final String body) {
      mMethod = method;
      mUrl = url;
      mBody = body;
    }

    public String getMethod() {
      return mMethod;
    }

    public String getUrl() {
      return mUrl;
    }

    public String getBody() {
      return mBody;
    }
  }

  public static class Response {
    private JSONObject mObject = null;

    public Response(JSONObject object) {
      mObject = object;
    }

    public final JSONObject getObject() {
      return mObject;
    }
  }

  public static String baseUrl(Settings settings) {
    String storedUrl = settings.getString(Settings.KEY_SERVER_URL);
    if (storedUrl == null)
      return null;
    if (!storedUrl.startsWith("http://") &&
        !storedUrl.startsWith("https://")) {
      storedUrl = "http://" + storedUrl;
    }

    while (storedUrl.endsWith("/")) {
      storedUrl = storedUrl.substring(0, storedUrl.length() - 1);
    }

    return storedUrl;
  }

  public static String authUrl(Settings settings) {
    return baseUrl(settings) + "/device/reg";
  }

  public static String pUrl(Settings settings) {
    return baseUrl(settings) + "/device/p";
  }

  public static String fuelUrl(Settings settings) {
    return baseUrl(settings) + "/device/fuel";
  }

  public static String repairUrl(Settings settings) {
    return baseUrl(settings) + "/device/repair";
  }


  public static String addParamUpdate(String s, Settings settings) {
    String ret = s;
    try {
      JSONObject object = new JSONObject(s);
      long lastCommandId = settings.getLong(Settings.KEY_LAST_COMMAND_ID);
      object.put("c", lastCommandId);

      long lastUpgradeTime = settings.getLong(Settings.KEY_LAST_UPGRADE_TIME);
      object.put("u", lastUpgradeTime);

      ret = object.toString();
    } catch (JSONException ex) {
      // cry
    }

    return ret;
  }

  public static synchronized Response one(Request request, int zip, Settings settings) throws Exception {
    HttpURLConnection connection = null;
    URL url = new URL(request.getUrl());
    connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(request.getMethod());
    connection.setReadTimeout((int) settings.getLong(Settings.KEY_NETWORK_TIMEOUT));
    connection.setConnectTimeout((int) settings.getLong(Settings.KEY_NETWORK_TIMEOUT));

    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
    String bodyString = request.getBody();
    if (bodyString == null || bodyString.isEmpty())
      bodyString = "{}";

    if (System.currentTimeMillis() - getmLastParamsRequest() >= settings.getLong(Settings.KEY_PARAM_UPDATE)) {
      bodyString = addParamUpdate(bodyString, settings);
    }

    byte[] bytes;

    if (zip == ZIP_NO || (zip == ZIP_AUTO && bodyString.length() < 255)) {
      // no zipping
      bytes = bodyString.getBytes("UTF-8");
    } else {
      connection.setRequestProperty("Content-Encoding", "gzip");

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      GZIPOutputStream gzos = new GZIPOutputStream(baos);
      gzos.write(bodyString.getBytes("UTF-8"));
      gzos.flush();
      gzos.close();

      bytes = baos.toByteArray();
    }
    int len = bytes.length;

    setTx(getTx()+len);

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

    try {
      status = connection.getResponseCode();
    } catch (EOFException ex) {
      // server closed connection??
    }

    //        Tools.log("cathcing eof: 2.5");
    if (status == HttpURLConnection.HTTP_OK) {
      //            Tools.log("cathcing eof: 3");
      InputStream is = connection.getInputStream();
      BufferedReader rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));
      StringBuilder responseString = new StringBuilder(); // or StringBuffer if not Java 5+
      String line;
      while ((line = rd.readLine()) != null) {
        responseString.append(line);
        responseString.append('\r');
      }
      //            Tools.log("cathcing eof: 4");
      rd.close();
      //            Tools.log("cathcing eof: 5");

      setRx(getRx()+responseString.length());

      JSONObject object = new JSONObject(responseString.toString());

      if (object.has("params")) {
        JSONObject params = object.getJSONObject("params");
        settings.networkUpdate(params);
        object.remove("params");
        setmLastParamsRequest(System.currentTimeMillis());
      }

      if (object.has("upgrades")) {
        JSONObject params = object.getJSONObject("upgrades");

        long storeTime = settings.getLong(Settings.KEY_LAST_UPGRADE_TIME);

        if (params.has("time")) {
          storeTime = params.getLong("time");
          params.remove("time");
        }

        if (PowerService.instance() != null) {
          Upgrades.upgradesFromNetwork(params);
        }
        settings.setLong(Settings.KEY_LAST_UPGRADE_TIME, storeTime);

        object.remove("upgrades");
      }
      //            Tools.log("cathcing eof: 6");

      Response response = new Response(object);
      wr.close();

      //            Tools.log("cathcing eof: 7");

      return response;
    } else {
      throw new Exception(connection.getResponseMessage());
    }
  }


  public static long getRx()
  {
    return mTrafficRx;
  }

  public static long getTx()
  {
    return mTrafficTx;
  }

  public static void setRx(long mTrafficRx) {
    NetworkTools.mTrafficRx = mTrafficRx;
  }

  public static void setTx(long mTrafficTx) {
    NetworkTools.mTrafficTx = mTrafficTx;
  }

  public static long getLastNetworkInteraction() {
    return mLastNetworkInteraction;
  }

  public static void setLastNetworkInteraction(long mLastNetworkInteraction) {
    NetworkTools.mLastNetworkInteraction = mLastNetworkInteraction;
  }

  public static long getmLastParamsRequest() {
    return mLastParamsRequest;
  }

  public static void setmLastParamsRequest(long mLastParamsRequest) {
    NetworkTools.mLastParamsRequest = mLastParamsRequest;
  }
}
