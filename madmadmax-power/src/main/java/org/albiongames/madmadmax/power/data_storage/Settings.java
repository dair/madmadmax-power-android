package org.albiongames.madmadmax.power.data_storage;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import nl.qbusict.cupboard.annotation.Ignore;
import org.albiongames.madmadmax.power.Tools;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Created by dair on 03/05/16.
 */
public class Settings extends SettingsKeys {
  private static final String TAG = Settings.class.getSimpleName();
  private Context context;
  private final StorageDatabaseHelper dbHelper;

  public static class Entity {
    public Long _id; // for cupboard
    //If _id field set, then any existing Entity will be replaced. If _id is null then a new Entity will be created in the table.
    public String key;
    public String value;
    @Ignore
    public boolean isChanged = true;

    public static Entity build(String key, String value){
      Entity res = new Entity();
      res.key = key;
      res.value = value;
      return res;
    }

  }

  static final Map<String, Entity> shortDataContainer = new HashMap<String, Entity>();

  private Handler saveHandler = new Handler(new Callback() {
    @Override
    public boolean handleMessage(Message msg) {
//      Log.d(TAG, "saveDb() called ");

      SQLiteDatabase db = dbHelper.getWritableDatabase();

      ArrayList<Entity> changeList= new ArrayList<>();

      for (Entity entity: shortDataContainer.values()) {
        if (entity.isChanged){
//          Log.d(TAG, "saveData key = "+ entity.key + " value = " + entity.value);
          changeList.add(entity);
          entity.isChanged = false;
        }
      }

      cupboard().withDatabase(db).put(changeList);

      db.close();

      return true;
    }
  });

  public Settings(@NonNull Context context) {

    super();
    this.context = context;
    dbHelper = new StorageDatabaseHelper(context);
  }

  /**
   * Use only in Application class
   */
  @Deprecated
  public void loadData() {
//    Log.d(TAG, "loadData() called with " + "");
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    List<Entity> list = cupboard().withDatabase(db).query(Entity.class).list();

    for (Entity entity : list) {
      entity.isChanged = false;
//      Log.d(TAG, "loadData key = "+ entity.key + " value = " + entity.value);
      shortDataContainer.put(entity.key,entity);
    }
    db.close();

  }

  public String getString(final String key) {
    return pGetString(key);
  }

  public void setString(final String key, final String value) {
//    Log.d(TAG, "setString() called with " + "key = [" + key + "], value = [" + value + "]");
    pSetString(key, value);
  }

  public long getLong(final String key) {
    long ret = 0;
    String string = getString(key);
    if (!TextUtils.isEmpty(string))
      ret = Long.parseLong(string);

    return ret;
  }

  public void setLong(final String key, long value) {
    setString(key, Long.toString(value));
  }

  public double getDouble(final String key) {
    double ret = 0.0;
    String string = getString(key);
    if (!TextUtils.isEmpty(string))
      ret = Double.parseDouble(string);

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

    Entity ret = shortDataContainer.get(key);
    if (ret == null && mDefaults.containsKey(key)){
      ret = Entity.build(key,mDefaults.get(key));
      shortDataContainer.put(key,ret);
    }

    return ret!=null? ret.value : null;
  }

  private void pSetString(final String key, final String value) {

    Entity ret = shortDataContainer.get(key);
    if (ret == null){
      ret = Entity.build(key,null);
    }

    ret.value = value;

    ret.isChanged = true;

    shortDataContainer.put(key,ret);

    if (mExpressions.containsKey(key))
      mExpressions.remove(key);

    if (!saveHandler.hasMessages(1)){
     saveHandler.sendEmptyMessageDelayed(1,1000);
    }
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



}
