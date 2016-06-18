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

    public static final int LED_RED = 1 << 0;
    public static final int LED_GREEN = 1 << 1;
    public static final int LED_YELLOW = 1 << 2;

    public void turnLedOn(int ledCode)
    {
        // TODO
    }

    public void turnLedOff(int ledCode)
    {

    }
}
