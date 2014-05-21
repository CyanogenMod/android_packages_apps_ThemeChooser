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
import android.content.Context;
import android.content.res.CustomTheme;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class BootAnimationHelper {
    public static final String THEME_INTERNAL_BOOT_ANI_PATH =
            "assets/bootanimation/bootanimation.zip";
    public static final String SYSTEM_BOOT_ANI_PATH = "/system/media/bootanimation.zip";

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
            this.playCount = playCount;
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
     * @param context
     * @param zip The bootanimation.zip
     * @return A list of AnimationPart if successful, null if not.
     * @throws IOException
     */
    public static List<AnimationPart> parseAnimation(Context context, ZipFile zip)
            throws IOException {
        List<AnimationPart> animationParts = null;

        ZipEntry ze = zip.getEntry("desc.txt");
        if (ze != null) {
            animationParts = parseDescription(zip.getInputStream(ze));
        }

        if (animationParts == null) return null;

        for (AnimationPart a : animationParts) {
            for (Enumeration<? extends ZipEntry> e = zip.entries();e.hasMoreElements();) {
                ze = e.nextElement();
                if (!ze.isDirectory() && ze.getName().contains(a.partName)) {
                    a.addFrame(ze.getName());
                }
            }
            Collections.sort(a.frames);
        }

        return animationParts;
    }

    /**
     * Parses the desc.txt of the boot animation
     * @param in InputStream to the desc.txt
     * @return A list of the parts as given in desc.txt
     * @throws IOException
     */
    private static List<AnimationPart> parseDescription(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line = reader.readLine();
        String[] details = line.split(" ");
        final int width = Integer.parseInt(details[0]);
        final int height = Integer.parseInt(details[1]);
        final int frameRateMillis = 1000 / Integer.parseInt(details[2]);

        List<AnimationPart> animationParts = new ArrayList<AnimationPart>();
        while ((line = reader.readLine()) != null) {
            String[] info = line.split(" ");
            if (info.length == 4 && (info[0].equals("p") || info[0].equals("c"))) {
                int playCount = Integer.parseInt(info[1]);
                int pause = Integer.parseInt(info[2]);
                String name = info[3];
                AnimationPart ap = new AnimationPart(playCount, pause, name, frameRateMillis,
                        width, height);
                animationParts.add(ap);
            }
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
                if (CustomTheme.HOLO_DEFAULT.equals(path)) {
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
}
