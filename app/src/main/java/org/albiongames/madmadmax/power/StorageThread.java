package org.albiongames.madmadmax.power;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by dair on 26/05/16.
 */
public class StorageThread extends GenericThread
{
    public static abstract class StorageDbEntry implements BaseColumns
    {
        public static final String TABLE_NAME = "geodata";
        public static final String COLUMN_NAME_TIME = "dt";
        public static final String COLUMN_NAME_JSON = "json";
        private static final String TEXT_TYPE = " TEXT";

        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + StorageDbEntry.TABLE_NAME + " (" +
                        StorageDbEntry.COLUMN_NAME_TIME + " INTEGER PRIMARY KEY, " +
                        StorageDbEntry.COLUMN_NAME_JSON + TEXT_TYPE +
                        " )";
        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + StorageDbEntry.TABLE_NAME;
    }

    public class StorageDbHelper extends SQLiteOpenHelper
    {
        public static final int DATABASE_VERSION = 1;
        public static final String DATABASE_NAME = "geodata.db";

        public StorageDbHelper(Context context)
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL(StorageDbEntry.SQL_CREATE_ENTRIES);
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {
            // This database is only a cache for online data, so its upgrade policy is
            // to simply to discard the data and start over
            db.execSQL(StorageDbEntry.SQL_DELETE_ENTRIES);
            onCreate(db);
        }

        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {
            onUpgrade(db, oldVersion, newVersion);
        }
    }

    Context mContext = null;
    StorageDbHelper mDbHelper = null;

    ConcurrentLinkedQueue<StorageEntry.Base> mTemporaryStorage = new ConcurrentLinkedQueue<>();

    public StorageThread(Context ctx)
    {
        mContext = ctx;
        mDbHelper = new StorageDbHelper(mContext);
    }

    public void addEntry(StorageEntry.Base entry)
    {
        // TODO
        mTemporaryStorage.add(entry);
        setTimeout(0);
    }


    @Override
    protected void periodicTask()
    {
        while (!mTemporaryStorage.isEmpty())
        {
            StorageEntry.Base entry = mTemporaryStorage.poll();
            if (entry != null)
            {
                insertEntryToDb(entry);
            }
        }

        setTimeout(200);
    }

    @Override
    protected void onStart()
    {
        // do nothing, reimplement in children
    }

    @Override
    protected void onStop()
    {
        // do nothing, reimplement in children
    }


    private void insertEntryToDb(StorageEntry.Base entry)
    {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(StorageDbEntry.COLUMN_NAME_TIME, entry.getTime());
        values.put(StorageDbEntry.COLUMN_NAME_JSON, entry.toString());

        db.insert(StorageDbEntry.TABLE_NAME, null, values);
    }
}
