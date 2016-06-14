package org.albiongames.madmadmax.power;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by dair on 03/05/16.
 */
public class Settings
{
    public static final String KEY_BLUETOOTH_DEVICE = "bluetooth_device";
    public static final String KEY_MIN_GPS_TIME = "gps_time";
    public static final String KEY_MIN_GPS_DISTANCE = "gps_distance";
    public static final String KEY_GPS_IDLE_INTERVAL = "gps_idle_interval";
    public static final String KEY_DEVICE_ID = "device_id";
    public static final String KEY_DEVICE_NAME = "device_name";
    public static final String KEY_SERVER_URL = "server_url";
    public static final String KEY_MIN_SATELLITES = "param:min_sat";

    private static Settings instance = new Settings();


    public static void setContext(Context context)
    {
        instance.pSetContext(context);
    }

    public static String getString(final String key)
    {
        return instance.pGetString(key);
    }

    public static void setString(final String key, final String value)
    {
        instance.pSetString(key, value);
    }

    public static long getLong(final String key)
    {
        long ret = 0;
        try
        {
            ret = Long.parseLong(getString(key));
        }
        catch (NumberFormatException ex)
        {

        }

        return ret;
    }

    public static void setLong(final String key, long value)
    {
        setString(key, Long.toString(value));
    }

    /////////////////////////
    private SharedPreferences sharedPreferences = null;
    private Map<String, String> mDefaults = new HashMap<>();

    Settings()
    {
        mDefaults.put(KEY_BLUETOOTH_DEVICE, null);
        mDefaults.put(KEY_MIN_GPS_TIME, "2000"); // milliseconds
        mDefaults.put(KEY_MIN_GPS_DISTANCE, "20"); //meters
        mDefaults.put(KEY_GPS_IDLE_INTERVAL, "5000"); // milliseconds
        mDefaults.put(KEY_DEVICE_ID, null);
        mDefaults.put(KEY_DEVICE_NAME, null);
        mDefaults.put(KEY_SERVER_URL, "http://192.168.43.101:3000");
        mDefaults.put(KEY_MIN_SATELLITES, "3");
    }

    private void pSetContext(Context context)
    {
        if (context == null)
            sharedPreferences = null;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        //context.getSharedPreferences("preferences", Context.MODE_PRIVATE);
    }

    private String pGetString(final String key)
    {
        if (sharedPreferences == null)
            return null;
        String ret = sharedPreferences.getString(key, null);
        if (ret == null)
        {
            if (mDefaults.containsKey(key))
                ret = mDefaults.get(key);
        }
        return ret;
    }

    private void pSetString(final String key, final String value)
    {
        if (sharedPreferences == null)
            return;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static void networkUpdate(JSONObject object)
    {
        if (object == null)
            return;

        Iterator<String> keys = object.keys();
        while (keys.hasNext())
        {
            String key = keys.next();
            try
            {
                if (object.get(key) instanceof String)
                {
                    setString(key, (String)object.get(key));
                }
                else if (object.get(key) instanceof Long)
                {
                    setLong(key, object.getLong(key));
                }
                else if (object.get(key) instanceof Integer)
                {
                    setLong(key, object.getInt(key));
                }
                else
                {
                    String className = object.get(key).getClass().getName();
                    Tools.log(className);
                }
            }
            catch (JSONException ex)
            {
            }
        }
    }
}
