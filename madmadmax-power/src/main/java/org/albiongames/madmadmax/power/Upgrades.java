package org.albiongames.madmadmax.power;

import android.content.Context;

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
    Context mContext;
    JSONObject mObject = null;

    Map<String, Double> mValues = new HashMap<>();

    public Upgrades(Context context)
    {
        mContext = context;

        readFile();
        recalculate();
    }

    public static String convertStreamToString(InputStream is) throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    void readFile()
    {
        try
        {
            FileInputStream fin = mContext.openFileInput(FILENAME);
            String jsonString = convertStreamToString(fin);
            fin.close();
            mObject = new JSONObject(jsonString);
        }
        catch (IOException ex)
        {

        }
        catch (JSONException ex)
        {

        }

        if (mObject == null)
            mObject = new JSONObject();
    }

    void writeFile()
    {
        FileOutputStream outputStream;

        try {
            outputStream = mContext.openFileOutput(FILENAME, Context.MODE_PRIVATE);
            outputStream.write(mObject.toString().getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Double getUpgrade(final String name)
    {
        if (mValues.containsKey(name))
            return mValues.get(name);
        else
            return null;
    }

    public void upgradesFromNetwork(JSONObject object)
    {
        try
        {
            Iterator<?> keys = object.keys();
            while (keys.hasNext())
            {
                String key = (String) keys.next();
                mObject.put(key, object.get(key));
            }

            writeFile();
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
