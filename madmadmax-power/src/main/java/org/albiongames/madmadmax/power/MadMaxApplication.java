package org.albiongames.madmadmax.power;

import android.app.Application;

import org.albiongames.madmadmax.power.data_storage.Settings;

/**
 * Created by Dmitry.Subbotenko on 09.08.2016.
 */
public class MadMaxApplication extends Application {
  @Override
  public void onCreate() {
    super.onCreate();

    new Settings(this).loadData();

  }
}
