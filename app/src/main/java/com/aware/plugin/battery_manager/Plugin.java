package com.aware.plugin.battery_manager;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.BatteryManager;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.Battery;
import com.aware.providers.Battery_Provider;
import com.aware.utils.Aware_Plugin;

public class Plugin extends Aware_Plugin {

    public static final String ACTION_AWARE_PLUGIN_BATTERY_MANAGER = "ACTION_AWARE_PLUGIN_BATTERY_MANAGER";
    private static ContextProducer contextProducer;

    @Override
    public void onCreate() {
        super.onCreate();

        TAG = "Battery Manager";
        DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

        Aware.setSetting(getApplicationContext(), Settings.STATUS_PLUGIN, true);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_BATTERY, true);

        //Listen to AWARE's battery changed events
        IntentFilter filter = new IntentFilter();
        filter.addAction(Battery.ACTION_AWARE_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, filter);

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
                context_batteryManager.setAction(ACTION_AWARE_PLUGIN_BATTERY_MANAGER);
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

        //Ask AWARE to apply settings
        sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));
    }

    private BatteryReceiver batteryReceiver = new BatteryReceiver();
    public static class BatteryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent batteryEstimator = new Intent(context, BatteryEstimator.class);
            context.startService(batteryEstimator);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        TAG = "Battery Manager";
        DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");
        return START_STICKY;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if( batteryReceiver != null ) unregisterReceiver(batteryReceiver);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_BATTERY, false);
    }

    public static class BatteryEstimator extends IntentService {
        public BatteryEstimator() {
            super("AWARE Battery Estimator");
        }

        @Override
        protected void onHandleIntent(Intent intent) {

            Cursor last_time = getApplicationContext().getContentResolver().query(Battery_Provider.Battery_Data.CONTENT_URI, null, null, null, Battery_Provider.Battery_Data.TIMESTAMP + " DESC LIMIT 60");
            if (last_time != null && last_time.moveToFirst()) {
                int current_status = last_time.getInt(last_time.getColumnIndex(Battery_Provider.Battery_Data.STATUS));
                double current_battery = last_time.getDouble(last_time.getColumnIndex(Battery_Provider.Battery_Data.LEVEL));
                double battery_scale = last_time.getDouble(last_time.getColumnIndex(Battery_Provider.Battery_Data.SCALE));
                double current_timestamp = last_time.getDouble(last_time.getColumnIndex(Battery_Provider.Battery_Data.TIMESTAMP));

                if( current_status == BatteryManager.BATTERY_STATUS_CHARGING ) {
                    int previous_status;
                    double previous_battery;
                    double previous_timestamp;

                    //battery rate is positive
                    while( last_time.moveToNext() ) {
                        previous_status = last_time.getInt(last_time.getColumnIndex(Battery_Provider.Battery_Data.STATUS));
                        previous_battery = last_time.getDouble(last_time.getColumnIndex(Battery_Provider.Battery_Data.LEVEL));
                        previous_timestamp = last_time.getDouble(last_time.getColumnIndex(Battery_Provider.Battery_Data.TIMESTAMP));

                        //we have a positive charge that has been longer than 1 minute worth of time
                        if( current_status == previous_status && previous_battery < current_battery && (current_timestamp-previous_timestamp) > (60*1000) ) {

                            double delta_time = (current_timestamp - previous_timestamp)/(60*1000); //we want %/minute
                            double delta_battery = (current_battery - previous_battery);

                            double charging_rate = delta_battery / delta_time; //this will be positive
                            double forecast = (battery_scale/charging_rate)*60*1000;

                            addData(charging_rate, 1, delta_time, forecast);

                            if( DEBUG ) Log.d(TAG, "Status: charging\nBattery rate: " + charging_rate + "\nEstimated:" + forecast + " minutes until full\n");

                            break;
                        }
                    }
                }
                if( current_status == BatteryManager.BATTERY_STATUS_DISCHARGING ) {
                    int previous_status;
                    double previous_battery;
                    double previous_timestamp;

                    //battery rate is negative
                    while( last_time.moveToNext() ) {
                        previous_status = last_time.getInt(last_time.getColumnIndex(Battery_Provider.Battery_Data.STATUS));
                        previous_battery = last_time.getDouble(last_time.getColumnIndex(Battery_Provider.Battery_Data.LEVEL));
                        previous_timestamp = last_time.getDouble(last_time.getColumnIndex(Battery_Provider.Battery_Data.TIMESTAMP));

                        //we have a negative charge that has been longer than 1 minute worth of time
                        if( current_status == previous_status && previous_battery > current_battery && (current_timestamp-previous_timestamp) > (60*1000) ) {

                            double delta_time = (current_timestamp - previous_timestamp)/(60*1000); //we want %/minute
                            double delta_battery = (current_battery - previous_battery);

                            double discharging_rate = delta_battery / delta_time; //this will be negative
                            double abs_bdr = Math.abs(discharging_rate);

                            double forecast = (current_battery/abs_bdr)*60*1000;

                            addData(discharging_rate, 0, delta_time, forecast);

                            if( DEBUG ) Log.d(TAG, "Status: discharging\nBattery rate: " + discharging_rate + "\nEstimated:" + forecast + " minutes until empty\n");

                            break;
                        }
                    }
                }
            }
            if( last_time != null && ! last_time.isClosed() ) last_time.close();
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