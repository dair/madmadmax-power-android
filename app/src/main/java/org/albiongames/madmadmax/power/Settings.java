package org.albiongames.madmadmax.power;

import android.content.Context;
import android.content.SharedPreferences;

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

    private static Settings instance = new Settings();

    private SharedPreferences sharedPreferences = null;

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

        return 0;
    }


    /////////////////////////

    private void pSetContext(Context context)
    {
        if (context == null)
            sharedPreferences = null;
        sharedPreferences = context.getSharedPreferences("preferences", Context.MODE_PRIVATE);
    }

    private String pGetString(final String key)
    {
        if (sharedPreferences == null)
            return null;
        return sharedPreferences.getString(key, null);
    }

    private void pSetString(final String key, final String value)
    {
        if (sharedPreferences == null)
            return;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }
}
