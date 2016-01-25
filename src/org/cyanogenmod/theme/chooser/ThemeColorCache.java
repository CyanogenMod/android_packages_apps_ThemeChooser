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
package org.cyanogenmod.theme.chooser;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.graphics.Palette;

import java.util.HashMap;

public final class ThemeColorCache {
    private static HashMap<String, ColorInformation> sCache = new HashMap<>();

    public static int getVibrantColor(String packageName) {
        synchronized (sCache) {
            ColorInformation info = sCache.get(packageName);
            return info != null ? info.vibrantColor : Color.TRANSPARENT;
        }
    }

    public static int getVibrantColorWithFallback(String packageName,
            Resources res, int fallbackColorResId) {
        int vibrantColor = getVibrantColor(packageName);
        if (vibrantColor != Color.TRANSPARENT) {
            return vibrantColor;
        }
        return res.getColor(fallbackColorResId);
    }

    public static void updateFromBitmap(String packageName, Bitmap bitmap) {
        synchronized (sCache) {
            if (sCache.containsKey(packageName)) {
                return;
            }
        }

        try {
            Palette palette = Palette.from(bitmap).generate();
            int vibrantColor = palette.getVibrantColor(Color.TRANSPARENT);
            if (vibrantColor == Color.TRANSPARENT) {
                return;
            }

            ColorInformation info = new ColorInformation();
            info.vibrantColor = vibrantColor;
            synchronized (sCache) {
                sCache.put(packageName, info);
            }
        } catch (IllegalArgumentException e) {
            // Theme (un)installed
        }
    }

    private static class ColorInformation {
        int vibrantColor;
    }
}
