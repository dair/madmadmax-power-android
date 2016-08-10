package org.albiongames.madmadmax.power.data_storage;

/**
 * Created by Dmitry.Subbotenko on 08.08.2016.
 */
public interface ISettingsKeys {

  String KEY_DEVICE_ID = "device_id";
  String KEY_DEVICE_NAME = "device_name";
  String KEY_SERVER_URL = "server_url";
  String KEY_BLUETOOTH_DEVICE = "bluetooth_device";

  String PARAMS_PREFIX = "param:";

  String KEY_GPS_IDLE_INTERVAL = PARAMS_PREFIX + "gps_idle_interval";
  String KEY_MIN_GPS_TIME = PARAMS_PREFIX + "gps_time";
  String KEY_MIN_GPS_DISTANCE = PARAMS_PREFIX + "gps_distance";
  String KEY_MIN_SATELLITES = PARAMS_PREFIX + "gps_satellites";
  String KEY_MIN_ACCURACY = PARAMS_PREFIX + "gps_accuracy";
  String KEY_AVERAGE_SPEED_TIME = PARAMS_PREFIX + "average_speed_time";

  String KEY_PARAM_UPDATE = PARAMS_PREFIX + "param_update";
  String KEY_CAR_STATE = PARAMS_PREFIX + "state";

  int CAR_STATE_OK = 0;
  int CAR_STATE_MALFUNCTION_1 = 1;
  int CAR_STATE_MALFUNCTION_2 = 2;

  String KEY_MAX_SPEED = PARAMS_PREFIX + "max_spd";
  String KEY_FUEL_NOW = PARAMS_PREFIX + "fuel";
  String KEY_FUEL_MAX = PARAMS_PREFIX + "max_fuel";
  String KEY_FUEL_PER_KM = PARAMS_PREFIX + "fuel_per_km";
  String KEY_RELIABILITY = PARAMS_PREFIX + "reliability";
  String KEY_HITPOINTS = PARAMS_PREFIX + "hit_points";
  String KEY_MAXHITPOINTS = PARAMS_PREFIX + "max_hit_points";
  String KEY_RED_ZONE = PARAMS_PREFIX + "red_zone";
  String KEY_DAMAGE_RESISTANCE = PARAMS_PREFIX + "damage_resistance";
  String KEY_P1_FORMULA = PARAMS_PREFIX + "p1_formula";
  String KEY_P2_FORMULA = PARAMS_PREFIX + "p2_formula";

  String KEY_LAST_COMMAND_ID = PARAMS_PREFIX + "last_command_id";
  String KEY_LAST_UPGRADE_TIME = PARAMS_PREFIX + "last_upgrade_time";

  String KEY_NETWORK_TIMEOUT = PARAMS_PREFIX + "timeout";

  String KEY_MALFUNCTION_CHECK_INTERVAL = PARAMS_PREFIX + "malfunction_interval"; // in meters!

  String KEY_RED_ZONE_RELIABILITY = PARAMS_PREFIX + "red_zone_reliability"; // formula from speed x
  String KEY_RED_ZONE_FUEL_PER_KM = PARAMS_PREFIX + "red_zone_fuel_per_km"; // formula from speed x

  String KEY_MALFUNCTION1_RED_ZONE = PARAMS_PREFIX + "malfunction1_red_zone";
  String KEY_MALFUNCTION1_RELIABILITY = PARAMS_PREFIX + "malfunction1_reliability"; // formula from speed x
  String KEY_MALFUNCTION1_RED_ZONE_RELIABILITY = PARAMS_PREFIX + "malfunction1_red_zone_reliability"; // formula from speed x
  String KEY_MALFUNCTION1_RED_ZONE_FUEL_PER_KM = PARAMS_PREFIX + "malfunction1_red_zone_fuel_per_km"; // formula from speed x
  String KEY_MALFUNCTION1_FUEL_PER_KM = PARAMS_PREFIX + "malfunction1_fuel_per_km"; // formula from speed x

  String KEY_DAMAGE_CODE = PARAMS_PREFIX + "damage_code"; // json of values from 0 to 15 to any integers

  String KEY_LATEST_SUCCESS_CONNECTION = "latest_success_connection";
  String KEY_LATEST_FAILED_CONNECTION = "latest_failed_connection";

  String KEY_AVERAGE_SPEED = "average_speed";
  String KEY_TRACK_DISTANCE = "track_distance";

  String KEY_LAST_INSTANT_SPEED = "last_instant_speed";
  String KEY_LAST_GPS_UPDATE= "last_gps_update";


  String KEY_LOCATION_THREAD_STATUS = "location_status";
  String KEY_LOCATION_THREAD_LAST_QUALITY = "location_last_quality";

  String KEY_REGISTER_NAME = "register_activity_name";


  String KEY_BLUETOOTH_STATUS = "bluetooth_status";

  String KEY_MOCK_DATA = "mock_data";
  int MOCK_DATA_OFF = 0;
  int MOCK_DATA_RECORD = 1;
  int MOCK_DATA_PLAY = 2;

  String KEY_SIEGE_STATE = "siege_state";
  int SIEGE_STATE_OFF = 0;
  int SIEGE_STATE_ON = 1;

  String KEY_MOCK_AVAILABLE = PARAMS_PREFIX + "mock_available";

  String KEY_DRIVE2SIEGE_DELAY = PARAMS_PREFIX + "drive2siege_delay";
  String KEY_SIEGE2DRIVE_DELAY = PARAMS_PREFIX + "siege2drive_delay";

  String KEY_MOCK_AVERAGE_SPEED = PARAMS_PREFIX + "mock_average_speed";

  String KEY_EXTRA_DEBUG = PARAMS_PREFIX + "extra_debug";

  String KEY_GPS_TIMEOUT = PARAMS_PREFIX + "gps_timeout";

  String KEY_RX_BYTES = "rx_bytes";
  String KEY_TX_BYTES = "tx_bytes";

  String KEY_LOCATION_PACKAGE_SIZE = PARAMS_PREFIX + "location_package_size";
  String KEY_INFO_PACKAGE_SIZE = PARAMS_PREFIX + "info_package_size";

  String KEY_INFO_KEYS = PARAMS_PREFIX + "info_keys";

  String KEY_RULES_BREAK_PERIOD = PARAMS_PREFIX + "rules_break_period";

  String KEY_FUEL_LOAD_SPEED = PARAMS_PREFIX + "fuel_load_speed";
  String KEY_HP_LOAD_SPEED = PARAMS_PREFIX + "hp_load_speed";


  String DAMAGE_ACTION = "DAMAGE_ACTION";

  String KEY_GPS_FILTER_DISTANCE = PARAMS_PREFIX + "gps_filter_distance";

}
