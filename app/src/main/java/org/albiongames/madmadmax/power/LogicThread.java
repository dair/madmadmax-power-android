package org.albiongames.madmadmax.power;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidParameterException;
import java.util.Random;

/**
 * Created by dair on 12/06/16.
 */
public class LogicThread extends StatusThread
{
    PowerService mService = null;
    Random mRandom = new Random();
    double mLastMalfunctionCheckDistance = 0.0;

    LogicThread(PowerService service)
    {
        mService = service;
    }


    void processLocationGasoline(double rangePassedMeters)
    {
        // gasoline
        double fuelNow = Settings.getDouble(Settings.KEY_FUEL_NOW); // units of gas
        double fuelPerKm = getCurrentFuelPerKm(); // units of gas per kilometer

        double fuelSpent = fuelPerKm * rangePassedMeters / 1000.0;

        double fuelBecome = fuelNow - fuelSpent;
        if (fuelBecome < 0.0)
            fuelBecome = 0.0;

        Settings.setDouble(Settings.KEY_FUEL_NOW, fuelBecome);
    }

    void processLocationHitPoints(double rangePassedMeters)
    {
        // gasoline
        double hpNow = Settings.getDouble(Settings.KEY_HITPOINTS); // units of gas
        double hpPerKm = getCurrentReliability(); // units of gas per kilometer

        double hpSpent = hpPerKm * rangePassedMeters / 1000.0;

        double hpBecome = hpNow - hpSpent;
        if (hpBecome < 0.0)
            hpBecome = 0.0;

        Settings.setDouble(Settings.KEY_HITPOINTS, hpBecome);
    }

    void processLocation(StorageEntry.Location location)
    {
        double rangePassedMeters = location.getDistance(); // in meters
        processLocationGasoline(rangePassedMeters);
        processLocationHitPoints(rangePassedMeters);

        double trackDistance = Settings.getDouble(Settings.KEY_TRACK_DISTANCE);
        trackDistance += rangePassedMeters;
        Settings.setDouble(Settings.KEY_TRACK_DISTANCE, trackDistance);

        double interval = Settings.getDouble(Settings.KEY_MALFUNCTION_CHECK_INTERVAL);
        if (trackDistance - mLastMalfunctionCheckDistance > interval)
        {
            probabilities();
            mLastMalfunctionCheckDistance += interval;
        }
    }

    void processDamage(StorageEntry.Damage damage)
    {
        double hpNow = Settings.getDouble(Settings.KEY_HITPOINTS);
        int damageNum = damage.getDamage();
        hpNow -= damageNum;
        if (hpNow < 0)
            hpNow = 0;
        Settings.setDouble(Settings.KEY_HITPOINTS, hpNow);

        mService.getBluetoothThread().setLed(BluetoothThread.LED_RED, true);
        mService.getBluetoothThread().setPause(500);
        mService.getBluetoothThread().setLed(BluetoothThread.LED_RED, false);
    }

    @Override
    public void run()
    {
        Tools.log("LogicThread: start");

        super.run();

        Settings.setDouble(Settings.KEY_TRACK_DISTANCE, 0.0);
        Settings.setDouble(Settings.KEY_AVERAGE_SPEED, 0.0);
        setStatus(STATUS_ON);

        boolean changed = true;

        while (true)
        {
            if (changed)
            {
                updateLeds();
                changed = false;
            }

            StorageEntry.Base entry = mService.getLogicStorage().get();

            if (entry != null)
            {

                if (entry.isTypeOf(StorageEntry.TYPE_LOCATION))
                {
                    processLocation((StorageEntry.Location)entry);
                    changed = true;
                }
                else if (entry.isTypeOf(StorageEntry.TYPE_DAMAGE))
                {
                    processDamage((StorageEntry.Damage)entry);
                    changed = true;
                }

                mService.getNetworkStorage().put(entry);
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
            }
        }

        setStatus(STATUS_OFF);

        Tools.log("LogicThread: stop");
    }

    protected void probabilities()
    {
        Expression ex1 = null;
        Expression ex2 = null;

        switch ((int)Settings.getLong(Settings.KEY_CAR_STATE))
        {
            case Settings.CAR_STATE_OK:
                ex1 = Settings.getExpression(Settings.KEY_P1_FORMULA);
                ex2 = Settings.getExpression(Settings.KEY_P2_FORMULA);
                break;
            case Settings.CAR_STATE_MALFUNCTION_1:
                ex2 = Settings.getExpression(Settings.KEY_P2_FORMULA);
                break;
            case Settings.CAR_STATE_MALFUNCTION_2:
                break;
        }

        double randomDouble = mRandom.nextDouble(); // [0.0, 1.0)

        double hp = getCurrentHitPoints();
        double maxHp = getMaxHitPoints();
        double ratio = hp / maxHp;

        if (ex2 != null)
        {
            double upBorder = Tools.clamp(ex2.setVariable("x", ratio).evaluate(), 0.0, 1.0);
            if (randomDouble < upBorder)
            {
                // malfunction 2

                Settings.setLong(Settings.KEY_CAR_STATE, Settings.CAR_STATE_MALFUNCTION_2);
                return;
            }
        }

        if (ex1 != null)
        {
            double upBorder = Tools.clamp(ex1.setVariable("x", ratio).evaluate(), 0.0, 1.0);

            if (randomDouble < upBorder)
            {
                Settings.setLong(Settings.KEY_CAR_STATE, Settings.CAR_STATE_MALFUNCTION_1);
            }
        }
    }

    public void graciousStop()
    {
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

    public double getCurrentRedZone()
    {
        double ret = 0.0;
        switch ((int)Settings.getLong(Settings.KEY_CAR_STATE))
        {
            case Settings.CAR_STATE_OK:
                ret = Settings.getDouble(Settings.KEY_RED_ZONE);
                break;
            case Settings.CAR_STATE_MALFUNCTION_1:
                ret = Settings.getDouble(Settings.KEY_MALFUNCTION1_RED_ZONE);
                break;
            case Settings.CAR_STATE_MALFUNCTION_2:
                ret = 0.0;
                break;
        }

        return ret;
    }

    double getCurrentFuelPerKm()
    {
        Expression expression = null;
        double averageSpeedMps = Settings.getDouble(Settings.KEY_AVERAGE_SPEED);
        double averageSpeedKmH = Tools.metersPerSecondToKilometersPerHour(averageSpeedMps);
        double redZone = getCurrentRedZone(); // in km/h

        switch ((int)Settings.getLong(Settings.KEY_CAR_STATE))
        {
            case Settings.CAR_STATE_OK:
                if (averageSpeedKmH > redZone)
                {
                    expression = Settings.getExpression(Settings.KEY_RED_ZONE_FUEL_PER_KM);
                }
                else
                {
                    expression = Settings.getExpression(Settings.KEY_FUEL_PER_KM);
                }
                break;
            case Settings.CAR_STATE_MALFUNCTION_1:
                if (averageSpeedKmH > redZone)
                {
                    expression = Settings.getExpression(Settings.KEY_MALFUNCTION1_RED_ZONE_FUEL_PER_KM);
                }
                else
                {
                    expression = Settings.getExpression(Settings.KEY_MALFUNCTION1_FUEL_PER_KM);
                }
                break;
            case Settings.CAR_STATE_MALFUNCTION_2:
                return 0.0;
        }

        double result = 0.0;

        if (expression != null)
        {
            result = expression.setVariable("x", averageSpeedKmH).setVariable("r", redZone).evaluate();
        }

        return result;
    }

    double getCurrentReliability()
    {
        Expression expression = null;
        double averageSpeedMps = Settings.getDouble(Settings.KEY_AVERAGE_SPEED);
        double averageSpeedKmH = Tools.metersPerSecondToKilometersPerHour(averageSpeedMps);
        double redZone = getCurrentRedZone();

        switch ((int)Settings.getLong(Settings.KEY_CAR_STATE))
        {
            case Settings.CAR_STATE_OK:
                if (averageSpeedKmH > redZone)
                {
                    expression = Settings.getExpression(Settings.KEY_RED_ZONE_RELIABILITY);
                }
                else
                {
                    expression = Settings.getExpression(Settings.KEY_RELIABILITY);
                }
                break;
            case Settings.CAR_STATE_MALFUNCTION_1:
                if (averageSpeedKmH > redZone)
                {
                    expression = Settings.getExpression(Settings.KEY_MALFUNCTION1_RED_ZONE_RELIABILITY);
                }
                else
                {
                    expression = Settings.getExpression(Settings.KEY_MALFUNCTION1_RELIABILITY);
                }
                break;
            case Settings.CAR_STATE_MALFUNCTION_2:
                return 0.0;
        }

        double result = 0.0;

        if (expression != null)
        {
            result = expression.setVariable("x", averageSpeedKmH).setVariable("r", redZone).evaluate();
        }

        return result;
    }

    double getCurrentHitPoints()
    {
        return Settings.getDouble(Settings.KEY_HITPOINTS);
    }

    double getMaxHitPoints()
    {
        return Settings.getDouble(Settings.KEY_MAXHITPOINTS);
    }

    void updateLeds()
    {
        double hp = getCurrentHitPoints();
        double maxHp = getMaxHitPoints();

        double ratio = hp / maxHp;
        double greenLed = Settings.getDouble(Settings.KEY_GREEN_LED);

        if (ratio >= greenLed)
        {
            mService.getBluetoothThread().setLed(BluetoothThread.LED_GREEN, true);
            mService.getBluetoothThread().setLed(BluetoothThread.LED_YELLOW, false);
            mService.getBluetoothThread().setLed(BluetoothThread.LED_RED, false);
        }
        else
        {
            mService.getBluetoothThread().setLed(BluetoothThread.LED_GREEN, false);
            mService.getBluetoothThread().setLed(BluetoothThread.LED_YELLOW, true);
            mService.getBluetoothThread().setLed(BluetoothThread.LED_RED, false);
        }
    }
}
