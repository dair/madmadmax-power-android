package org.albiongames.madmadmax.power.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import org.albiongames.madmadmax.power.R;
import org.albiongames.madmadmax.power.Settings;
import org.albiongames.madmadmax.power.Tools;
import org.albiongames.madmadmax.power.data_storage.Upgrades;

public class FuelDropActivity extends Activity
{
    boolean mTimerActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fuel_drop);

        final ProgressBar bar = (ProgressBar)findViewById(R.id.progressBarFuel);
        bar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                float y;
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                        y = event.getY();
                        break;
                    default:
                        return false;
                }

                float h = v.getHeight();
                double ratio = Tools.clamp((h - y) / h, 0, 1);

                int max = bar.getMax();
                int progress = (int)Math.round(max * ratio);
                int fuelNow = (int)Math.round(Settings.getDouble(Settings.KEY_FUEL_NOW));
                if (progress > fuelNow)
                    progress = fuelNow;
                bar.setProgress(progress);

                return true;
            }
        });


        Button btn = (Button)findViewById(R.id.button);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                AlertDialog dlgAlert = new AlertDialog.Builder(FuelDropActivity.this).create();
                dlgAlert.setMessage(getString(R.string.fuel_drop_confirm));
                dlgAlert.setTitle(R.string.app_name);
                dlgAlert.setButton(AlertDialog.BUTTON_POSITIVE,
                        getString(R.string.yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startDropFuel();
                                dialog.dismiss();
                            }
                        });
                dlgAlert.setButton(AlertDialog.BUTTON_NEGATIVE,
                        getString(R.string.no), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                dlgAlert.setCancelable(true);
                dlgAlert.show();

            }
        });
    }

    @Override
    public void onResume()
    {
        super.onResume();

        double fuelNow = Settings.getDouble(Settings.KEY_FUEL_NOW);
        double fuelMax = Settings.getDouble(Settings.KEY_FUEL_MAX);
        fuelMax = Upgrades.upgradeValue(Settings.KEY_FUEL_MAX, fuelMax);

        ProgressBar bar = (ProgressBar)findViewById(R.id.progressBarFuel);
        bar.setMax((int)Math.round(fuelMax));
        bar.setProgress((int)Math.round(fuelNow));
    }

    void startDropFuel()
    {
        ProgressBar bar = (ProgressBar)findViewById(R.id.progressBarFuel);
        int fuel = bar.getProgress();
        double fuelMax = Settings.getDouble(Settings.KEY_FUEL_MAX);
        double fuelDrop = fuelMax - fuel;

        long ratio = Settings.getLong(Settings.KEY_FUEL_LOAD_SPEED);
        long timeout = Math.round(fuelDrop * ratio);

        mTimerActive = true;
        Tools.showTimer(this, timeout, R.string.fuel_drop_comment, new Runnable() {
            @Override
            public void run() {
                apply();
            }
        });
    }

    void apply()
    {
        // No progress bar show now.
        Settings.setDouble(Settings.KEY_FUEL_NOW, 0);

        FuelDropActivity.this.finish();
    }

    @Override
    public void onBackPressed()
    {
        if (mTimerActive)
            return;

        super.onBackPressed();
    }
}
