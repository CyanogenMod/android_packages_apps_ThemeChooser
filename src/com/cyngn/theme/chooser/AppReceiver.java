/*
 * Copyright (C) 2014 Cyanogen, Inc.
 */
package com.cyngn.theme.chooser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import com.cyngn.theme.util.NotificationHelper;
import com.cyngn.theme.util.PreferenceUtils;

public class AppReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Uri uri = intent.getData();
        String pkgName = uri != null ? uri.getSchemeSpecificPart() : null;
        String action = intent.getAction();
        boolean isReplacing = intent.getExtras().getBoolean(Intent.EXTRA_REPLACING, false);

        if (Intent.ACTION_PACKAGE_ADDED.equals(action) && !isReplacing) {
            try {
                if (isTheme(context, pkgName)) {
                    NotificationHelper.postThemeInstalledNotification(context, pkgName);
                }
            } catch (NameNotFoundException e) {
            }
        } else if (Intent.ACTION_PACKAGE_FULLY_REMOVED.equals(action)) {
            try {
                if (isTheme(context, pkgName)) {
                    PreferenceUtils.removeUpdatedTheme(context, pkgName);
                }
            } catch (NameNotFoundException e) {
            }
            NotificationHelper.cancelNotificationForPackage(context, pkgName);
        } else if (Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
            try {
                if (isTheme(context, pkgName)) {
                    PreferenceUtils.addUpdatedTheme(context, pkgName);
                }
            } catch (NameNotFoundException e) {
            }
        }
    }

    private boolean isTheme(Context context, String pkgName) throws NameNotFoundException {
        PackageInfo pi = context.getPackageManager().getPackageInfo(pkgName, 0);
        if (pi == null) return false;

        if ((pi.themeInfos != null && pi.themeInfos.length > 0) ||
                (pi.legacyThemeInfos != null && pi.legacyThemeInfos.length > 0)) {
            return true;
        }

        return false;
    }
}
