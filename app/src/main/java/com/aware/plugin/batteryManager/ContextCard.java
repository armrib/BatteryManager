package com.aware.plugin.batteryManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.aware.ui.Stream_UI;
import com.aware.utils.IContextCard;

import java.util.concurrent.TimeUnit;

public class ContextCard implements IContextCard {

    //Set how often your card needs to refresh if the stream is visible (in milliseconds)
    private int refresh_interval = 5 * 1000; //1 second = 1000 milliseconds
    private TextView rateString;
    private TextView chargeString;
    private TextView timerString;
    private TextView forecastString;
    private TextView totalChargeString;
    private TextView totalDischargeString;

    private Handler uiRefresher = new Handler(Looper.getMainLooper());
    private Runnable uiChanger = new Runnable() {
        @Override
        public void run() {

            //Modify card's content here once it's initialized
            if( card != null ) {

                Cursor last_time = sContext.getContentResolver().query(Provider.BatteryManager_Data.CONTENT_URI, null, null, null, Provider.BatteryManager_Data.TIMESTAMP + " DESC LIMIT 1");
                if (last_time != null && last_time.moveToFirst()) {
                    double rate = last_time.getDouble(last_time.getColumnIndex(Provider.BatteryManager_Data.RATE));
                    double charge = last_time.getDouble(last_time.getColumnIndex(Provider.BatteryManager_Data.CHARGE));
                    double timer = last_time.getDouble(last_time.getColumnIndex(Provider.BatteryManager_Data.TIME));
                    double forecast = last_time.getDouble(last_time.getColumnIndex(Provider.BatteryManager_Data.FORECAST));
                    String time_string = toTime(timer);
                    String forecastTxt = toTime(forecast);
                    rateString.setText("Rate : "+rate+" %/min");
                    chargeString.setText("In charge ? : "+charge);
                    timerString.setText("Timer : "+time_string);
                    forecastString.setText("Forecast : "+forecastTxt);
                }
                if (last_time != null && !last_time.isClosed()) {
                    last_time.close();
                }
                Cursor timerCharge = sContext.getContentResolver().query(Provider.BatteryManager_Data.CONTENT_URI, null, "double_time != 0 AND double_rate = -1 AND double_charge = 1", null, Provider.BatteryManager_Data.TIMESTAMP + " DESC LIMIT 1");
                if (timerCharge != null && timerCharge.moveToFirst()) {
                    double totalCharge = timerCharge.getDouble(timerCharge.getColumnIndex(Provider.BatteryManager_Data.TIME));
                    String txt = toTime(totalCharge);
                    totalChargeString.setText("total time for last charging : "+txt);
                }
                if (timerCharge != null && !timerCharge.isClosed()) {
                    timerCharge.close();
                }
                Cursor timerDischarge = sContext.getContentResolver().query(Provider.BatteryManager_Data.CONTENT_URI, null, "double_time != 0 AND double_rate = -1 AND double_charge = 0", null, Provider.BatteryManager_Data.TIMESTAMP + " DESC LIMIT 1");
                if (timerDischarge != null && timerDischarge.moveToFirst()) {
                    double totalDischarge = timerDischarge.getDouble(timerDischarge.getColumnIndex(Provider.BatteryManager_Data.TIME));
                    String txt = toTime(totalDischarge);
                    totalDischargeString.setText("total time for last discharging : "+txt);
                }
                if (timerDischarge != null && !timerDischarge.isClosed()) {
                    timerDischarge.close();
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
    private TextView counter_txt;

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

        //Begin refresh cycle
        uiRefresher.postDelayed(uiChanger, refresh_interval);
        Log.d("test","card");

        rateString = (TextView) card.findViewById(R.id.textView);
        rateString.setTextColor(Color.BLACK);
        chargeString = (TextView) card.findViewById(R.id.textView2);
        chargeString.setTextColor(Color.BLACK);
        timerString = (TextView) card.findViewById(R.id.textView3);
        timerString.setTextColor(Color.BLACK);
        forecastString = (TextView) card.findViewById(R.id.textView4);
        forecastString.setTextColor(Color.BLACK);
        totalChargeString = (TextView) card.findViewById(R.id.textView5);
        totalChargeString.setTextColor(Color.BLACK);
        totalDischargeString = (TextView) card.findViewById(R.id.textView6);
        totalDischargeString.setTextColor(Color.BLACK);
        Cursor last_time = sContext.getContentResolver().query(Provider.BatteryManager_Data.CONTENT_URI, null, null, null, Provider.BatteryManager_Data.TIMESTAMP + " DESC LIMIT 1");
        if (last_time != null && last_time.moveToFirst()) {
            double rate = last_time.getDouble(last_time.getColumnIndex(Provider.BatteryManager_Data.RATE));
            double charge = last_time.getDouble(last_time.getColumnIndex(Provider.BatteryManager_Data.CHARGE));
            double timer = last_time.getDouble(last_time.getColumnIndex(Provider.BatteryManager_Data.TIME));
            double forecast = last_time.getDouble(last_time.getColumnIndex(Provider.BatteryManager_Data.FORECAST));
            String time_string = toTime(timer);
            String forecastTxt = toTime(forecast);
            rateString.setText("Rate : "+rate+" %/min");
            chargeString.setText("In charge ? : "+charge);
            timerString.setText("Timer : "+time_string);
            forecastString.setText("Forecast : "+forecastTxt);
        }
        if (last_time != null && !last_time.isClosed()) {
            last_time.close();
        }
        Cursor timerCharge = sContext.getContentResolver().query(Provider.BatteryManager_Data.CONTENT_URI, null, "double_time != 0 AND double_rate = -1 AND double_charge = 1", null, Provider.BatteryManager_Data.TIMESTAMP + " DESC LIMIT 1");
        if (timerCharge != null && timerCharge.moveToFirst()) {
            double totalCharge = timerCharge.getDouble(timerCharge.getColumnIndex(Provider.BatteryManager_Data.TIME));
            String txt = toTime(totalCharge);
            totalChargeString.setText("total time for last charging : "+txt);
        }
        if (timerCharge != null && !timerCharge.isClosed()) {
            timerCharge.close();
        }
        Cursor timerDischarge = sContext.getContentResolver().query(Provider.BatteryManager_Data.CONTENT_URI, null, "double_time != 0 AND double_rate = -1 AND double_charge = 0", null, Provider.BatteryManager_Data.TIMESTAMP + " DESC LIMIT 1");
        if (timerDischarge != null && timerDischarge.moveToFirst()) {
            double totalDischarge = timerDischarge.getDouble(timerDischarge.getColumnIndex(Provider.BatteryManager_Data.TIME));
            String txt = toTime(totalDischarge);
            totalDischargeString.setText("total time for last discharging : "+txt);
        }
        if (timerDischarge != null && !timerDischarge.isClosed()) {
            timerDischarge.close();
        }

        return card;
    }
    //This is a BroadcastReceiver that keeps track of stream status. Used to stop the refresh when user leaves the stream and restart again otherwise
    private StreamObs streamObs = new StreamObs();
    public class StreamObs extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if( intent.getAction().equals(Stream_UI.ACTION_AWARE_STREAM_OPEN) ) {
                //start refreshing when user enters the stream
                uiRefresher.postDelayed(uiChanger, refresh_interval);

            }
            if( intent.getAction().equals(Stream_UI.ACTION_AWARE_STREAM_CLOSED) ) {
                //stop refreshing when user leaves the stream
                uiRefresher.removeCallbacks(uiChanger);
                uiRefresher.removeCallbacksAndMessages(null);
            }
        }
    }
    public String toTime(double millisecond)
    {
        long seconds = (long) (millisecond/1000);
        int day = (int)TimeUnit.SECONDS.toDays(seconds);
        long hours = TimeUnit.SECONDS.toHours(seconds) - (day *24);
        long minute = TimeUnit.SECONDS.toMinutes(seconds) - (TimeUnit.SECONDS.toHours(seconds)* 60);
        long second = TimeUnit.SECONDS.toSeconds(seconds) - (TimeUnit.SECONDS.toMinutes(seconds) *60);
        return day+" day "+hours+" h "+minute+" min "+second+" s";
    }
}
