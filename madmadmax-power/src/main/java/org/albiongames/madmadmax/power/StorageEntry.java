package org.albiongames.madmadmax.power;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by dair on 26/05/16.
 */

public class StorageEntry
{
    public static final String TYPE_MARKER = "marker";
    public static final String TYPE_LOCATION = "location";
    public static final String TYPE_DAMAGE = "damage";
    public static final String TYPE_DUMP = "dump";


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

        public boolean isTypeOf(final String type)
        {
            if (mType == null && type == null)
                return true;
            if (mType == null || type == null)
                return false;

            return mType.equals(type);
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
        float mAccuracy;
        float mSpeed;
        double mDistance;
        int mSatellites;

        public Location(long time, double lat, double lon, float acc, float speed, double distance, int satellites)
        {
            super(time, TYPE_LOCATION);
            mLat = lat;
            mLon = lon;
            mAccuracy = acc;
            mSpeed = speed;
            mDistance = distance;
            mSatellites = satellites;
        }

        public Location(JSONObject object) throws JSONException
        {
            super(object);
            mLat = object.getDouble("lat");
            mLon = object.getDouble("lon");
            mAccuracy = (float)object.getDouble("acc");
            mSpeed = (float)object.getDouble("speed");
            mDistance = object.getDouble("distance");
            mSatellites = object.getInt("sat");
        }

        public double getDistance()
        {
            return mDistance;
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
                jsonObject.put("acc", mAccuracy);
                jsonObject.put("speed", mSpeed);
                jsonObject.put("distance", mDistance);
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
            super(TYPE_MARKER);
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
        public JSONObject toJsonObject()
        {
            JSONObject jsonObject = super.toJsonObject();
            try
            {
                jsonObject.put("info", mInfo);
                return jsonObject;
            }
            catch (JSONException e)
            {
                e.printStackTrace();
                return null;
            }
        }
    }

    public static class Damage extends Base
    {
        String mRaw = null;

        public Damage(String raw)
        {
            super(TYPE_DAMAGE);
            mRaw = raw;
        }

        public int getDamage()
        {
            if (mRaw == null || mRaw.isEmpty())
                return 0;

            int code = Integer.valueOf(mRaw.substring(mRaw.length()-1), 16); // last symbol

            return Settings.getDamageForCode(code);
        }

        public Damage(JSONObject object) throws  JSONException
        {
            super(object);
            mRaw = object.getString("raw");
        }

        @Override
        public JSONObject toJsonObject()
        {
            JSONObject jsonObject = super.toJsonObject();
            try
            {
                jsonObject.put("damage", getDamage());
                if (mRaw != null)
                    jsonObject.put("raw", mRaw);
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
            return jsonObject;
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

            if (type.equalsIgnoreCase("dump"))
            {
                ret = new Dump(object);
            }
            else if (type.equalsIgnoreCase("info"))
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
            else if (type.equalsIgnoreCase("damage"))
            {
                ret = new Damage(object);
            }
        }
        catch (JSONException ex)
        {
        }

        return ret;
    }

    public static class Dump extends Base
    {
        String mText = null;

        Dump(final String text)
        {
            super(TYPE_DUMP);
            mText = text;
        }

        Dump(JSONObject object) throws JSONException
        {
            super(object);
            mText = object.getString("text");
        }

        @Override
        public JSONObject toJsonObject()
        {
            JSONObject jsonObject = super.toJsonObject();
            try
            {
                if (mText != null)
                    jsonObject.put("text", mText);
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
            return jsonObject;
        }
    }
}