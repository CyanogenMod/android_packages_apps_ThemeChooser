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

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.ThemesContract;
import android.provider.ThemesContract.ThemesColumns;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Utils {
    private static final String TAG = Utils.class.getSimpleName();

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

    public static Bitmap loadBitmapBlob(Cursor cursor, int columnIdx) {
        if (columnIdx < 0) {
            Log.w(TAG, "loadBitmapBlob(): Invalid index provided, returning null");
            return null;
        }
        byte[] blob = cursor.getBlob(columnIdx);
        if (blob == null) return null;
        return BitmapFactory.decodeByteArray(blob, 0, blob.length);
    }

    public static Bitmap getPreviewBitmap(Context context, String pkgName, String previewColumn) {
        if (pkgName == null) return null;

        Uri uri = ThemesContract.PreviewColumns.CONTENT_URI;
        String[] projection = new String[] { previewColumn };
        String selection = ThemesContract.ThemesColumns.PKG_NAME + "=?";
        String[] selectionArgs = new String[] { pkgName };

        Cursor cursor = context.getContentResolver()
                .query(uri, projection, selection, selectionArgs, null);

        if (cursor != null) {
            try {
                if (cursor != null && cursor.moveToFirst())
                return loadBitmapBlob(cursor, 0);
            } finally {
                if (cursor != null) cursor.close();
            }
        }
        return null;
    }

    public static boolean hasNavigationBar(Context context) {
        return !ViewConfiguration.get(context).hasPermanentMenuKey();
    }
}
