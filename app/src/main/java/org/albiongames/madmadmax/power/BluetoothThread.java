package org.albiongames.madmadmax.power;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;

/**
 * Created by dair on 31/03/16.
 */
public class BluetoothThread extends Thread
{
    public static final int STATUS_OFF = 0;
    public static final int STATUS_DISCONNECTED = 1;
    public static final int STATUS_CONNECTED = 2;
    public static final int STATUS_FAILED = 3;
    public static final int STATUS_STOPPING = 4;

    int mStatus = STATUS_OFF;

    public static final int LED_RED = 1 << 0;
    public static final int LED_GREEN = 1 << 1;
    public static final int LED_YELLOW = 1 << 2;

    Context mContext = null;
    BluetoothSPP mSPP = null;

    String mAddress = null;

    Looper mLooper = null;
    Handler mHandler = null;

    class Processor implements Runnable
    {
        public void run()
        {
            switch (getStatus())
            {
                case STATUS_DISCONNECTED:
                case STATUS_FAILED:
                    connect();
                    break;
                case STATUS_CONNECTED:
                    break;
                case STATUS_STOPPING:
                    break;
            }
        }
    }

    Processor mProcessor = new Processor();

    BluetoothThread(Context context)
    {
        mContext = context;
    }

    @Override
    public void run()
    {
        Looper.prepare();
        mLooper = Looper.myLooper();

        mSPP = new BluetoothSPP(mContext);
        mSPP.setupService();
        mSPP.startService(BluetoothState.DEVICE_OTHER);

        mSPP.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener()
        {
            @Override
            public void onDataReceived(byte[] data, String message)
            {
                parseDeviceMessage(message);
            }
        });

        mSPP.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener()
        {
            @Override
            public void onDeviceConnected(String name, String address)
            {
                setStatus(STATUS_CONNECTED);
                mHandler.post(mProcessor);
            }

            @Override
            public void onDeviceDisconnected()
            {
                setStatus(STATUS_DISCONNECTED);
                mHandler.post(mProcessor);
            }

            @Override
            public void onDeviceConnectionFailed()
            {
                setStatus(STATUS_FAILED);
                mHandler.post(mProcessor);
            }
        });

        setStatus(STATUS_DISCONNECTED);

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg)
            {
                Tools.log(msg.toString());
                // process incoming messages here
            }
        };

        mHandler.post(mProcessor);

        Looper.loop();

        mSPP.stopService();
        mSPP = null;
        mLooper = null;
        setStatus(STATUS_OFF);
    }

    public void setStatus(int status)
    {
        Settings.setLong(Settings.KEY_BLUETOOTH_STATUS, status);
        mStatus = status;
    }

    public int getStatus()
    {
        return mStatus;
    }

    public void graciousStop()
    {
        if (mLooper != null)
            mLooper.quit();

        setStatus(STATUS_STOPPING);
    }

    boolean checkAddressChange()
    {
        String storedAddress = Settings.getString(Settings.KEY_BLUETOOTH_DEVICE);
        if (mAddress == null ||
                !mAddress.equals(storedAddress))
        {
            mAddress = storedAddress;
            return true;
        }
        return false;
    }

    boolean applyAddressChange()
    {
        if (checkAddressChange())
        {
            mSPP.disconnect();
            setStatus(STATUS_DISCONNECTED);
            return true;
        }
        return false;
    }

    void connect()
    {
        checkAddressChange();

        if (mAddress != null)
        {
            mSPP.connect(mAddress);
        }
    }

    void parseDeviceMessage(String message)
    {

    }

    public void turnLedOn(int ledCode)
    {
        // TODO
    }

    public void turnLedOff(int ledCode)
    {

    }
}
