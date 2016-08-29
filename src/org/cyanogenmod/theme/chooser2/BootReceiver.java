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

package org.cyanogenmod.theme.chooser2;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class BootReceiver extends BroadcastReceiver {
    private static final String CHOOSER_PKG_NAME = "org.cyanogenmod.theme.chooser";
    private static final String CHOOSER_ACTIVITY = "org.cyanogenmod.theme.chooser.ChooserLauncher";

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
