/*
 * Copyright (C) 2014 The Cyanogen, Inc
 */
package org.cyanogenmod.theme.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ThemeUtils;
import android.content.res.Resources;
import android.content.res.ThemeConfig;
import android.text.TextUtils;

import java.util.HashSet;
import java.util.Set;

public class PreferenceUtils {
    public static final String PREF_INSTALLED_THEMES_PROCESSING = "installed_themes_processing";

    public static SharedPreferences getSharedPreferences(Context context) {
        if (context == null) return null;
        return context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
    }

    public static Set<String> getInstalledThemesBeingProcessed(Context context) {
        SharedPreferences prefs = getSharedPreferences(context);
        if (prefs == null) return null;

        return prefs.getStringSet(PREF_INSTALLED_THEMES_PROCESSING, null);
    }

    public static void addThemeBeingProcessed(Context context, String pkgName) {
        SharedPreferences prefs = getSharedPreferences(context);
        if (prefs != null) {
            Set<String> updatedThemes = new HashSet<String>();
            Set<String> current = prefs.getStringSet(PREF_INSTALLED_THEMES_PROCESSING, null);
            if (current != null) {
                updatedThemes.addAll(current);
            }
            if (updatedThemes.add(pkgName)) {
                prefs.edit().putStringSet(PREF_INSTALLED_THEMES_PROCESSING, updatedThemes).apply();
            }
        }
    }

    public static void removeThemeBeingProcessed(Context context, String pkgName) {
        SharedPreferences prefs = getSharedPreferences(context);
        if (prefs != null) {
            Set<String> updatedThemes = new HashSet<String>();
            Set<String> current = prefs.getStringSet(PREF_INSTALLED_THEMES_PROCESSING, null);
            if (current != null) {
                updatedThemes.addAll(current);
            }
            if (updatedThemes.remove(pkgName)) {
                prefs.edit().putStringSet(PREF_INSTALLED_THEMES_PROCESSING, updatedThemes).apply();
            }
        }
    }
}
