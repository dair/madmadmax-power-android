package org.albiongames.madmadmax.power;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ServiceStatusActivity extends AppCompatActivity
{
    long mLastTimeChanged = 0;

    ScheduledThreadPoolExecutor mExecutor = null;

    ArrayAdapter mAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_status);

        Button button = (Button)findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                ServiceStatusActivity.this.clickOnOff();
            }
        });

        mAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1);

        ListView listView = (ListView)findViewById(R.id.listView);
        listView.setAdapter(mAdapter);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

//        bindService();

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
                                      updateText();
                                      updatePositions();
                                      updateThreadsState();
                                  }
                              }
                );
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    @Override
    protected void onPause()
    {
//        unbindService();
        mExecutor.shutdownNow();
        mExecutor = null;
        super.onPause();
    }

    protected void clickOnOff()
    {
        long time = System.currentTimeMillis();
        if (time - mLastTimeChanged < 2500)
            return;

        mLastTimeChanged = time;

        if (isMyServiceRunning(PowerService.class))
        {
            // stop service
            PowerService.graciousStop();
        }
        else
        {
            startService(new Intent(this, PowerService.class));
        }
    }

    protected void updateText()
    {
        Button button = (Button)findViewById(R.id.button);
        TextView label = (TextView)findViewById(R.id.serviceStatusView);
        if (isMyServiceRunning(PowerService.class))
        {
            label.setText("ON");
            button.setText("Turn service OFF");
        }
        else
        {
            label.setText("OFF");
            button.setText("Turn service ON");
        }
    }

    protected void updatePositions()
    {
        if (PowerService.instance() == null)
            return;

        List<StorageEntry.Base> list = PowerService.instance().getPositions();
        if (list == null)
            return;

        for (StorageEntry.Base item: list)
        {
            mAdapter.add(item);
        }

        list.clear();
    }

    void applyStatusToTextView(int status, TextView textView)
    {
        if (textView == null)
            return;

        int color = Color.GRAY;
        switch (status)
        {
            case StatusThread.STATUS_OFF:
                color = Color.RED;
                break;
            case StatusThread.STATUS_ON:
                color = Color.GREEN;
                break;
            case StatusThread.STATUS_STARTING:
                color = Color.BLUE;
                break;
            case StatusThread.STATUS_STOPPING:
                color = Color.YELLOW;
                break;
        }
        textView.setBackgroundColor(color);
    }

    protected void updateThreadsState()
    {
        int logicStatus = StatusThread.STATUS_OFF;
        int locationStatus = StatusThread.STATUS_OFF;
        int networkStatus = StatusThread.STATUS_OFF;

        if (PowerService.instance() != null)
        {
            logicStatus = PowerService.instance().getLogicThreadStatus();
            locationStatus = PowerService.instance().getLocationThreadStatus();
            networkStatus = PowerService.instance().getNetworkThreadStatus();
        }

        TextView logicText = (TextView)findViewById(R.id.logicThreadStatusText);
        applyStatusToTextView(logicStatus, logicText);
        TextView locationText = (TextView)findViewById(R.id.locationThreadStatusText);
        applyStatusToTextView(locationStatus, locationText);
        TextView networkingText = (TextView)findViewById(R.id.networkThreadStatusText);
        applyStatusToTextView(networkStatus, networkingText);
    }

    protected void updateNetworkState()
    {
        long success = Settings.getLong(Settings.KEY_LATEST_SUCCESS_CONNECTION);
        long fail = Settings.getLong(Settings.KEY_LATEST_FAILED_CONNECTION);

        TextView text = (TextView)findViewById(R.id.networkStatusText);
        if (text == null)
            return;

        if (success > fail)
        {
            text.setText("OK");
            if (System.currentTimeMillis() - success < 2*Settings.getLong(Settings.KEY_GPS_IDLE_INTERVAL))
            {
                text.setTextColor(Color.GREEN);
            }
            else
            {
                text.setTextColor(Color.DKGRAY);
            }
        }
        else
        {
            text.setText("FAIL");
            text.setTextColor(Color.RED);
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass)
    {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
        {
            if (serviceClass.getName().equals(service.service.getClassName()))
            {
                return true;
            }
        }
        return false;
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
        // Handle item selection
        switch (item.getItemId())
        {
            case R.id.bt_device:
                Intent intent = new Intent(this, BluetoothDeviceActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
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
                        ServiceStatusActivity.this.finish();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }
}
