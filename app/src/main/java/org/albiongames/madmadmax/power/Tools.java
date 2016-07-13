package org.albiongames.madmadmax.power;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.MenuItem;

/**
 * Created by dair on 28/05/16.
 */
public class Tools
{
    public static void messageBox(Activity activity, int id)
    {
        String msg = activity.getString(id);
        messageBox(activity, msg);
    }

    public static void messageBox(final Activity activity, final String message)
    {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                AlertDialog dlgAlert  = new AlertDialog.Builder(activity).create();
                dlgAlert.setMessage(message);
                dlgAlert.setTitle(R.string.app_name);
                dlgAlert.setButton(AlertDialog.BUTTON_POSITIVE,
                        "OK", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                dialog.dismiss();
                            }
                        });
                dlgAlert.setCancelable(true);
                dlgAlert.show();
            }
        });
    }

    public static void log(String message)
    {
        Log.e("MadMax", message);
    }

    public static double clamp(double value, double min, double max)
    {
        if (value < min)
            value = min;

        if (value > max)
            value = max;

        return value;
    }

    public static void sleep(long milliseconds)
    {
        try
        {
            Thread.sleep(milliseconds);
        }
        catch (InterruptedException ex)
        {
        }
    }

    public static double kilometersPerHourToMetersPerSecond(double kmh)
    {
        return kmh / 3.6;
    }

    public static double metersPerSecondToKilometersPerHour(double mps)
    {
        return mps * 3.6;
    }

    public static boolean isMyServiceRunning(Activity activity)
    {
        ActivityManager manager = (ActivityManager)activity.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
        {
            if (PowerService.class.getName().equals(service.service.getClassName()))
            {
                return true;
            }
        }
        return false;
    }

    public static boolean processMenu(MenuItem item, Activity activity)
    {
        Intent intent;
        switch (item.getItemId())
        {
            case R.id.menu_bluetooth:
                intent = new Intent(activity, BluetoothDeviceActivity.class);
                activity.startActivity(intent);
                return true;
            case R.id.menu_status:
                intent = new Intent(activity, ServiceStatusActivity.class);
                activity.startActivity(intent);
                return true;
            case R.id.menu_settings:
                intent = new Intent(activity, SettingsActivity.class);
                activity.startActivity(intent);
                return true;
            case R.id.menu_about:
                intent = new Intent(activity, AboutActivity.class);
                activity.startActivity(intent);
                return true;
        }

        return false;
    }

    static int[] splitColors(int c)
    {
        int[] ret = new int[4];
        ret[0] = (c & 0xFF000000) >> 24;
        ret[1] = (c & 0x00FF0000) >> 16;
        ret[2] = (c & 0x0000FF00) >> 8;
        ret[3] = (c & 0x000000FF);
        return ret;
    }

    public static int colorMiddle(int c1, int c2, double ratio)
    {
        int[] p1 = splitColors(c1);
        int[] p2 = splitColors(c2);
        int[] p = new int[4];

        for (int i = 0; i < 4; ++i)
        {
            p[i] = p1[i] + (int)Math.round((p2[i] - p1[i]) * ratio);
        }

        return Color.argb(p[0], p[1], p[2], p[3]);
    }
}
