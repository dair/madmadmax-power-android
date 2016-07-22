package org.albiongames.madmadmax.power;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

public class FuelLoadActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fuel_load);

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
                object.put("dev_id", Settings.getString(Settings.KEY_DEVICE_ID));
                NetworkingThread.Request request = new NetworkingThread.Request("POST", NetworkingThread.fuelUrl(), object);
                NetworkingThread.Response response = NetworkingThread.one(request);

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
        protected void onPostExecute(JSONObject result) {
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

    void processResponse(JSONObject object)
    {
        if (object == null)
        {
            Tools.messageBox(this, R.string.fuel_load_unknown);
        }
        else
        {
            try {
                if (object.getBoolean("code")) {
                    // got some fuel
                    int amount = object.getInt("amount");

                    double fuelNow = Settings.getDouble(Settings.KEY_FUEL_NOW);
                    double fuelMax = Settings.getDouble(Settings.KEY_FUEL_MAX);
                    fuelMax = Upgrades.upgradeValue(Settings.KEY_FUEL_MAX, fuelMax);

                    double fuelBecome = Tools.clamp(fuelNow + amount, 0, fuelMax);
                    Settings.setDouble(Settings.KEY_FUEL_NOW, fuelBecome);

                    Tools.messageBox(this, R.string.fuel_load_success, new Runnable() {
                        @Override
                        public void run() {
                            Tools.hideKeyboard(FuelLoadActivity.this);
                            FuelLoadActivity.this.finish();
                            return;
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
                }
            } catch (JSONException ex) {
                Tools.messageBox(this, R.string.fuel_load_unknown);
            }
        }

        setEditText();
    }
}
