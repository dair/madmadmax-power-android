package org.albiongames.madmadmax.power.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import org.albiongames.madmadmax.power.data_storage.FuelQuality;
import org.albiongames.madmadmax.power.data_storage.Settings;
import org.albiongames.madmadmax.power.data_storage.Upgrades;

public class MainActivity extends Activity
{
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Upgrades.setPath(getFilesDir().getPath());
        FuelQuality.setPath(getFilesDir().getPath());

        final Class<? extends Activity> activityClass;

        if (new Settings(this).getString(Settings.KEY_DEVICE_ID) == null)
        {
            activityClass = RegisterActivity.class;
        }
        else
        {
//            activityClass = ServiceStatusActivity.class;
            activityClass = GraphicActivity.class;
        }

        Intent newActivity = new Intent(this, activityClass);
        startActivity(newActivity);
        finish();
    }
}
