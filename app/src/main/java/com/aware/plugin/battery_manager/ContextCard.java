package com.aware.plugin.battery_manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.aware.providers.Battery_Provider;
import com.aware.ui.Stream_UI;
import com.aware.utils.IContextCard;

import java.util.concurrent.TimeUnit;

public class ContextCard implements IContextCard {

    //Set how often your card needs to refresh if the stream is visible (in milliseconds)
    private int refresh_interval = 60 * 1000;

    private Handler uiRefresher = new Handler(Looper.getMainLooper());
    private Runnable uiChanger = new Runnable() {
        @Override
        public void run() {

            //Modify card's content here once it's initialized
            if( card != null ) {
                boolean is_full = false;
                Cursor battery = sContext.getContentResolver().query(Battery_Provider.Battery_Data.CONTENT_URI, null, null, null, Battery_Provider.Battery_Data.TIMESTAMP + " DESC LIMIT 1");
                if( battery != null && battery.moveToFirst() ) {
                    is_full = (battery.getInt(battery.getColumnIndex(Battery_Provider.Battery_Data.STATUS)) == BatteryManager.BATTERY_STATUS_FULL || battery.getInt(battery.getColumnIndex(Battery_Provider.Battery_Data.LEVEL)) == 100);
                }
                if( battery != null && ! battery.isClosed() ) battery.close();

                if( ! is_full ) {
                    Cursor last_time = sContext.getContentResolver().query(Provider.BatteryManager_Data.CONTENT_URI, null, null, null, Provider.BatteryManager_Data.TIMESTAMP + " DESC LIMIT 1");
                    if (last_time != null && last_time.moveToFirst()) {
                        double rate = last_time.getDouble(last_time.getColumnIndex(Provider.BatteryManager_Data.RATE));
                        double forecast = last_time.getDouble(last_time.getColumnIndex(Provider.BatteryManager_Data.FORECAST));
                        if( rate < 0 ) {
                            battery_rate.setText("Discharging: " + String.format("%.3f", rate) + " %/minute");
                            battery_rate.setTextColor(Color.RED);
                        } else {
                            battery_rate.setText("Charging: " + String.format("%.3f", rate) + " %/minute");
                            battery_rate.setTextColor(Color.GREEN);
                        }
                        battery_estimation.setText(toTime(forecast) + " of battery");
                    }
                    if (last_time != null && ! last_time.isClosed()) last_time.close();
                } else {
                    battery_rate.setText("Battery is full");

                    Cursor last_time = sContext.getContentResolver().query(Provider.BatteryManager_Data.CONTENT_URI, null, null, null, Provider.BatteryManager_Data.TIMESTAMP + " DESC LIMIT 1");
                    if (last_time != null && last_time.moveToFirst()) {
                        double forecast = last_time.getDouble(last_time.getColumnIndex(Provider.BatteryManager_Data.FORECAST));
                        battery_estimation.setText(toTime(forecast) + " of battery");
                    }
                    if (last_time != null && ! last_time.isClosed()) last_time.close();
                }
            }
            //Reset timer and schedule the next card refresh
            uiRefresher.postDelayed(uiChanger, refresh_interval);
        }
    };

    //Empty constructor used to instantiate this card
    public ContextCard(){};

    //You may use sContext on uiChanger to do queries to databases, etc.
    private Context sContext;

    //Declare here all the UI elements you'll be accessing
    private View card;
    private TextView battery_rate;
    private TextView battery_estimation;

    //Used to load your context card
    private LayoutInflater sInflater;

    @Override
    public View getContextCard(Context context) {
        sContext = context;

        //Tell Android that you'll monitor the stream statuses
        IntentFilter filter = new IntentFilter();
        filter.addAction(Stream_UI.ACTION_AWARE_STREAM_OPEN);
        filter.addAction(Stream_UI.ACTION_AWARE_STREAM_CLOSED);
        context.registerReceiver(streamObs, filter);

        //Load card information to memory
        sInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        card = sInflater.inflate(R.layout.card, null);

        battery_rate = (TextView) card.findViewById(R.id.battery_rate);
        battery_estimation = (TextView) card.findViewById(R.id.battery_estimation);

        //Begin refresh cycle
        uiRefresher.post(uiChanger);

        return card;
    }
    //This is a BroadcastReceiver that keeps track of stream status. Used to stop the refresh when user leaves the stream and restart again otherwise
    private StreamObs streamObs = new StreamObs();
    public class StreamObs extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if( intent.getAction().equals(Stream_UI.ACTION_AWARE_STREAM_OPEN) ) {
                //start refreshing when user enters the stream
                uiRefresher.post(uiChanger);
            }
            if( intent.getAction().equals(Stream_UI.ACTION_AWARE_STREAM_CLOSED) ) {
                //stop refreshing when user leaves the stream
                uiRefresher.removeCallbacks(uiChanger);
                uiRefresher.removeCallbacksAndMessages(null);
            }
        }
    }

    public String toTime(double millisecond) {
        long seconds = (long) (millisecond/1000);
        int day = (int)TimeUnit.SECONDS.toDays(seconds);
        long hours = TimeUnit.SECONDS.toHours(seconds) - (day*24);
        long minute = TimeUnit.SECONDS.toMinutes(seconds) - (TimeUnit.SECONDS.toHours(seconds)*60);
        return day+" days "+hours+" hours, "+minute+" minutes";
    }
}
