package org.albiongames.madmadmax.power.data_storage;

import net.objecthunter.exp4j.Expression;
import org.albiongames.madmadmax.power.service.BluetoothThread;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Dmitry.Subbotenko on 08.08.2016.
 */
public abstract class SettingsKeys implements ISettingsKeys {
  protected Map<String, String> mDefaults = new HashMap<>();
  protected Set<String> mFormulaValues = new HashSet<>();
  protected Map<String, Expression> mExpressions = new HashMap<>();

  SettingsKeys()
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

    mDefaults.put(KEY_RULES_BREAK_PERIOD, "10000"); // 10 seconds

    mDefaults.put(KEY_FUEL_LOAD_SPEED, "360"); // milliseconds for one unit of fuel
    mDefaults.put(KEY_HP_LOAD_SPEED, "25000"); // milliseconds for one Hit Point

    mDefaults.put(KEY_GPS_FILTER_DISTANCE, "2"); // meters

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
}
