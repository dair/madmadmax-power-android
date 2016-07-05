package org.albiongames.madmadmax.power;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BluetoothDeviceActivity extends AppCompatActivity
{
//    public class
    int REQUEST_ENABLE_BT = 0xBB02;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_device);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        final ProgressDialog mDialog = new ProgressDialog(this);
        mDialog.setMessage("Please wait...");
        mDialog.setCancelable(false);
        mDialog.show();

        new Thread()
        {
            @Override
            public void run()
            {
                super.run();
                startBtAdapter();
                mDialog.dismiss();
            }
        }.start();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK)
        {
            continueBtAdapter();
        }
        else
        {
            Tools.messageBox(this, R.string.bluetooth_error_init);
        }
    }


    void startBtAdapter()
    {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null)
        {
            Tools.messageBox(this, R.string.bluetooth_error_init);
            return;
        }

        if (!mBluetoothAdapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else
        {
            continueBtAdapter();
        }
    }

    void continueBtAdapter()
    {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null)
        {
            Tools.messageBox(this, R.string.bluetooth_error_init);
            return;
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0)
        {
            List items = new ArrayList(pairedDevices.size());
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices)
            {
                items.add(device);
            }

            final ListView list = (ListView)findViewById(R.id.listView);
            final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items);
            this.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    list.setAdapter(adapter);
                }
            });
        }
    }
}
