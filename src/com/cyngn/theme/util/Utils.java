/*
 * Copyright (C) 2014 Cyanogen, Inc.
 */
package com.cyngn.theme.util;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Point;
import android.graphics.Rect;
import android.provider.ThemesContract;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Utils {
    private static final String TAG = Utils.class.getSimpleName();
    private static final boolean DEBUG = false;

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
        return !ViewConfiguration.get(context).hasPermanentMenuKey();
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

    public static String getBatteryIndex(int type) {
        switch(type) {
            case 2:
                return ThemesContract.PreviewColumns.STATUSBAR_BATTERY_CIRCLE;
            case 5:
                return ThemesContract.PreviewColumns.STATUSBAR_BATTERY_LANDSCAPE;
            default:
                return ThemesContract.PreviewColumns.STATUSBAR_BATTERY_PORTRAIT;
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

        int xOffset = wm.getLastWallpaperX();
        // x offset
        if (xOffset == -1) {
            xOffset = 0;
        } else {
            xOffset *= -1;
        }

        // y offsets
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
                int spaceB = (goldenWidth - Math.round(dh/scale)) / 2;

                bitmap = Bitmap.createBitmap(wallpaper, spaceA, 0, goldenWidth,
                        wallpaper.getHeight());
                int left = spaceB + Math.round(xOffset/scale);
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
        return bitmap;
    }
}
