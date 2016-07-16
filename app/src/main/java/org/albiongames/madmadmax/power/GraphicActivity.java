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
import android.support.v7.widget.PopupMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class GraphicActivity extends AppCompatActivity {

    public static final int STATUS_FAIL = -1;
    public static final int STATUS_OK = 1;
    public static final int STATUS_UNKNOWN = 0;
    public static final int STATUS_INITIAL = -100;

    double mHitPoints = 0;
    double mCarStatusHitPoints = 0;
    float mCarStatusAverageSpeed = -100;
    double mMaxHitPoints = 0;

    double mFuel = 0;
    double mMaxFuel = 0;

    int mGpsStatus = STATUS_INITIAL;
    int mNetworkStatus = STATUS_INITIAL;

    int mCarState = -100;
    double mFuelNow = -100;

    long counter = 0; //tmp

    boolean mServerRunning = false;

    ImageView mCoverImage = null;

    ScheduledThreadPoolExecutor mExecutor = null;
    ScheduledThreadPoolExecutor mFlashExecutor = null;
    long mFlashExecutorDuration = 0;
    long mFlashExecutorStart = 0;

    int mBluetoothState = -100;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graphic);
        mCoverImage = (ImageView)findViewById(R.id.coverImage);

        final ImageButton menuButton = (ImageButton)findViewById(R.id.menuButton);
        if (menuButton != null)
        {
            boolean hasMenuKey = ViewConfiguration.get(this).hasPermanentMenuKey();
            int visibility = hasMenuKey? View.INVISIBLE : View.VISIBLE;
            menuButton.setVisibility(visibility);
            if (visibility == View.VISIBLE)
            {
                menuButton.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        showPopupMenu(menuButton);
                    }
                });
            }
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        mCarState = -100;
        mHitPoints = -100;
        mCarStatusHitPoints = -100;
        mMaxHitPoints = -100;
        mCarStatusAverageSpeed = -100;

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
                                      updateBluetoothStatus();
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
        double currentFuel = Settings.getDouble(Settings.KEY_FUEL_NOW);
        double currentHp = Settings.getDouble(Settings.KEY_HITPOINTS);
        double averageSpeed = Settings.getDouble(Settings.KEY_AVERAGE_SPEED);

        if (mCarState == state && Math.abs(currentFuel - mFuelNow) < 0.5 &&
                Math.abs(currentHp - mCarStatusHitPoints) < 0.1 &&
                Math.abs(averageSpeed - mCarStatusAverageSpeed) < 1)
            return;

        ImageView logo = (ImageView)findViewById(R.id.logoImageView);
        ImageView engine = (ImageView)findViewById(R.id.engineImageView);
        ImageView fuel = (ImageView)findViewById(R.id.fuelImageView);
        ImageView stop = (ImageView)findViewById(R.id.stopSignImageView);
        ImageView death = (ImageView)findViewById(R.id.deathImageView);

        LinearLayout background = (LinearLayout)findViewById(R.id.background);

        View parent = findViewById(R.id.statusParent);


        if (currentHp <= 0)
        {
            // death
            logo.setVisibility(View.INVISIBLE);
            engine.setVisibility(View.INVISIBLE);
            fuel.setVisibility(View.INVISIBLE);
            stop.setVisibility(View.INVISIBLE);
            death.setVisibility(View.VISIBLE);

            death.setColorFilter(Color.RED);
            background.setBackgroundColor(Color.BLACK);

            mCarStatusHitPoints = currentHp;
        }
        else
        if (state == Settings.CAR_STATE_MALFUNCTION_2)
        {
            if (averageSpeed > 1)
            {
                logo.setVisibility(View.INVISIBLE);
                engine.setVisibility(View.VISIBLE);
                fuel.setVisibility(View.INVISIBLE);
                stop.setVisibility(View.VISIBLE);
                death.setVisibility(View.INVISIBLE);
            }
            else
            {
                //Malfunction 2
                logo.setVisibility(View.INVISIBLE);
                engine.setVisibility(View.VISIBLE);
                fuel.setVisibility(View.INVISIBLE);
                stop.setVisibility(View.INVISIBLE);
                death.setVisibility(View.INVISIBLE);

                engine.setColorFilter(Color.RED);
            }
            background.setBackgroundColor(Color.argb(0xFF, 0x77, 0, 0));
        }
        else
        {
            // no malfunction 2
            if (currentFuel < 1.0)
            {
                if (averageSpeed > 1.0)
                {
                    logo.setVisibility(View.INVISIBLE);
                    engine.setVisibility(View.VISIBLE);
                    fuel.setVisibility(View.INVISIBLE);
                    stop.setVisibility(View.VISIBLE);
                    death.setVisibility(View.INVISIBLE);
                }
                else
                {
                    // out of fuel
                    logo.setVisibility(View.INVISIBLE);
                    engine.setVisibility(View.INVISIBLE);
                    fuel.setVisibility(View.VISIBLE);
                    stop.setVisibility(View.INVISIBLE);
                    death.setVisibility(View.INVISIBLE);

                    fuel.setColorFilter(Color.RED);
                }
                background.setBackgroundColor(Color.BLACK);
            }
            else
            {
                // enough fuel
                switch (state)
                {
                    case Settings.CAR_STATE_OK:
                        logo.setVisibility(View.VISIBLE); // color will be updated by speed
                        engine.setVisibility(View.INVISIBLE);
                        fuel.setVisibility(View.INVISIBLE);
                        stop.setVisibility(View.INVISIBLE);
                        death.setVisibility(View.INVISIBLE);

                        background.setBackgroundColor(Color.BLACK);
                        break;
                    case Settings.CAR_STATE_MALFUNCTION_1:
                        logo.setVisibility(View.VISIBLE); // color will be updated by speed
                        engine.setVisibility(View.INVISIBLE);
                        fuel.setVisibility(View.INVISIBLE);
                        stop.setVisibility(View.INVISIBLE);
                        death.setVisibility(View.INVISIBLE);

                        background.setBackgroundColor(Color.argb(0xFF, 0x77, 0, 0));
                        break;
                }
            }
        }

        mCarState = state;
        mFuelNow = currentFuel;
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
                networkingImageView.setColorFilter(Color.WHITE);
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
                return; // already done in updateCarStatus()
            }

            if (averageSpeed < 0.5)
            {
                logo.setColorFilter(Color.GRAY);
                return;
            }

            if (averageSpeed > redZoneSpeed)
            {
                logo.setColorFilter(Color.RED);
                return;
            }

            if (averageSpeed > redZoneSpeed * 0.75)
            {
                int color = Tools.colorMiddle(Color.WHITE, Color.RED, (averageSpeed - redZoneSpeed * 0.75) / (redZoneSpeed * 0.25));
                logo.setColorFilter(color);
                return;
            }
            else
            {
                logo.setColorFilter(Color.WHITE);
                return;
            }
        }
        else
        {
            logo.setColorFilter(Color.DKGRAY);
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
        double currentHP = Settings.getDouble(Settings.KEY_HITPOINTS);
        double maxHP = Settings.getDouble(Settings.KEY_MAXHITPOINTS);

        if (currentHP == mHitPoints && maxHP == mMaxHitPoints)
            return;

        mHitPoints = currentHP;
        mMaxHitPoints = maxHP;

        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBarHP);
        progressBar.setMax(Math.round((float)mMaxHitPoints));
        progressBar.setProgress(Math.round((float)mHitPoints));

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
        double currentFuel = Settings.getDouble(Settings.KEY_FUEL_NOW);
        double maxFuel = (double)Settings.getDouble(Settings.KEY_FUEL_MAX);

        if (currentFuel == mFuel && maxFuel == mMaxFuel)
            return;

        mFuel = currentFuel;
        mMaxFuel = maxFuel;

        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBarFuel);
        progressBar.setMax((int)Math.round(mMaxFuel));
        progressBar.setProgress((int)Math.round(mFuel));

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

    void updateBluetoothStatus()
    {
        int status = (int)Settings.getLong(Settings.KEY_BLUETOOTH_STATUS);

        int srv = mServerRunning? 1000 : 0;

        status += srv;

        if (status == mBluetoothState)
            return;

        mBluetoothState = status;

        status -= srv;

        int color = Color.DKGRAY;

        if (mServerRunning)
        {
            switch (status)
            {
                case BluetoothThread.STATUS_CONNECTED:
                    color = Color.WHITE;
                    break;
                case BluetoothThread.STATUS_DISCONNECTED:
                    color = Color.argb(0xFF, 0x77, 0, 0);
                    break;
                case BluetoothThread.STATUS_FAILED:
                    color = Color.RED;
                    break;
                case BluetoothThread.STATUS_OFF:
                    color = Color.GRAY;
                    break;
                case BluetoothThread.STATUS_STOPPING:
                    color = Color.LTGRAY;
                    break;
            }
        }

        ImageView image = (ImageView)findViewById(R.id.bluetoothImageView);
        image.setColorFilter(color);
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

    void updateServiceMenu(MenuItem item)
    {

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
        boolean ret = Tools.processMenu(item, this);

        if (ret)
            return ret;

        switch (item.getItemId())
        {
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

    public void showPopupMenu(View view)
    {
        PopupMenu menu = new PopupMenu(this, view);
        menu.inflate(R.menu.graphic_activity_menu);
        onPrepareOptionsMenu(menu.getMenu());

        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick(MenuItem item)
            {
                return onOptionsItemSelected(item);
            }
        });

        menu.show();
    }
}
