package org.albiongames.madmadmax.power;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

public class MainActivity extends Activity
{
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        final Class<? extends Activity> activityClass;

        if (Settings.getString(Settings.KEY_DEVICE_ID) == null)
        {
            activityClass = RegisterActivity.class;
        }
        else
        {
            activityClass = ServiceStatusActivity.class;
        }

        Intent newActivity = new Intent(this, activityClass);
        startActivity(newActivity);
    }
}
