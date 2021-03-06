package com.arvind.quote.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.arvind.quote.services.CommonUtilService;
import com.arvind.quote.services.NotificationService;

import java.util.Calendar;

public class SomeBroadReceiver extends BroadcastReceiver {

    private final String TAG = "SomeBroadReceiver";

    private AlarmManager alarmManager;
    private PendingIntent notifPendingIntent;
    private SharedPreferences sharedPreferences;

    @Override
    public void onReceive(Context context, Intent intent) {

        // Create an intent which would call the BroadcastReceiver Again
        Intent someIntent = new Intent(context, SomeBroadReceiver.class);
        // But this time, with a different action!
        someIntent.setAction("com.arvind.quote.SHOW_QUOTE");
        // Get System's AlarmManager
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // Create a Pending Intent, which would be executed by the AlarmManager
        // At the Given Time and Interval
        notifPendingIntent = PendingIntent.getBroadcast(context, 0, someIntent, 0);

        if (intent.getAction() != null) {
            Log.d(TAG, "Action: " + intent.getAction());
            switch (intent.getAction()) {
                case "android.intent.action.BOOT_COMPLETED":
                case "com.arvind.quote.TIME_SET_BY_USER":
                    Log.d(TAG, "Scheduling QoTD Notifications");
                    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                    // Get the Calendar, we've got to set the time
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(System.currentTimeMillis());
                    // Set the Time
                    calendar.set(Calendar.HOUR_OF_DAY, sharedPreferences.getInt("QOTD_HOUR", 0));
                    calendar.set(Calendar.MINUTE, sharedPreferences.getInt("QOTD_MIN", 0));
                    calendar.set(Calendar.SECOND, 0);
                    Log.i(TAG, "QoTD at : " + calendar.getTime().toString());
                    alarmManager.setRepeating(
                            AlarmManager.RTC_WAKEUP, // Ctrl+Q please
                            calendar.getTimeInMillis(), // Time at which intent has to be executed
                            AlarmManager.INTERVAL_DAY, // Publish the notifications daily
                            notifPendingIntent); // The Intent to be executed
                    break;
                case "com.arvind.quote.SHOW_QUOTE":
                    Log.i(TAG, "Waking up Notification Service");
                    context.startService(new Intent(context, NotificationService.class));
                    break;
                case "com.arvind.quote.CANCEL_QOTD":
                    Log.i(TAG, "Cancelling QoTD Alarms");
                    alarmManager.cancel(notifPendingIntent);
                    break;
                case "com.arvind.quote.COMMON_UTILS":
                    Log.i(TAG, "Waking up CommonUtils Service");
                    // Send Broadcast to close System Dialogs (Closes the Notification Drawer :D)
                    context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
                    // Start CommonUtilsService!
                    context.startService(new Intent(context, CommonUtilService.class).putExtras(intent));
                    break;
                default:
                    break;
            }
        }
    }
}
