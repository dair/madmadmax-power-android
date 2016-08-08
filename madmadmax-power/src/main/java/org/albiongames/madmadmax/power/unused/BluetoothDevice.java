package org.albiongames.madmadmax.power.unused;

import android.bluetooth.BluetoothAdapter;

/**
 * Created by dair on 10/07/16.
 */
public class BluetoothDevice
{
    android.bluetooth.BluetoothDevice mDevice = null;

    public BluetoothDevice(String address)
    {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null)
            return;

        android.bluetooth.BluetoothDevice foundDevice = null;
        for (android.bluetooth.BluetoothDevice device: bluetoothAdapter.getBondedDevices())
        {
            if (device.getAddress().equals(address))
            {
                mDevice = device;
                break;
            }
        }

        if (mDevice == null)
            return;


    }


}
