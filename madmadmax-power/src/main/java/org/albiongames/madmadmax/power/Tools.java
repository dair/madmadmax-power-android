package org.albiongames.madmadmax.power;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.LocationManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.io.File;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;

/**
 * Created by dair on 28/05/16.
 */
public class Tools {
    public static void messageBox(Activity activity, int id) {
        messageBox(activity, id, null);
    }

    public static void messageBox(Activity activity, int id, Runnable runnable) {
        String msg = activity.getString(id);
        messageBox(activity, msg, runnable);
    }

    public static void messageBox(final Activity activity, final String message) {
        messageBox(activity, message, null);
    }

    public static void messageBox(final Activity activity, final String message, final Runnable runnable) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog dlgAlert = new AlertDialog.Builder(activity).create();
                dlgAlert.setMessage(message);
                dlgAlert.setTitle(R.string.app_name);
                dlgAlert.setButton(AlertDialog.BUTTON_POSITIVE,
                        "OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                if (runnable != null) {
                                    runnable.run();
                                }
                            }
                        });
                dlgAlert.setCancelable(true);
                dlgAlert.show();
            }
        });
    }

    public static void log(String message) {
        Log.e("MadMax", message);
    }

    public static double clamp(double value, double min, double max) {
        if (value < min)
            value = min;

        if (value > max)
            value = max;

        return value;
    }

    public static void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException ex) {
        }
    }

    public static double kilometersPerHourToMetersPerSecond(double kmh) {
        return kmh / 3.6;
    }

    public static double metersPerSecondToKilometersPerHour(double mps) {
        return mps * 3.6;
    }

    public static boolean isMyServiceRunning(Activity activity) {
        ActivityManager manager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (PowerService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean processMenu(MenuItem item, Activity activity) {
        Intent intent;
        switch (item.getItemId()) {
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

    static int[] splitColors(int c) {
        int[] ret = new int[4];
        ret[0] = (c & 0xFF000000) >> 24;
        ret[1] = (c & 0x00FF0000) >> 16;
        ret[2] = (c & 0x0000FF00) >> 8;
        ret[3] = (c & 0x000000FF);
        return ret;
    }

    public static int colorMiddle(int c1, int c2, double ratio) {
        int[] p1 = splitColors(c1);
        int[] p2 = splitColors(c2);
        int[] p = new int[4];

        for (int i = 0; i < 4; ++i) {
            p[i] = p1[i] + (int) Math.round((p2[i] - p1[i]) * ratio);
        }

        return Color.argb(p[0], p[1], p[2], p[3]);
    }

    public static void showKeyboard(Activity activity)
    {
        InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,InputMethodManager.HIDE_IMPLICIT_ONLY);
    }


    public static void hideKeyboard(Activity activity)
    {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static boolean checkServerStart(Activity activity)
    {
        LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null)
        {
            messageBox(activity, R.string.tools_check_gps_unavailable);
            return false;
        }

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
        {
            messageBox(activity, R.string.tools_check_gps_disabled);
            return false;
        }

        BluetoothSPP bt = new BluetoothSPP(activity);
        if (!bt.isBluetoothAvailable())
        {
            messageBox(activity, R.string.tools_check_bluetooth_unavailable);
            return false;
        }

        if (!bt.isBluetoothEnabled())
        {
            messageBox(activity, R.string.tools_check_bluetooth_disabled);
            // and it is fine
        }

        return true;
    }
}
