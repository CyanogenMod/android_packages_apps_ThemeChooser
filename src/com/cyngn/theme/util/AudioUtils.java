/*
 * Copyright (C) 2014 The Cyanogen, Inc
 */
package com.cyngn.theme.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ThemeUtils;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.ThemeConfig;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class AudioUtils {
    private static final String TAG = AudioUtils.class.getSimpleName();

    public static void loadThemeAudible(Context context, int type, String pkgName, MediaPlayer mp)
            throws PackageManager.NameNotFoundException {
        if (ThemeConfig.HOLO_DEFAULT.equals(pkgName)) {
            loadSystemAudible(type, mp);
            return;
        }
        PackageInfo pi = context.getPackageManager().getPackageInfo(pkgName, 0);
        Context themeCtx = context.createPackageContext(pkgName, 0);
        if (pi.isLegacyThemeApk) {
            loadLegacyThemeAudible(themeCtx, type, pi, mp);
            return;
        }
        AssetManager assetManager = themeCtx.getAssets();
        String assetPath;
        switch (type) {
            case RingtoneManager.TYPE_ALARM:
                assetPath = "alarms";
                break;
            case RingtoneManager.TYPE_NOTIFICATION:
                assetPath = "notifications";
                break;
            case RingtoneManager.TYPE_RINGTONE:
                assetPath = "ringtones";
                break;
            default:
                assetPath = null;
                break;
        }
        if (assetPath != null) {
            try {
                String[] assetList = assetManager.list(assetPath);
                if (assetList != null && assetList.length > 0) {
                    AssetFileDescriptor afd = assetManager.openFd(assetPath
                            + File.separator + assetList[0]);
                    if (mp != null) {
                        mp.reset();
                        mp.setDataSource(afd.getFileDescriptor(),
                                afd.getStartOffset(), afd.getLength());
                        mp.prepare();
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Unable to load sound for " + pkgName, e);
            }
        }
    }

    public static void loadLegacyThemeAudible(Context themeCtx, int type, PackageInfo pi,
            MediaPlayer mp) {
        if (pi.legacyThemeInfos == null || pi.legacyThemeInfos.length == 0)
            return;
        AssetManager assetManager = themeCtx.getAssets();
        String assetPath;
        switch (type) {
            case RingtoneManager.TYPE_NOTIFICATION:
                assetPath = pi.legacyThemeInfos[0].notificationFileName;
                break;
            case RingtoneManager.TYPE_RINGTONE:
                assetPath = pi.legacyThemeInfos[0].ringtoneFileName;
                break;
            default:
                assetPath = null;
                break;
        }
        if (assetPath != null) {
            try {
                AssetFileDescriptor afd = assetManager.openFd(assetPath);
                if (mp != null) {
                    mp.reset();
                    mp.setDataSource(afd.getFileDescriptor(),
                            afd.getStartOffset(), afd.getLength());
                    mp.prepare();
                }
            } catch (IOException e) {
                Log.e(TAG, "Unable to load legacy sound for " + pi.packageName, e);
            }
        }
    }

    public static void loadSystemAudible(int type, MediaPlayer mp) {
        final String audiblePath = ThemeUtils.getDefaultAudiblePath(type);
        if (audiblePath != null && (new File(audiblePath)).exists()) {
            try {
                mp.reset();
                mp.setDataSource(audiblePath);
                mp.prepare();
            } catch (IOException e) {
                Log.e(TAG, "Unable to load system sound " + audiblePath, e);
            }
        }
    }

    public static Uri loadDefaultAudible(Context context, int type, MediaPlayer mp)
            throws IOException {
        Uri ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, type);
        mp.reset();
        mp.setDataSource(context, ringtoneUri);
        mp.prepare();

        return ringtoneUri;
    }
}
