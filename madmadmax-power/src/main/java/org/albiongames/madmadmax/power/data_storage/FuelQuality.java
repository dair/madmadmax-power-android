package org.albiongames.madmadmax.power.data_storage;

import org.albiongames.madmadmax.power.Tools;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by dair on 24/07/16.
 */
public class FuelQuality
{
    private final String FILENAME = "fuel_quality.json";
    String mPath = null;

    Map<String, Double> mValues = new HashMap<>();

    private static FuelQuality instance = new FuelQuality();

    public static void setPath(final String path)
    {
        instance.pSetPath(path);
    }

    private void pSetPath(final String path)
    {
        if (mPath != null && mPath.equals(path))
            return;

        mPath = path;

        JSONObject object = Tools.readFileToJson(mPath + "/" + FILENAME);
        if (object != null)
        {
            mValues.clear();

            Iterator<?> keys = object.keys();
            while (keys.hasNext())
            {
                String key = (String) keys.next();
                try
                {
                    mValues.put(key, object.getDouble(key));
                }
                catch (JSONException ex)
                {
                }
            }
        }
    }

    public double pUpgradeValue(final String key, double value)
    {
        if (!mValues.containsKey(key))
            return value;

        Double v = mValues.get(key);

        if (v == null || v == 0)
        {
            return value;
        }

        return value * v;
    }

    public static double upgradeValue(final String key, double value)
    {
        return instance.pUpgradeValue(key, value);
    }

    public static void fuelAdd(double qtyWas, double qtyAdded, JSONObject quality)
    {
        instance.pFuelAdd(qtyWas, qtyAdded, quality);
    }

    public void pFuelAdd(double qtyWas, double qtyAdded, JSONObject quality)
    {
        Set<String> names = new HashSet<>();

        if (quality != null)
        {
            Iterator<?> keys = quality.keys();
            while (keys.hasNext())
            {
                String key = (String) keys.next();
                names.add(key);
            }
        }
        names.addAll(mValues.keySet());

        // now "names" contains all keys from both new and old

        for (String key: names)
        {
            double homeValue = 0.0;
            if (mValues.containsKey(key))
            {
                homeValue = mValues.get(key);
            }

            double addedValue = 0.0;
            if (quality.has(key))
            {
                try {
                    addedValue = quality.getDouble(key);
                }
                catch (JSONException ex)
                {
                    // not double? screw that then
                }
            }

            double newValue = (homeValue * qtyWas + addedValue * qtyAdded) / (qtyWas + qtyAdded);

            mValues.put(key, newValue);
        }


        JSONObject object = new JSONObject(mValues);
        Tools.writeJsonToFile(mPath + "/" + FILENAME, object);
    }


}
