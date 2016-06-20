package org.albiongames.madmadmax.power;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.security.InvalidParameterException;

/**
 * Created by dair on 12/06/16.
 */
public class LogicThread extends GenericThread
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
    protected void periodicTask()
    {
        if (mService == null || mService.getStatus() != PowerService.STATUS_ON)
            return;

        probabilities();

        while (true)
        {
            StorageEntry.Base entry = mService.getLogicStorage().get();
            if (entry == null)
                break;

            mService.getNetworkStorage().put(entry);
            mService.getLogicStorage().remove();

            Tools.log("LogicThread: Logic: " + Integer.toString(mService.getLogicStorage().size()) + ", Network: " +
                Integer.toString(mService.getNetworkStorage().size()));
        }
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
                Expression expression = new ExpressionBuilder(goodEx1).
                        variable("x").build();
                ret = expression;
                Settings.setString(keyNew, newEx1);
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

        long hp = Settings.getLong(Settings.KEY_HITPOINTS);
        double p1 = mExpressionP1.setVariable("x", hp).evaluate();
        double p2 = mExpressionP2.setVariable("x", hp).evaluate();

        Tools.log("P1: " + Double.toString(p1));
        Tools.log("P2: " + Double.toString(p2));
    }
}
