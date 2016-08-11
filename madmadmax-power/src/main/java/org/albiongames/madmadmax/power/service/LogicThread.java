package org.albiongames.madmadmax.power.service;

import static org.albiongames.madmadmax.power.Tools.getCurrentRedZone;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import net.objecthunter.exp4j.Expression;

import org.albiongames.madmadmax.power.data_storage.Settings;
import org.albiongames.madmadmax.power.Tools;
import org.albiongames.madmadmax.power.data_storage.FuelQuality;
import org.albiongames.madmadmax.power.data_storage.StorageEntry;
import org.albiongames.madmadmax.power.data_storage.Upgrades;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by dair on 12/06/16.
 */
public class LogicThread extends StatusThread
{
    final String COMPONENT = "LogicThread";
    private static final String TAG = LogicThread.class.getSimpleName();

    PowerService mService = null;
    Random mRandom = new Random();
    double mLastMalfunctionCheckDistance = 0.0;
    long mFirstHighSpeedTime = 0;
    long mMalfunction2DriveTime = 0;
    long mNoFuelDriveTime = 0;

    StorageEntry.Location mLastLocation = null;

    LogicThread(PowerService service, Settings settings)
    {
        super(settings);
        mService = service;
    }


    void processLocationGasoline(double rangePassedMeters)
    {
        if (rangePassedMeters <= 0.0)
            return;
        // gasoline
        double fuelNow = getSettings().getDouble(Settings.KEY_FUEL_NOW); // units of gas

        double fuelPerKm = getCurrentFuelPerKm(); // units of gas per kilometer

        double fuelSpent = fuelPerKm * rangePassedMeters / 1000.0;


        double fuelBecome = fuelNow - fuelSpent;
        if (fuelBecome < 0.0)
            fuelBecome = 0.0;

        mService.dump(COMPONENT, "fuel at start: " + Double.toString(fuelNow) + ", fuelPerKm: " + Double.toString(fuelPerKm) + ", fuelSpent = " + Double.toString(fuelSpent) + ", fuelBecome: " + Double.toString(fuelBecome));

        getSettings().setDouble(Settings.KEY_FUEL_NOW, fuelBecome);
    }

    void processLocationHitPoints(double rangePassedMeters)
    {
        if (rangePassedMeters <= 0)
            return;

        // hit points
        double hpNow = getSettings().getDouble(Settings.KEY_HITPOINTS); // units of gas
        double hpPerKm = getCurrentReliability(); // units of gas per kilometer

        double hpSpent = hpPerKm * rangePassedMeters / 1000.0;

        double hpBecome = hpNow - hpSpent;
        if (hpBecome < 0.0)
            hpBecome = 0.0;

        mService.dump(COMPONENT, "HP at start: " + Double.toString(hpNow) + ", hpPerKm: " + Double.toString(hpPerKm) + ", hpSpent = " + Double.toString(hpSpent) + ", hpBecome: " + Double.toString(hpBecome));

        getSettings().setDouble(Settings.KEY_HITPOINTS, hpBecome);
    }

    void processLocation(StorageEntry.Location location)
    {
        mService.dump(COMPONENT, "processing location {" + location.toString() + "}");

        double rangePassedMeters = location.getDistance(); // in meters
        mService.dump(COMPONENT, "distance: " + Double.toString(rangePassedMeters));
        processLocationGasoline(rangePassedMeters);
        processLocationHitPoints(rangePassedMeters);

        double hp = getSettings().getDouble(Settings.KEY_HITPOINTS);
        double fuel = getSettings().getDouble(Settings.KEY_FUEL_NOW);
        Tools.log("hp: " + Double.toString(hp) + ", fuel: " + Double.toString(fuel));

        double trackDistance = getSettings().getDouble(Settings.KEY_TRACK_DISTANCE);
        double trackDistanceBecome = trackDistance + rangePassedMeters;
        mService.dump(COMPONENT, "Total distance: was: " + Double.toString(trackDistance) + ", passed: " + Double.toString(rangePassedMeters) + ", become: " + Double.toString(trackDistanceBecome));
        getSettings().setDouble(Settings.KEY_TRACK_DISTANCE, trackDistanceBecome);

        double interval = getSettings().getDouble(Settings.KEY_MALFUNCTION_CHECK_INTERVAL);
        mService.dump(COMPONENT, "Check interval is " + Double.toString(interval) + ", lastCheckDistance: " + Double.toString(mLastMalfunctionCheckDistance));
        if (trackDistance - mLastMalfunctionCheckDistance > interval)
        {
            mService.dump(COMPONENT, "Checking probabilities");
            probabilities();
            mLastMalfunctionCheckDistance += interval;
        }
        else
        {
            mService.dump(COMPONENT, "NOT Checking probabilities");
        }

        double averageSpeed = Tools.getAverageSpeed(getSettings());
        double maxSpeed = getSettings().getDouble(Settings.KEY_MAX_SPEED);
        if (averageSpeed > maxSpeed)
        {
            if (mFirstHighSpeedTime == 0)
                mFirstHighSpeedTime = location.getTime();
            else
            {
                long duration = location.getTime() - mFirstHighSpeedTime;
                if (duration > getSettings().getLong(Settings.KEY_RULES_BREAK_PERIOD)) // 10 seconds of high speed
                {
                    getSettings().setLong(Settings.KEY_CAR_STATE, Settings.CAR_STATE_MALFUNCTION_2);
                }
            }
        }
        else
        {
            mFirstHighSpeedTime = 0;
        }

        // MALFUNCTION2

        if (getSettings().getLong(Settings.KEY_CAR_STATE) == Settings.CAR_STATE_MALFUNCTION_2)
        {

            if (averageSpeed > 0.1)
            {
                if (mMalfunction2DriveTime == 0)
                    mMalfunction2DriveTime = location.getTime();
                else
                {
                    long duration = location.getTime() - mMalfunction2DriveTime;
                    if (duration > getSettings().getLong(Settings.KEY_RULES_BREAK_PERIOD)) // 10 seconds of driving in malfunction2 mode
                    {
                        getSettings().setDouble(Settings.KEY_HITPOINTS, 0); // zed's dead baby
                    }
                }
            } else
            {
                mMalfunction2DriveTime = 0;
            }
        }
        else if (getSettings().getDouble(Settings.KEY_FUEL_NOW) <= 0.0)
        {
            // driving without fuel
            if (averageSpeed > 0.1)
            {
                if (mNoFuelDriveTime == 0)
                    mNoFuelDriveTime = location.getTime();
                else
                {
                    long duration = location.getTime() - mNoFuelDriveTime;
                    if (duration > getSettings().getLong(Settings.KEY_RULES_BREAK_PERIOD)) // 10 seconds of driving in malfunction2 mode
                    {
                        getSettings().setLong(Settings.KEY_CAR_STATE, Settings.CAR_STATE_MALFUNCTION_2); // breaking the car
                    }
                }
            }
            else
            {
                mNoFuelDriveTime = 0;
            }
        }

        mService.dump(COMPONENT, "Done processing location {" + location.toString() + "}");
//        generateInfo();
    }

    void processDamage(StorageEntry.Damage damage)
    {
        double hpNow = getSettings().getDouble(Settings.KEY_HITPOINTS);
        int damageNum = damage.getDamage();
        double damageModified = damageNum - getCurrentDamageResistance();

        if (damageModified > 0)
        {

            hpNow -= damageModified;
            if (hpNow < 0)
                hpNow = 0;
            getSettings().setDouble(Settings.KEY_HITPOINTS, hpNow);

            Intent intent = new Intent(Settings.DAMAGE_ACTION);
            intent.putExtra("DAMAGE", damageModified);
            mService.sendBroadcast(intent);

            generateInfo();
        }
    }

    long mLastInfoSent = 0;

    @Override
    public void run()
    {
        Tools.log("LogicThread: start");

        super.run();

        getSettings().setDouble(Settings.KEY_TRACK_DISTANCE, 0.0);
        getSettings().setDouble(Settings.KEY_AVERAGE_SPEED, 0.0);
        mLastMalfunctionCheckDistance = 0;
        mLastLocation = null;
        setStatus(STATUS_ON);

        while (true)
        {
            StorageEntry.Base entry = mService.getLogicStorage().get();

            if (entry != null)
            {
                boolean sendToNetwork = true;
                if (entry.isTypeOf(StorageEntry.TYPE_LOCATION))
                {
                    StorageEntry.Location location = (StorageEntry.Location)entry;
                    processLocation(location);

                    if (location.getDistance() < 0.1 && mLastLocation != null && mLastLocation.getDistance() < 0.1)
                    {
                        sendToNetwork = false;
                    }
                    mLastLocation = location;
                }
                else if (entry.isTypeOf(StorageEntry.TYPE_DAMAGE))
                {
                    processDamage((StorageEntry.Damage)entry);
                }

                if (sendToNetwork)
                {
                    mService.getNetworkStorage().put(entry);
                }
                mService.getLogicStorage().remove();

//                Tools.log("LogicThread: Logic: " + Integer.toString(mService.getLogicStorage().size()) + ", Network: " +
//                        Integer.toString(mService.getNetworkStorage().size()));

                if (getStatus() == STATUS_STOPPING && checkMarkerStop(entry))
                {
                    break;
                }
            }
            else
            {
                Tools.sleep(250);
                if (System.currentTimeMillis() - mLastInfoSent > 10000)
                {
                    generateInfo();
                }
            }
        }

        setStatus(STATUS_OFF);

        Tools.log("LogicThread: stop");
    }

    protected void probabilities()
    {
        Expression ex1 = null;
        Expression ex2 = null;

        int state = (int) getSettings().getLong(Settings.KEY_CAR_STATE);

        mService.dump(COMPONENT, "Checking probabilities: car state is "+ Integer.toString(state));

        switch (state)
        {
            case Settings.CAR_STATE_OK:
                ex1 = getSettings().getExpression(Settings.KEY_P1_FORMULA);
                ex2 = getSettings().getExpression(Settings.KEY_P2_FORMULA);
                break;
            case Settings.CAR_STATE_MALFUNCTION_1:
                ex2 = getSettings().getExpression(Settings.KEY_P2_FORMULA);
                break;
            case Settings.CAR_STATE_MALFUNCTION_2:
                break;
        }

        double randomDouble = mRandom.nextDouble(); // [0.0, 1.0)
        mService.dump(COMPONENT, "random double is " + Double.toString(randomDouble));

        double hp = getCurrentHitPoints();
        double maxHp = getMaxHitPoints();
        double ratio = hp / maxHp;

        mService.dump(COMPONENT, "HP: " + Double.toString(hp) + " of max " + Double.toString(maxHp) + ", ratio = " + Double.toString(ratio));

        if (ex2 != null)
        {
            double upBorder = Tools.clamp(ex2.setVariable("x", ratio).evaluate(), 0.0, 1.0);
            mService.dump(COMPONENT, "upBorder for malfunction2 check is " + Double.toString(upBorder));
            if (randomDouble < upBorder)
            {
                // malfunction 2
                mService.dump(COMPONENT, "And now car is broken down, malfunction 2");

                getSettings().setLong(Settings.KEY_CAR_STATE, Settings.CAR_STATE_MALFUNCTION_2);
                return;
            }
        }

        if (ex1 != null)
        {
            double upBorder = Tools.clamp(ex1.setVariable("x", ratio).evaluate(), 0.0, 1.0);
            mService.dump(COMPONENT, "upBorder for malfunction1 check is " + Double.toString(upBorder));

            if (randomDouble < upBorder)
            {
                mService.dump(COMPONENT, "And now car is broken down, malfunction 1");
                getSettings().setLong(Settings.KEY_CAR_STATE, Settings.CAR_STATE_MALFUNCTION_1);
            }
        }
    }

    public void graciousStop()
    {
        mService.dump(COMPONENT, "Gracious stop");
        if (getStatus() == STATUS_ON)
            setStatus(STATUS_STOPPING);
    }

    boolean checkMarkerStop(StorageEntry.Base entry)
    {
        if (entry == null)
            return false;

        JSONObject object = entry.toJsonObject();

        try
        {
            if (object.has("type") && object.getString("type").equals("marker") &&
                    object.has("tag") && object.getString("tag").equals("stop"))
                return true;
        }
        catch (JSONException ex)
        {
        }

        return false;
    }

    double getCurrentFuelPerKm()
    {
        Expression expression = null;
        double averageSpeedMps = Tools.getAverageSpeed(getSettings());
        double averageSpeedKmH = Tools.metersPerSecondToKilometersPerHour(averageSpeedMps);
        double redZone = getCurrentRedZone(getSettings()); // in km/h
        String key = null;

        switch ((int) getSettings().getLong(Settings.KEY_CAR_STATE))
        {
            case Settings.CAR_STATE_OK:
                if (averageSpeedKmH > redZone)
                {
                    key = Settings.KEY_RED_ZONE_FUEL_PER_KM;
                }
                else
                {
                 key = Settings.KEY_FUEL_PER_KM;
                }
                break;
            case Settings.CAR_STATE_MALFUNCTION_1:
                if (averageSpeedKmH > redZone)
                {
                    key = Settings.KEY_MALFUNCTION1_RED_ZONE_FUEL_PER_KM;
                }
                else
                {
                    key = Settings.KEY_MALFUNCTION1_FUEL_PER_KM;
                }
                break;
            case Settings.CAR_STATE_MALFUNCTION_2:
                return 0.0;
        }

        if (key == null)
        {
            return 0.0;
        }

        expression = getSettings().getExpression(key);

        double result = 0.0;

        if (expression != null)
        {
            result = expression.setVariable("x", averageSpeedKmH).setVariable("r", redZone).evaluate();
            result = Upgrades.upgradeValue(key, result);
            result = FuelQuality.upgradeValue(key, result);
        }

        return result;
    }

    double getCurrentReliability()
    {
        Expression expression = null;
        double averageSpeedMps = Tools.getAverageSpeed(getSettings());
        double averageSpeedKmH = Tools.metersPerSecondToKilometersPerHour(averageSpeedMps);
        double redZone = getCurrentRedZone(getSettings());

        String key = null;

        switch ((int) getSettings().getLong(Settings.KEY_CAR_STATE))
        {
            case Settings.CAR_STATE_OK:
                if (averageSpeedKmH > redZone)
                {
                    key = Settings.KEY_RED_ZONE_RELIABILITY;
                }
                else
                {
                    key = Settings.KEY_RELIABILITY;
                }
                break;
            case Settings.CAR_STATE_MALFUNCTION_1:
                if (averageSpeedKmH > redZone)
                {
                    key = Settings.KEY_MALFUNCTION1_RED_ZONE_RELIABILITY;
                }
                else
                {
                    key = Settings.KEY_MALFUNCTION1_RELIABILITY;
                }
                break;
            case Settings.CAR_STATE_MALFUNCTION_2:
                return 0.0;
        }

        if (key == null)
            return 0.0;

        expression = getSettings().getExpression(key);

        double result = 0.0;


        if (expression != null)
        {
            result = expression.setVariable("x", averageSpeedKmH).setVariable("r", redZone).evaluate();
            result = Upgrades.upgradeValue(key, result);
            result = FuelQuality.upgradeValue(key, result);
        }

        return result;
    }

    double getCurrentDamageResistance()
    {
        double baseValue = getSettings().getDouble(Settings.KEY_DAMAGE_RESISTANCE);
        double value = Upgrades.upgradeValue(Settings.KEY_DAMAGE_RESISTANCE, baseValue);
        return value;
    }

    double getCurrentHitPoints()
    {
        return getSettings().getDouble(Settings.KEY_HITPOINTS);
    }

    double getMaxHitPoints()
    {
        double baseValue = getSettings().getDouble(Settings.KEY_MAXHITPOINTS);
        double value = Upgrades.upgradeValue(Settings.KEY_MAXHITPOINTS, baseValue);
        return value;
    }

    Map<String, String> mInfoMap = new HashMap<>();

    synchronized void generateInfo()
    {
        Map<String, String> info = new HashMap<>();
        Map<String, String> diff = new HashMap<>();

        String keysString = getSettings().getString(Settings.KEY_INFO_KEYS);
        try
        {
            if (TextUtils.isEmpty(keysString)){
                Log.e(TAG, "generateInfo KEY_INFO_KEYS contains null string!");
                return;
            }

            JSONArray keysArray = new JSONArray(keysString);

            for (int i = 0; i < keysArray.length(); ++i)
            {
                String key = keysArray.getString(i);

                String value = getSettings().getString(key);
                if (value != null)
                    info.put(key, value);

                if (mInfoMap.containsKey(key) &&
                        mInfoMap.get(key) != null &&
                        mInfoMap.get(key).equals(value))
                    continue;

                diff.put(key, value);
            }
        }
        catch (JSONException ex)
        {

        }

        mInfoMap = info;

        if (!diff.isEmpty())
        {
            StorageEntry.Info infoPackage = new StorageEntry.Info(diff);
            mService.getNetworkStorage().put(infoPackage);
        }

        mLastInfoSent = System.currentTimeMillis();
    }
}
