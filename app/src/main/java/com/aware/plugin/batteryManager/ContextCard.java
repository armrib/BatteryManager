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

import com.aware.providers.Battery_Provider;
import com.aware.ui.Stream_UI;
import com.aware.utils.IContextCard;

import java.security.Provider;

public class ContextCard implements IContextCard {

    //Set how often your card needs to refresh if the stream is visible (in milliseconds)
    private int refresh_interval = 5 * 1000; //1 second = 1000 milliseconds
    private TextView inout;

    private Handler uiRefresher = new Handler(Looper.getMainLooper());
    private Runnable uiChanger = new Runnable() {
        @Override
        public void run() {

            //Modify card's content here once it's initialized
            if( card != null ) {

                Cursor last_time = sContext.getContentResolver().query(Provider.IOMeter_Data.CONTENT_URI, null, null, null, Provider.IOMeter_Data.TIMESTAMP + " DESC LIMIT 1");
                if (last_time != null && last_time.moveToFirst()) {
                    String x = last_time.getString(last_time.getColumnIndex(Provider.IOMeter_Data.IO_STATUS));
                    double y = last_time.getDouble(last_time.getColumnIndex(Provider.IOMeter_Data.IO_CONFIDENCE));
                    double percent = y*100;
                    int confidence = (int) percent;
                    inout.setText("Phone "+x+", confidence : "+confidence);
                }
                if (last_time != null && !last_time.isClosed()) {
                    last_time.close();
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

        inout = (TextView) card.findViewById(R.id.textView);
        inout.setTextColor(Color.BLACK);
        Cursor last_time = context.getContentResolver().query(Provider.IOMeter_Data.CONTENT_URI, null, null, null, Provider.IOMeter_Data.TIMESTAMP + " DESC LIMIT 1");
        if (last_time != null && last_time.moveToFirst()) {
            String x = last_time.getString(last_time.getColumnIndex(Provider.IOMeter_Data.IO_STATUS));
            double y = last_time.getDouble(last_time.getColumnIndex(Provider.IOMeter_Data.IO_CONFIDENCE));
            double percent = y*100;
            int confidence = (int) percent;
            inout.setText("Phone "+x+", confidence : "+confidence);
        }
        if (last_time != null && !last_time.isClosed()) {
            last_time.close();
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
}
