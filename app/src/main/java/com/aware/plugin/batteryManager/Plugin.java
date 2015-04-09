package com.aware.plugin.batteryManager;



import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.Battery;
import com.aware.providers.Battery_Provider;
import com.aware.utils.Aware_Plugin;

public class Plugin extends Aware_Plugin {

    public static final String ACTION_AWARE_PLUGIN_BATTERYMANAGER = "ACTION_AWARE_PLUGIN_BATTERYMANAGER";
    public BatteryAlarm alarm = new BatteryAlarm();
    public static int temp_interval = 1;

    private static boolean is_charging = false;
    private static long timer = 0;
    private static long last_timestamp = 0;
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
            Aware.setSetting(getApplicationContext(), Settings.FREQUENCY_PLUGIN, 1);
        }

        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_BATTERY, true);
        sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));

        int interval_min =  Integer.parseInt(Aware.getSetting(getApplicationContext(), Settings.FREQUENCY_PLUGIN));
        alarm.SetAlarm(Plugin.this, interval_min);
        temp_interval = interval_min;

        IntentFilter filter = new IntentFilter();
        filter.addAction(Battery.ACTION_AWARE_BATTERY_CHANGED);
        registerReceiver(dataReceiver, filter);

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

    private SensorDataReceiver dataReceiver = new SensorDataReceiver();
    public static class SensorDataReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) Log.d(TAG, "OnReceive");
            int statue = 0;
            Cursor last_time = context.getApplicationContext().getContentResolver().query(Battery_Provider.Battery_Data.CONTENT_URI, null, null, null, Battery_Provider.Battery_Data.TIMESTAMP + " DESC LIMIT 1");
            if (last_time != null && last_time.moveToFirst()) {
                statue = last_time.getInt(last_time.getColumnIndex(Battery_Provider.Battery_Data.STATUS));
            }
            if (last_time != null && !last_time.isClosed()) {
                last_time.close();
            }
            boolean charging = false;
            if (statue == 2) charging = true;
            boolean a = is_charging && !charging;
            boolean b = !is_charging && charging;
            if (DEBUG) Log.d(TAG, "test change statue "+is_charging+" "+charging+" "+a+" "+b);
            if (a||b) {
                if (DEBUG) Log.d(TAG, "Battery change statue");
                double charge = 0;
                if (is_charging)
                    charge = 1;
                else charge = 0;

                ContentValues context_data = new ContentValues();
                context_data.put(Provider.BatteryManager_Data.TIMESTAMP, System.currentTimeMillis());
                context_data.put(Provider.BatteryManager_Data.DEVICE_ID, Aware.getSetting(context.getApplicationContext(), Aware_Preferences.DEVICE_ID));
                context_data.put(Provider.BatteryManager_Data.RATE, -1);
                context_data.put(Provider.BatteryManager_Data.CHARGE, charge);
                context_data.put(Provider.BatteryManager_Data.TIME, timer);
                context_data.put(Provider.BatteryManager_Data.FORECAST, 0);
                context.getContentResolver().insert(Provider.BatteryManager_Data.CONTENT_URI, context_data);
                contextProducer.onContext();

                timer = 0;
            }

            if(charging) {
                is_charging = true;
            }
            else is_charging = false;

            if( intent.getAction().equals(Battery.ACTION_AWARE_BATTERY_CHANGED) ) {
                if( last_timestamp == 0 ) last_timestamp = System.currentTimeMillis();
                    timer += System.currentTimeMillis() - last_timestamp;
                    last_timestamp = System.currentTimeMillis();
            }
        }
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

        unregisterReceiver(dataReceiver);

        if (DEBUG) Log.d(TAG, "Plugin terminated");
    }

    protected static void getBat(Context context) {
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
            double rate = 0;
            double charge = 0;
            double time = 0;
            double forecast = 0;

            double rate_last = 0;
            double scale = 0;
            double time_last = 0;
            double rate_previous = 0;
            double time_previous = 0;

            Cursor last_time = getApplicationContext().getContentResolver().query(Battery_Provider.Battery_Data.CONTENT_URI, null, null, null, Battery_Provider.Battery_Data.TIMESTAMP + " DESC LIMIT 60");
            if (last_time != null && last_time.moveToFirst()) {
                if(last_time.getDouble(last_time.getColumnIndex(Battery_Provider.Battery_Data.STATUS))== 2) charge = 1;
                rate_last = last_time.getDouble(last_time.getColumnIndex(Battery_Provider.Battery_Data.LEVEL));
                scale = last_time.getDouble(last_time.getColumnIndex(Battery_Provider.Battery_Data.SCALE));
                time_last = last_time.getDouble(last_time.getColumnIndex(Battery_Provider.Battery_Data.TIMESTAMP));
                double test = 0;
                if (charge == 1)
                    test = 1;
                while (test == charge && !last_time.isLast()  )
                {
                    last_time.moveToNext();
                    if(last_time.getDouble(last_time.getColumnIndex(Battery_Provider.Battery_Data.STATUS))== 2)
                        test = 1;
                    else
                        test = 0;
                }
                rate_previous = last_time.getDouble(last_time.getColumnIndex(Battery_Provider.Battery_Data.LEVEL));
                time_previous = last_time.getDouble(last_time.getColumnIndex(Battery_Provider.Battery_Data.TIMESTAMP));
            }
            if (last_time != null && !last_time.isClosed()) {
                last_time.close();
            }

            time = (time_last - time_previous) / (1000*60);

            rate = (rate_last - rate_previous) / time;
            if (rate <= 0)
            {
                rate = rate*(-1);
            }

            if(charge == 1 && rate>0)
            forecast = ((scale - rate_last) / rate)*60*1000;
            else if (charge == 0 && rate>0) forecast = (rate_last / rate)*60*1000;
            else forecast = 0;

            if( last_timestamp == 0 ) last_timestamp = System.currentTimeMillis();
            timer += System.currentTimeMillis() - last_timestamp;
            last_timestamp = System.currentTimeMillis();

            if (DEBUG) Log.d("test", "Rate : "+rate+" %/min, in charge ? : "+charge+" timer : "+timer+" forecast : "+forecast+" min left");

            addData(rate,charge,timer,forecast);
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