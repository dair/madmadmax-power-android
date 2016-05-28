package org.albiongames.madmadmax.power;

import android.app.AlertDialog;
import android.content.Context;

/**
 * Created by dair on 28/05/16.
 */
public class Tools
{
    public static void messageBox(Context ctx, int id)
    {
        String msg = ctx.getString(id);
        messageBox(ctx, msg);
    }

    public static void messageBox(Context ctx, final String message)
    {
        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(ctx);
        dlgAlert.setMessage(message);
        dlgAlert.setTitle(R.string.app_name);
        dlgAlert.setPositiveButton("OK", null);
        dlgAlert.setCancelable(true);
        dlgAlert.create().show();
    }
}
