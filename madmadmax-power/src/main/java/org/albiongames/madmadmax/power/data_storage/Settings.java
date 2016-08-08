package org.albiongames.madmadmax.power.data_storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import org.albiongames.madmadmax.power.Tools;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Created by dair on 03/05/16.
 */
public class Settings extends SettingsKeys {
  private static final String TAG = Settings.class.getSimpleName();

  private SharedPreferences sharedPreferences;


  public Settings(@NonNull Context context) {
    super();

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
  }


  public String getString(final String key) {
    return pGetString(key);
  }

  public void setString(final String key, final String value) {
    pSetString(key, value);
  }

  public long getLong(final String key) {
    long ret = 0;
    try {
      ret = Long.parseLong(getString(key));
    } catch (NumberFormatException ex) {

    }

    return ret;
  }

  public void setLong(final String key, long value) {
    setString(key, Long.toString(value));
  }

  public double getDouble(final String key) {
    double ret = 0.0;
    try {
      String string = getString(key);
      ret = Double.parseDouble(string);
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return ret;
  }

  public void setDouble(final String key, double value) {
    setString(key, Double.toString(value));
  }

  public Expression getExpression(final String key) {
    return pGetExpression(key);
  }

  /////////////////////////


  private String pGetString(final String key) {
    String ret = getSharedPreferences().getString(key, null);
    if (ret == null) {
      if (mDefaults.containsKey(key))
        ret = mDefaults.get(key);
    }
    return ret;
  }

  private void pSetString(final String key, final String value) {
    Log.d(TAG, "pSetString() called with " + "key = [" + key + "], value = [" + value + "]");
    SharedPreferences.Editor editor = getSharedPreferences().edit();
    editor.putString(key, value);
    editor.apply();

    if (mExpressions.containsKey(key))
      mExpressions.remove(key);
  }

  private Expression pGetExpression(final String key) {
    if (!mFormulaValues.contains(key))
      return null;

    if (mExpressions.containsKey(key) && mExpressions.get(key) != null) {
      return mExpressions.get(key);
    } else {
      try {
        Expression expression = new ExpressionBuilder(pGetString(key)).
            variable("x").build();
        mExpressions.put(key, expression);
        return expression;
      } catch (RuntimeException ex) {
        // fail
        return null;
      }
    }
  }

  public void networkUpdate(JSONObject object) {
    if (object == null)
      return;

    Iterator<String> keys = object.keys();
    while (keys.hasNext()) {
      String key = keys.next();
      try {
        if (object.get(key) instanceof String) {
          setString(Settings.PARAMS_PREFIX + key, (String) object.get(key));
        } else if (object.get(key) instanceof Long) {
          setLong(Settings.PARAMS_PREFIX + key, object.getLong(key));
        } else if (object.get(key) instanceof Integer) {
          setLong(Settings.PARAMS_PREFIX + key, object.getInt(key));
        } else {
          String className = object.get(key).getClass().getName();
          Tools.log(className);
        }
      } catch (JSONException ex) {
      }
    }
  }


  public SharedPreferences getSharedPreferences() {
    assert sharedPreferences != null;
    return sharedPreferences;
  }
}
