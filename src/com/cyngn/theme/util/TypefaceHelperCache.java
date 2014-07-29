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
package com.cyngn.theme.util;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

public class TypefaceHelperCache {
    private static TypefaceHelperCache sHelperCache;
    private final Map<String, ThemedTypefaceHelper> mCache;

    private TypefaceHelperCache() {
        mCache = new HashMap<String, ThemedTypefaceHelper>();
    }

    public static synchronized TypefaceHelperCache getInstance() {
        if (sHelperCache == null) {
            sHelperCache = new TypefaceHelperCache();
        }
        return sHelperCache;
    }

    public ThemedTypefaceHelper getHelperForTheme(Context context, String pkgName) {
        synchronized (mCache) {
            ThemedTypefaceHelper helper = mCache.get(pkgName);
            if (helper == null) {
                helper = new ThemedTypefaceHelper();
                helper.load(context, pkgName);
                mCache.put(pkgName, helper);
            }
            return helper;
        }
    }

    public int getTypefaceCount() {
        synchronized (mCache) {
            return mCache.size();
        }
    }
}
