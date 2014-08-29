/*
 * Copyright (C) 2014 Cyanogen, Inc.
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
