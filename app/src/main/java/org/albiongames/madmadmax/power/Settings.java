package org.albiongames.madmadmax.power;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by dair on 03/05/16.
 */
public class Settings
{
    public static final String KEY_BLUETOOTH_DEVICE = "bluetooth_device";



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
