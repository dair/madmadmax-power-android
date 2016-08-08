package org.albiongames.madmadmax.power.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import org.albiongames.madmadmax.power.R;
import org.albiongames.madmadmax.power.Tools;

public class BluetoothConsoleActivity extends AppCompatActivity {

    BluetoothSPP mSPP = null;
    String mAddress = null;
    EditText mConsole = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_console);

        mSPP = new BluetoothSPP(this);
        mSPP.setupService();
        mSPP.startService(BluetoothState.DEVICE_OTHER);

        Intent intent = getIntent();
        mAddress = intent.getStringExtra("address");

        mConsole = (EditText)findViewById(R.id.consoleText);

        mSPP.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener()
        {
            @Override
            public void onDataReceived(byte[] data, final String message)
            {
                Tools.log(message);
                BluetoothConsoleActivity.this.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        mConsole.getText().append(message + "\n");
                        mConsole.scrollTo(0, mConsole.getBottom());
                    }
                });
            }
        });


        EditText editText = (EditText)findViewById(R.id.editText);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
            {
                if (actionId == EditorInfo.IME_ACTION_DONE)
                {
                    String text = v.getText().toString().trim();
                    mSPP.send(text + "\n", false);
                }
                return false;
            }
        });
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (!mSPP.isBluetoothAvailable())
        {
            Tools.messageBox(this, "Bluetooth is not available");
            return;
        }

        try
        {
            mSPP.connect(mAddress);
        }
        catch (Exception ex)
        {
            Tools.log(ex.toString());
        }
    }

    @Override
    public void onPause()
    {
        mSPP.disconnect();
        super.onPause();
    }

    @Override
    public void onDestroy()
    {
        mSPP.stopService();
        super.onDestroy();
    }
}
