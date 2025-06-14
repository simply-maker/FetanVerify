package com.example.fetanverify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

public class GebiTriggerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("com.example.gebi.VERIFY_TRANSACTION".equals(intent.getAction())) {
            String transactionId = intent.getStringExtra("transaction_id");
            
            try {
                // Try to launch Gebi app
                PackageManager pm = context.getPackageManager();
                Intent launchIntent = pm.getLaunchIntentForPackage("com.example.gebi");
                
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    launchIntent.putExtra("transaction_id", transactionId);
                    launchIntent.putExtra("trigger_verification", true);
                    context.startActivity(launchIntent);
                }
            } catch (Exception e) {
                // Handle silently
            }
        }
    }
}