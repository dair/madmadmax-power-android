package org.albiongames.madmadmax.power.activity;

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
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import android.provider.Settings.Secure;

import org.albiongames.madmadmax.power.data_storage.Settings;
import org.albiongames.madmadmax.power.network.NetworkTools;
import org.albiongames.madmadmax.power.service.NetworkingThread;
import org.albiongames.madmadmax.power.R;
import org.albiongames.madmadmax.power.Tools;
import org.json.JSONException;
import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity
{
    private String mStoredDeviceName = null;
    Button mRegisterButton = null;
    EditText mDeviceNameWidget = null;

    private Settings settings;

    public Settings getSettings() {
        assert settings!=null;
        return settings;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        settings = new Settings(this);

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

        mStoredDeviceName = getSettings().getString(Settings.KEY_DEVICE_NAME);
        if (mStoredDeviceName == null)
        {
            mStoredDeviceName = getSettings().getString(Settings.KEY_REGISTER_NAME);
            if (mStoredDeviceName == null)
            {
                mStoredDeviceName = "";
            }
        }

        mDeviceNameWidget.setText(mStoredDeviceName);
        if (mDeviceNameWidget.requestFocus())
        {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();

        if (mDeviceNameWidget != null)
        {
            String name = mDeviceNameWidget.getText().toString().trim();
            getSettings().setString(Settings.KEY_REGISTER_NAME, name);
        }
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

        final NetworkTools.Request request = new NetworkTools.Request("POST", NetworkTools.authUrl(getSettings()), object);

        AsyncTask task = new AsyncTask()
        {
            @Override
            protected Object doInBackground(Object[] params)
            {
                try
                {
                    final NetworkTools.Response response = NetworkTools.one(request, NetworkTools.ZIP_AUTO, getSettings());
                    final String deviceId = response.getObject().getString("id");

                    RegisterActivity.this.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            getSettings().setString(Settings.KEY_DEVICE_ID, deviceId);
                            Tools.messageBox(RegisterActivity.this, R.string.reg_success);

                            EditText deviceNameWidget = (EditText)RegisterActivity.this.findViewById(R.id.deviceName);
                            mStoredDeviceName = deviceNameWidget.getText().toString();
                            getSettings().setString(Settings.KEY_DEVICE_NAME, mStoredDeviceName);
                            Tools.hideKeyboard(RegisterActivity.this);

                            Intent newActivity = new Intent(RegisterActivity.this, GraphicActivity.class);
                            startActivity(newActivity);
                            finish();
                        }
                    });
                }
                catch (Exception ex)
                {
                    Tools.messageBox(RegisterActivity.this, ex.toString());
                    mStoredDeviceName = null;
                }

                return null;
            }
        };
        task.execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        MenuItem item = menu.findItem(R.id.menu_status);
        if (item != null)
        {
            item.setEnabled(false);
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
                intent = new Intent(this, SettingsActivity.class);
                break;
            case R.id.menu_bluetooth:
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
