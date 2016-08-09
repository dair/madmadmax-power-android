package org.albiongames.madmadmax.power.data_storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import static nl.qbusict.cupboard.CupboardFactory.cupboard;

/**
 * Created by Dmitry.Subbotenko on 09.08.2016.
 */
public class StorageDatabaseHelper extends SQLiteOpenHelper {

  private static final String DATABASE_NAME = "maxStorage.db";
  private static final int DATABASE_VERSION = 1;

  public StorageDatabaseHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  static {
    cupboard().register(Settings.Entity.class);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    cupboard().withDatabase(db).createTables();
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    cupboard().withDatabase(db).upgradeTables();
  }

}