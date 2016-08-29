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

package org.cyanogenmod.theme.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.text.TextUtils;

import org.cyanogenmod.theme.chooser2.ChooserActivity;
import org.cyanogenmod.theme.chooser2.R;

public class NotificationHelper {
    private static final int NOTIFICATION_ID = 0x434D5443;

    public static void postThemeInstalledNotification(Context context, String pkgName) {
        String themeName = null;
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(pkgName, 0);
            if (pi.themeInfo != null) {
                themeName = pi.themeInfo.name;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return;
        }
        if (TextUtils.isEmpty(themeName)) {
            return;
        }

        int themeCount = PreferenceUtils.getNewlyInstalledThemeCount(context) + 1;

        Intent intent = new Intent(context, ChooserActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.putExtra("pkgName", pkgName);
        PendingIntent pi = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_CANCEL_CURRENT);

        String title = null;
        String content = null;
        final Resources res = context.getResources();
        if (themeCount == 1) {
            title = String.format(res.getString(
                    R.string.theme_installed_notification_title), themeName);
            content = res.getString(R.string.theme_installed_notification_text);
        } else {
            title = String.format(res.getString(R.string.themes_installed_notification_title),
                    themeCount);
            content = String.format(res.getQuantityString(
                            R.plurals.themes_installed_notification_text, themeCount -1),
                            themeName, themeCount - 1);
        }
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notice = new Notification.Builder(context)
                .setAutoCancel(true)
                .setOngoing(false)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(pi)
                .setSmallIcon(R.drawable.ic_notify)
                .setWhen(System.currentTimeMillis())
                .build();
        if (themeCount > 1) notice.number = themeCount;
        nm.notify(NOTIFICATION_ID, notice);
        PreferenceUtils.setNewlyInstalledThemeCount(context, themeCount);
    }

    public static void cancelNotifications(Context context) {
        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_ID);
        PreferenceUtils.setNewlyInstalledThemeCount(context, 0);
    }
}
