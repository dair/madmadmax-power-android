package org.albiongames.madmadmax.power.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import org.albiongames.madmadmax.power.R;
import org.albiongames.madmadmax.power.Tools;
import org.albiongames.madmadmax.power.data_storage.Settings;
import org.albiongames.madmadmax.power.data_storage.Upgrades;
import org.albiongames.madmadmax.power.service.BluetoothThread;
import org.albiongames.madmadmax.power.service.PowerService;

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

    boolean mServerRunning = false;

    //Views:
    @BindView(R.id.coverImage)
    ImageView mCoverImage;
    @BindView(R.id.menuButton)
    ImageButton menuButton;
    @BindView(R.id.timerText)
    TextView mScreenTimerTextView;
    @BindView(R.id.progressBarFuel)
    ProgressBar fuelBar;
    @BindView(R.id.progressBarHP)
    ProgressBar hpBar;
    @BindView(R.id.fuelText)
    TextView fuelText;
    @BindView(R.id.logoImageView)
    ImageView logo;
    @BindView(R.id.fireImageView)
    ImageView fire;

    @BindView(R.id.engineImageView)
    ImageView engine;
    @BindView(R.id.fuelImageView)
    ImageView fuel;
    @BindView(R.id.stopSignImageView)
    ImageView stop;
    @BindView(R.id.deathImageView)
    ImageView death;
    @BindView(R.id.gpsErrorImageView)
    ImageView gpsError;
    @BindView(R.id.networkingImageView)
    ImageView networkingImageView;
    @BindView(R.id.gpsImageView)
    ImageView gpsImageView;
    @BindView(R.id.bluetoothImageView)
    ImageView bluetoothImageView;


    @BindView(R.id.background)
    LinearLayout background;
    @BindView(R.id.versionTextView)
    TextView versionView;

    @BindView(R.id.nameTextView)
    TextView nameView;
    @BindView(R.id.hpText)
    TextView hpText;









    ScheduledThreadPoolExecutor mExecutor = null;
    ScheduledThreadPoolExecutor mFlashExecutor = null;
    long mFlashExecutorDuration = 0;
    long mFlashExecutorStart = 0;
    ScheduledThreadPoolExecutor mTimerExecutor = null;
    long mScreenTimerTime = 0;

    int mBluetoothState = -100;

    DamageReceiver mDamageReceiver = new DamageReceiver();
    private Settings settings;

    public Settings getSettings() {
        assert settings!=null;
        return settings;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graphic);

        ButterKnife.bind(this);



      settings = new Settings(this);

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


        fuelBar.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                if (Tools.isMyServiceRunning(GraphicActivity.this))
                {
                    long lastGpsSignal = getSettings().getLong(Settings.KEY_LAST_GPS_UPDATE);
                    long now = System.currentTimeMillis();
                    long timeout = getSettings().getLong(Settings.KEY_GPS_TIMEOUT);
                    if (now - lastGpsSignal > timeout)
                    {
                        Tools.messageBox(GraphicActivity.this, R.string.graphic_car_should_stop);
                    }
                    else
                    {
                        if (!Tools.isCarMoving(getSettings()))
                        {
                            if (getSettings().getDouble(Settings.KEY_HITPOINTS) > 0)
                            {
                                PopupMenu menu = new PopupMenu(GraphicActivity.this, fuelText);
                                menu.inflate(R.menu.fuel_menu);

                                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
                                {
                                    @Override
                                    public boolean onMenuItemClick(MenuItem item)
                                    {
                                        switch (item.getItemId())
                                        {
                                            case R.id.menu_fuel_load:
                                                Intent intent = new Intent(GraphicActivity.this, FuelLoadActivity.class);
                                                startActivity(intent);
                                                return true;

                                            case R.id.menu_fuel_drop:
                                                intent = new Intent(GraphicActivity.this, FuelDropActivity.class);
                                                startActivity(intent);
                                                return true;

                                        }
                                        return false;
                                    }
                                });

                                menu.show();


                            }
                        }
                        else
                        {
                            Tools.messageBox(GraphicActivity.this, R.string.graphic_car_should_stop);
                        }
                    }
                }
                else
                {
                    Tools.messageBox(GraphicActivity.this, R.string.graphic_service_should_run);
                }
                return false;
            }
        });

        hpBar.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                if (Tools.isMyServiceRunning(GraphicActivity.this))
                {
                    long lastGpsSignal = getSettings().getLong(Settings.KEY_LAST_GPS_UPDATE);
                    long now = System.currentTimeMillis();
                    long timeout = getSettings().getLong(Settings.KEY_GPS_TIMEOUT);
                    if (now - lastGpsSignal > timeout)
                    {
                        Tools.messageBox(GraphicActivity.this, R.string.graphic_car_should_stop);
                    }
                    else
                    {
                        if (!Tools.isCarMoving(getSettings()))
                        {
                            if (getSettings().getDouble(Settings.KEY_HITPOINTS) > 0)
                            {
                                Intent intent = new Intent(GraphicActivity.this, RepairLoadActivity.class);
                                startActivity(intent);
                            }
                        }
                        else
                        {
                            Tools.messageBox(GraphicActivity.this, R.string.graphic_car_should_stop);
                        }
                    }
                }
                else
                {
                    Tools.messageBox(GraphicActivity.this, R.string.graphic_service_should_run);
                }
                return false;
            }
        });

        logo.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                if (Tools.isMyServiceRunning(GraphicActivity.this))
                {
                    long siegeState = getSettings().getLong(Settings.KEY_SIEGE_STATE);
                    if (siegeState == Settings.SIEGE_STATE_OFF)
                    {
                        if (!Tools.isCarMoving(getSettings()))
                        {
                            startScreenTimer(getSettings().getLong(Settings.KEY_DRIVE2SIEGE_DELAY), new Runnable() {
                                @Override
                                public void run()
                                {
                                    getSettings().setLong(Settings.KEY_SIEGE_STATE, Settings.SIEGE_STATE_ON);
                                    updateEverything();
                                }
                            });
                        }
                        else
                        {
                            Tools.messageBox(GraphicActivity.this, R.string.graphic_car_should_stop);
                        }
                    }
                }
                else
                {
                    Tools.messageBox(GraphicActivity.this, R.string.graphic_service_should_run);
                }
                return false;
            }
        });

        fire.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                if (Tools.isMyServiceRunning(GraphicActivity.this))
                {
                    startScreenTimer(getSettings().getLong(Settings.KEY_SIEGE2DRIVE_DELAY), new Runnable() {
                        @Override
                        public void run()
                        {
                            getSettings().setLong(Settings.KEY_SIEGE_STATE, Settings.SIEGE_STATE_OFF);
                            updateEverything();
                        }
                    });
                }
                else
                {
                    Tools.messageBox(GraphicActivity.this, R.string.graphic_service_should_run);
                }
                return false;
            }
        });
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
                                      updateEverything();
                                  }
                              }
                );
            }
        }, 0, 1, TimeUnit.SECONDS);

        String version = "???";
        try
        {
            PackageInfo info = getPackageManager().getPackageInfo("org.albiongames.madmadmax.power", 0);
            version = info.versionName;
        }
        catch (PackageManager.NameNotFoundException ex)
        {
            ex.printStackTrace();
        }

        versionView.setText(version);
        nameView.setText(getSettings().getString(Settings.KEY_DEVICE_NAME));

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Settings.DAMAGE_ACTION);
        registerReceiver(mDamageReceiver, intentFilter);

    }

    synchronized void updateEverything()
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

    @Override
    public void onPause()
    {
        super.onPause();

        unregisterReceiver(mDamageReceiver);

        mExecutor.shutdownNow();
        mExecutor = null;
    }

    void updateCarStatus()
    {
        int state = (int) getSettings().getLong(Settings.KEY_CAR_STATE);
        double currentFuel = getSettings().getDouble(Settings.KEY_FUEL_NOW);
        double currentHp = getSettings().getDouble(Settings.KEY_HITPOINTS);
        double averageSpeedKmh = Tools.metersPerSecondToKilometersPerHour(Tools.getAverageSpeed(getSettings()));

        if (mCarState == state && Math.abs(currentFuel - mFuelNow) < 0.5 &&
                Math.abs(currentHp - mCarStatusHitPoints) < 0.1 &&
                Math.abs(averageSpeedKmh - mCarStatusAverageSpeed) < 1)
            return;


        logo.setVisibility(View.INVISIBLE);
        engine.setVisibility(View.INVISIBLE);
        fuel.setVisibility(View.INVISIBLE);
        stop.setVisibility(View.INVISIBLE);
        death.setVisibility(View.INVISIBLE);
        fire.setVisibility(View.INVISIBLE);
        gpsError.setVisibility(View.INVISIBLE);
        background.setBackgroundColor(Color.BLACK);

        if (!mServerRunning)
        {
            logo.setVisibility(View.VISIBLE);
        }
        else
        if (currentHp <= 0)
        {
            if (averageSpeedKmh > 1)
            {
                stop.setVisibility(View.VISIBLE);
            }
            else
            {
                // death
                death.setVisibility(View.VISIBLE);
                death.setColorFilter(Color.RED);
            }
            mCarStatusHitPoints = currentHp;
        }
        else
        if (state == Settings.CAR_STATE_MALFUNCTION_2)
        {
            if (averageSpeedKmh > 1)
            {
                stop.setVisibility(View.VISIBLE);
            }
            else
            {
                //Malfunction 2
                engine.setVisibility(View.VISIBLE);
                engine.setColorFilter(Color.RED);
            }
            background.setBackgroundColor(Color.argb(0xFF, 0x77, 0, 0));
        }
        else
        {
            // no malfunction 2
            if (currentFuel < 1.0)
            {
                if (averageSpeedKmh > 1.0)
                {
                    stop.setVisibility(View.VISIBLE);
                }
                else
                {
                    // out of fuel
                    fuel.setVisibility(View.VISIBLE);
                    fuel.setColorFilter(Color.RED);
                }
            }
            else
            {
                long lastGpsSignal = getSettings().getLong(Settings.KEY_LAST_GPS_UPDATE);
                long now = System.currentTimeMillis();
                long timeout = getSettings().getLong(Settings.KEY_GPS_TIMEOUT);
                if (now - lastGpsSignal > timeout)
                {
                    gpsError.setVisibility(View.VISIBLE);
                    stop.setVisibility(View.VISIBLE);
                }
                else
                {

                    // enough fuel
                    long siegeState = getSettings().getLong(Settings.KEY_SIEGE_STATE);
                    ImageView view = null;
                    if (siegeState == Settings.SIEGE_STATE_OFF)
                    {
                        view = logo;
                    }
                    else
                    {
                        view = fire;
                        fire.setColorFilter(Color.YELLOW);
                    }

                    switch (state)
                    {
                        case Settings.CAR_STATE_OK:
                            view.setVisibility(View.VISIBLE); // color will be updated by speed
                            background.setBackgroundColor(Color.BLACK);
                            break;
                        case Settings.CAR_STATE_MALFUNCTION_1:
                            view.setVisibility(View.VISIBLE); // color will be updated by speed
                            background.setBackgroundColor(Color.argb(0xFF, 0x77, 0, 0));
                            break;
                    }
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
            long success = getSettings().getLong(Settings.KEY_LATEST_SUCCESS_CONNECTION);
            long fail = getSettings().getLong(Settings.KEY_LATEST_FAILED_CONNECTION);

            if (success > fail)
            {
                if (System.currentTimeMillis() - success < 2 * getSettings().getLong(Settings.KEY_GPS_IDLE_INTERVAL))
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
        int state = (int) getSettings().getLong(Settings.KEY_CAR_STATE);

        if (Tools.isMyServiceRunning(this))
        {
            double averageSpeedMps = Tools.getAverageSpeed(getSettings());
            double averageSpeedKmh = Tools.metersPerSecondToKilometersPerHour(averageSpeedMps);

            double redZoneSpeed = Tools.getCurrentRedZone(getSettings());

            switch (state)
            {
                case Settings.CAR_STATE_MALFUNCTION_1:
                    redZoneSpeed = (float) getSettings().getDouble(Settings.KEY_MALFUNCTION1_RED_ZONE);
                    break;
                case Settings.CAR_STATE_MALFUNCTION_2:
                    redZoneSpeed = -1;
                    break;
                case Settings.CAR_STATE_OK:
                    redZoneSpeed = (float) getSettings().getDouble(Settings.KEY_RED_ZONE);
                    break;
            }

            if (state == Settings.CAR_STATE_MALFUNCTION_2)
            {
                return; // already done in updateCarStatus()
            }

            if (!Tools.isCarMoving(getSettings()))
            {
                logo.setColorFilter(Color.GRAY);
                return;
            }

            if (averageSpeedKmh > redZoneSpeed)
            {
                logo.setColorFilter(Color.RED);
                return;
            }

            if (averageSpeedKmh > redZoneSpeed * 0.75)
            {
                int color = Tools.colorMiddle(Color.WHITE, Color.RED, (averageSpeedKmh - redZoneSpeed * 0.75) / (redZoneSpeed * 0.25));
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
            logo.setColorFilter(Color.argb(255, 25, 25, 25));
        }
    }

    void updateGpsStatus()
    {

        int quality = (int) getSettings().getLong(Settings.KEY_LOCATION_THREAD_LAST_QUALITY);

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
        double currentHP = getSettings().getDouble(Settings.KEY_HITPOINTS);
        double maxHP = getSettings().getDouble(Settings.KEY_MAXHITPOINTS);

        if (currentHP == mHitPoints && maxHP == mMaxHitPoints)
            return;

        mHitPoints = currentHP;
        mMaxHitPoints = maxHP;

        hpBar.setMax(Math.round((float)mMaxHitPoints));
        hpBar.setProgress(Math.round((float)mHitPoints));

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
        double currentFuel = getSettings().getDouble(Settings.KEY_FUEL_NOW);
        double maxFuel = getSettings().getDouble(Settings.KEY_FUEL_MAX);
        maxFuel = Upgrades.upgradeValue(Settings.KEY_FUEL_MAX, maxFuel);

        if (currentFuel == mFuel && maxFuel == mMaxFuel)
            return;

        mFuel = currentFuel;
        mMaxFuel = maxFuel;

        fuelBar.setMax((int)Math.round(mMaxFuel));
        fuelBar.setProgress((int)Math.round(mFuel));

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
        int status = (int) getSettings().getLong(Settings.KEY_BLUETOOTH_STATUS);

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
                case BluetoothThread.STATUS_DISABLED:
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

        bluetoothImageView.setColorFilter(color);
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
            if (Tools.checkServerStart(this))
            {
                startService(new Intent(this, PowerService.class));
            }
        }
    }

    public synchronized void damageReceived(final long duration)
    {
        if (mFlashExecutor != null)
        {
            mFlashExecutor.shutdownNow();
            mFlashExecutor = null;
        }

        mFlashExecutor = new ScheduledThreadPoolExecutor(1);

        final long frame = 1000 / 30;

        mFlashExecutor.scheduleAtFixedRate(new Runnable()
        {
            int mCount = 0;
            long mDuration = duration;
            long mFrame = frame;

            @Override
            public void run()
            {
                runOnUiThread(new Runnable()
                              {
                                  @Override
                                  public void run()
                                  {
                                      long timePassed = mCount * mFrame;

                                      double rate = (double)timePassed / (double)mDuration;
                                      if (rate > 1.0)
                                          rate = 1.0;

                                      double alpha = 1.0f - Math.pow(rate, 4);
                                      if (alpha < 0)
                                          alpha = 0.0;

                                      mCoverImage.setAlpha((float)alpha);
                                      //mCoverImage.setBackgroundColor(Color.argb((int)(alpha*255), 255, 0, 0));

                                      if (timePassed > mDuration)
                                      {
                                          mFlashExecutor.shutdownNow();
                                          return;
                                      }

                                      ++mCount;
                                  }
                              }
                );
            }
        }, 0, frame, TimeUnit.MILLISECONDS);
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

    void startScreenTimer(long time, final Runnable runnable)
    {
        if (mTimerExecutor != null)
        {
            mTimerExecutor.shutdownNow();
            mTimerExecutor = null;
        }

        mScreenTimerTime = System.currentTimeMillis() + time;
        mScreenTimerTextView.setVisibility(View.VISIBLE);

        new Thread(new Runnable() {
            @Override
            public void run()
            {
                while (true) {
                    long remain = mScreenTimerTime - System.currentTimeMillis();
                    if (remain <= 0)
                        break;

                    final String remainString = String.format("%d.%03d", remain / 1000, remain % 1000);

                    GraphicActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mScreenTimerTextView.setText(remainString);
                        }
                    });
                    Tools.sleep(30);
                }

                GraphicActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mScreenTimerTextView.setVisibility(View.INVISIBLE);
                    }
                });

                GraphicActivity.this.runOnUiThread(runnable);
            }
        }).start();
    }


    private class DamageReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            double damage = intent.getDoubleExtra("DAMAGE", 0.0);
            if (damage > 0.0)
            {
                damageReceived(Math.round(200 + damage * 5));
            }
        }
    }
}
