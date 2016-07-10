package org.albiongames.madmadmax.power;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.Image;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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

    int mCarState = -100;

    long counter = 0; //tmp

    boolean mServerRunning = false;

    ImageView mCoverImage = null;

    ScheduledThreadPoolExecutor mExecutor = null;
    ScheduledThreadPoolExecutor mFlashExecutor = null;
    long mFlashExecutorDuration = 0;
    long mFlashExecutorStart = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graphic);
        mCoverImage = (ImageView)findViewById(R.id.coverImage);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        mCarState = -100;
        mHitPoints = 0;
        mMaxHitPoints = 0;

        mFuel = 0;
        mMaxFuel = 0;

        mGpsStatus = STATUS_INITIAL;
        mNetworkStatus = STATUS_INITIAL;



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
                                      boolean serverRunning = Tools.isMyServiceRunning(GraphicActivity.this);
                                      if (mServerRunning != serverRunning)
                                      {
                                          mServerRunning = serverRunning;
                                          invalidateOptionsMenu();
                                      }

                                      updateCarStatus();
                                      updateAverageSpeed();
                                      updateNetworkState();
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

    void updateCarStatus()
    {
        int state = (int)Settings.getLong(Settings.KEY_CAR_STATE);

        if (mCarState == state)
            return;

        ImageView logo = (ImageView)findViewById(R.id.logoImageView);
        ImageView engine = (ImageView)findViewById(R.id.engineImageView);

        switch (state)
        {
            case Settings.CAR_STATE_OK:
                engine.setVisibility(View.INVISIBLE);
                engine.setBackgroundColor(Color.BLACK);
                logo.setVisibility(View.VISIBLE);
                break;
            case Settings.CAR_STATE_MALFUNCTION_1:
                engine.setVisibility(View.INVISIBLE);
                logo.setBackgroundColor(Color.argb(0xFF, 0x88, 0, 0));
                logo.setVisibility(View.VISIBLE);
                break;
            case Settings.CAR_STATE_MALFUNCTION_2:
                engine.setVisibility(View.VISIBLE);
                engine.setColorFilter(Color.RED);
                logo.setVisibility(View.INVISIBLE);
                break;
        }

        mCarState = state;
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
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.graphic_activity_menu, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        super.onPrepareOptionsMenu(menu);

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
            case R.id.menu_about:
                intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
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

    public synchronized void damageReceived(long duration)
    {
        if (mFlashExecutor != null)
        {
            mFlashExecutor.shutdownNow();
            mFlashExecutor = null;
        }

        mFlashExecutor = new ScheduledThreadPoolExecutor(1);
        mFlashExecutorDuration = duration;
        mFlashExecutorStart = System.currentTimeMillis();

        mFlashExecutor.scheduleAtFixedRate(new Runnable()
        {
            @Override
            public void run()
            {
                runOnUiThread(new Runnable()
                              {
                                  @Override
                                  public void run()
                                  {
                                      long timePassed = System.currentTimeMillis() - mFlashExecutorStart;
                                      double rate = (double)timePassed / (double)mFlashExecutorDuration;
                                      if (rate > 1.0)
                                          rate = 1.0;

                                      double alpha = 1.0f - Math.pow(rate, 4);
                                      if (alpha < 0)
                                          alpha = 0.0;

                                      mCoverImage.setBackgroundColor(Color.argb((int)(alpha*255), 255, 0, 0));

                                      if (timePassed > mFlashExecutorDuration)
                                      {
                                          mFlashExecutor.shutdown();
                                          return;
                                      }
                                  }
                              }
                );
            }
        }, 0, duration/40, TimeUnit.MILLISECONDS);
    }
}
