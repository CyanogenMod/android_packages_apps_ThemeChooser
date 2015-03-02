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
import android.net.Uri;
import android.provider.ThemesContract;
import org.cyanogenmod.theme.util.NotificationHelper;

import java.util.Set;

public class AppReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Uri uri = intent.getData();
        String pkgName = uri != null ? uri.getSchemeSpecificPart() : null;
        String action = intent.getAction();

        if (ThemesContract.Intent.ACTION_THEME_INSTALLED.equals(action)) {
            NotificationHelper.postThemeInstalledNotification(context, pkgName);
        } else if (ThemesContract.Intent.ACTION_THEME_REMOVED.equals(action)) {
            NotificationHelper.cancelNotificationForPackage(context, pkgName);
        }
    }
}
