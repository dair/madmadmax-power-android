package org.albiongames.madmadmax.power;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import android.provider.Settings.Secure;

import org.json.JSONException;
import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity implements NetworkingThread.Listener
{
    private String mStoredDeviceName = null;
    Button mRegisterButton = null;
    EditText mDeviceNameWidget = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        mRegisterButton = (Button)findViewById(R.id.registerButton);
        mDeviceNameWidget = (EditText)findViewById(R.id.deviceName);
        mDeviceNameWidget.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                if (!s.toString().trim().equals(mStoredDeviceName))
                {
                    mRegisterButton.setEnabled(true);
                }
                else
                {
                    mRegisterButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s)
            {

            }
        });

        mRegisterButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                EditText deviceNameWidget = (EditText)findViewById(R.id.deviceName);
                String deviceName = deviceNameWidget.getText().toString();
                deviceName = deviceName.trim();
                if (deviceName.equals(""))
                {
                    Tools.messageBox(RegisterActivity.this, R.string.no_empty_device_name);
                }
                else
                {
                    registerWithDeviceName(deviceName);
                }
            }
        });
    }

    @Override
    public void onResume()
    {
        super.onResume();

        mStoredDeviceName = Settings.getString(Settings.KEY_DEVICE_ID);
        if (mStoredDeviceName == null)
            mStoredDeviceName = "";

        mDeviceNameWidget.setText(mStoredDeviceName);
    }

    private void registerWithDeviceName(String name)
    {
        String android_id = Secure.getString(getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID);
        String manufacturer = Build.MANUFACTURER;
        String brand = Build.BRAND;
        String model = Build.MODEL;

        JSONObject object = new JSONObject();
        try
        {
            object.put("hw_id", android_id);
            object.put("desc", manufacturer + ":" + brand + ":" + model);
            object.put("name", name);
        }
        catch (JSONException ex)
        {
            return;
        }

        final NetworkingThread.Request request = new NetworkingThread.Request("POST", NetworkingThread.authUrl(), object);

        AsyncTask task = new AsyncTask()
        {
            @Override
            protected Object doInBackground(Object[] params)
            {
                NetworkingThread.one(request, RegisterActivity.this);
                return null;
            }
        };
        task.execute();
    }

    @Override
    public void onNetworkError(NetworkingThread.Request request, Error error)
    {
        Tools.messageBox(this, error.toString());
        mStoredDeviceName = null;
    }

    @Override
    public void onNetworkSuccess(NetworkingThread.Request request, NetworkingThread.Response response)
    {
        try
        {
            String deviceId = response.getObject().getString("id");
            Settings.setString(Settings.KEY_DEVICE_ID, deviceId);
            Tools.messageBox(this, R.string.reg_success);

            EditText deviceNameWidget = (EditText)findViewById(R.id.deviceName);
            mStoredDeviceName = deviceNameWidget.getText().toString();
            Settings.setString(Settings.KEY_DEVICE_NAME, mStoredDeviceName);


            final Class<? extends Activity> activityClass;
            activityClass = ServiceStatusActivity.class;
            Intent newActivity = new Intent(this, activityClass);
            startActivity(newActivity);
        }
        catch (JSONException ex)
        {
            // fail actually
            onNetworkError(request, new Error(ex.toString()));
        }
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
            case R.id.settings:
                intent = new Intent(this, SettingsActivity.class);
                break;
            case R.id.bt_device:
                intent = new Intent(this, BluetoothDeviceActivity.class);
                break;
        }

        if (intent != null)
        {
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
