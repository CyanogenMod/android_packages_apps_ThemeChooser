/*
 * Copyright (C) 2014 The Cyanogen, Inc
 */
package com.cyngn.theme.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ThemeUtils;
import android.content.res.Resources;
import android.content.res.ThemeConfig;
import android.text.TextUtils;

import java.util.HashSet;
import java.util.Set;

public class PreferenceUtils {
    public static final String PREF_APPLIED_BASE_THEME = "applied_base_theme";
    public static final String PREF_UPDATED_THEMES = "updated_themes";

    public static SharedPreferences getSharedPreferences(Context context) {
        if (context == null) return null;
        return context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
    }

    public static String getAppliedBaseTheme(Context context) {
        SharedPreferences prefs = getSharedPreferences(context);
        if (prefs == null) return null;

        final Resources res = context.getResources();
        final ThemeConfig config = res.getConfiguration().themeConfig;
        String appliedTheme = config != null ? config.getOverlayPkgName() : null;
        return prefs.getString(PREF_APPLIED_BASE_THEME,
                (!TextUtils.isEmpty(appliedTheme)) ? appliedTheme :
                ThemeUtils.getDefaultThemePackageName(context));
    }

    public static void setAppliedBaseTheme(Context context, String pkgName) {
        SharedPreferences prefs = getSharedPreferences(context);
        if (prefs != null) {
            prefs.edit().putString(PREF_APPLIED_BASE_THEME, pkgName).apply();
        }
    }

    public static Set<String> getUpdatedThemes(Context context) {
        SharedPreferences prefs = getSharedPreferences(context);
        if (prefs == null) return null;

        return prefs.getStringSet(PREF_UPDATED_THEMES, null);
    }

    public static void addUpdatedTheme(Context context, String pkgName) {
        SharedPreferences prefs = getSharedPreferences(context);
        if (prefs != null) {
            Set<String> updatedThemes = new HashSet<String>();
            Set<String> current = prefs.getStringSet(PREF_UPDATED_THEMES, null);
            if (current != null) {
                updatedThemes.addAll(current);
            }
            if (updatedThemes.add(pkgName)) {
                prefs.edit().putStringSet(PREF_UPDATED_THEMES, updatedThemes).apply();
            }
        }
    }

    public static void removeUpdatedTheme(Context context, String pkgName) {
        SharedPreferences prefs = getSharedPreferences(context);
        if (prefs != null) {
            Set<String> updatedThemes = new HashSet<String>();
            Set<String> current = prefs.getStringSet(PREF_UPDATED_THEMES, null);
            if (current != null) {
                updatedThemes.addAll(current);
            }
            if (updatedThemes.remove(pkgName)) {
                prefs.edit().putStringSet(PREF_UPDATED_THEMES, updatedThemes).apply();
            }
        }
    }

    public static boolean hasThemeBeenUpdated(Context context, String pkgName) {
        Set<String> updatedThemes = getUpdatedThemes(context);
        return updatedThemes != null && updatedThemes.contains(pkgName);
    }
}
