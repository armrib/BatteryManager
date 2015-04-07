package com.aware.plugin.batteryManager;

import java.util.HashMap;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Environment;
import android.provider.BaseColumns;
import android.util.Log;
import com.aware.Aware;
import com.aware.utils.DatabaseHelper;
/**
 * Created by Armand on 12/03/2015.
 */
public class Provider extends ContentProvider {

    public static final int DATABASE_VERSION = 1;

    public static String AUTHORITY = "com.aware.plugin.batteryManager.provider.batteryManager";

    private static final int BATTERYMANAGER = 1;
    private static final int BATTERYMANAGER_ID = 2;

    public static final String DATABASE_NAME = Environment.getExternalStorageDirectory() + "/AWARE/plugin_batteryManager.db";

    public static final String[] DATABASE_TABLES = {
            "plugin_batteryManager"
    };

    public static final String[] TABLES_FIELDS = {
            BatteryManager_Data._ID + " integer primary key autoincrement," +
                    BatteryManager_Data.TIMESTAMP + " real default 0," +
                    BatteryManager_Data.DEVICE_ID + " text default ''," +
                    BatteryManager_Data.RATE + " real default 0," +
                    BatteryManager_Data.CHARGE + " real default 0," +
                    BatteryManager_Data.TIME + " real default 0," +
                    BatteryManager_Data.FORECAST + " real default 0," +
                    "UNIQUE("+BatteryManager_Data.TIMESTAMP+","+BatteryManager_Data.DEVICE_ID+")"
    };

    public static final class BatteryManager_Data implements BaseColumns {
        private BatteryManager_Data(){};

        public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/plugin_batteryManager");
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.aware.plugin.batteryManager";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.aware.plugin.batteryManager";

        public static final String _ID = "_id";
        public static final String TIMESTAMP = "timestamp";
        public static final String DEVICE_ID = "device_id";
        public static final String RATE = "double_rate";
        public static final String CHARGE= "double_charge";
        public static final String TIME = "double_time";
        public static final String FORECAST= "double_forecast";
    }

    private static UriMatcher URIMatcher;
    private static HashMap<String, String> databaseMap;
    private static DatabaseHelper databaseHelper;
    private static SQLiteDatabase database;

    @Override
    public boolean onCreate() {

        AUTHORITY = getContext().getPackageName() + ".provider.batteryManager";

        URIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        URIMatcher.addURI(AUTHORITY, DATABASE_TABLES[0], BATTERYMANAGER);
        URIMatcher.addURI(AUTHORITY, DATABASE_TABLES[0]+"/#", BATTERYMANAGER_ID);

        databaseMap = new HashMap<String, String>();
        databaseMap.put(BatteryManager_Data._ID, BatteryManager_Data._ID);
        databaseMap.put(BatteryManager_Data.TIMESTAMP, BatteryManager_Data.TIMESTAMP);
        databaseMap.put(BatteryManager_Data.DEVICE_ID, BatteryManager_Data.DEVICE_ID);
        databaseMap.put(BatteryManager_Data.RATE, BatteryManager_Data.RATE);
        databaseMap.put(BatteryManager_Data.CHARGE, BatteryManager_Data.CHARGE);
        databaseMap.put(BatteryManager_Data.TIME, BatteryManager_Data.TIME);
        databaseMap.put(BatteryManager_Data.FORECAST, BatteryManager_Data.FORECAST);

        return true;
    }

    private boolean initializeDB() {
        if (databaseHelper == null) {
            databaseHelper = new DatabaseHelper( getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS );
        }
        if( databaseHelper != null && ( database == null || ! database.isOpen() )) {
            database = databaseHelper.getWritableDatabase();
        }
        return( database != null && databaseHelper != null);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return 0;
        }

        int count = 0;
        switch (URIMatcher.match(uri)) {
            case BATTERYMANAGER:
                count = database.delete(DATABASE_TABLES[0], selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (URIMatcher.match(uri)) {
            case BATTERYMANAGER:
                return BatteryManager_Data.CONTENT_TYPE;
            case BATTERYMANAGER_ID:
                return BatteryManager_Data.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return null;
        }

        ContentValues values = (initialValues != null) ? new ContentValues(
                initialValues) : new ContentValues();

        switch (URIMatcher.match(uri)) {
            case BATTERYMANAGER:
                long bat_id = database.insert(DATABASE_TABLES[0], BatteryManager_Data.DEVICE_ID, values);

                if (bat_id > 0) {
                    Uri new_uri = ContentUris.withAppendedId(
                            BatteryManager_Data.CONTENT_URI,
                            bat_id);
                    getContext().getContentResolver().notifyChange(new_uri,
                            null);
                    return new_uri;
                }
                throw new SQLException("Failed to insert row into " + uri);
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return null;
        }

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (URIMatcher.match(uri)) {
            case BATTERYMANAGER:
                qb.setTables(DATABASE_TABLES[0]);
                qb.setProjectionMap(databaseMap);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Cursor c = qb.query(database, projection, selection, selectionArgs,
                    null, null, sortOrder);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        } catch (IllegalStateException e) {
            if (Aware.DEBUG)
                Log.e(Aware.TAG, e.getMessage());

            return null;
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return 0;
        }

        int count = 0;
        switch (URIMatcher.match(uri)) {
            case BATTERYMANAGER:
                count = database.update(DATABASE_TABLES[0], values, selection,
                        selectionArgs);
                break;
            default:

                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}