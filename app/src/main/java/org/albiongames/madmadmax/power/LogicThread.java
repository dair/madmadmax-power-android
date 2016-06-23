package org.albiongames.madmadmax.power;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidParameterException;

/**
 * Created by dair on 12/06/16.
 */
public class LogicThread extends StatusThread
{
    PowerService mService = null;

    String mExpressionP1String = null;
    Expression mExpressionP1 = null;
    String mExpressionP2String = null;
    Expression mExpressionP2 = null;

    LogicThread(PowerService service)
    {
        mService = service;
    }


    @Override
    public void run()
    {
        Tools.log("LogicThread: start");

        super.run();

        setStatus(STATUS_ON);

        while (true)
        {
            StorageEntry.Base entry = mService.getLogicStorage().get();

            if (entry != null)
            {
                mService.getNetworkStorage().put(entry);
                mService.getLogicStorage().remove();

//                Tools.log("LogicThread: Logic: " + Integer.toString(mService.getLogicStorage().size()) + ", Network: " +
//                        Integer.toString(mService.getNetworkStorage().size()));

                if (getStatus() == STATUS_STOPPING && checkMarkerStop(entry))
                {
                    setStatus(STATUS_OFF);
                    break;
                }
            }
            else
            {
                Tools.sleep(1000);
            }
        }

        setStatus(STATUS_OFF);

        Tools.log("LogicThread: stop");
    }

    protected Expression generateExpressions(final String keyGood, final String keyNew, Expression oldExpression)
    {
        String goodEx1 = Settings.getString(keyGood);
        String newEx1 = Settings.getString(keyNew);

        Expression ret = oldExpression;

        if (goodEx1 == null || !goodEx1.equals(newEx1) || oldExpression == null)
        {
            try
            {
                Expression expression = new ExpressionBuilder(newEx1).
                        variable("x").build();
                ret = expression;
                Settings.setString(keyGood, newEx1);
            }
            catch (RuntimeException ex)
            {
                // couldn't parse, leave as it was
            }
        }
        return ret;
    }

    protected void probabilities()
    {
        mExpressionP1 = generateExpressions(Settings.KEY_LOGIC_LAST_GOOD_P1_FORMULA, Settings.KEY_P1_FORMULA, mExpressionP1);
        mExpressionP2 = generateExpressions(Settings.KEY_LOGIC_LAST_GOOD_P2_FORMULA, Settings.KEY_P2_FORMULA, mExpressionP2);

        double hp = (double)Settings.getLong(Settings.KEY_HITPOINTS) / (double)Settings.getLong(Settings.KEY_MAXHITPOINTS);

        double p1 = Tools.clamp(mExpressionP1.setVariable("x", hp).evaluate(), 0.0, 1.0);
        double p2 = Tools.clamp(mExpressionP2.setVariable("x", hp).evaluate(), 0.0, 1.0);

//        Tools.log("P1: " + Double.toString(p1));
//        Tools.log("P2: " + Double.toString(p2));
    }

    protected void decreaseGazoline()
    {

    }

    public void graciousStop()
    {
        if (getStatus() == STATUS_ON)
            setStatus(STATUS_STOPPING);
    }

    boolean checkMarkerStop(StorageEntry.Base entry)
    {
        if (entry == null)
            return false;

        JSONObject object = entry.toJsonObject();

        try
        {
            if (object.has("type") && object.getString("type").equals("marker") &&
                    object.has("tag") && object.getString("tag").equals("stop"))
                return true;
        }
        catch (JSONException ex)
        {
        }

        return false;
    }
}
