package org.albiongames.madmadmax.power;

import android.content.Context;
import android.os.Environment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by dair on 21/07/16.
 */
public class Upgrades
{
    private final String FILENAME = "upgrades.json";
    String mPath = null;
    JSONObject mObject = null;

    Map<String, Double> mValues = new HashMap<>();

    private static Upgrades instance = new Upgrades();

    public static void setPath(final String path)
    {
        instance.pSetPath(path);
    }

    private void pSetPath(final String path)
    {
        if (mPath != null && mPath.equals(path))
            return;

        mPath = path;

        mObject = Tools.readFileToJson(mPath + "/" + FILENAME);
        if (mObject == null)
        {
            mObject = new JSONObject();
        }

        recalculate();
    }

    public Upgrades()
    {
    }

    public static double upgradeValue(final String name, double value)
    {
        if (name == null)
            return value;
        Double u = instance.pGetUpgrade(name);
        if (u == null || u.doubleValue() == 0)
        {
            return value;
        }

        return value * u;
    }

    public static Double getUpgrade(final String name)
    {
        return instance.pGetUpgrade(name);
    }

    public Double pGetUpgrade(final String name)
    {
        if (mValues.containsKey(name))
            return mValues.get(name);
        else
            return null;
    }

    public static void upgradesFromNetwork(JSONObject object)
    {
        instance.pUpgradesFromNetwork(object);
    }

    private void pUpgradesFromNetwork(JSONObject object)
    {
        try
        {
            Iterator<?> keys = object.keys();
            while (keys.hasNext())
            {
                String key = (String) keys.next();
                mObject.put(key, object.get(key));
            }

            Tools.writeJsonToFile(mPath + "/" + FILENAME, mObject);
            recalculate();
        }
        catch (JSONException ex)
        {
        }
    }

    void recalculate()
    {
        synchronized (mValues)
        {
            mValues.clear();

            Iterator<?> keys = mObject.keys();
            while (keys.hasNext())
            {
                String upgId = (String) keys.next();
                try
                {
                    JSONObject object = mObject.getJSONObject(upgId);
                    Iterator<?> it2 = object.keys();
                    while (it2.hasNext())
                    {
                        String key = (String)it2.next();
                        try
                        {
                            double value = Double.valueOf(object.getString(key));
                            if (value != 0) {
                                double was = 0;
                                if (mValues.containsKey(key))
                                {
                                    was = mValues.get(key);
                                }
                                double become = was + value;
                                mValues.put(key, become);
                            }
                        }
                        catch (NumberFormatException ex)
                        {
                        }
                    }
                }
                catch (JSONException ex)
                {}
            }
        }
    }
}
