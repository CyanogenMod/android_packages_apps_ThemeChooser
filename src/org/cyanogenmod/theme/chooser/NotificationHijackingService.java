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

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

public class NotificationHijackingService extends NotificationListenerService {
    private static final String TAG = NotificationHijackingService.class.getName();
    private static final String GOOGLE_PLAY_PACKAGE_NAME = "com.android.vending";
    private static final String ACTION_INSTALLED =
            "com.android.vending.SUCCESSFULLY_INSTALLED_CLICKED";
    private static final String EXTRA_PACKAGE_NAME = "package_name";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (GOOGLE_PLAY_PACKAGE_NAME.equals(sbn.getPackageName())) {
            PendingIntent contentIntent = sbn.getNotification().contentIntent;
            if (contentIntent == null) return;
            Intent intent = contentIntent.getIntent();
            if (intent == null) return;
            String action = intent.getAction();
            if (ACTION_INSTALLED.equals(action)) {
                String pkgName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
                try {
                    PackageInfo pi = getPackageManager().getPackageInfo(pkgName, 0);
                    if (pi != null) {
                        if ((pi.themeInfos != null && pi.themeInfos.length > 0) ||
                                (pi.legacyThemeInfos != null && pi.legacyThemeInfos.length > 0)) {
                            cancelNotification(GOOGLE_PLAY_PACKAGE_NAME, sbn.getTag(), sbn.getId());
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
    }

    // ensure that this notification listener is enabled.
    // the service watches for google play notifications
    public static void ensureEnabled(Context context) {
        ComponentName me = new ComponentName(context, NotificationHijackingService.class);
        String meFlattened = me.flattenToString();

        String existingListeners = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ENABLED_NOTIFICATION_LISTENERS);

        if (!TextUtils.isEmpty(existingListeners)) {
            if (existingListeners.contains(meFlattened)) {
                return;
            } else {
                existingListeners += ":" + meFlattened;
            }
        } else {
            existingListeners = meFlattened;
        }

        Settings.Secure.putString(context.getContentResolver(),
                Settings.Secure.ENABLED_NOTIFICATION_LISTENERS,
                existingListeners);
    }
}