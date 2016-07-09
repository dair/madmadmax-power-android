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
import android.widget.TextView;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class GraphicActivity extends Activity {

    public static final int STATUS_FAIL = -1;
    public static final int STATUS_OK = 1;
    public static final int STATUS_UNKNOWN = 0;
    public static final int STATUS_INITIAL = -100;

    int mHitPoints = 0;
    int mMaxHitPoints = 0;

    int mFuel = 0;
    int mMaxFuel = 0;

    int mGpsStatus = STATUS_INITIAL;
    int mNetworkStatus = STATUS_INITIAL;

    boolean mServerRunning = false;

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
                                      mServerRunning = Tools.isMyServiceRunning(GraphicActivity.this);
                                      updateNetworkState();
                                      updateAverageSpeed();
                                      updateGpsStatus();
                                      updateHitPoints();
                                      updateFuel();
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
        int newStatus = STATUS_UNKNOWN;

        if (mServerRunning)
        {
            long success = Settings.getLong(Settings.KEY_LATEST_SUCCESS_CONNECTION);
            long fail = Settings.getLong(Settings.KEY_LATEST_FAILED_CONNECTION);

            if (success > fail)
            {
                if (System.currentTimeMillis() - success < 2 * Settings.getLong(Settings.KEY_GPS_IDLE_INTERVAL))
                {
                    newStatus = STATUS_OK;
                } else
                {
                    newStatus = STATUS_UNKNOWN;
                }
            } else
            {
                newStatus = STATUS_FAIL;
            }
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
        int state = (int) Settings.getLong(Settings.KEY_CAR_STATE);
        ImageView logo = (ImageView) findViewById(R.id.logoImageView);

        if (Tools.isMyServiceRunning(this))
        {
            float averageSpeed = (float) Settings.getDouble(Settings.KEY_AVERAGE_SPEED);
            float redZoneSpeed = 0;

            switch (state)
            {
                case Settings.CAR_STATE_MALFUNCTION_1:
                    redZoneSpeed = (float) Settings.getDouble(Settings.KEY_MALFUNCTION1_RED_ZONE);
                    break;
                case Settings.CAR_STATE_MALFUNCTION_2:
                    redZoneSpeed = -1;
                    break;
                case Settings.CAR_STATE_OK:
                    redZoneSpeed = (float) Settings.getDouble(Settings.KEY_RED_ZONE);
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
            } else
            {
                logo.setColorFilter(Color.WHITE);
                return;
            }
        }
        else
        {
            if (state == Settings.CAR_STATE_MALFUNCTION_2)
            {
                logo.setBackgroundColor(Color.RED);
                logo.setColorFilter(Color.GRAY);
            }
            else
            {
                logo.setBackgroundColor(Color.BLACK);
                logo.setColorFilter(Color.DKGRAY);
            }
        }
    }

    void updateGpsStatus()
    {
        ImageView gpsImageView = (ImageView)findViewById(R.id.gpsImageView);

        int quality = (int)Settings.getLong(Settings.KEY_LOCATION_THREAD_LAST_QUALITY);

        int newStatus = STATUS_UNKNOWN;

        if (mServerRunning)
        {
            switch (quality)
            {
                case -1:
                    newStatus = STATUS_UNKNOWN;
                    break;
                case 0:
                    newStatus = STATUS_FAIL;
                    break;
                case 1:
                    newStatus = STATUS_OK;
                    break;
            }
        }

        if (newStatus == mGpsStatus)
            return;

        mGpsStatus = newStatus;

        switch (mGpsStatus)
        {
            case STATUS_FAIL:
                gpsImageView.setColorFilter(Color.RED);
                break;
            case STATUS_UNKNOWN:
                gpsImageView.setColorFilter(Color.DKGRAY);
                break;
            case STATUS_OK:
                gpsImageView.setColorFilter(Color.WHITE);
                break;
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

        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBarHP);
        progressBar.setMax(mMaxHitPoints);
        progressBar.setProgress(mHitPoints);

        TextView hpText = (TextView)findViewById(R.id.hpText);
        if (mServerRunning)
        {
            hpText.setTextColor(Color.WHITE);
        }
        else
        {
            hpText.setTextColor(Color.DKGRAY);
        }
    }

    void updateFuel()
    {
        int currentFuel = (int)Settings.getLong(Settings.KEY_FUEL_NOW);
        int maxFuel = (int)Settings.getLong(Settings.KEY_FUEL_MAX);

        if (currentFuel == mFuel && maxFuel == mMaxFuel)
            return;

        mFuel = currentFuel;
        mMaxFuel = maxFuel;

        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBarFuel);
        progressBar.setMax(mMaxFuel);
        progressBar.setProgress(mFuel);

        TextView fuelText = (TextView)findViewById(R.id.fuelText);
        if (mServerRunning)
        {
            fuelText.setTextColor(Color.WHITE);
        }
        else
        {
            fuelText.setTextColor(Color.DKGRAY);
        }
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
        inflater.inflate(R.menu.graphic_activity_menu, menu);

        MenuItem item = menu.findItem(R.id.menu_service_on_off);

        if (Tools.isMyServiceRunning(this))
        {
            item.setTitle(R.string.menu_service_off);
            item.setIcon(R.drawable.stop);
        }
        else
        {
            item.setTitle(R.string.menu_service_on);
            item.setIcon(R.drawable.play);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        Intent intent = null;
        // Handle item selection
        switch (item.getItemId())
        {
            case R.id.menu_settings:
                intent = new Intent(this, ServiceStatusActivity.class);
                startActivity(intent);
                return true;
            case R.id.menu_service_on_off:
                toggleService();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void toggleService()
    {
        if (Tools.isMyServiceRunning(this))
        {
            // stop service
            PowerService.graciousStop();
        }
        else
        {
            startService(new Intent(this, PowerService.class));
        }
    }

}
