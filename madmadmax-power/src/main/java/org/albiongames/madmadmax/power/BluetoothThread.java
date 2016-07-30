package org.albiongames.madmadmax.power;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.squareup.tape.QueueFile;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Pattern;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;

/**
 * Created by dair on 31/03/16.
 */
public class BluetoothThread extends Thread
{
    static final String COMPONENT = "bluetooth";

    public static final int STATUS_OFF = 0;
    public static final int STATUS_DISCONNECTED = 1;
    public static final int STATUS_CONNECTING = 2;
    public static final int STATUS_CONNECTED = 3;
    public static final int STATUS_FAILED = 4;
    public static final int STATUS_STOPPING = 5;
    public static final int STATUS_DISABLED = 6;
    public static final int STATUS_FINALIZING = 7;

    int mStatus = STATUS_OFF;

    public static final char LED_RED = 'R';
    public static final char LED_GREEN = 'G';
    public static final char LED_YELLOW = 'Y';

    char mCurrentLed = 0;

    long mLastStatusTime = 0;

    PowerService mService = null;
    BluetoothSPP mSPP = null;

    Looper mLooper = null;
    Handler mHandler = null;

    Pattern mCommandResponsePattern = Pattern.compile("^[RGY][01]OK$");
    boolean mStopping = false;

    private static class LedState
    {
        public boolean on = false;
        public long lastTime = 0;

        LedState(boolean b, long time)
        {
            on = b;
            lastTime = time;
        }
    }


    Map<Character, LedState> mCommandsWaiting = new  HashMap<>();
    final String mCommandsWaitingSync = "Sync";

    void resetCommandsWaiting()
    {
        synchronized (mCommandsWaitingSync)
        {
            mCommandsWaiting.clear();

            long now = 0;
            mCommandsWaiting.put(LED_GREEN, new LedState(false, now));
            mCommandsWaiting.put(LED_YELLOW, new LedState(false, now));
            mCommandsWaiting.put(LED_RED, new LedState(false, now));
        }
    }

    boolean hasCommandWaiting()
    {
        synchronized (mCommandsWaitingSync)
        {
            for (Character c : mCommandsWaiting.keySet())
            {
                if (mCommandsWaiting.get(c).lastTime > 0)
                {
                    return true;
                }
            }
        }
        return false;
    }

    BluetoothThread(PowerService service)
    {
        mService = service;
        setStatus(STATUS_OFF);
    }

    void setupSPP()
    {
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
                Tools.log("BLUETOOTH: " + "connected to " + name + ", " + address);
                mService.dump(COMPONENT, "connected to " + name + ", " + address);
                setStatus(STATUS_CONNECTED);
            }

            @Override
            public void onDeviceDisconnected() {
                Tools.log("BLUETOOTH: disconnected");
                mService.dump(COMPONENT, "disconnected");
                if (getStatus() != STATUS_STOPPING && getStatus() != STATUS_FINALIZING)
                {
                    setStatus(STATUS_DISCONNECTED);
                }
            }

            @Override
            public void onDeviceConnectionFailed()
            {
                Tools.log("BLUETOOTH: connection failes");
                mService.dump(COMPONENT, "connection failed");
                if (getStatus() != STATUS_STOPPING)
                {
                    setStatus(STATUS_FAILED);
                }
            }
        });
    }

    @Override
    public void run()
    {
        Looper.prepare();
        mLooper = Looper.myLooper();
        resetCommandsWaiting();
        mStopping = false;

        setStatus(STATUS_DISABLED);
        mSPP = new BluetoothSPP(mService);

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg)
            {
                Tools.log(msg.toString());
                // process incoming messages here
            }
        };

        Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                processLoop();
            }
        });
        thread.start();

        Looper.loop();

        mLooper = null;
        try
        {
            synchronized (thread)
            {
                thread.wait();
            }
        }
        catch (InterruptedException ex)
        {
        }

        setStatus(STATUS_OFF);
    }

    void processLoop()
    {
        boolean keepRunning = true;
        while (keepRunning)
        {
            switch (getStatus())
            {
                case STATUS_DISABLED:
                    if (mSPP.isBluetoothAvailable() && mSPP.isBluetoothEnabled())
                    {
                        setupSPP();
                        setStatus(STATUS_DISCONNECTED);
                    }
                    else
                    {
                        Tools.sleep(1000);
                    }
                    break;
                case STATUS_CONNECTING:
                    Tools.sleep(50);
                    break;
                case STATUS_CONNECTED:
                    Tools.sleep(250);
                    if (mStopping)
                    {
                        setStatus(STATUS_STOPPING);
                    }
                    else
                    {
                        updateState();
                    }

                    break;
                case STATUS_DISCONNECTED:
                case STATUS_FAILED:
                    resetCommandsWaiting();
                    mCurrentLed = 0;
                    if (mStopping)
                    {
                        setStatus(STATUS_FINALIZING);
                    }
                    else
                    {
                        connect();
                    }
                    break;
                case STATUS_STOPPING:
                    setLed(LED_GREEN, false);
                    setLed(LED_RED, false);
                    setLed(LED_YELLOW, false);
                    setStatus(STATUS_FINALIZING);
                    break;
                case STATUS_FINALIZING:
                    if (!hasCommandWaiting())
                    {
                        keepRunning = false;
                        mHandler.post(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                mSPP.disconnect();
                                mSPP.stopService();
                                mSPP = null;
                            }
                        });

                        Tools.sleep(500);

                        mHandler.post(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                mLooper.quit();
                            }
                        });

                    }
                    else
                    {
                        Tools.sleep(50);
                        updateCommandsWaiting();
                    }

                    break;
                case STATUS_OFF:
                    keepRunning = false;
                    break;

            }
        }
    }

    public void setStatus(int status)
    {
        Settings.setLong(Settings.KEY_BLUETOOTH_STATUS, status);
        mLastStatusTime = System.currentTimeMillis();
        Tools.log("setStatus: " + Integer.toString(status));
        mStatus = status;
    }

    public int getStatus()
    {
        return mStatus;
    }

    public void graciousStop()
    {
        if (mLooper == null)
            return;

        mStopping = true;
    }

    void connect()
    {
        if (getStatus() == STATUS_STOPPING)
            return;

        String address = Settings.getString(Settings.KEY_BLUETOOTH_DEVICE);
        if (address != null)
        {
            setStatus(STATUS_CONNECTING);
            mSPP.connect(address);
        }
    }

    void parseDeviceMessage(String message)
    {
        Tools.log("BLUETOOTH: << " + message);

        if (mCommandResponsePattern.matcher(message).matches())
        {
            // command response
            char ledCode = message.charAt(0);
            synchronized (mCommandsWaitingSync)
            {
                if (mCommandsWaiting.containsKey(ledCode))
                {
                    boolean on = message.charAt(1) == '1';
                    LedState ledState = mCommandsWaiting.get(ledCode);
                    if (ledState.lastTime > 0 && ledState.on == on)
                    {
                        ledState.lastTime = 0;
                    }
                }
            }
        }
        else
        {
            mService.dump(COMPONENT, "Bang! " + message);
            // shooting number is hex number
            mService.getLogicStorage().put(new StorageEntry.Damage(message));
        }
    }

    void sendCommand(String command)
    {
        synchronized (mCommandsWaitingSync)
        {
            char ledCode = command.charAt(0);
            boolean on = command.charAt(1) == '1';

            mCommandsWaiting.get(ledCode).on = on;
            mCommandsWaiting.get(ledCode).lastTime = System.currentTimeMillis();
        }

        Tools.log("BLUETOOTH: >> " + command);

        synchronized (mSPP)
        {
            mSPP.send(command + "\n", false);
        }
    }

    public String ledCode(int ledCode)
    {
        String code = "";
        switch (ledCode)
        {
            case LED_GREEN:
                code = "G";
                break;
            case LED_RED:
                code = "R";
                break;
            case LED_YELLOW:
                code = "Y";
                break;
        }

        return code;
    }

    void setLed(int ledCode, boolean on)
    {
        String code = ledCode(ledCode);
        if (code.isEmpty())
            return;

        String num = on ? "1" : "0";

        String command = code + num;

        sendCommand(command);
    }

    void updateState()
    {
        char newLed = 0;
        if (Settings.getDouble(Settings.KEY_HITPOINTS) <= 0.0)
        {
            // red
            newLed = LED_RED;
        }
        else if (Settings.getLong(Settings.KEY_SIEGE_STATE) == Settings.SIEGE_STATE_ON)
        {
            newLed = LED_YELLOW;
        }
        else
        {
            newLed = LED_GREEN;
        }

        if (newLed != mCurrentLed)
        {
            setLed(LED_RED, newLed == LED_RED);
            setLed(LED_YELLOW, newLed == LED_YELLOW);
            setLed(LED_GREEN, newLed == LED_GREEN);
            mCurrentLed = newLed;
        }
        else
        {
            updateCommandsWaiting();
        }
    }

    void updateCommandsWaiting()
    {
        Map<Character, Boolean> updateList = new HashMap<>();

        long now = System.currentTimeMillis();
        synchronized (mCommandsWaitingSync)
        {
            for (Character c: mCommandsWaiting.keySet())
            {
                if (mCommandsWaiting.get(c).lastTime > 0 && now - mCommandsWaiting.get(c).lastTime > 1000)
                {
                    updateList.put(c, mCommandsWaiting.get(c).on);
                }
            }
        }

        for (Character c: updateList.keySet())
        {
            setLed(c, updateList.get(c));
        }
    }
}
