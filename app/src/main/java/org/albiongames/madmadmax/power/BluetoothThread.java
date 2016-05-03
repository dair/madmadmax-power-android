package org.albiongames.madmadmax.power;

/**
 * Created by dair on 31/03/16.
 */
public class BluetoothThread extends GenericThread
{
    @Override
    protected void periodicTask()
    {
        String deviceId = Settings.getString(Settings.KEY_BLUETOOTH_DEVICE);
        if (deviceId == null)
            return;


    }
}
