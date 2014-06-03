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

import android.app.ActivityManager;
import android.app.IconPackHelper;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;

/**
 * This class handles all the logic to build a preview icon
 * If the system currently has a theme applied we do NOT
 * want this code to be impacted by it. So code in this
 * class creates special "no theme attached" resource objects
 * to retrieve objects from.
 */
public class IconPreviewHelper {
    private static final String TAG = IconPreviewHelper.class.getSimpleName();
    private final static float ICON_SCALE_FACTOR = 1.3f; //Arbitrary. Looks good

    private Context mContext;
    private DisplayMetrics mDisplayMetrics;
    private Configuration mConfiguration;
    private int mIconDpi = 0;
    private String mThemePkgName;
    private IconPackHelper mIconPackHelper;
    private int mIconSize;

    /**
     * @param themePkgName - The package name of the theme we wish to preview
     */
    public IconPreviewHelper(Context context, String themePkgName) {
        mContext = context;
        mDisplayMetrics = context.getResources().getDisplayMetrics();
        mConfiguration = context.getResources().getConfiguration();
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        mIconDpi = (int) (am.getLauncherLargeIconDensity() * ICON_SCALE_FACTOR);
        mThemePkgName = themePkgName;
        mIconPackHelper = new IconPackHelper(mContext);
        try {
            mIconPackHelper.loadIconPack(mThemePkgName);
        } catch (NameNotFoundException e) {}
        mIconSize = (int) (am.getLauncherLargeIconSize() * ICON_SCALE_FACTOR);
    }

    /**
     * Returns the actual label name for a given component
     * If the activity does not have a label it will return app's label
     * If neither has a label returns empty string
     */
    public String getLabel(ComponentName component) {
        String label = "";
        try {
            PackageManager pm = mContext.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(component.getPackageName(), 0);
            ActivityInfo activityInfo = pm.getActivityInfo(component, 0);

            AssetManager assets = new AssetManager();
            assets.addAssetPath(appInfo.publicSourceDir);
            Resources res = new Resources(assets, mDisplayMetrics, mConfiguration);

            if (activityInfo.labelRes != 0) {
                label = res.getString(activityInfo.labelRes);
            } else if (appInfo.labelRes != 0) {
                label = res.getString(appInfo.labelRes);
            }
        } catch(NameNotFoundException exception) {
            Log.e(TAG, "unable to find pkg for " + component.toString());
        }
        return label;
    }

    /**
     * Returns the icon for the given component regardless of the system's
     * currently applied theme. If the preview theme does not support the icon, then
     * return the system default icon.
     */
    public Drawable getIcon(ComponentName component) {
        String packageName = component.getPackageName();
        String activityName = component.getClassName();
        Drawable icon = getThemedIcon(packageName, activityName);
        if (icon == null) {
            icon = getIconNoTheme(packageName, activityName);
        }
        if (icon != null) {
            icon.setBounds(0, 0, mIconSize, mIconSize);
        }
        return icon;
    }

    private Drawable getThemedIcon(String pkgName, String activityName) {
        Drawable drawable = null;
        ActivityInfo info = new ActivityInfo();
        info.packageName = pkgName;
        info.name = activityName;
        drawable = mIconPackHelper.getDrawableForActivityWithDensity(info, mIconDpi);

        return drawable;
    }

    private Drawable getIconNoTheme(String pkgName, String activityName) {
        Drawable drawable = null;
        ComponentName component = new ComponentName(pkgName, activityName);
        PackageManager pm = mContext.getPackageManager();
        try {
            ActivityInfo info = pm.getActivityInfo(component, 0);
            ApplicationInfo appInfo = pm.getApplicationInfo(pkgName, 0);

            AssetManager assets = new AssetManager();
            assets.addAssetPath(appInfo.publicSourceDir);
            Resources res = new Resources(assets, mDisplayMetrics, mConfiguration);

            final int iconId = info.icon != 0 ? info.icon : appInfo.icon;
            drawable = getFullResIcon(res, iconId);
        } catch (NameNotFoundException e2) {
           Log.w(TAG, "Unable to get the icon for " + pkgName + " using default");
        }
        drawable = (drawable != null) ? drawable : getFullResDefaultActivityIcon();
        return drawable;
    }

    private Drawable getFullResIcon(Resources resources, int iconId) {
        Drawable d;
        try {
            d = resources.getDrawableForDensity(iconId, mIconDpi);
        } catch (Resources.NotFoundException e) {
            d = null;
        }
        return (d != null) ? d : getFullResDefaultActivityIcon();
    }

    private Drawable getFullResDefaultActivityIcon() {
        return getFullResIcon(Resources.getSystem(), android.R.mipmap.sym_def_app_icon);
    }
}
