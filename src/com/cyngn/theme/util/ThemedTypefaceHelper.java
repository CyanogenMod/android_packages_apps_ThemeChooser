/*
 * Copyright (C) 2014 The CyanogenMod, Inc.
 */
package com.cyngn.theme.util;

import android.content.Context;
import android.content.pm.ThemeUtils;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.util.Log;

import com.cyngn.theme.util.FontConfigParser.Family;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

/**
 * Assists in loading a themes font typefaces.
 * Will load system default if there is a load issue
 */
public class ThemedTypefaceHelper {
    private static final String TAG = ThemedTypefaceHelper.class.getName();
    private static final String FAMILY_SANS_SERIF = "sans-serif";
    private static final String FONTS_DIR = "fonts/";
    private static final String SYSTEM_FONTS_XML = "/system/etc/system_fonts.xml";
    private static final String SYSTEM_FONTS_DIR = "/system/fonts/";

    private boolean mIsLoaded;
    private Context mThemeContext;
    private List<Family> mFamilies;
    private Typeface[] mTypefaces = new Typeface[4];

    public void load(Context context, String pkgName) {
        try {
            loadThemedFonts(context, pkgName);
            return;
        } catch(Exception e) {
            Log.w(TAG, "Unable to parse and load themed fonts. Falling back to system fonts");
        }

        try {
            loadSystemFonts();
            return;
        } catch(Exception e) {
            Log.e(TAG, "Parsing system fonts failed. Falling back to Typeface loaded fonts");

        }

        // There is no reason for this to happen unless someone
        // messed up the system_fonts.xml
        loadDefaultFonts();
    }

    private void loadThemedFonts(Context context, String pkgName) throws Exception {
        //Parse the font XML
        mThemeContext = context.createPackageContext(pkgName, Context.CONTEXT_IGNORE_SECURITY);
        AssetManager assetManager = mThemeContext.getAssets();
        InputStream is = assetManager.open(FONTS_DIR + ThemeUtils.FONT_XML);
        mFamilies = FontConfigParser.parse(is);

        //Load the typefaces for sans-serif
        Family sanSerif = getFamily(FAMILY_SANS_SERIF);
        mTypefaces[Typeface.NORMAL] = loadTypeface(sanSerif, Typeface.NORMAL);
        mTypefaces[Typeface.BOLD] = loadTypeface(sanSerif, Typeface.BOLD);
        mTypefaces[Typeface.ITALIC] = loadTypeface(sanSerif, Typeface.ITALIC);
        mTypefaces[Typeface.BOLD_ITALIC] = loadTypeface(sanSerif, Typeface.BOLD_ITALIC);
        mIsLoaded = true;
    }

    private void loadSystemFonts() throws Exception {
        //Parse the system font XML
        File file = new File(SYSTEM_FONTS_XML);
        InputStream is = new FileInputStream(file);
        mFamilies = FontConfigParser.parse(is);

        //Load the typefaces for sans-serif
        Family sanSerif = getFamily(FAMILY_SANS_SERIF);
        if (mTypefaces[Typeface.NORMAL] == null) {
            mTypefaces[Typeface.NORMAL] = loadSystemTypeface(sanSerif, Typeface.NORMAL);
        }
        if (mTypefaces[Typeface.BOLD] == null) {
            mTypefaces[Typeface.BOLD] = loadSystemTypeface(sanSerif, Typeface.BOLD);
        }
        if (mTypefaces[Typeface.ITALIC] == null) {
            mTypefaces[Typeface.ITALIC] = loadSystemTypeface(sanSerif, Typeface.ITALIC);
        }
        if (mTypefaces[Typeface.BOLD_ITALIC] == null) {
            mTypefaces[Typeface.BOLD_ITALIC] = loadSystemTypeface(sanSerif, Typeface.BOLD_ITALIC);
        }
        mIsLoaded = true;
    }

    private void loadDefaultFonts() {
        mTypefaces[Typeface.NORMAL] = Typeface.DEFAULT;
        mTypefaces[Typeface.BOLD] = Typeface.DEFAULT_BOLD;
        mIsLoaded = true;
    }

    private Family getFamily(String familyName) throws Exception {
        for(Family family : mFamilies) {
            if (family.nameset.contains(familyName)) {
                return family;
            }
        }
        throw new Exception("Unable to find " + familyName);
    }

    private Typeface loadTypeface(Family family, int style) {
        AssetManager assets = mThemeContext.getAssets();
        String path = FONTS_DIR + family.fileset.get(style);
        return Typeface.createFromAsset(assets, path);
    }

    private Typeface loadSystemTypeface(Family family, int style) {
        return Typeface.createFromFile(SYSTEM_FONTS_DIR + family.fileset.get(style));
    }

    public Typeface getTypeface(int style) {
        if (!mIsLoaded) throw new IllegalStateException("Helper was not loaded");
        return mTypefaces[style];
    }
}
