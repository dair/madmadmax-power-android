package org.albiongames.madmadmax.power;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.concurrent.ExecutionException;
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

    ImageView mCoverImage = null;

    ScheduledThreadPoolExecutor mExecutor = null;
    ScheduledThreadPoolExecutor mFlashExecutor = null;
    long mFlashExecutorDuration = 0;
    long mFlashExecutorStart = 0;
    ScheduledThreadPoolExecutor mTimerExecutor = null;
    long mScreenTimerTime = 0;
    TextView mScreenTimerTextView = null;

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

        mScreenTimerTextView = (TextView)findViewById(R.id.timerText);

        ProgressBar fuelBar = (ProgressBar)findViewById(R.id.progressBarFuel);
        fuelBar.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                if (Tools.isMyServiceRunning(GraphicActivity.this))
                {
                    double averageSpeedKmh = Tools.metersPerSecondToKilometersPerHour(Tools.getAverageSpeed());
                    if (averageSpeedKmh < 1.0)
                    {
                        if (Settings.getDouble(Settings.KEY_HITPOINTS) > 0)
                        {
                            Intent intent = new Intent(GraphicActivity.this, FuelLoadActivity.class);
                            startActivity(intent);
                        }
                    }
                    else
                    {
                        Tools.messageBox(GraphicActivity.this, R.string.graphic_car_should_stop);
                    }
                }
                else
                {
                    Tools.messageBox(GraphicActivity.this, R.string.graphic_service_should_run);
                }
                return false;
            }
        });

        ProgressBar hpBar = (ProgressBar)findViewById(R.id.progressBarHP);
        hpBar.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                if (Tools.isMyServiceRunning(GraphicActivity.this))
                {
                    double averageSpeedKmh = Tools.metersPerSecondToKilometersPerHour(Tools.getAverageSpeed());
                    if (averageSpeedKmh < 1.0)
                    {
                        if (Settings.getDouble(Settings.KEY_HITPOINTS) > 0)
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
                else
                {
                    Tools.messageBox(GraphicActivity.this, R.string.graphic_service_should_run);
                }
                return false;
            }
        });

        ImageView logo = (ImageView)findViewById(R.id.logoImageView);
        logo.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                if (Tools.isMyServiceRunning(GraphicActivity.this))
                {
                    double averageSpeedKmh = Tools.metersPerSecondToKilometersPerHour(Tools.getAverageSpeed());

                    long siegeState = Settings.getLong(Settings.KEY_SIEGE_STATE);
                    if (siegeState == Settings.SIEGE_STATE_OFF)
                    {
                        if (averageSpeedKmh < 1.0)
                        {
                            startScreenTimer(Settings.getLong(Settings.KEY_DRIVE2SIEGE_DELAY), new Runnable() {
                                @Override
                                public void run()
                                {
                                    Settings.setLong(Settings.KEY_SIEGE_STATE, Settings.SIEGE_STATE_ON);
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

        ImageView fire = (ImageView)findViewById(R.id.fireImageView);
        fire.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                if (Tools.isMyServiceRunning(GraphicActivity.this))
                {
                    startScreenTimer(Settings.getLong(Settings.KEY_SIEGE2DRIVE_DELAY), new Runnable() {
                        @Override
                        public void run()
                        {
                            Settings.setLong(Settings.KEY_SIEGE_STATE, Settings.SIEGE_STATE_OFF);
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

        }

        TextView versionView = (TextView)findViewById(R.id.versionTextView);
        versionView.setText(version);

        TextView nameView = (TextView)findViewById(R.id.nameTextView);
        nameView.setText(Settings.getString(Settings.KEY_DEVICE_NAME));
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

        mExecutor.shutdownNow();
        mExecutor = null;
    }

    void updateCarStatus()
    {
        int state = (int)Settings.getLong(Settings.KEY_CAR_STATE);
        double currentFuel = Settings.getDouble(Settings.KEY_FUEL_NOW);
        double currentHp = Settings.getDouble(Settings.KEY_HITPOINTS);
        double averageSpeedKmh = Tools.metersPerSecondToKilometersPerHour(Tools.getAverageSpeed());

        if (mCarState == state && Math.abs(currentFuel - mFuelNow) < 0.5 &&
                Math.abs(currentHp - mCarStatusHitPoints) < 0.1 &&
                Math.abs(averageSpeedKmh - mCarStatusAverageSpeed) < 1)
            return;

        ImageView logo = (ImageView)findViewById(R.id.logoImageView);
        ImageView engine = (ImageView)findViewById(R.id.engineImageView);
        ImageView fuel = (ImageView)findViewById(R.id.fuelImageView);
        ImageView stop = (ImageView)findViewById(R.id.stopSignImageView);
        ImageView death = (ImageView)findViewById(R.id.deathImageView);
        ImageView fire = (ImageView)findViewById(R.id.fireImageView);

        LinearLayout background = (LinearLayout)findViewById(R.id.background);

        logo.setVisibility(View.INVISIBLE);
        engine.setVisibility(View.INVISIBLE);
        fuel.setVisibility(View.INVISIBLE);
        stop.setVisibility(View.INVISIBLE);
        death.setVisibility(View.INVISIBLE);
        fire.setVisibility(View.INVISIBLE);
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
                // enough fuel
                long siegeState = Settings.getLong(Settings.KEY_SIEGE_STATE);
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
            double averageSpeedMps = Tools.getAverageSpeed();
            double averageSpeedKmh = Tools.metersPerSecondToKilometersPerHour(averageSpeedMps);

            double redZoneSpeed = LogicThread.getCurrentRedZone();

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

            if (averageSpeedKmh < 1)
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
        double maxFuel = Settings.getDouble(Settings.KEY_FUEL_MAX);
        maxFuel = Upgrades.upgradeValue(Settings.KEY_FUEL_MAX, maxFuel);

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
            if (Tools.checkServerStart(this))
            {
                startService(new Intent(this, PowerService.class));
            }
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
}
