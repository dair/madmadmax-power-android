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
    public static final String KEY_DEVICE_ID = "device_id";
    public static final String KEY_DEVICE_NAME = "device_name";
    public static final String KEY_SERVER_URL = "server_url";
    public static final String KEY_BLUETOOTH_DEVICE = "bluetooth_device";

    public static final String PARAMS_PREFIX = "param:";

    public static final String KEY_GPS_IDLE_INTERVAL = PARAMS_PREFIX + "gps_idle_interval";
    public static final String KEY_MIN_GPS_TIME = PARAMS_PREFIX + "gps_time";
    public static final String KEY_MIN_GPS_DISTANCE = PARAMS_PREFIX + "gps_distance";
    public static final String KEY_MIN_SATELLITES = PARAMS_PREFIX + "gps_satellites";
    public static final String KEY_MIN_ACCURACY = PARAMS_PREFIX + "gps_accuracy";
    public static final String KEY_AVERAGE_SPEED_COUNT = PARAMS_PREFIX + "spdn";

    public static final String KEY_PARAM_UPDATE = PARAMS_PREFIX + "param_update";
    public static final String KEY_CAR_STATE = PARAMS_PREFIX + "state";

    public static final long CAR_STATE_OK = 0;
    public static final long CAR_STATE_BREAK_1 = 1;
    public static final long CAR_STATE_BREAK_2 = 2;

    public static final String KEY_MAX_SPEED = PARAMS_PREFIX + "max_spd";
    public static final String KEY_FUEL_NOW = PARAMS_PREFIX + "fuel";
    public static final String KEY_FUEL_MAX = PARAMS_PREFIX + "max_fuel";
    public static final String KEY_FUEL_PER_KM = PARAMS_PREFIX + "fuel_per_km";
    public static final String KEY_RELIABILITY = PARAMS_PREFIX + "reliability";
    public static final String KEY_HITPOINTS = PARAMS_PREFIX + "hit_points";
    public static final String KEY_MAXHITPOINTS = PARAMS_PREFIX + "max_hit_points";
    public static final String KEY_RED_ZONE = PARAMS_PREFIX + "red_zone";
    public static final String KEY_DAMAGE_RESISTANCE = PARAMS_PREFIX + "damage_resistance";
    public static final String KEY_P1_FORMULA = PARAMS_PREFIX + "p1_formula";
    public static final String KEY_P2_FORMULA = PARAMS_PREFIX + "p2_formula";

    public static final String KEY_LAST_COMMAND_ID = PARAMS_PREFIX + "last_command_id";
    public static final String KEY_NETWORK_TIMEOUT = PARAMS_PREFIX + "timeout";

    public static final String KEY_LOGIC_LAST_GOOD_P1_FORMULA = "good_p1_formula";
    public static final String KEY_LOGIC_LAST_GOOD_P2_FORMULA = "good_p2_formula";

    public static final String KEY_LATEST_SUCCESS_CONNECTION = "latest_success_connection";
    public static final String KEY_LATEST_FAILED_CONNECTION = "latest_failed_connection";

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

    public static double getDouble(final String key)
    {
        double ret = 0.0;
        try
        {
            ret = Double.parseDouble(getString(key));
        }
        catch (NumberFormatException ex)
        {

        }

        return ret;
    }

    public static void setDouble(final String key, double value)
    {
        setString(key, Double.toString(value));
    }

    /////////////////////////
    private SharedPreferences sharedPreferences = null;
    private Map<String, String> mDefaults = new HashMap<>();

    Settings()
    {
        mDefaults.put(KEY_DEVICE_ID, null);
        mDefaults.put(KEY_DEVICE_NAME, null);
        mDefaults.put(KEY_SERVER_URL, "http://172.20.10.3:3000");
        mDefaults.put(KEY_BLUETOOTH_DEVICE, null);

        mDefaults.put(KEY_GPS_IDLE_INTERVAL, "5000"); // milliseconds
        mDefaults.put(KEY_MIN_GPS_TIME, "2000"); // milliseconds
        mDefaults.put(KEY_MIN_GPS_DISTANCE, "20"); //meters
        mDefaults.put(KEY_MIN_SATELLITES, "3");
        mDefaults.put(KEY_MIN_ACCURACY, "20");
        mDefaults.put(KEY_AVERAGE_SPEED_COUNT, "5");

        mDefaults.put(KEY_PARAM_UPDATE, "3000");
        mDefaults.put(KEY_CAR_STATE, "0");

        mDefaults.put(KEY_MAX_SPEED, "40");
        mDefaults.put(KEY_FUEL_NOW, "0");
        mDefaults.put(KEY_FUEL_MAX, "1000");
        mDefaults.put(KEY_FUEL_PER_KM, "200");
        mDefaults.put(KEY_RELIABILITY, "1");
        mDefaults.put(KEY_HITPOINTS, "100");
        mDefaults.put(KEY_MAXHITPOINTS, "100");
        mDefaults.put(KEY_RED_ZONE, "25");
        mDefaults.put(KEY_DAMAGE_RESISTANCE, "0");

        mDefaults.put(KEY_P1_FORMULA, "1-(4*x/3)^0.3");
        mDefaults.put(KEY_P2_FORMULA, "1-(2*x)^0.3");

        mDefaults.put(KEY_LOGIC_LAST_GOOD_P1_FORMULA, "100-(61887.9*x)^0.3");
        mDefaults.put(KEY_LOGIC_LAST_GOOD_P2_FORMULA, "100-(92831.8*x)^0.3");

        mDefaults.put(KEY_LAST_COMMAND_ID, "0");
        mDefaults.put(KEY_NETWORK_TIMEOUT, "10000");

        mDefaults.put(KEY_LATEST_FAILED_CONNECTION, "0");
        mDefaults.put(KEY_LATEST_SUCCESS_CONNECTION, "0");
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
                    setString(Settings.PARAMS_PREFIX + key, (String)object.get(key));
                }
                else if (object.get(key) instanceof Long)
                {
                    setLong(Settings.PARAMS_PREFIX + key, object.getLong(key));
                }
                else if (object.get(key) instanceof Integer)
                {
                    setLong(Settings.PARAMS_PREFIX + key, object.getInt(key));
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
