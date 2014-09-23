/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cyanogenmod.theme.chooser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.ThemeManager;
import android.net.Uri;
import org.cyanogenmod.theme.util.NotificationHelper;
import org.cyanogenmod.theme.util.PreferenceUtils;

import java.util.Set;

public class AppReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Uri uri = intent.getData();
        String pkgName = uri != null ? uri.getSchemeSpecificPart() : null;
        String action = intent.getAction();
        boolean isReplacing = intent.getExtras().getBoolean(Intent.EXTRA_REPLACING, false);

        if (Intent.ACTION_PACKAGE_ADDED.equals(action) && !isReplacing) {
            if (!isThemeBeingProcessed(context, pkgName)) {
                NotificationHelper.postThemeInstalledNotification(context, pkgName);
            } else {
                // store this package name so we know it's being processed
                PreferenceUtils.addThemeBeingProcessed(context, pkgName);
            }
        } else if (Intent.ACTION_PACKAGE_FULLY_REMOVED.equals(action)) {
            NotificationHelper.cancelNotificationForPackage(context, pkgName);
        } else if (Intent.ACTION_THEME_RESOURCES_CACHED.equals(action)) {
            final String themePkgName = intent.getStringExtra(Intent.EXTRA_THEME_PACKAGE_NAME);
            final int result = intent.getIntExtra(Intent.EXTRA_THEME_RESULT,
                    PackageManager.INSTALL_FAILED_THEME_UNKNOWN_ERROR);
            Set<String> processingThemes =
                    PreferenceUtils.getInstalledThemesBeingProcessed(context);
            if (processingThemes != null &&
                    processingThemes.contains(themePkgName) && result >= 0) {
                NotificationHelper.postThemeInstalledNotification(context, themePkgName);
                PreferenceUtils.removeThemeBeingProcessed(context, themePkgName);
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

    private boolean isThemeBeingProcessed(Context context, String pkgName) {
        ThemeManager tm = (ThemeManager) context.getSystemService(Context.THEME_SERVICE);
        return tm.isThemeBeingProcessed(pkgName);
    }
}
