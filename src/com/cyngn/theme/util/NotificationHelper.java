/*
 * Copyright (C) 2014 Cyanogen, Inc.
 */
package com.cyngn.theme.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.text.TextUtils;

import com.cyngn.theme.chooser.ChooserActivity;
import com.cyngn.theme.chooser.R;

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
