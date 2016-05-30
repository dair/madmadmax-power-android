package org.albiongames.madmadmax.power;

import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.net.HttpURLConnection;
import java.net.URL;
import android.provider.Settings.Secure;

import org.json.JSONException;
import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity implements NetworkingThread.Listener
{
    private String mStoredDeviceId = null;
    Button mRegisterButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        Button registerButton = (Button)findViewById(R.id.registerButton);
        mStoredDeviceId = Settings.getString(Settings.KEY_DEVICE_ID);
        EditText deviceNameWidget = (EditText)findViewById(R.id.deviceName);
        if (mStoredDeviceId != null)
        {
            deviceNameWidget.setText(mStoredDeviceId);
        }
        else
        {
            mStoredDeviceId = "";
        }

        deviceNameWidget.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                if (!s.toString().trim().equals(mStoredDeviceId))
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

        registerButton.setOnClickListener(new View.OnClickListener()
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
            object.put("id", android_id);
            object.put("desc", manufacturer + ":" + brand + ":" + model);
            object.put("name", name);
        }
        catch (JSONException ex)
        {
            return;
        }

        final NetworkingThread.Request request = new NetworkingThread.Request("POST", NetworkingThread.authUrl(), object.toString());

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
        mStoredDeviceId = null;
    }

    @Override
    public void onNetworkSuccess(NetworkingThread.Request request, NetworkingThread.Response response)
    {
        Tools.messageBox(this, response.getBody());
    }
}
