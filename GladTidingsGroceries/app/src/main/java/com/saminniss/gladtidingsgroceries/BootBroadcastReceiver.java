package com.saminniss.gladtidingsgroceries;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by Sam Inniss on 10/11/2016.
 */
public class BootBroadcastReceiver extends BroadcastReceiver {
    static final String ACTION = "android.intent.action.BOOT_COMPLETED";
    @Override
    public void onReceive(Context context, Intent intent) {
        // BOOT_COMPLETED” start Service
        if (intent.getAction().equals(ACTION)) {
            //Service
            Intent serviceIntent = new Intent(context, MyFirebaseMessagingService.class);
            serviceIntent.putExtra("update_db", true);
            context.startService(serviceIntent);

            TabContentFragment.DbHelper db_helper = new TabContentFragment.DbHelper(context);
            SQLiteDatabase db = db_helper.getWritableDatabase();
            TabContentFragment.UpdateDatabase(db);
        }
    }
}