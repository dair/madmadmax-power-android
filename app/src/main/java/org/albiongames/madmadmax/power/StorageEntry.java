package org.albiongames.madmadmax.power;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by dair on 26/05/16.
 */

public class StorageEntry
{
    public static abstract class Base
    {
        private long mTime = 0;
        private String mType = "base";

        public Base(final String type)
        {
            mTime = System.currentTimeMillis();
            mType = type;
        }

        public Base(long t, final String type)
        {
            mTime = t;
            mType = type;
        }

        public long getTime()
        {
            return mTime;
        }

        protected JSONObject toJsonObject()
        {
            JSONObject object = new JSONObject();
            try
            {
                object.put("type", mType);
                object.put("time", mTime);
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
            return object;
        }

        public abstract String toString();
    }

    public static class Location extends Base
    {
        double mLat;
        double mLon;
        float mSpeed;
        public Location(long time, double lat, double lon, float speed)
        {
            super(time, "location");
            mLat = lat;
            mLon = lon;
            mSpeed = speed;
        }

        @Override
        public String toString()
        {
            JSONObject jsonObject = toJsonObject();
            try
            {
                jsonObject.put("lat", mLat);
                jsonObject.put("lon", mLon);
                jsonObject.put("speed", mSpeed);

                return jsonObject.toString();
            }
            catch (JSONException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return "";
            }
        }
    }

    public static class Marker extends Base
    {
        private String mTag = null;

        public Marker(final String tag)
        {
            super("marker");
            mTag = tag;
        }

        @Override
        public String toString()
        {
            JSONObject jsonObject = toJsonObject();
            try
            {
                jsonObject.put("tag", mTag);
                return jsonObject.toString();
            }
            catch (JSONException e)
            {
                e.printStackTrace();
                return "";
            }
        }
    }

    public static class MarkerStart extends Marker
    {
        public MarkerStart()
        {
            super("start");
        }
    }

    public static class MarkerStop extends Marker
    {
        public MarkerStop()
        {
            super("stop");
        }
    }

    public static class Info extends Base
    {
        String mInfo = null;
        public Info(String info)
        {
            super("info");
            mInfo = info;
        }

        @Override
        public String toString()
        {
            JSONObject jsonObject = toJsonObject();
            try
            {
                jsonObject.put("info", mInfo);
                return jsonObject.toString();
            }
            catch (JSONException e)
            {
                e.printStackTrace();
                return "";
            }
        }
    }
}
