package org.albiongames.madmadmax.power;

import android.app.Activity;
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
}
