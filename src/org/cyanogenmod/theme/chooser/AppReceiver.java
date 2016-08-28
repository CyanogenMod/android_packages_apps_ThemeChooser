/*
 * Copyright (C) 2016 Cyanogen, Inc.
 * Copyright (C) 2016 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.text.TextUtils;

import org.cyanogenmod.theme.util.NotificationHelper;
import org.cyanogenmod.theme.util.PreferenceUtils;
import org.cyanogenmod.theme.util.Utils;

import cyanogenmod.providers.ThemesContract;

public class AppReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Uri uri = intent.getData();
        String pkgName = uri != null ? uri.getSchemeSpecificPart() : null;
        String action = intent.getAction();

        if (cyanogenmod.content.Intent.ACTION_THEME_INSTALLED.equals(action)) {
            if (!pkgName.equals(Utils.getDefaultThemePackageName(context))) {
                NotificationHelper.postThemeInstalledNotification(context, pkgName);
            }
        } else if (cyanogenmod.content.Intent.ACTION_THEME_REMOVED.equals(action)) {
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
        } else if (cyanogenmod.content.Intent.ACTION_THEME_UPDATED.equals(action)) {
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
