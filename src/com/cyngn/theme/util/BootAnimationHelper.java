/*
 * Copyright (C) 2014 Cyanogen, Inc.
 */
package com.cyngn.theme.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.ThemeConfig;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class BootAnimationHelper {
    private static final String TAG = BootAnimationHelper.class.getSimpleName();
    private static final int MAX_REPEAT_COUNT = 3;

    public static final String THEME_INTERNAL_BOOT_ANI_PATH =
            "assets/bootanimation/bootanimation.zip";
    public static final String SYSTEM_BOOT_ANI_PATH = "/system/media/bootanimation.zip";
    public static final String CACHED_SUFFIX = "_bootanimation.zip";

    public static final int NUM_FIRST_LINE_PARAMETERS = 3;
    public static final int NUM_PART_LINE_PARAMETERS = 4;

    public static class AnimationPart {
        /**
         * Number of times to play this part
         */
        public int playCount;
        /**
         * If non-zero, pause for the given # of seconds before moving on to next part.
         */
        public int pause;
        /**
         * The name of this part
         */
        public String partName;
        /**
         * Time each frame is displayed
         */
        public int frameRateMillis;
        /**
         * List of file names for the given frames in this part
         */
        public List<String> frames;
        /**
         * width of the animation
         */
        public int width;
        /**
         * height of the animation
         */
        public int height;

        public AnimationPart(int playCount, int pause, String partName, int frameRateMillis,
                             int width, int height) {
            this.playCount = playCount == 0 ? MAX_REPEAT_COUNT : playCount;
            this.pause = pause;
            this.partName = partName;
            this.frameRateMillis = frameRateMillis;
            this.width = width;
            this.height = height;
            frames = new ArrayList<String>();
        }

        public void addFrame(String frame) {
            frames.add(frame);
        }
    }

    /**
     * Gather up all the details for the given boot animation
     * @param zip The bootanimation.zip
     * @return A list of AnimationPart if successful, null if not.
     * @throws IOException
     */
    public static List<AnimationPart> parseAnimation(ZipFile zip)
            throws IOException, BootAnimationException {
        if (zip == null) {
            // To make tracking down boot animation problems we'll throw a BootAnimationException
            // instead of an IllegalArgumentException.
            throw new BootAnimationException("Boot animation ZipFile cannot be null");
        }
        List<AnimationPart> animationParts = null;

        ZipEntry ze = zip.getEntry("desc.txt");
        if (ze != null) {
            animationParts = parseDescription(zip.getInputStream(ze));
        } else {
            throw new BootAnimationException("Missing desc.txt in root of bootanimation.zip");
        }

        if (animationParts == null) {
            // We really should not end up here but in case we do here's an exception for ya!
            throw new BootAnimationException("Unable to load boot animation.");
        }

        Iterator<AnimationPart> iterator = animationParts.iterator();
        while(iterator.hasNext()) {
            AnimationPart a = iterator.next();
            for (Enumeration<? extends ZipEntry> e = zip.entries();e.hasMoreElements();) {
                ze = e.nextElement();
                if (!ze.isDirectory() && ze.getName().contains(a.partName)) {
                    a.addFrame(ze.getName());
                }
            }
            if (a.frames.size() > 0) {
                Collections.sort(a.frames);
            } else {
                // This boot animation may be salvageable if there are still some other parts
                // that are good.  We'll remove this part and if there are no parts left by
                // the time we have iterated over all the parts then we can throw an exception.
                Log.w(TAG, String.format("No frames in part %s, removing from animation",
                        a.partName));
                iterator.remove();
            }
        }
        if (animationParts.size() == 0) {
            throw new BootAnimationException("Boot animation must have at least one part.");
        }

        return animationParts;
    }

    /**
     * Parses the desc.txt of the boot animation
     * @param in InputStream to the desc.txt
     * @return A list of the parts as given in desc.txt
     * @throws IOException
     */
    private static List<AnimationPart> parseDescription(InputStream in)
            throws IOException, BootAnimationException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        // read in suggested width, height, and frame rate from first line
        String line = reader.readLine();
        String[] details = line.split(" ");
        if (details.length != NUM_FIRST_LINE_PARAMETERS) {
            throw new BootAnimationException(String.format(
                    "Invalid # of parameters on first line of desc.txt; exptected %d, read %d  " +
                            "(\"%s\")",
                    NUM_FIRST_LINE_PARAMETERS, details.length, line));
        }

        // The items should be in the following order: width, height, frame rate
        final int width = Integer.parseInt(details[0]);
        final int height = Integer.parseInt(details[1]);
        final int frameRateMillis = 1000 / Integer.parseInt(details[2]);

        List<AnimationPart> animationParts = new ArrayList<AnimationPart>();
        while ((line = reader.readLine()) != null) {
            // trim off any leading and trailing spaces
            line = line.trim();
            // if the line is empty continue on to the next
            if (TextUtils.isEmpty(line)) continue;

            String[] info = line. split(" ");
            if (info.length != NUM_PART_LINE_PARAMETERS) {
                Log.w(TAG, String.format(
                        "Invalid # of part parameters; exptected %d, read %d  (\"%s\")",
                        NUM_PART_LINE_PARAMETERS, info.length, line));
                // let's continue in case there are parts that are valid
                continue;
            }
            if (!info[0].equals("p") && !info[0].equals("c")) {
                Log.w(TAG, String.format(
                   "Unknown part type; expected 'p' or 'c', read %s  (\"%s\")", info[0], line));

                // let's continue in case there are parts that are valid
                continue;
            }
            int playCount = Integer.parseInt(info[1]);
            int pause = Integer.parseInt(info[2]);
            String name = info[3];
            AnimationPart ap = new AnimationPart(playCount, pause, name, frameRateMillis,
                    width, height);
            animationParts.add(ap);
        }

        return animationParts;
    }

    public static String getPreviewFrameEntryName(InputStream is) throws IOException {
        ZipInputStream zis = (is instanceof ZipInputStream) ? (ZipInputStream) is
                : new ZipInputStream(new BufferedInputStream(is));
        ZipEntry ze;
        // First thing to do is iterate over all the entries and the zip and store them
        // for building the animations afterwards
        String previewName = null;
        while ((ze = zis.getNextEntry()) != null) {
            final String entryName = ze.getName();
            if (entryName.contains("/")
                    && (entryName.endsWith(".png") || entryName.endsWith(".jpg"))) {
                previewName = entryName;
            }
        }

        return previewName;
    }

    public static Bitmap loadPreviewFrame(Context context, InputStream is, String previewName)
            throws IOException {
        ZipInputStream zis = (is instanceof ZipInputStream) ? (ZipInputStream) is
                : new ZipInputStream(new BufferedInputStream(is));
        ZipEntry ze;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = am.isLowRamDevice() ? 4 : 2;
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        // First thing to do is iterate over all the entries and the zip and store them
        // for building the animations afterwards
        Bitmap preview = null;
        while ((ze = zis.getNextEntry()) != null && preview == null) {
            final String entryName = ze.getName();
            if (entryName.equals(previewName)) {
                preview = BitmapFactory.decodeStream(zis, null, opts);
            }
        }
        zis.close();

        return preview;
    }

    public static void clearBootAnimationCache(Context context) {
        File cache = context.getCacheDir();
        if (cache.exists()) {
            for(File f : cache.listFiles()) {
                // volley stores stuff in cache so don't delete the volley directory
                if(!f.isDirectory() && f.getName().endsWith(CACHED_SUFFIX)) f.delete();
            }
        }
    }

    public static class LoadBootAnimationImage extends AsyncTask<Object, Void, Bitmap> {
        private ImageView imv;
        private String path;
        private Context context;

        public LoadBootAnimationImage(ImageView imv, Context context, String path) {
            this.imv = imv;
            this.context = context;
            this.path = path;
        }

        @Override
        protected Bitmap doInBackground(Object... params) {
            Bitmap bitmap = null;
            String previewName = null;
            // this is ugly, ugly, ugly.  Did I mention this is ugly?
            try {
                if (ThemeConfig.HOLO_DEFAULT.equals(path)) {
                    previewName = getPreviewFrameEntryName(
                            new FileInputStream(SYSTEM_BOOT_ANI_PATH));
                    bitmap = loadPreviewFrame(
                            context, new FileInputStream(SYSTEM_BOOT_ANI_PATH), previewName);
                } else {
                    final Context themeCtx = context.createPackageContext(path, 0);
                    previewName = getPreviewFrameEntryName(
                            themeCtx.getAssets().open("bootanimation/bootanimation.zip"));
                    bitmap = loadPreviewFrame(context,
                            themeCtx.getAssets().open("bootanimation/bootanimation.zip"),
                            previewName);
                }
            } catch (Exception e) {
                // don't care since a null bitmap will be returned
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null && imv != null) {
                imv.setVisibility(View.VISIBLE);
                imv.setImageBitmap(result);
            }
        }
    }

    public static class BootAnimationException extends Exception {
        public BootAnimationException(String detailMessage) {
            super(detailMessage);
        }
    }
}
