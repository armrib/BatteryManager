package com.aware.plugin.batteryManager;

/**
 * Created by Armand on 08/04/2015.
 */
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;


public class BatteryAlarm extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("test", "InOutAlarm onReceive");
        Intent apply = new Intent(Aware.ACTION_AWARE_REFRESH);
        context.sendBroadcast(apply);

        Plugin.getInout(context);
    }

    public void SetAlarm(Context context, int interval) {
        Log.d("test", "InOutAlarm SetAlarm");
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, BatteryAlarm.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval*60*1000, pi);
    }

    public void CancelAlarm(Context context) {
        Log.d("asd", "InOutAlarm CancelAlarm");
        Intent intent = new Intent(context, BatteryAlarm.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }
}
