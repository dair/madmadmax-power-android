package org.albiongames.madmadmax.power.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.albiongames.madmadmax.power.data_storage.FuelQuality;
import org.albiongames.madmadmax.power.data_storage.Settings;
import org.albiongames.madmadmax.power.network.NetworkTools;
import org.albiongames.madmadmax.power.service.NetworkingThread;
import org.albiongames.madmadmax.power.R;
import org.albiongames.madmadmax.power.Tools;
import org.albiongames.madmadmax.power.data_storage.Upgrades;
import org.json.JSONException;
import org.json.JSONObject;

public class FuelLoadActivity extends Activity
{
    boolean mTimerActive = false;

    private Settings settings;

    public Settings getSettings() {
        assert settings!=null;
        return settings;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fuel_load);

        settings = new Settings(this);

        Button sendButton = (Button)findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                handleSend();
            }
        });
    }

    @Override
    public void onResume()
    {
        super.onResume();

        setEditText();
    }

    void setEditText()
    {
        EditText text = (EditText)findViewById(R.id.fuelCodeText);
        text.requestFocus();
        Tools.showKeyboard(this);
    }

    void handleSend()
    {
        Tools.hideKeyboard(this);


        EditText text = (EditText)findViewById(R.id.fuelCodeText);
        String code = text.getText().toString().trim();


        if (code.length() != 8)
        {
            Tools.messageBox(this, R.string.fuel_load_mistype_code);
            return;
        }

        // code good enough
        sendFuelCode(code);
    }

    JSONObject mReturnObject = null;
    String mCode = null;
    ProgressDialog mProgressDialog = null;

    private class SendCode extends AsyncTask<String, Void, JSONObject>
    {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mProgressDialog = ProgressDialog.show(FuelLoadActivity.this,
                    null, getString(R.string.wait_dialog));
        }

        @Override
        protected JSONObject doInBackground(String... params) {
            if (mCode == null)
                return null;

            JSONObject object = new JSONObject();
            JSONObject ret = null;
            try {
                object.put("code", mCode);
                object.put("dev_id", getSettings().getString(Settings.KEY_DEVICE_ID));
                NetworkTools.Request request = new NetworkTools.Request("POST", NetworkTools.fuelUrl(getSettings()), object);
                NetworkTools.Response response = NetworkTools.one(request, NetworkTools.ZIP_AUTO,getSettings());

                ret = response.getObject();
            } catch (JSONException ex) {
                // wat?
            } catch (Exception ex) {
                ret = new JSONObject();
                try {
                    ret.put("code", 0);
                    ret.put("error", ex.getMessage());
                } catch (JSONException ex2) {
                }
            }
            return ret;
        }

        @Override
        protected void onPostExecute(JSONObject result)
        {
            super.onPostExecute(result);

            mProgressDialog.dismiss();

            processResponse(result);
        }
    }

    void sendFuelCode(final String code)
    {
        mCode = code;

        new SendCode().execute(code);
    }

    @Override
    public void onBackPressed()
    {
        if (mTimerActive)
            return;

        super.onBackPressed();
    }

    void processResponse(JSONObject object)
    {
        if (object == null)
        {
            Tools.messageBox(this, R.string.fuel_load_unknown);
        }
        else
        {
            try {
                if (object.getBoolean("code"))
                {
                    mReturnObject = object;
                    int amount = mReturnObject.getInt("amount");
                    long timeRatio = getSettings().getLong(Settings.KEY_FUEL_LOAD_SPEED);

                    long timeout = amount * timeRatio;

                    mTimerActive = true;

                    EditText text = (EditText)findViewById(R.id.fuelCodeText);
                    text.setEnabled(false);

                    Tools.showTimer(this, timeout, R.string.fuel_load_comment, new Runnable()
                    {
                        @Override
                        public void run() {
                            afterTimer();
                        }
                    });

                } else {
                    switch (object.getInt("amount")) {
                        case -1:
                            // invalid code
                            Tools.messageBox(this, R.string.fuel_load_invalid_code);
                            break;

                        case 0:
                            // used code
                            Tools.messageBox(this, R.string.fuel_load_used_code);
                            break;

                    }
                    setEditText();
                }
            } catch (JSONException ex) {
                Tools.messageBox(this, R.string.fuel_load_unknown);
            }
        }
    }

    void afterTimer()
    {
        try {
            // got some fuel
            int amount = mReturnObject.getInt("amount");

            double fuelNow = getSettings().getDouble(Settings.KEY_FUEL_NOW);
            double fuelMax = getSettings().getDouble(Settings.KEY_FUEL_MAX);
            fuelMax = Upgrades.upgradeValue(Settings.KEY_FUEL_MAX, fuelMax);

            double fuelBecome = Tools.clamp(fuelNow + amount, 0, fuelMax);
            getSettings().setDouble(Settings.KEY_FUEL_NOW, fuelBecome);

            JSONObject upgrades = null;
            if (mReturnObject.has("upgrades")) {
                upgrades = mReturnObject.getJSONObject("upgrades");
            }
            FuelQuality.fuelAdd(fuelNow, fuelBecome - fuelNow, upgrades);

            Tools.messageBox(this, R.string.fuel_load_success, new Runnable() {
                @Override
                public void run() {
                    FuelLoadActivity.this.finish();
                    return;
                }
            });
        }
        catch (JSONException ex)
        {

        }
    }
}
