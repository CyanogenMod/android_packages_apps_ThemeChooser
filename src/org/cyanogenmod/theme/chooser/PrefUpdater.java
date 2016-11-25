/*
 * Copyright (C) 2016 The CyanogenMod Project
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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import cyanogenmod.preference.RemotePreference;
import cyanogenmod.preference.RemotePreferenceUpdater;
import cyanogenmod.providers.ThemesContract.MixnMatchColumns;
import cyanogenmod.providers.ThemesContract.ThemesColumns;
import org.cyanogenmod.internal.util.ThemeUtils;

public class PrefUpdater extends RemotePreferenceUpdater {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (ThemeUtils.ACTION_THEME_CHANGED.equals(intent.getAction())) {
            notifyChanged(context, "theme_settings");
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    protected String getSummary(Context context, String key) {
        if ("theme_settings".equals(key)) {
            final ContentResolver cr = context.getContentResolver();
            String overlayTheme = null;
            Cursor overlayCursor = cr.query(MixnMatchColumns.CONTENT_URI,
                    new String[] { MixnMatchColumns.COL_VALUE },
                    MixnMatchColumns.COL_KEY + "=?",
                    new String[] { MixnMatchColumns.KEY_OVERLAYS }, null);
            if (overlayCursor != null) {
                if (overlayCursor.moveToFirst()) {
                    overlayTheme = overlayCursor.getString(0);
                }
                overlayCursor.close();
            }

            if (overlayTheme != null) {
                // We got the theme's package name, now query its title
                Cursor titleCursor = cr.query(ThemesColumns.CONTENT_URI,
                        new String[] { ThemesColumns.TITLE },
                        ThemesColumns.PKG_NAME + "=?",
                        new String[] { overlayTheme }, null);
                if (titleCursor != null) {
                    String title = titleCursor.moveToFirst() ? titleCursor.getString(0) : null;
                    titleCursor.close();
                    return title;
                }
            }
        }
        return null;
    }
}
