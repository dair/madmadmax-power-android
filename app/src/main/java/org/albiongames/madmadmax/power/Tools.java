package org.albiongames.madmadmax.power;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

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

}
