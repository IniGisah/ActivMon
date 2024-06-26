package com.example.activmon;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public class AccessibilityEnabler {

    private final Context context;
    private final DevicePolicyManager devicePolicyManager;
    private final ComponentName deviceOwnerComponent;

    public AccessibilityEnabler(Context context) {
        this.context = context;
        devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        deviceOwnerComponent = new ComponentName(context, DeviceAdminReceiver.class);
    }

    public void enableAccessibilityService(String accessibilityServicePackageName) {
        // Check if the device owner app is enabled
        if (!devicePolicyManager.isDeviceOwnerApp(context.getPackageName())) {
            throw new IllegalStateException("Device owner app is not enabled");
        }

        // Enable the accessibility service
        //devicePolicyManager.enableAccessibilityService(deviceOwnerComponent, accessibilityServicePackageName);
    }
}

