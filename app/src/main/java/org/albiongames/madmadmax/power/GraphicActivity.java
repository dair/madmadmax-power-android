package org.albiongames.madmadmax.power;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.Image;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class GraphicActivity extends Activity {

    public static final int STATUS_FAIL = -1;
    public static final int STATUS_OK = 1;
    public static final int STATUS_UNKNOWN = 0;
    public static final int STATUS_INITIAL = -100;

    int mHitPoints = 0;
    int mMaxHitPoints = 0;

    int mGpsStatus = STATUS_INITIAL;
    int mNetworkStatus = STATUS_INITIAL;

    ScheduledThreadPoolExecutor mExecutor = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graphic);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        ImageView logo = (ImageView)findViewById(R.id.logoImageView);
        logo.setColorFilter(Color.YELLOW);

        mExecutor = new ScheduledThreadPoolExecutor(1);
        mExecutor.scheduleAtFixedRate(new Runnable()
        {
            @Override
            public void run()
            {
                runOnUiThread(new Runnable()
                              {
                                  @Override
                                  public void run()
                                  {
                                      updateNetworkState();
                                      updateAverageSpeed();
                                      updateGpsStatus();
                                      updateHitPoints();
                                  }
                              }
                );
            }
        }, 0, 1, TimeUnit.SECONDS);

    }

    @Override
    public void onPause()
    {
        super.onPause();

        mExecutor.shutdownNow();
        mExecutor = null;
    }

    void updateNetworkState()
    {
        long success = Settings.getLong(Settings.KEY_LATEST_SUCCESS_CONNECTION);
        long fail = Settings.getLong(Settings.KEY_LATEST_FAILED_CONNECTION);
        int newStatus = STATUS_UNKNOWN;

        if (success > fail)
        {
            if (System.currentTimeMillis() - success < 2*Settings.getLong(Settings.KEY_GPS_IDLE_INTERVAL))
            {
                newStatus = STATUS_OK;
            }
            else
            {
                newStatus = STATUS_UNKNOWN;
            }
        }
        else
        {
            newStatus = STATUS_FAIL;
        }


        if (newStatus == mNetworkStatus)
            return;

        mNetworkStatus = newStatus;
        ImageView networkingImageView = (ImageView)findViewById(R.id.networkingImageView);

        switch (mNetworkStatus)
        {
            case STATUS_FAIL:
                networkingImageView.setColorFilter(Color.argb(0xFF, 0x77, 0x00, 0x00));
                break;
            case STATUS_OK:
                networkingImageView.setColorFilter(Color.GREEN);
                break;
            case STATUS_UNKNOWN:
                networkingImageView.setColorFilter(Color.DKGRAY);
                break;
        }
    }

    void updateAverageSpeed()
    {
        int newStatus = STATUS_UNKNOWN;
        float averageSpeed = (float)Settings.getDouble(Settings.KEY_AVERAGE_SPEED);
        float redZoneSpeed = 0;
        ImageView logo = (ImageView)findViewById(R.id.logoImageView);

        int state = (int)Settings.getLong(Settings.KEY_CAR_STATE);
        switch (state)
        {
            case Settings.CAR_STATE_MALFUNCTION_1:
                redZoneSpeed = (float)Settings.getDouble(Settings.KEY_MALFUNCTION1_RED_ZONE);
                break;
            case Settings.CAR_STATE_MALFUNCTION_2:
                redZoneSpeed = -1;
                break;
            case Settings.CAR_STATE_OK:
                redZoneSpeed = (float)Settings.getDouble(Settings.KEY_RED_ZONE);
                break;
        }

        if (state == Settings.CAR_STATE_MALFUNCTION_2)
        {
            logo.setBackgroundColor(Color.RED);
            logo.setColorFilter(Color.BLACK);
        }

        logo.setBackgroundColor(Color.BLACK);
        if (averageSpeed < 0.5)
        {
            logo.setColorFilter(Color.DKGRAY);
            return;
        }

        if (averageSpeed > redZoneSpeed)
        {
            logo.setColorFilter(Color.RED);
            return;
        }

        if (averageSpeed > redZoneSpeed * 0.75)
        {
            logo.setColorFilter(Color.YELLOW);
            return;
        }
        else
        {
            logo.setColorFilter(Color.WHITE);
            return;
        }
    }

    void updateGpsStatus()
    {
        ImageView gpsImageView = (ImageView)findViewById(R.id.gpsImageView);

        int satellites = (int)Settings.getLong(Settings.KEY_LOCATION_THREAD_SATELLITES);
        int minSatellites = (int)Settings.getLong(Settings.KEY_MIN_SATELLITES);

        if (satellites == 0)
        {
            gpsImageView.setColorFilter(Color.DKGRAY);
        }
        else if (satellites < minSatellites)
        {
            gpsImageView.setColorFilter(Color.RED);
        }
        else
        {
            gpsImageView.setColorFilter(Color.WHITE);
        }
    }

    void updateHitPoints()
    {
        int currentHP = (int)Settings.getLong(Settings.KEY_HITPOINTS);
        int maxHP = (int)Settings.getLong(Settings.KEY_MAXHITPOINTS);

        if (currentHP == mHitPoints && maxHP == mMaxHitPoints)
            return;

        mHitPoints = currentHP;
        mMaxHitPoints = maxHP;

        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBar);
        progressBar.setMax(mMaxHitPoints);
        progressBar.setProgress(mHitPoints);
    }

    @Override
    public void onBackPressed()
    {
        new AlertDialog.Builder(this)
                .setMessage(R.string.exit_confirmation)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        GraphicActivity.this.finish();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        Intent intent = null;
        // Handle item selection
        switch (item.getItemId())
        {
            case R.id.menu_status:
                intent = new Intent(this, ServiceStatusActivity.class);
                startActivity(intent);
                return true;
            case R.id.menu_bluetooth:
                intent = new Intent(this, BluetoothDeviceActivity.class);
                startActivity(intent);
                return true;
            case R.id.menu_settings:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
