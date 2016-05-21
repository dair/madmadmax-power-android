package org.albiongames.madmadmax.power;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ServiceStatusActivity extends AppCompatActivity
{
    PowerService mService = null;
    boolean mBound = false;

    ScheduledThreadPoolExecutor mExecutor = null;

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
    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }

    private void bindService()
    {
        if (!mBound)
        {
            Intent intent = new Intent(this, PowerService.class);
            bindService(intent, mConnection, Context.BIND_NOT_FOREGROUND);
        }
    }

    private void unbindService()
    {
        if (mBound)
        {
            mService.setActivity(null);
            mService = null;
            unbindService(mConnection);
            mBound = false;
        }
    }


    @Override
    protected void onResume()
    {
        super.onResume();

        bindService();

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
                                      updateText();
                                  }
                              }
                );
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    @Override
    protected void onPause()
    {
        unbindService();
        mExecutor.shutdownNow();
        mExecutor = null;
        super.onPause();
    }

    protected void clickOnOff()
    {
        if (isMyServiceRunning(PowerService.class))
        {
            // stop service
            stopService(new Intent(this, PowerService.class));
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


    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            PowerService.LocalBinder binder = (PowerService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;

            mService.setActivity(ServiceStatusActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0)
        {
            mService = null;
            mBound = false;
        }
    };

}
