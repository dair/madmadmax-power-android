package org.albiongames.madmadmax.power.activity;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.albiongames.madmadmax.power.R;
import org.albiongames.madmadmax.power.data_storage.Settings;
import org.albiongames.madmadmax.power.Tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BluetoothDeviceActivity extends AppCompatActivity
{
//    public class
    int REQUEST_ENABLE_BT = 0xBB02;
    private Settings settings;



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_device);

        settings = new Settings(this);

        ListView listView = (ListView)findViewById(R.id.listView);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                showPopupMenu(view, position);
                return false;
            }
        });
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
                items.add(new DeviceInfo(device));
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

    void showPopupMenu(View view, final int position)
    {
        PopupMenu menu = new PopupMenu(this, view);
        menu.inflate(R.menu.bluetooth_popup_menu);

        final ListView list = (ListView)findViewById(R.id.listView);
        final String itemAddress = ((DeviceInfo)list.getAdapter().getItem(position)).getDevice().getAddress();
        String storedAddress = getSettings().getString(Settings.KEY_BLUETOOTH_DEVICE);

        if (itemAddress.equals(storedAddress))
        {
            MenuItem item = menu.getMenu().findItem(R.id.menu_bluetooth_use_this);
            item.setVisible(false);
        }

        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item)
            {
                switch (item.getItemId())
                {
                    case R.id.menu_bluetooth_use_this:
                        useThisDevice(position);
                        break;
                    case R.id.menu_bluetooth_console:
                        Intent intent = new Intent(BluetoothDeviceActivity.this, BluetoothConsoleActivity.class);
                        intent.putExtra("address", itemAddress);
                        startActivity(intent);
                        break;
                }
                return false;
            }
        });
        menu.show();
    }

    void useThisDevice(int position)
    {
        final ListView list = (ListView)findViewById(R.id.listView);
        DeviceInfo info = (DeviceInfo)list.getAdapter().getItem(position);

        String address = info.getDevice().getAddress();
        if (address == null)
        {
            Tools.messageBox(this, R.string.bluetooth_activity_error_device);
            return;
        }

        getSettings().setString(Settings.KEY_BLUETOOTH_DEVICE, address);

        ((ArrayAdapter)list.getAdapter()).notifyDataSetChanged();
    }

    public Settings getSettings() {
        assert settings!=null;
        return settings;
    }

    class DeviceInfo
    {
        BluetoothDevice mDevice = null;

        public DeviceInfo(BluetoothDevice device)
        {
            mDevice = device;
        }

        public BluetoothDevice getDevice()
        {
            return mDevice;
        }

        @Override
        public String toString()
        {
            if (mDevice == null)
                return "NULL";

            String current = "";
            String currentDevice = getSettings().getString(Settings.KEY_BLUETOOTH_DEVICE);
            if (currentDevice != null && currentDevice.equals(mDevice.getAddress()))
            {
                current = "X ";
            }

            return current + mDevice.getName() + " (" + mDevice.getAddress() + ")";
        }
    }

}
