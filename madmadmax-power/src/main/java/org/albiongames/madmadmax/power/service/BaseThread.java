package org.albiongames.madmadmax.power.service;

import org.albiongames.madmadmax.power.data_storage.Settings;

/**
 * Created by Dmitry.Subbotenko on 08.08.2016.
 */
public class BaseThread extends Thread {
  protected Settings settings;

  public BaseThread(Settings settings) {
    this.settings = settings;
  }

  public Settings getSettings() {
      assert settings != null;
      return settings;
  }
}
