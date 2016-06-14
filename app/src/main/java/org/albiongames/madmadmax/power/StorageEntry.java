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

        public Base(JSONObject object) throws JSONException
        {
            mType = object.getString("type");
            mTime = object.getLong("time");
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

        public String toString()
        {
            return toJsonObject().toString();
        }
    }

    public static class Location extends Base
    {
        double mLat;
        double mLon;
        float mSpeed;
        int mSatellites;

        public Location(long time, double lat, double lon, float speed, int satellites)
        {
            super(time, "location");
            mLat = lat;
            mLon = lon;
            mSpeed = speed;
            mSatellites = satellites;
        }

        public Location(JSONObject object) throws JSONException
        {
            super(object);
            mLat = object.getDouble("lat");
            mLon = object.getDouble("lon");
            mSpeed = (float)object.getDouble("speed");
            mSatellites = object.getInt("sat");
        }

        @Override
        public JSONObject toJsonObject()
        {
            JSONObject jsonObject = super.toJsonObject();
            if (jsonObject == null)
                return null;
            try
            {
                jsonObject.put("lat", mLat);
                jsonObject.put("lon", mLon);
                jsonObject.put("speed", mSpeed);
                jsonObject.put("sat", mSatellites);

                return jsonObject;
            }
            catch (JSONException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
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

        public Marker(JSONObject object) throws JSONException
        {
            super(object);
            mTag = object.getString("tag");
        }

        @Override
        public JSONObject toJsonObject()
        {
            JSONObject jsonObject = super.toJsonObject();
            if (jsonObject == null)
                return null;

            try
            {
                jsonObject.put("tag", mTag);
                return jsonObject;
            }
            catch (JSONException e)
            {
                e.printStackTrace();
                return null;
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

        public Info(JSONObject object) throws JSONException
        {
            super(object);
            mInfo = object.getString("info");
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

    public static Base createFromJson(JSONObject object)
    {
        if (object == null)
            return null;

        Base ret = null;

        try
        {
            String type = object.getString("type");

            if (type.equalsIgnoreCase("info"))
            {
                ret = new Info(object);
            }
            else if (type.equalsIgnoreCase("marker"))
            {
                ret = new Marker(object);
            }
            else if (type.equalsIgnoreCase("location"))
            {
                ret = new Location(object);
            }
        }
        catch (JSONException ex)
        {
        }

        return ret;
    }
}
