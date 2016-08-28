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

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import cyanogenmod.app.ThemeVersion;
import cyanogenmod.providers.ThemesContract;
import cyanogenmod.providers.ThemesContract.PreviewColumns;
import cyanogenmod.providers.ThemesContract.ThemesColumns;

import static cyanogenmod.providers.ThemesContract.ThemesColumns.MODIFIES_ALARMS;
import static cyanogenmod.providers.ThemesContract.ThemesColumns.MODIFIES_BOOT_ANIM;
import static cyanogenmod.providers.ThemesContract.ThemesColumns.MODIFIES_LAUNCHER;
import static cyanogenmod.providers.ThemesContract.ThemesColumns.MODIFIES_LIVE_LOCK_SCREEN;
import static cyanogenmod.providers.ThemesContract.ThemesColumns.MODIFIES_LOCKSCREEN;
import static cyanogenmod.providers.ThemesContract.ThemesColumns.MODIFIES_NOTIFICATIONS;
import static cyanogenmod.providers.ThemesContract.ThemesColumns.MODIFIES_OVERLAYS;
import static cyanogenmod.providers.ThemesContract.ThemesColumns.MODIFIES_RINGTONES;
import static cyanogenmod.providers.ThemesContract.ThemesColumns.MODIFIES_STATUS_BAR;
import static cyanogenmod.providers.ThemesContract.ThemesColumns.MODIFIES_NAVIGATION_BAR;
import static cyanogenmod.providers.ThemesContract.ThemesColumns.MODIFIES_ICONS;
import static cyanogenmod.providers.ThemesContract.ThemesColumns.MODIFIES_FONTS;

public class CursorLoaderHelper {

    public static final int LOADER_ID_INVALID = -1;
    public static final int LOADER_ID_ALL = 0;
    public static final int LOADER_ID_STATUS_BAR = 1;
    public static final int LOADER_ID_FONT = 2;
    public static final int LOADER_ID_ICONS = 3;
    public static final int LOADER_ID_WALLPAPER = 4;
    public static final int LOADER_ID_NAVIGATION_BAR = 5;
    public static final int LOADER_ID_LOCKSCREEN = 6;
    public static final int LOADER_ID_STYLE = 7;
    public static final int LOADER_ID_BOOT_ANIMATION = 8;
    public static final int LOADER_ID_RINGTONE = 9;
    public static final int LOADER_ID_NOTIFICATION = 10;
    public static final int LOADER_ID_ALARM = 11;
    public static final int LOADER_ID_LIVE_LOCK_SCREEN = 12;
    public static final int LOADER_ID_INSTALLED_THEMES = 1000;
    public static final int LOADER_ID_APPLIED = 1001;

    private static final long DEFAULT_COMPONENT_ID = 0;

    private static int mThemeVersion = ThemeVersion.getVersion();

    public static Loader<Cursor> chooserActivityCursorLoader(Context context, int id,
            String appliedBaseTheme) {
        String selection = null;
        String selectionArgs[] = null;
        String sortOrder = null;
        String[] projection = null;
        Uri contentUri = null;

        switch (id) {
            case LOADER_ID_INSTALLED_THEMES:
                selection = ThemesColumns.PRESENT_AS_THEME + "=? AND " +
                        ThemesColumns.INSTALL_STATE + "=?";
                selectionArgs = new String[] { "1", "" + ThemesColumns.InstallState.INSTALLED};
                // sort in ascending order but make sure the "default" theme is always first
                sortOrder = "(" + ThemesColumns.IS_DEFAULT_THEME + "=1) DESC, "
                        + "(" + ThemesColumns.PKG_NAME + "='" + appliedBaseTheme + "') DESC, "
                        + ThemesColumns.INSTALL_TIME + " DESC";
                contentUri = ThemesColumns.CONTENT_URI;
                projection = new String[] {ThemesColumns.PKG_NAME, ThemesColumns.TITLE,
                        ThemesColumns.AUTHOR};
                break;
            case LOADER_ID_APPLIED:
                //TODO: Mix n match query should only be done once
                contentUri = ThemesContract.MixnMatchColumns.CONTENT_URI;
                selection = null;
                selectionArgs = null;
                break;
        }

        return new CursorLoader(context, contentUri, projection, selection,
                selectionArgs, sortOrder);
    }

    public static Loader<Cursor> componentSelectorCursorLoader(Context context, int id) {
        Uri uri = PreviewColumns.CONTENT_URI;
        String selection;
        String[] selectionArgs = { "1" };
        String[] projection = { ThemesColumns.TITLE, ThemesColumns.PKG_NAME };
        switch(id) {
            case LOADER_ID_STATUS_BAR:
                selection = MODIFIES_STATUS_BAR + "=?";
                projection = new String[] {
                        PreviewColumns.STATUSBAR_WIFI_ICON,
                        PreviewColumns.STATUSBAR_SIGNAL_ICON,
                        PreviewColumns.STATUSBAR_BLUETOOTH_ICON,
                        PreviewColumns.STATUSBAR_BACKGROUND,
                        PreviewColumns.STATUSBAR_BATTERY_CIRCLE,
                        PreviewColumns.STATUSBAR_BATTERY_LANDSCAPE,
                        PreviewColumns.STATUSBAR_BATTERY_PORTRAIT,
                        ThemesColumns.TITLE,
                        ThemesColumns.PKG_NAME
                };
                break;
            case LOADER_ID_NAVIGATION_BAR:
                selection = MODIFIES_NAVIGATION_BAR + "=?";
                projection = new String[] {
                        PreviewColumns.NAVBAR_BACK_BUTTON,
                        PreviewColumns.STATUSBAR_BACKGROUND,
                        ThemesColumns.TITLE,
                        ThemesColumns.PKG_NAME,
                };
                break;
            case LOADER_ID_FONT:
                // fonts don't have generated previews so use the ThemesColumns.CONTENT_URI
                uri = ThemesColumns.CONTENT_URI;
                selection = MODIFIES_FONTS + "=?";
                break;
            case LOADER_ID_ICONS:
                selection = MODIFIES_ICONS + "=?";
                projection = new String[] {
                        PreviewColumns.ICON_PREVIEW_1,
                        ThemesColumns.TITLE,
                        ThemesColumns.PKG_NAME
                };
                break;
            case LOADER_ID_STYLE:
                selection = MODIFIES_OVERLAYS + "=?";
                projection = new String[] {
                        PreviewColumns.STYLE_THUMBNAIL,
                        ThemesColumns.TITLE,
                        ThemesColumns.PKG_NAME
                };
                break;
            case LOADER_ID_WALLPAPER:
                selection = MODIFIES_LAUNCHER + "=?";
                if (mThemeVersion >= 3) {
                    uri = PreviewColumns.COMPONENTS_URI;
                    projection = new String[]{
                            PreviewColumns.WALLPAPER_THUMBNAIL,
                            ThemesColumns.TITLE,
                            ThemesColumns.PKG_NAME,
                            PreviewColumns.COMPONENT_ID
                    };
                } else {
                    projection = new String[]{
                            PreviewColumns.WALLPAPER_THUMBNAIL,
                            ThemesColumns.TITLE,
                            ThemesColumns.PKG_NAME
                    };
                }
                break;
            case LOADER_ID_BOOT_ANIMATION:
                selection = MODIFIES_BOOT_ANIM + "=?";
                projection = new String[] {
                        PreviewColumns.BOOTANIMATION_THUMBNAIL,
                        ThemesColumns.TITLE,
                        ThemesColumns.PKG_NAME
                };
                break;
            case LOADER_ID_RINGTONE:
                selection = MODIFIES_RINGTONES + "=?";
                break;
            case LOADER_ID_NOTIFICATION:
                selection = MODIFIES_NOTIFICATIONS + "=?";
                break;
            case LOADER_ID_ALARM:
                selection = MODIFIES_ALARMS + "=?";
                break;
            case LOADER_ID_LOCKSCREEN:
                selection = MODIFIES_LOCKSCREEN + "=?";
                selectionArgs = new String[] { "1" };
                if (mThemeVersion >= 3) {
                    projection = new String[]{
                            PreviewColumns.LOCK_WALLPAPER_THUMBNAIL,
                            PreviewColumns.LIVE_LOCK_SCREEN_THUMBNAIL,
                            ThemesColumns.TITLE,
                            ThemesColumns.PKG_NAME,
                            ThemesColumns.MODIFIES_LIVE_LOCK_SCREEN,
                            PreviewColumns.COMPONENT_ID
                    };
                } else {
                    projection = new String[]{
                            PreviewColumns.LOCK_WALLPAPER_THUMBNAIL,
                            PreviewColumns.LIVE_LOCK_SCREEN_THUMBNAIL,
                            ThemesColumns.TITLE,
                            ThemesColumns.MODIFIES_LIVE_LOCK_SCREEN,
                            ThemesColumns.PKG_NAME
                    };
                }
                break;
            case LOADER_ID_LIVE_LOCK_SCREEN:
                selection = MODIFIES_LIVE_LOCK_SCREEN + "=?";
                selectionArgs = new String[] { "1" };
                if (mThemeVersion >= 3) {
                    projection = new String[]{
                            PreviewColumns.LIVE_LOCK_SCREEN_THUMBNAIL,
                            ThemesColumns.TITLE,
                            ThemesColumns.PKG_NAME,
                            ThemesColumns.MODIFIES_LIVE_LOCK_SCREEN,
                            PreviewColumns.COMPONENT_ID
                    };
                } else {
                    projection = new String[]{
                            PreviewColumns.LOCK_WALLPAPER_THUMBNAIL,
                            PreviewColumns.LIVE_LOCK_SCREEN_THUMBNAIL,
                            ThemesColumns.TITLE,
                            ThemesColumns.MODIFIES_LIVE_LOCK_SCREEN,
                            ThemesColumns.PKG_NAME
                    };
                }
                break;
            default:
                return null;
        }
        // sort in ascending order but make sure the "default" theme is always first
        String sortOrder = "(" + ThemesContract.ThemesColumns.IS_DEFAULT_THEME + "=1) DESC, "
                + ThemesContract.ThemesColumns.TITLE + " ASC";
        return new CursorLoader(context, uri, projection, selection, selectionArgs, sortOrder);
    }

    public static Loader<Cursor> myThemeFragmentCursorLoader(Context context, int id) {
        Uri uri;
        String[] projection;
        projection = new String[]{
                PreviewColumns.WALLPAPER_PREVIEW,
                PreviewColumns.STATUSBAR_BACKGROUND,
                PreviewColumns.STATUSBAR_WIFI_ICON,
                PreviewColumns.STATUSBAR_WIFI_COMBO_MARGIN_END,
                PreviewColumns.STATUSBAR_BLUETOOTH_ICON,
                PreviewColumns.STATUSBAR_SIGNAL_ICON,
                PreviewColumns.STATUSBAR_CLOCK_TEXT_COLOR,
                PreviewColumns.STATUSBAR_BATTERY_CIRCLE,
                PreviewColumns.STATUSBAR_BATTERY_LANDSCAPE,
                PreviewColumns.STATUSBAR_BATTERY_PORTRAIT,
                PreviewColumns.NAVBAR_BACK_BUTTON,
                PreviewColumns.NAVBAR_HOME_BUTTON,
                PreviewColumns.NAVBAR_RECENT_BUTTON,
                PreviewColumns.ICON_PREVIEW_1,
                PreviewColumns.ICON_PREVIEW_2,
                PreviewColumns.ICON_PREVIEW_3,
                PreviewColumns.LOCK_WALLPAPER_PREVIEW,
                PreviewColumns.STYLE_PREVIEW,
                PreviewColumns.NAVBAR_BACKGROUND,
                PreviewColumns.LIVE_LOCK_SCREEN_PREVIEW
        };
        uri = PreviewColumns.APPLIED_URI;
        return new CursorLoader(context, uri, projection, null, null, null);
    }

    public static Loader<Cursor> themeFragmentCursorLoader(Context context, int id, String pkgName,
            long componentId) {
        Uri uri = PreviewColumns.CONTENT_URI;
        String selection = ThemesContract.ThemesColumns.PKG_NAME + "= ?";
        String[] selectionArgs = new String[] { pkgName };
        String[] projection = null;
        switch (id) {
            case LOADER_ID_ALL:
                if (mThemeVersion >= 3) {
                    // Load all default component previews (component_id == 0)
                    selection += " AND " + PreviewColumns.COMPONENT_ID + "=?";
                    selectionArgs = new String[] { pkgName, String.valueOf(DEFAULT_COMPONENT_ID) };
                } else {
                    // SQL query will fail if we ask for PreviewColumns.COMPONENT_ID, don't add it.
                    selectionArgs = new String[]{pkgName};
                }
                projection = new String[] {
                        ThemesColumns.PKG_NAME,
                        ThemesColumns.TITLE,
                        ThemesColumns.AUTHOR,
                        ThemesColumns.WALLPAPER_URI,
                        ThemesColumns.HOMESCREEN_URI,
                        ThemesColumns.TARGET_API,
                        // Theme abilities
                        ThemesColumns.MODIFIES_LAUNCHER,
                        ThemesColumns.MODIFIES_LOCKSCREEN,
                        ThemesColumns.MODIFIES_ALARMS,
                        ThemesColumns.MODIFIES_BOOT_ANIM,
                        ThemesColumns.MODIFIES_FONTS,
                        ThemesColumns.MODIFIES_ICONS,
                        ThemesColumns.MODIFIES_NAVIGATION_BAR,
                        ThemesColumns.MODIFIES_OVERLAYS,
                        ThemesColumns.MODIFIES_RINGTONES,
                        ThemesColumns.MODIFIES_STATUS_BAR,
                        ThemesColumns.MODIFIES_NOTIFICATIONS,
                        //Previews
                        PreviewColumns.WALLPAPER_PREVIEW,
                        PreviewColumns.STATUSBAR_BACKGROUND,
                        PreviewColumns.STATUSBAR_WIFI_ICON,
                        PreviewColumns.STATUSBAR_WIFI_COMBO_MARGIN_END,
                        PreviewColumns.STATUSBAR_BLUETOOTH_ICON,
                        PreviewColumns.STATUSBAR_SIGNAL_ICON,
                        PreviewColumns.STATUSBAR_CLOCK_TEXT_COLOR,
                        PreviewColumns.STATUSBAR_BATTERY_CIRCLE,
                        PreviewColumns.STATUSBAR_BATTERY_LANDSCAPE,
                        PreviewColumns.STATUSBAR_BATTERY_PORTRAIT,
                        PreviewColumns.NAVBAR_BACK_BUTTON,
                        PreviewColumns.NAVBAR_HOME_BUTTON,
                        PreviewColumns.NAVBAR_RECENT_BUTTON,
                        PreviewColumns.ICON_PREVIEW_1,
                        PreviewColumns.ICON_PREVIEW_2,
                        PreviewColumns.ICON_PREVIEW_3,
                        PreviewColumns.LOCK_WALLPAPER_PREVIEW,
                        PreviewColumns.STYLE_PREVIEW,
                        PreviewColumns.LIVE_LOCK_SCREEN_PREVIEW
                };
                break;
            case LOADER_ID_STATUS_BAR:
                projection = new String[] {
                        ThemesColumns.PKG_NAME,
                        ThemesColumns.TITLE,
                        PreviewColumns.STATUSBAR_BACKGROUND,
                        PreviewColumns.STATUSBAR_WIFI_ICON,
                        PreviewColumns.STATUSBAR_WIFI_COMBO_MARGIN_END,
                        PreviewColumns.STATUSBAR_BLUETOOTH_ICON,
                        PreviewColumns.STATUSBAR_SIGNAL_ICON,
                        PreviewColumns.STATUSBAR_CLOCK_TEXT_COLOR,
                        PreviewColumns.STATUSBAR_BATTERY_CIRCLE,
                        PreviewColumns.STATUSBAR_BATTERY_LANDSCAPE,
                        PreviewColumns.STATUSBAR_BATTERY_PORTRAIT
                };
                break;
            case LOADER_ID_FONT:
                uri = ThemesColumns.CONTENT_URI;
                projection = new String[] {
                        ThemesColumns.PKG_NAME,
                        ThemesColumns.TITLE
                };
                break;
            case LOADER_ID_ICONS:
                projection = new String[] {
                        ThemesColumns.PKG_NAME,
                        ThemesColumns.TITLE,
                        PreviewColumns.ICON_PREVIEW_1,
                        PreviewColumns.ICON_PREVIEW_2,
                        PreviewColumns.ICON_PREVIEW_3,
                };
                break;
            case LOADER_ID_WALLPAPER:
                if (mThemeVersion >= 3) {
                    uri = PreviewColumns.COMPONENTS_URI;
                    // Load specified wallpaper previews (component_id is specified)
                    selection += " AND " + PreviewColumns.COMPONENT_ID + "=?";
                    selectionArgs = new String[]{pkgName, String.valueOf(componentId)};
                    projection = new String[]{
                            ThemesColumns.PKG_NAME,
                            ThemesColumns.TITLE,
                            PreviewColumns.WALLPAPER_PREVIEW,
                            PreviewColumns.COMPONENT_ID
                    };
                } else {
                    projection = new String[]{
                            ThemesColumns.PKG_NAME,
                            ThemesColumns.TITLE,
                            PreviewColumns.WALLPAPER_PREVIEW
                    };
                }
                break;
            case LOADER_ID_NAVIGATION_BAR:
                projection = new String[] {
                        ThemesColumns.PKG_NAME,
                        ThemesColumns.TITLE,
                        PreviewColumns.STATUSBAR_BACKGROUND,
                        PreviewColumns.NAVBAR_BACK_BUTTON,
                        PreviewColumns.NAVBAR_HOME_BUTTON,
                        PreviewColumns.NAVBAR_RECENT_BUTTON
                };
                break;
            case LOADER_ID_LOCKSCREEN:
                projection = new String[]{
                        ThemesColumns.PKG_NAME,
                        ThemesColumns.TITLE,
                        PreviewColumns.LOCK_WALLPAPER_PREVIEW,
                };
                break;
            case LOADER_ID_LIVE_LOCK_SCREEN:
                projection = new String[]{
                        ThemesColumns.PKG_NAME,
                        ThemesColumns.TITLE,
                        ThemesColumns.MODIFIES_LIVE_LOCK_SCREEN,
                        PreviewColumns.LIVE_LOCK_SCREEN_PREVIEW
                };
                break;
            case LOADER_ID_STYLE:
                projection = new String[] {
                        ThemesColumns.PKG_NAME,
                        ThemesColumns.TITLE,
                        PreviewColumns.STYLE_PREVIEW
                };
                break;
            case LOADER_ID_BOOT_ANIMATION:
                projection = new String[] {
                        ThemesColumns.PKG_NAME,
                        ThemesColumns.TITLE
                };
                break;
            case LOADER_ID_RINGTONE:
            case LOADER_ID_NOTIFICATION:
            case LOADER_ID_ALARM:
                projection = new String[] {
                        ThemesColumns.PKG_NAME,
                        ThemesColumns.TITLE
                };
                break;
        }
        return new CursorLoader(context, uri, projection, selection, selectionArgs, null);
    }

    public static Object[] getRowFromCursor(Cursor cursor) {
        Object[] row = null;
        if (cursor != null) {
            int colCount = cursor.getColumnCount();
            row = new Object[colCount];
            for (int indx = 0; indx < colCount; indx++) {
                row[indx] = getFieldValueFromRow(cursor, indx);
            }
        }
        return row;
    }

    public static Object getFieldValueFromRow(Cursor cursor, int position) {
        switch (cursor.getType(position)) {
            case Cursor.FIELD_TYPE_BLOB: return cursor.getBlob(position);
            case Cursor.FIELD_TYPE_FLOAT: return cursor.getFloat(position);
            case Cursor.FIELD_TYPE_INTEGER: return cursor.getInt(position);
            case Cursor.FIELD_TYPE_STRING: return cursor.getString(position);
            case Cursor.FIELD_TYPE_NULL:
            default:
                return null;
        }
    }
}