/*
 * Copyright (C) 2014 Cyanogen, Inc.
 */
package com.cyngn.theme.util;

import android.app.ActivityManager;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ThemeUtils;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.ThemeConfig;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.RemoteException;
import android.provider.Settings;
import android.provider.ThemesContract;
import android.util.Log;
import android.util.TypedValue;
import android.view.IWindowManager;
import android.view.WindowManager;

import android.view.WindowManagerGlobal;
import com.cyngn.theme.chooser.ChooserActivity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public class Utils {
    private static final String TAG = Utils.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final String OVERLAY_BASE_PATH = "overlays" + File.separator;

    public static Bitmap decodeFile(String path, int reqWidth, int reqHeight) {
        BitmapFactory.Options opts = new BitmapFactory.Options();

        // Determine insample size
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, opts);
        opts.inSampleSize = calculateInSampleSize(opts, reqWidth, reqHeight);

        // Decode the bitmap, regionally if necessary
        Bitmap bitmap = null;
        opts.inJustDecodeBounds = false;
        Rect rect = getCropRectIfNecessary(opts, reqWidth, reqHeight);
        try {
            if (rect != null) {
                BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(path, false);
                // Check if we can downsample more now that we cropped
                opts.inSampleSize = calculateInSampleSize(rect.width(), rect.height(),
                        reqWidth, reqHeight);
                bitmap = decoder.decodeRegion(rect, opts);
            } else {
                bitmap = BitmapFactory.decodeFile(path, opts);
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable to open resource in path" + path, e);
        }
        return bitmap;
    }

    public static Bitmap decodeResource(Resources res, int resId, int reqWidth, int reqHeight) {
        BitmapFactory.Options opts = new BitmapFactory.Options();

        // Determine insample size
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, opts);
        opts.inSampleSize = calculateInSampleSize(opts, reqWidth, reqHeight);

        // Decode the bitmap, regionally if necessary
        Bitmap bitmap = null;
        opts.inJustDecodeBounds = false;
        Rect rect = getCropRectIfNecessary(opts, reqWidth, reqHeight);

        InputStream stream = null;
        try {
            if (rect != null) {
                stream = res.openRawResource(resId, new TypedValue());
                if (stream == null) return null;
                BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(stream, false);
                // Check if we can downsample a little more now that we cropped
                opts.inSampleSize = calculateInSampleSize(rect.width(), rect.height(),
                        reqWidth, reqHeight);
                bitmap = decoder.decodeRegion(rect, opts);
            } else {
                bitmap = BitmapFactory.decodeResource(res, resId, opts);
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable to open resource " + resId, e);
        } finally {
            closeQuiet(stream);
        }
        return bitmap;
    }


    public static Bitmap getBitmapFromAsset(Context ctx, String path,int reqWidth, int reqHeight) {
        if (ctx == null || path == null)
            return null;

        String ASSET_BASE = "file:///android_asset/";
        path = path.substring(ASSET_BASE.length());


        Bitmap bitmap = null;
        try {
            AssetManager assets = ctx.getAssets();
            InputStream is = assets.open(path);

            // Determine insample size
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, opts);
            opts.inSampleSize = calculateInSampleSize(opts, reqWidth, reqHeight);
            is.close();

            // Decode the bitmap, regionally if neccessary
            is = assets.open(path);
            opts.inJustDecodeBounds = false;
            Rect rect = getCropRectIfNecessary(opts, reqWidth, reqHeight);
            if (rect != null) {
                BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(is, false);
                // Check if we can downsample a little more now that we cropped
                opts.inSampleSize = calculateInSampleSize(rect.width(), rect.height(),
                        reqWidth, reqHeight);
                bitmap = decoder.decodeRegion(rect, opts);
            } else {
                bitmap = BitmapFactory.decodeStream(is);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }


    /**
     * For excessively large images with an awkward ratio we
     * will want to crop them
     * @return
     */
    public static Rect getCropRectIfNecessary(
            BitmapFactory.Options options,int reqWidth, int reqHeight) {
        Rect rect = null;
        // Determine downsampled size
        int width = options.outWidth / options.inSampleSize;
        int height = options.outHeight / options.inSampleSize;

        if ((reqHeight * 1.5 < height)) {
            int bottom = height/ 4;
            int top = bottom + height/2;
            rect = new Rect(0, bottom, width, top);
        } else if ((reqWidth * 1.5 < width)) {
            int left = width / 4;
            int right = left + height/2;
            rect = new Rect(left, 0, right, height);
        }
        return rect;
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        return calculateInSampleSize(options.outWidth, options.outHeight, reqWidth, reqHeight);
    }

    // Modified from original source:
    // http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
    public static int calculateInSampleSize(
            int decodeWidth, int decodeHeight, int reqWidth, int reqHeight) {
        // Raw height and width of image
        int inSampleSize = 1;

        if (decodeHeight > reqHeight || decodeWidth > reqWidth) {
            final int halfHeight = decodeHeight / 2;
            final int halfWidth = decodeWidth / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight &&
                    (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static InputStream getInputStreamFromAsset(
            Context ctx, String path) throws IOException {
        if (ctx == null || path == null)
            return null;
        InputStream is = null;
        String ASSET_BASE = "file:///android_asset/";
        path = path.substring(ASSET_BASE.length());
        AssetManager assets = ctx.getAssets();
        is = assets.open(path);
        return is;
    }

    public static void copy(InputStream is, OutputStream os) throws IOException {
        final byte[] bytes = new byte[4096];
        int len;
        while ((len = is.read(bytes)) > 0) {
            os.write(bytes, 0, len);
        }
    }

    public static void closeQuiet(InputStream stream) {
        if (stream == null)
            return;
        try {
            stream.close();
        } catch (IOException e) {
        }
    }

    public static void closeQuiet(OutputStream stream) {
        if (stream == null)
            return;
        try {
            stream.close();
        } catch (IOException e) {
        }
    }

    //Note: will not delete populated subdirs
    public static void deleteFilesInDir(String dirPath) {
        File fontDir = new File(dirPath);
        File[] files = fontDir.listFiles();
        if (files != null) {
            for(File file : fontDir.listFiles()) {
                file.delete();
            }
        }
    }

    public static boolean hasNavigationBar(Context context) {
        boolean needsNavigationBar = false;
        try {
            IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
            needsNavigationBar = wm.needsNavigationBar();
        } catch (RemoteException e) {
        }
        // Need to also check for devices with hardware keys where the user has chosen to use
        // the on screen navigation bar
        needsNavigationBar = needsNavigationBar ||
                Settings.Secure.getInt(context.getContentResolver(),
                        Settings.Secure.DEV_FORCE_SHOW_NAVBAR, 0) == 1;
        return needsNavigationBar;
    }

    public static Bitmap loadBitmapBlob(Cursor cursor, int columnIdx) {
        if (columnIdx < 0) {
            Log.w(TAG, "loadBitmapBlob(): Invalid index provided, returning null");
            return null;
        }
        byte[] blob = cursor.getBlob(columnIdx);
        if (blob == null) return null;
        return BitmapFactory.decodeByteArray(blob, 0, blob.length);
    }

    public static String getBatteryKey(int type) {
        switch(type) {
            case 2:
                return ThemesContract.PreviewColumns.KEY_STATUSBAR_BATTERY_CIRCLE;
            case 5:
                return ThemesContract.PreviewColumns.KEY_STATUSBAR_BATTERY_LANDSCAPE;
            default:
                return ThemesContract.PreviewColumns.KEY_STATUSBAR_BATTERY_PORTRAIT;
        }
    }

    public static Bitmap getRegularWallpaperBitmap(Context context) {
        WallpaperManager wm = WallpaperManager.getInstance(context);

        Bitmap bitmap = null;
        // desktop wallpaper here
        Bitmap wallpaper = wm.getBitmap();
        if (wallpaper == null) {
            return null;
        }

        Point size = new Point();
        WindowManager windowManager =
                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealSize(size);

        final int dw = size.x;
        final int dh = size.y;

        // Center the scaled image
        float scale = Math.max(1f, Math.max(dw / (float) wallpaper.getWidth(),
                dh / (float) wallpaper.getHeight()));

        final int scaledWidth = Math.round((wallpaper.getWidth() * scale));
        final int scaledHeight = Math.round((wallpaper.getHeight() * scale));

        // TODO: set xOffset to wm.getLastWallpaperX() once available
        int xOffset = wm.getLastWallpaperX();
        // x offset
        if (xOffset == -1) {
            xOffset = 0;
        } else {
            xOffset *= -1;
        }

        // y offsets
        // TODO: set yOffset to wm.getLastWallpaperY() once available
        int yOffset = wm.getLastWallpaperY();
        if (yOffset == -1) {
            yOffset = 0;
        } else {
            yOffset *= -1;
        }

        if (DEBUG) {
            Log.d(TAG, "scale: " + scale);
            Log.d(TAG, "scaledWidth: " + scaledWidth);
            Log.d(TAG, "scaledHeight: " + scaledHeight);
            Log.d(TAG, "wallpaper size: width: " + wallpaper.getWidth() +
                    ", height: " + wallpaper.getHeight());
            Log.d(TAG, "xOffset: " + xOffset);
            Log.d(TAG, "yOffset: " + yOffset);
        }

        try {
            if (wallpaper.getHeight() < dh) {
                // need to scale it up vertically

                if (wallpaper.getHeight() > wallpaper.getWidth()) {
                    // handle portrait wallpaper
                    float diff = scaledWidth - dw;
                    int diffhalf = Math.round(diff / 2);

                    bitmap = Bitmap.createScaledBitmap(wallpaper, scaledWidth, scaledHeight, true);
                    bitmap = Bitmap.createBitmap(bitmap, diffhalf, 0, dw, dh);
                    bitmap = Bitmap.createBitmap(bitmap, xOffset, 0, dw, dh);
                } else {
                    int goldenWidth = Math.round(wallpaper.getHeight() * 1.125f);
                    int spaceA = (wallpaper.getWidth() - goldenWidth) / 2;
                    int spaceB = (goldenWidth - Math.round(dh / scale)) / 2;

                    bitmap = Bitmap.createBitmap(wallpaper, spaceA, 0, goldenWidth,
                            wallpaper.getHeight());
                    int left = spaceB + Math.round(xOffset / scale);
                    bitmap = Bitmap.createBitmap(bitmap, left, 0, Math.round(dw / scale),
                            Math.round(dh / scale));
                }

            } else if (wallpaper.getWidth() < dw) {
                // need to scale it up horizontally

                if (wallpaper.getHeight() > wallpaper.getWidth()) {
                    // handle portrait wallpaper
                    return wallpaper;

                } else {
                    // handle landscape wallpaper
                    float diff = wallpaper.getHeight() - wallpaper.getWidth();
                    int diffhalf = Math.round(diff / 2);

                    if (diffhalf < 0) {
                        return wallpaper;
                    }

                    bitmap = Bitmap.createBitmap(
                            wallpaper, diffhalf, 0,
                            wallpaper.getWidth(), wallpaper.getWidth());

                    // blow it up
                    bitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledWidth, true);

                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, dw, dh);
                }

            } else {
                // sometimes the wallpaper manager gives incorrect offsets,
                // and adds like 200 pixels randomly. If it's bigger than we can handle, calculate
                // our own :)
                if (yOffset + dh > wallpaper.getHeight()) {
                    yOffset = (wallpaper.getHeight() - dh) / 2;
                }
                if (xOffset + dw > wallpaper.getWidth()) {
                    yOffset = (wallpaper.getWidth() - dw) / 2;
                }
                bitmap = Bitmap.createBitmap(wallpaper, xOffset, yOffset, dw, dh);
            }
        } catch (IllegalArgumentException e) {
            // Cropping/resizing failed so return the original
            bitmap = wallpaper;
        }
        return bitmap;
    }

    public static boolean isRecentTaskHome(Context context) {
        final ActivityManager am =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        final List<ActivityManager.RecentTaskInfo> recentTasks = am.getRecentTasks(
                2, ActivityManager.RECENT_IGNORE_UNAVAILABLE);
        if (recentTasks.size() > 1) {
            ActivityManager.RecentTaskInfo recentInfo = recentTasks.get(1);

            Intent intent = new Intent(recentInfo.baseIntent);
            if (recentInfo.origActivity != null) {
                intent.setComponent(recentInfo.origActivity);
            }

            // Now check if this recent task is a launcher
            if (isCurrentHomeActivity(context, intent.getComponent())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isRecentTaskThemeStore(Context context) {
        final ActivityManager am =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        final List<ActivityManager.RecentTaskInfo> recentTasks = am.getRecentTasks(
                2, ActivityManager.RECENT_IGNORE_UNAVAILABLE);
        if (recentTasks.size() > 0) {
            ActivityManager.RecentTaskInfo recentInfo = recentTasks.get(0);

            Intent intent = new Intent(recentInfo.baseIntent);
            if (recentInfo.origActivity != null) {
                intent.setComponent(recentInfo.origActivity);
            }

            if (intent.getComponent()
                    .getPackageName().equals(ChooserActivity.THEME_STORE_PACKAGE)) {
              return true;
            }
        }
        return false;
    }


    public static String getTopTaskPackageName(Context context) {
        final ActivityManager am =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RecentTaskInfo> recentTasks = am.getRecentTasks(1, 0);
        if (recentTasks.size() > 0) {
            ActivityManager.RecentTaskInfo recentInfo = recentTasks.get(0);
            if (recentInfo.origActivity != null) {
                return recentInfo.origActivity.getPackageName();
            }
            if (recentInfo.baseIntent != null) {
                return recentInfo.baseIntent.getComponent().getPackageName();
            }
        }
        return null;
    }

    public static boolean hasPerAppThemesApplied(Context context) {
        final Configuration config = context.getResources().getConfiguration();
        final ThemeConfig themeConfig = config != null ? config.themeConfig : null;
        if (themeConfig != null) {
            Map<String, ThemeConfig.AppTheme> themes = themeConfig.getAppThemes();
            for (String appPkgName : themes.keySet()) {
                if (ThemeUtils.isPerAppThemeComponent(appPkgName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Method to identify if a theme explicitly overlays a particular app.  Explicit is defined
     * as having files in overlays/appPkgName/
     * @param context
     * @param appPkgNane
     * @param themePkgName
     * @return
     */
    public static boolean themeHasOverlayForApp(Context context, String appPkgNane,
            String themePkgName) {
        boolean hasExplicitOverlay = false;
        try {
            Context themeContext = context.createPackageContext(themePkgName, 0);
            if (themeContext != null) {
                AssetManager assets = themeContext.getAssets();
                String[] files = assets.list(OVERLAY_BASE_PATH + appPkgNane);
                if (files != null && files.length > 0) hasExplicitOverlay = true;
            }
        } catch (Exception e) {
            // don't care, we'll return false and let the caller handle things
        }
        return hasExplicitOverlay;
    }

    private static boolean isCurrentHomeActivity(Context context,
            ComponentName component) {
        final PackageManager pm = context.getPackageManager();
        ActivityInfo homeInfo = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                .resolveActivityInfo(pm, 0);

        return homeInfo != null
                && homeInfo.packageName.equals(component.getPackageName())
                && homeInfo.name.equals(component.getClassName());
    }

    /**
     * Returns the resource-IDs for all attributes specified in the given
     * <declare-styleable>-resource tag as an int array.
     * stackoverflow.com/questions/13816596/accessing-declare-styleable-resources-programatically
     *
     * @param name
     * @return
     */
    public static final int[] getResourceDeclareStyleableIntArray(String pkgName, String name) {
        try {
            //use reflection to access the resource class
            Field[] fields2 =
                    Class.forName(pkgName + ".R$styleable").getFields();

            //browse all fields
            for (Field f : fields2) {
                //pick matching field
                if (f.getName().equals(name)) {
                    //return as int array
                    int[] ret = (int[])f.get(null);
                    return ret;
                }
            }
        }
        catch (Throwable t) {
        }

        return null;
    }

    /**
     * This allows pivoting key/value pairs as column/entry pairs.
     * This is only needed when querying multiple keys at a time.
     * @param keyValue
     * @return
     */
    public static String getProjectionFromKeyValue(String keyValue) {
        return String.format("MAX( CASE %s WHEN '%s' THEN %s ELSE NULL END) AS %s",
                ThemesContract.PreviewColumns.COL_KEY, keyValue,
                ThemesContract.PreviewColumns.COL_VALUE, keyValue);
    }
}
