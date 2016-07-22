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
import java.util.Map;
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

    int mStatus = STATUS_OFF;

    public static final char LED_RED = 'R';
    public static final char LED_GREEN = 'G';
    public static final char LED_YELLOW = 'Y';

    PowerService mService = null;
    BluetoothSPP mSPP = null;

    String mAddress = null;

    Looper mLooper = null;
    Handler mHandler = null;

    Map<String, Long> mCommandsWaiting = new HashMap<>();

    Map<Integer, Boolean> mLedStatus = new HashMap<>();

    Pattern mCommandResponsePattern = Pattern.compile("^[RGY][01]OK$");

    Map<Character, QueueFile> mQueueFiles = new HashMap<>();
    Map<Character, Long> mTimeWaiting = new HashMap<>();
    boolean mWaitingResponse = false;

    BluetoothThread(PowerService service)
    {
        mService = service;
        mQueueFiles.clear();

        setStatus(STATUS_OFF);
        try
        {
            File file = new File(mService.getFilesDir() + "/bluetooth_red");
            mQueueFiles.put(LED_RED, new QueueFile(file));
            file = new File(mService.getFilesDir() + "/bluetooth_green");
            mQueueFiles.put(LED_GREEN, new QueueFile(file));
            file = new File(mService.getFilesDir() + "/bluetooth_yellow");
            mQueueFiles.put(LED_YELLOW, new QueueFile(file));
        }
        catch (IOException ex)
        {
            return;
        }
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
                mService.dump(COMPONENT, "connected to " + name + ", " + address);
                setStatus(STATUS_CONNECTED);
            }

            @Override
            public void onDeviceDisconnected() {
                mService.dump(COMPONENT, "disconnected");
                if (getStatus() != STATUS_STOPPING)
                {
                    setStatus(STATUS_DISCONNECTED);
                }
//                mTimeWaiting = 0;
                mWaitingResponse = false;
            }

            @Override
            public void onDeviceConnectionFailed()
            {
                mService.dump(COMPONENT, "connection failed");
                if (getStatus() != STATUS_STOPPING)
                {
                    setStatus(STATUS_FAILED);
                }
//                mTimeWaiting = 0;
                mWaitingResponse = false;
            }
        });
    }

    @Override
    public void run()
    {
        Looper.prepare();
        mLooper = Looper.myLooper();
//        mTimeWaiting = 0;

        mWaitingResponse = false;

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

        try
        {
            Iterator it = mQueueFiles.entrySet().iterator();
            while (it.hasNext())
            {
                Map.Entry pair = (Map.Entry)it.next();
                ((QueueFile)pair.getValue()).close();
            }
        }
        catch (IOException ex)
        {

        }
        mQueueFiles.clear();

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
                    processQueues();
                    break;
                case STATUS_DISCONNECTED:
                case STATUS_FAILED:
                    connect();
                    break;
                case STATUS_OFF:
                case STATUS_STOPPING:
                    keepRunning = false;
                    break;

            }
        }
    }

    void processQueues()
    {
        Iterator it = mQueueFiles.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry pair = (Map.Entry)it.next();
            processQueue((Character)pair.getKey());
        }
    }

    boolean processQueue(char queueCode)
    {
        QueueFile queue = mQueueFiles.get(queueCode);

        if (queue == null || queue.isEmpty())
        {
            return false;
        }

        long time = System.currentTimeMillis();
        if (time < mTimeWaiting.get(queueCode))
        {
            return false;
        }

        try
        {
            byte[] data = queue.peek();
            if (data == null)
            {
                return false;
            }

            String command = new String(data, "UTF-8");
            addCommand(command);
            queue.remove();
        }
        catch (IOException ex)
        {

        }

        return true;
    }

    public void setStatus(int status)
    {
        Settings.setLong(Settings.KEY_BLUETOOTH_STATUS, status);
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

        setStatus(STATUS_STOPPING);

        mHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                mSPP.stopService();
                mSPP = null;
            }
        });

        mHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                mLooper.quit();
            }
        });
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
        if (getStatus() == STATUS_STOPPING)
            return false;

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
        if (getStatus() == STATUS_STOPPING)
            return;

        checkAddressChange();

        if (mAddress != null)
        {
            setStatus(STATUS_CONNECTING);
            mSPP.connect(mAddress);
        }
    }

    void parseDeviceMessage(String message)
    {
        if (mCommandResponsePattern.matcher(message).matches())
        {
            // command response
            String command = message.substring(0, 2);
            if (mCommandsWaiting.containsKey(command))
            {
                long count = mCommandsWaiting.get(command);
                if (count > 0)
                {
                    --count;
                }
                else
                {
                    count = 0;
                }
                mCommandsWaiting.put(command, count);
            }
        }
        else
        {
            mService.dump(COMPONENT, "Bang! " + message);
            // shooting number is hex number
            mService.getLogicStorage().put(new StorageEntry.Damage(message));
        }
    }

    synchronized void addCommand(String command)
    {
        char code = command.charAt(0);

        if (command.charAt(1) == 'W')
        {
            // wait
            long duration = Long.parseLong(command.substring(2));
            mTimeWaiting.put(code, System.currentTimeMillis() + duration);
            return;
        }

        sendCommand(command);
    }

    void sendCommand(String command)
    {
        long count = 0;
        if (mCommandsWaiting.containsKey(command))
        {
            count = mCommandsWaiting.get(command);
        }
        ++count;
        mCommandsWaiting.put(command, count);

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

    void enqueueCommand(String command)
    {
        QueueFile file = mQueueFiles.get(command.charAt(0));
        if (file == null)
            return;

        try
        {
            byte[] data = command.getBytes("UTF-8");
            file.add(data);
        }
        catch (UnsupportedEncodingException ex)
        {}
        catch (IOException ex)
        {}
    }

    public void setLed(int ledCode, boolean on)
    {
        String code = ledCode(ledCode);
        if (code.isEmpty())
            return;

        String num = on ? "1" : "0";

        String command = code + num;

        enqueueCommand(command);
    }

    public void setPause(int ledCode, long duration)
    {
        String code = ledCode(ledCode);
        if (code.isEmpty())
            return;

        String command = code + "W" + Long.toString(duration);
        enqueueCommand(command);
    }
}
