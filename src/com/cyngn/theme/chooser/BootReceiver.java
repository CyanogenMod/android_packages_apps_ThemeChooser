/*
 * Copyright (C) 2014 The Cyanogen, Inc
 */
package com.cyngn.theme.chooser;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class BootReceiver extends BroadcastReceiver {
    private static final String CHOOSER_PKG_NAME = "com.cyngn.theme.chooser";
    private static final String CHOOSER_ACTIVITY = "com.cyngn.theme.chooser.ChooserLauncher";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        PackageManager pm = context.getPackageManager();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            try {
                PackageInfo info = pm.getPackageInfo(ChooserActivity.THEME_STORE_PACKAGE, 0);
                if (info != null) {
                    ComponentName cn = new ComponentName(CHOOSER_PKG_NAME, CHOOSER_ACTIVITY);
                    pm.setComponentEnabledSetting(cn,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP);
                }
            } catch (PackageManager.NameNotFoundException e) {
                // no store so nothing to do.
            }

            // now disable this receiver so we don't get called on future boots
            ComponentName cn = new ComponentName(CHOOSER_PKG_NAME,
                    BootReceiver.class.getCanonicalName());
            pm.setComponentEnabledSetting(cn,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        }
    }
}
