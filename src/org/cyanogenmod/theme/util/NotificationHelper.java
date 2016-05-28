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
package org.cyanogenmod.theme.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.text.TextUtils;
import org.cyanogenmod.theme.chooser.ChooserActivity;
import org.cyanogenmod.theme.chooser.R;

public class NotificationHelper {
    public static void postThemeInstalledNotification(Context context,
            String pkgName) {
        Log.d("OHAI", "postThemeInstalledNotification started");
        String themeName = null;
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(pkgName, 0);
            if (pi.themeInfo != null) {
                themeName = pi.themeInfo.name;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            Log.e("OHAI", "NameNotFoundException");
            return;
        }
        if (TextUtils.isEmpty(themeName)) {
            Log.e("OHAI", "themeName: " +  themeName);
            return;
        }

        Intent intent = new Intent(context, ChooserActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.putExtra("pkgName", pkgName);
        PendingIntent pi = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notice = new Notification.Builder(context)
                .setAutoCancel(true)
                .setOngoing(false)
                .setContentTitle(String.format(
                        context.getString(R.string.theme_installed_notification_title), themeName))
                .setContentText(context.getString(R.string.theme_installed_notification_text))
                .setContentIntent(pi)
                .setSmallIcon(R.drawable.ic_notify)
                .setWhen(System.currentTimeMillis())
                .build();
        nm.notify(pkgName.hashCode(), notice);
        Log.d("OHAI", "Notification created!");
    }

    public static void cancelNotificationForPackage(Context context, String pkgName) {
        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(pkgName.hashCode());
    }
}
