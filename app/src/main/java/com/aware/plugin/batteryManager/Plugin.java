package com.aware.plugin.batteryManager;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.util.Log;

import com.aware.Accelerometer;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.utils.Aware_Plugin;

import java.util.List;

public class Plugin extends Aware_Plugin {

    public static Boolean magnetometer = false;
    public static Boolean light = false;
    public static Boolean temperature = false;
    public static final String ACTION_AWARE_PLUGIN_BATTERYMANAGER = "ACTION_AWARE_PLUGIN_BATTERYMANAGER";
    public BatteryAlarm alarm = new BatteryAlarm();
    public static int temp_interval = 5;

    private static ContextProducer contextProducer;

    @Override
    public void onCreate() {
        super.onCreate();

        TAG = "BatteryManager";
        DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

        Intent aware = new Intent(this, Aware.class);
        startService(aware);

        Aware.setSetting(getApplicationContext(), Settings.STATUS_PLUGIN, true);

        if( Aware.getSetting(getApplicationContext(), Settings.FREQUENCY_PLUGIN).length() == 0 ) {
            Aware.setSetting(getApplicationContext(), Settings.FREQUENCY_PLUGIN, 5);
        }

        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_BATTERY, true);
        sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));

        int interval_min =  Integer.parseInt(Aware.getSetting(getApplicationContext(), Settings.FREQUENCY_PLUGIN));
        alarm.SetAlarm(Plugin.this, interval_min);
        temp_interval = interval_min;

        CONTEXT_PRODUCER = new ContextProducer() {
            @Override
            public void onContext() {
                double rate = 0;
                double charge = 0;
                double time = 0;
                double forecast = 0;
                Cursor last_time = getApplicationContext().getContentResolver().query(Provider.BatteryManager_Data.CONTENT_URI, null, null, null, Provider.BatteryManager_Data.TIMESTAMP + " DESC LIMIT 1");
                if (last_time != null && last_time.moveToFirst()) {
                    rate = last_time.getDouble(last_time.getColumnIndex(Provider.BatteryManager_Data.RATE));
                    charge = last_time.getDouble(last_time.getColumnIndex(Provider.BatteryManager_Data.CHARGE));
                    time = last_time.getDouble(last_time.getColumnIndex(Provider.BatteryManager_Data.TIME));
                    forecast = last_time.getDouble(last_time.getColumnIndex(Provider.BatteryManager_Data.FORECAST));
                }
                if (last_time != null && !last_time.isClosed()) {
                    last_time.close();
                }
                Intent context_batteryManager = new Intent();
                context_batteryManager.setAction(ACTION_AWARE_PLUGIN_BATTERYMANAGER);
                context_batteryManager.putExtra(Provider.BatteryManager_Data.RATE, rate);
                context_batteryManager.putExtra(Provider.BatteryManager_Data.CHARGE, charge);
                context_batteryManager.putExtra(Provider.BatteryManager_Data.TIME, time);
                context_batteryManager.putExtra(Provider.BatteryManager_Data.FORECAST, forecast);
                sendBroadcast(context_batteryManager);
            }
        };
        contextProducer = CONTEXT_PRODUCER;

        DATABASE_TABLES = Provider.DATABASE_TABLES;
        TABLES_FIELDS = Provider.TABLES_FIELDS;
        CONTEXT_URIS = new Uri[]{ Provider.BatteryManager_Data.CONTENT_URI };

        if (DEBUG) Log.d(TAG, "Plugin running");
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("test", "onStartCommand");
        int interval_min =  Integer.parseInt(Aware.getSetting(getApplicationContext(), Settings.FREQUENCY_PLUGIN));

        if (interval_min != temp_interval) {
            Log.d("test", "differ");
            if(interval_min >= 1) {
                Log.d("test", "bigger");
                alarm.CancelAlarm(Plugin.this);
                alarm.SetAlarm(Plugin.this, interval_min);
                temp_interval = interval_min;
            } else {
                Log.d("test", "zero");
                temp_interval = interval_min;
                alarm.CancelAlarm(Plugin.this);

                Intent apply = new Intent(Aware.ACTION_AWARE_REFRESH);
                getApplicationContext().sendBroadcast(apply);
            }
        }

        return START_STICKY;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();

        alarm.CancelAlarm(Plugin.this);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_BATTERY, false);

        sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));

        if (DEBUG) Log.d(TAG, "Plugin terminated");
    }

    protected static void getInout(Context context) {
        Log.d("test", "getBat");
        try {
            Thread.sleep(10000);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        Intent bat_service = new Intent(context, Bat_Service.class);
        context.startService(bat_service);
    }
    public static class Bat_Service extends IntentService {
        public Bat_Service() {
            super("AWARE BAT");
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            Log.d("test", "onHandleIntent");

            try {
                Thread.sleep(15000);
            } catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            double rate = 0;
            double charge = 0;
            double time = 0;
            double forecast = 0;

            //do the job

            if (DEBUG) Log.d("test", " ");

            addData(rate,charge,time,forecast);
        }
        public void addData(double rate,double charge,double time,double forecast)
        {
            ContentValues context_data = new ContentValues();
            context_data.put(Provider.BatteryManager_Data.TIMESTAMP, System.currentTimeMillis());
            context_data.put(Provider.BatteryManager_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
            context_data.put(Provider.BatteryManager_Data.RATE, rate);
            context_data.put(Provider.BatteryManager_Data.CHARGE, charge);
            context_data.put(Provider.BatteryManager_Data.TIME, time);
            context_data.put(Provider.BatteryManager_Data.FORECAST, forecast);

            if( DEBUG ) Log.d(TAG, context_data.toString());

            //insert data to table
            getContentResolver().insert(Provider.BatteryManager_Data.CONTENT_URI, context_data);
            contextProducer.onContext();
        }
    }

}