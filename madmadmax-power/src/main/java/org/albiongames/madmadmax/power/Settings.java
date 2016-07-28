package org.albiongames.madmadmax.power;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public static final String KEY_AVERAGE_SPEED_TIME = PARAMS_PREFIX + "average_speed_time";

    public static final String KEY_PARAM_UPDATE = PARAMS_PREFIX + "param_update";
    public static final String KEY_CAR_STATE = PARAMS_PREFIX + "state";

    public static final int CAR_STATE_OK = 0;
    public static final int CAR_STATE_MALFUNCTION_1 = 1;
    public static final int CAR_STATE_MALFUNCTION_2 = 2;

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
    public static final String KEY_LAST_UPGRADE_TIME = PARAMS_PREFIX + "last_upgrade_time";

    public static final String KEY_NETWORK_TIMEOUT = PARAMS_PREFIX + "timeout";

    public static final String KEY_MALFUNCTION_CHECK_INTERVAL = PARAMS_PREFIX + "malfunction_interval"; // in meters!

    public static final String KEY_RED_ZONE_RELIABILITY = PARAMS_PREFIX + "red_zone_reliability"; // formula from speed x
    public static final String KEY_RED_ZONE_FUEL_PER_KM = PARAMS_PREFIX + "red_zone_fuel_per_km"; // formula from speed x

    public static final String KEY_MALFUNCTION1_RED_ZONE = PARAMS_PREFIX + "malfunction1_red_zone";
    public static final String KEY_MALFUNCTION1_RELIABILITY = PARAMS_PREFIX + "malfunction1_reliability"; // formula from speed x
    public static final String KEY_MALFUNCTION1_RED_ZONE_RELIABILITY = PARAMS_PREFIX + "malfunction1_red_zone_reliability"; // formula from speed x
    public static final String KEY_MALFUNCTION1_RED_ZONE_FUEL_PER_KM = PARAMS_PREFIX + "malfunction1_red_zone_fuel_per_km"; // formula from speed x
    public static final String KEY_MALFUNCTION1_FUEL_PER_KM = PARAMS_PREFIX + "malfunction1_fuel_per_km"; // formula from speed x

    public static final String KEY_DAMAGE_CODE = PARAMS_PREFIX + "damage_code"; // json of values from 0 to 15 to any integers

    public static final String KEY_LATEST_SUCCESS_CONNECTION = "latest_success_connection";
    public static final String KEY_LATEST_FAILED_CONNECTION = "latest_failed_connection";

    public static final String KEY_AVERAGE_SPEED = "average_speed";
    public static final String KEY_TRACK_DISTANCE = "track_distance";

    public static final String KEY_LAST_INSTANT_SPEED = "last_instant_speed";
    public static final String KEY_LAST_GPS_UPDATE= "last_gps_update";


    public static final String KEY_LOCATION_THREAD_STATUS = "location_status";
    public static final String KEY_LOCATION_THREAD_LAST_QUALITY = "location_last_quality";

    public static final String KEY_REGISTER_NAME = "register_activity_name";


    public static final String KEY_BLUETOOTH_STATUS = "bluetooth_status";

    public static final String KEY_MOCK_DATA = "mock_data";
    public static final int MOCK_DATA_OFF = 0;
    public static final int MOCK_DATA_RECORD = 1;
    public static final int MOCK_DATA_PLAY = 2;

    public static final String KEY_SIEGE_STATE = "siege_state";
    public static final int SIEGE_STATE_OFF = 0;
    public static final int SIEGE_STATE_ON = 1;

    public static final String KEY_MOCK_AVAILABLE = PARAMS_PREFIX + "mock_available";

    public static final String KEY_DRIVE2SIEGE_DELAY = PARAMS_PREFIX + "drive2siege_delay";
    public static final String KEY_SIEGE2DRIVE_DELAY = PARAMS_PREFIX + "siege2drive_delay";

    public static final String KEY_MOCK_AVERAGE_SPEED = PARAMS_PREFIX + "mock_average_speed";

    public static final String KEY_EXTRA_DEBUG = PARAMS_PREFIX + "extra_debug";

    public static final String KEY_GPS_TIMEOUT = PARAMS_PREFIX + "gps_timeout";

    public static final String KEY_RX_BYTES = "rx_bytes";
    public static final String KEY_TX_BYTES = "tx_bytes";

    public static final String KEY_LOCATION_PACKAGE_SIZE = PARAMS_PREFIX + "location_package_size";
    public static final String KEY_INFO_PACKAGE_SIZE = PARAMS_PREFIX + "info_package_size";

    public static final String KEY_INFO_KEYS = PARAMS_PREFIX + "info_keys";




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
        catch (Exception ex)
        {

        }

        return ret;
    }

    public static void setDouble(final String key, double value)
    {
        setString(key, Double.toString(value));
    }

    public static Expression getExpression(final String key)
    {
        return instance.pGetExpression(key);
    }

    /////////////////////////
    private SharedPreferences sharedPreferences = null;
    private Map<String, String> mDefaults = new HashMap<>();
    private Set<String> mFormulaValues = new HashSet<>();
    private Map<String, Expression> mExpressions = new HashMap<>();

    Settings()
    {
        mDefaults.put(KEY_DEVICE_ID, null);
        mDefaults.put(KEY_DEVICE_NAME, null);
        mDefaults.put(KEY_SERVER_URL, "http://p.madmax.su");
        mDefaults.put(KEY_BLUETOOTH_DEVICE, null);

        mDefaults.put(KEY_GPS_IDLE_INTERVAL, "30000"); // milliseconds
        mDefaults.put(KEY_MIN_GPS_TIME, "0"); // milliseconds
        mDefaults.put(KEY_MIN_GPS_DISTANCE, "0"); //meters
        mDefaults.put(KEY_MIN_SATELLITES, "3");
        mDefaults.put(KEY_MIN_ACCURACY, "20");
        mDefaults.put(KEY_AVERAGE_SPEED_TIME, "3000"); // milliseconds

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

        mDefaults.put(KEY_LAST_COMMAND_ID, "0");
        mDefaults.put(KEY_LAST_UPGRADE_TIME, "0");
        mDefaults.put(KEY_NETWORK_TIMEOUT, "10000");

        mDefaults.put(KEY_LATEST_FAILED_CONNECTION, "0");
        mDefaults.put(KEY_LATEST_SUCCESS_CONNECTION, "0");

        mDefaults.put(KEY_MALFUNCTION_CHECK_INTERVAL, "50");

        mDefaults.put(KEY_TRACK_DISTANCE, "0");

        mDefaults.put(KEY_RED_ZONE_RELIABILITY, "3"); // formula from speed x
        mDefaults.put(KEY_RED_ZONE_FUEL_PER_KM, "500"); // formula from speed x

        mDefaults.put(KEY_MALFUNCTION1_RED_ZONE, "15");

        mDefaults.put(KEY_MALFUNCTION1_RELIABILITY, "2"); // formula from speed x
        mDefaults.put(KEY_MALFUNCTION1_FUEL_PER_KM, "300"); // formula from speed x

        mDefaults.put(KEY_MALFUNCTION1_RED_ZONE_RELIABILITY, "6"); // formula from speed x
        mDefaults.put(KEY_MALFUNCTION1_RED_ZONE_FUEL_PER_KM, "1000"); // formula from speed x

        mDefaults.put(KEY_LAST_INSTANT_SPEED, "0");
        mDefaults.put(KEY_LAST_GPS_UPDATE, "0");

        mDefaults.put(KEY_BLUETOOTH_STATUS, Integer.toString(BluetoothThread.STATUS_OFF));

        mDefaults.put(KEY_EXTRA_DEBUG, "0");

        mDefaults.put(KEY_DAMAGE_CODE,
                "{" +
                        "\"0\": 1," +
                        " \"1\": 2," +
                        "\"2\": 4," +
                        "\"3\": 5," +
                        "\"4\": 7," +
                        "\"5\": 10," +
                        "\"6\": 15," +
                        "\"7\": 17," +
                        "\"8\": 20," +
                        "\"9\": 25," +
                        "\"10\": 30," +
                        "\"11\": 35," +
                        "\"12\": 40," +
                        "\"13\": 50," +
                        "\"14\": 75," +
                        "\"15\": 100" +
                "}");

        mDefaults.put(KEY_MOCK_DATA, Integer.toString(MOCK_DATA_OFF));

        mDefaults.put(KEY_MOCK_AVAILABLE, "0");
        mDefaults.put(KEY_SIEGE_STATE, Integer.toString(SIEGE_STATE_OFF));

        mDefaults.put(KEY_DRIVE2SIEGE_DELAY, Long.toString(10000));
        mDefaults.put(KEY_SIEGE2DRIVE_DELAY, Long.toString(5000));

        mDefaults.put(KEY_MOCK_AVERAGE_SPEED, "-1"); // negative means use real one
        mDefaults.put(KEY_GPS_TIMEOUT, "20000");
        mDefaults.put(KEY_LOCATION_PACKAGE_SIZE, "100");
        mDefaults.put(KEY_INFO_PACKAGE_SIZE, "30");

        mDefaults.put(KEY_INFO_KEYS, "[\"param:state\", \"param:fuel\", \"param:hit_points\", \"bluetooth_status\", \"siege_state\"]");
// FORMULAS

        mFormulaValues.add(KEY_P1_FORMULA);
        mFormulaValues.add(KEY_P2_FORMULA);

        // normal driving
        mFormulaValues.add(KEY_RELIABILITY);
        mFormulaValues.add(KEY_FUEL_PER_KM);

        // "red zone" (i.e., exceeding speed limit) driving
        mFormulaValues.add(KEY_RED_ZONE_RELIABILITY);
        mFormulaValues.add(KEY_RED_ZONE_FUEL_PER_KM);

        // malfunction 1 driving
        mFormulaValues.add(KEY_MALFUNCTION1_RELIABILITY);
        mFormulaValues.add(KEY_MALFUNCTION1_FUEL_PER_KM);

        // malfunction 1 AND red zone driving
        mFormulaValues.add(KEY_MALFUNCTION1_RED_ZONE_RELIABILITY);
        mFormulaValues.add(KEY_MALFUNCTION1_RED_ZONE_FUEL_PER_KM);
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

        if (mExpressions.containsKey(key))
            mExpressions.remove(key);
    }

    private Expression pGetExpression(final String key)
    {
        if (!mFormulaValues.contains(key))
            return null;

        if (mExpressions.containsKey(key) && mExpressions.get(key) != null)
        {
            return mExpressions.get(key);
        }
        else
        {
            try
            {
                Expression expression = new ExpressionBuilder(pGetString(key)).
                        variable("x").build();
                mExpressions.put(key, expression);
                return expression;
            }
            catch (RuntimeException ex)
            {
                // fail
                return null;
            }
        }
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

    public static int getDamageForCode(int code)
    {
        String json = getString(KEY_DAMAGE_CODE);
        int ret = 1;
        if (json != null)
        {
            try
            {
                JSONObject object = new JSONObject(json);
                String codeString = Integer.toString(code);
                ret = object.getInt(codeString);
            }
            catch (JSONException ex)
            {

            }
        }

        return ret;
    }
}
