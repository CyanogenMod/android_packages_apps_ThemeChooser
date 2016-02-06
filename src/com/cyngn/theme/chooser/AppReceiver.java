/*
 * Copyright (C) 2014 Cyanogen, Inc.
 */
package com.cyngn.theme.chooser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ThemeUtils;
import android.net.Uri;
import android.provider.ThemesContract;
import android.text.TextUtils;
import com.cyngn.theme.util.NotificationHelper;
import com.cyngn.theme.util.PreferenceUtils;
import com.cyngn.theme.util.Utils;

public class AppReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Uri uri = intent.getData();
        String pkgName = uri != null ? uri.getSchemeSpecificPart() : null;
        String action = intent.getAction();

        if (ThemesContract.Intent.ACTION_THEME_INSTALLED.equals(action)) {
            if (!pkgName.equals(Utils.getDefaultThemePackageName(context))) {
                NotificationHelper.postThemeInstalledNotification(context, pkgName);
            }
        } else if (ThemesContract.Intent.ACTION_THEME_REMOVED.equals(action)) {
            // remove updated status for this theme (if one exists)
            PreferenceUtils.removeUpdatedTheme(context, pkgName);

            // If the theme being removed was the currently applied theme we need
            // to update the applied base theme in preferences to the default theme.
            String appliedBaseTheme = PreferenceUtils.getAppliedBaseTheme(context);
            if (!TextUtils.isEmpty(appliedBaseTheme) && appliedBaseTheme.equals(pkgName)) {
                PreferenceUtils.setAppliedBaseTheme(context,
                Utils.getDefaultThemePackageName(context));
            }
            NotificationHelper.cancelNotifications(context);
        } else if (ThemesContract.Intent.ACTION_THEME_UPDATED.equals(action)) {
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

        return pi != null && pi.themeInfo != null;
    }
}
