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

public class RepairLoadActivity extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_repair_load);

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
        EditText text = (EditText)findViewById(R.id.repairCodeText);
        text.requestFocus();
        Tools.showKeyboard(this);
    }

    void handleSend()
    {
        EditText text = (EditText)findViewById(R.id.repairCodeText);
        String code = text.getText().toString().trim();

        if (code.length() != 8)
        {
            Tools.messageBox(this, R.string.repair_load_mistype_code);
            return;
        }

        sendRepairCode(code);
    }

    JSONObject mReturnObject = null;
    String mCode = null;
    ProgressDialog mProgressDialog = null;

    private class SendCode extends AsyncTask<String, Void, JSONObject>
    {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mProgressDialog = ProgressDialog.show(RepairLoadActivity.this,
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
                NetworkingThread.Request request = new NetworkingThread.Request("POST", NetworkingThread.repairUrl(), object);
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

    void sendRepairCode(final String code)
    {
        mCode = code;

        new SendCode().execute(code);
    }

    void processResponse(JSONObject object)
    {
        if (object == null)
        {
            Tools.messageBox(this, R.string.repair_load_unknown);
        }
        else
        {
            try {
                if (object.getBoolean("code")) {
                    // got some repair
                    int amount = object.getInt("amount");

                    double repairNow = Settings.getDouble(Settings.KEY_HITPOINTS);
                    double repairMax = Settings.getDouble(Settings.KEY_MAXHITPOINTS);
                    repairMax = Upgrades.upgradeValue(Settings.KEY_MAXHITPOINTS, repairMax);

                    double repairBecome = Tools.clamp(repairNow + amount, 0, repairMax);
                    Settings.setDouble(Settings.KEY_HITPOINTS, repairBecome);

                    int newState = Settings.CAR_STATE_OK;
                    switch ((int)Settings.getLong(Settings.KEY_CAR_STATE))
                    {
                        case Settings.CAR_STATE_OK:
                            // do nothing
                            break;
                        case Settings.CAR_STATE_MALFUNCTION_1:
                            newState = Settings.CAR_STATE_OK;
                            break;
                        case Settings.CAR_STATE_MALFUNCTION_2:
                            newState = Settings.CAR_STATE_MALFUNCTION_1;
                            break;
                    }
                    Settings.setLong(Settings.KEY_CAR_STATE, (long)newState);

                    Tools.messageBox(this, R.string.repair_load_success, new Runnable() {
                        @Override
                        public void run() {
                            Tools.hideKeyboard(RepairLoadActivity.this);
                            RepairLoadActivity.this.finish();
                            return;
                        }
                    });
                } else {
                    switch (object.getInt("amount")) {
                        case -1:
                            // invalid code
                            Tools.messageBox(this, R.string.repair_load_invalid_code);
                            break;

                        case 0:
                            // used code
                            Tools.messageBox(this, R.string.repair_load_used_code);
                            break;

                    }
                }
            } catch (JSONException ex) {
                Tools.messageBox(this, R.string.repair_load_unknown);
            }
        }

        setEditText();
    }
}
