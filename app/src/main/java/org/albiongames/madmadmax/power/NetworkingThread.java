package org.albiongames.madmadmax.power;

/**
 * Created by dair on 31/03/16.
 */
public class NetworkingThread extends GenericThread
{
    public static final String BASE_URL="http://192.168.100.100:3000";

    public static String authUrl()
    {
        return BASE_URL + "/device/auth";
    }

    @Override
    protected void periodicTask()
    {

    }
}

