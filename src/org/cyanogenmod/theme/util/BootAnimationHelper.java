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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;
import org.cyanogenmod.theme.widget.PartAnimationDrawable;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class BootAnimationHelper {
    public static final String THEME_INTERNAL_BOOT_ANI_PATH =
            "assets/bootanimation/bootanimation.zip";
    public static final String SYSTEM_BOOT_ANI_PATH = "/system/media/bootanimation.zip";

    /**
     * Takes an InputStream to a bootanimation.zip and turns it into a set of
     * PartAnimationDrawables which can be played inside an ImageView
     * @param is InputStream to the bootanimation.zip to process
     * @return The list of ParteAnimationDrawables loaded
     * @throws IOException
     */
    public static List<PartAnimationDrawable> loadAnimation(Context context, InputStream is) throws IOException {
        ZipInputStream zis = (is instanceof ZipInputStream) ? (ZipInputStream) is
                : new ZipInputStream(new BufferedInputStream(is));
        ZipEntry ze;
        Map<String, TreeMap<String, Drawable>> framesMap =
                new HashMap<String, TreeMap<String, Drawable>>();
        List<AnimationPart> animationParts = null;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = am.isLowRamDevice() ? 4 : 2;
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        // First thing to do is iterate over all the entries and the zip and store them
        // for building the animations afterwards
        while ((ze = zis.getNextEntry()) != null) {
            final String entryName = ze.getName();
            if ("desc.txt".equals(entryName)) {
                animationParts = parseDescription(zis);
            } else if (entryName.contains("/") && !entryName.endsWith("/")) {
                int splitAt = entryName.lastIndexOf('/');
                final String part = entryName.substring(0, splitAt);
                final String name = entryName.substring(splitAt + 1);
                final Drawable d;
                try {
                    d = loadFrame(zis, opts);
                } catch (OutOfMemoryError oome) {
                    // better to have something rather than nothing?
                    break;
                }
                TreeMap<String, Drawable> parts = framesMap.get(part);
                if (parts == null) {
                    parts = new TreeMap<String, Drawable>();
                    framesMap.put(part, parts);
                }
                parts.put(name, d);
            }
        }
        zis.close();
        if (animationParts == null) return null;

        // Now that the desc.txt and images are loaded we can assemble the variouse
        // parts into one PartAnimationDrawable per part
        List<PartAnimationDrawable> animations = new ArrayList<PartAnimationDrawable>(animationParts.size());
        for (AnimationPart a : animationParts) {
            PartAnimationDrawable anim = new PartAnimationDrawable();
            anim.setPlayCount(a.playCount);
            final TreeMap<String, Drawable> parts = framesMap.get(a.partName);
            for (Drawable d : parts.values()) {
                anim.addFrame(d, a.frameRateMillis);
            }
            if (a.playCount <= 0) {
                anim.setOneShot(false);
            } else {
                anim.setOneShot(true);
            }
            animations.add(anim);
        }

        return animations;
    }

    /**
     * Parses the desc.txt of the boot animation
     * @param in InputStream to the desc.txt
     * @return A list of the parts as given in desc.txt
     * @throws IOException
     */
    private static List<AnimationPart> parseDescription(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        // first line, 3rd column has # of frames per second
        final int frameRateMillis = 1000 / Integer.parseInt(reader.readLine().split(" ")[2]);
        String line;
        List<AnimationPart> animationParts = new ArrayList<AnimationPart>();
        while ((line = reader.readLine()) != null) {
            String[] info = line.split(" ");
            if (info.length == 4 && info[0].equals("p")) {
                int playCount = Integer.parseInt(info[1]);
                int pause = Integer.parseInt(info[2]);
                String name = info[3];
                AnimationPart ap = new AnimationPart(playCount, pause, name, frameRateMillis);
                animationParts.add(ap);
            }
        }

        return animationParts;
    }

    /**
     * Load a frame of the boot animation into a BitmapDrawable
     * @param is The frame to load
     * @param opts Options to use when decoding the bitmap
     * @return The loaded BitmapDrawable
     * @throws FileNotFoundException
     */
    private static BitmapDrawable loadFrame(InputStream is, BitmapFactory.Options opts)
            throws FileNotFoundException {
        BitmapDrawable drawable = new BitmapDrawable(BitmapFactory.decodeStream(is, null, opts));
        drawable.setAntiAlias(true);
        drawable.setFilterBitmap(true);
        return drawable;
    }

    private static class AnimationPart {
        public int playCount;
        public int pause;
        String partName;
        int frameRateMillis;

        public AnimationPart(int playCount, int pause, String partName, int frameRateMillis) {
            this.playCount = playCount;
            this.pause = pause;
            this.partName = partName;
            this.frameRateMillis = frameRateMillis;
        }
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
                if ("default".equals(path)) {
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
