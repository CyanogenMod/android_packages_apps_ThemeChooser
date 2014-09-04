/*
 * Copyright (C) 2014 Cyanogen, Inc.
 */
package com.cyngn.theme.widget;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

import java.io.File;

/**
 * A custom TextView that always uses the Lato font
 */
public class LatoTextView extends TextView {
    private static final int NUM_TYPEFACE_PER_FAMILY = 4;

    private static final String FONT_ASSSET_DIR = "fonts";
    // Regular fonts
    private static final String LATO_REGULAR_PATH =
            FONT_ASSSET_DIR + File.separator + "Lato-Regular.ttf";
    private static final String LATO_REGULAR_BOLD_PATH =
            FONT_ASSSET_DIR + File.separator + "Lato-RegBold.ttf";
    private static final String LATO_REGULAR_ITALIC_PATH =
            FONT_ASSSET_DIR + File.separator + "Lato-RegItalic.otf";
    private static final String LATO_REGULAR_BOLD_ITALIC_PATH =
            FONT_ASSSET_DIR + File.separator + "Lato-RegBoldItalic.ttf";
    // Condensed fonts
    private static final String LATO_CONDENSED_PATH =
            FONT_ASSSET_DIR + File.separator + "Lato-Cond.ttf";
    private static final String LATO_CONDENSED_BOLD_PATH =
            FONT_ASSSET_DIR + File.separator + "Lato-CondBold.ttf";
    private static final String LATO_CONDENSED_ITALIC_PATH =
            FONT_ASSSET_DIR + File.separator + "Lato-CondItalic.ttf";
    private static final String LATO_CONDENSED_BOLD_ITALIC_PATH =
            FONT_ASSSET_DIR + File.separator + "Lato-CondBoldItalic.ttf";
    // Light fonts
    private static final String LATO_LIGHT_PATH =
            FONT_ASSSET_DIR + File.separator + "Lato-Light.ttf";
    private static final String LATO_LIGHT_ITALIC_PATH =
            FONT_ASSSET_DIR + File.separator + "Lato-LightItalic.ttf";

    private static final String CONDENSED = "condensed";
    private static final String LIGHT = "light";

    private static Typeface[] sLatoRegularTypeface;
    private static Typeface[] sLatoCondensedTypeface;
    private static Typeface[] sLatoLightTypeface;
    private static final Object sLock = new Object();

    public LatoTextView(Context context) {
        this(context, null);
    }

    public LatoTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LatoTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        synchronized (sLock) {
            AssetManager assets = context.getAssets();
            if (sLatoRegularTypeface == null) {
                sLatoRegularTypeface = new Typeface[NUM_TYPEFACE_PER_FAMILY];
                sLatoRegularTypeface[Typeface.NORMAL] =
                        Typeface.createFromAsset(assets, LATO_REGULAR_PATH);
                sLatoRegularTypeface[Typeface.BOLD] =
                        Typeface.createFromAsset(assets, LATO_REGULAR_BOLD_PATH);
                sLatoRegularTypeface[Typeface.ITALIC] =
                        Typeface.createFromAsset(assets, LATO_REGULAR_ITALIC_PATH);
                sLatoRegularTypeface[Typeface.BOLD_ITALIC] =
                        Typeface.createFromAsset(assets, LATO_REGULAR_BOLD_ITALIC_PATH);
            }
            if (sLatoCondensedTypeface == null) {
                sLatoCondensedTypeface = new Typeface[NUM_TYPEFACE_PER_FAMILY];
                sLatoCondensedTypeface[Typeface.NORMAL] =
                        Typeface.createFromAsset(assets, LATO_CONDENSED_PATH);
                sLatoCondensedTypeface[Typeface.BOLD] =
                        Typeface.createFromAsset(assets, LATO_CONDENSED_BOLD_PATH);
                sLatoCondensedTypeface[Typeface.ITALIC] =
                        Typeface.createFromAsset(assets, LATO_CONDENSED_ITALIC_PATH);
                sLatoCondensedTypeface[Typeface.BOLD_ITALIC] =
                        Typeface.createFromAsset(assets, LATO_CONDENSED_BOLD_ITALIC_PATH);
            }
            if (sLatoLightTypeface == null) {
                sLatoLightTypeface = new Typeface[NUM_TYPEFACE_PER_FAMILY];
                sLatoLightTypeface[Typeface.NORMAL] =
                        Typeface.createFromAsset(assets, LATO_LIGHT_PATH);
                sLatoLightTypeface[Typeface.BOLD] =
                        sLatoRegularTypeface[Typeface.BOLD];
                sLatoLightTypeface[Typeface.ITALIC] =
                        Typeface.createFromAsset(assets, LATO_LIGHT_ITALIC_PATH);
                sLatoLightTypeface[Typeface.BOLD_ITALIC] =
                        sLatoRegularTypeface[Typeface.BOLD_ITALIC];
            }
        }

        final Resources.Theme theme = context.getTheme();
        TypedArray a = theme.obtainStyledAttributes(
                attrs, com.android.internal.R.styleable.TextView, defStyle, 0);
        String fontFamily = "sans-serif";
        int styleIndex = Typeface.NORMAL;
        if (a != null) {
            int n = a.getIndexCount();
            for (int i = 0; i < n; i++) {
                int attr = a.getIndex(i);

                switch (attr) {
                    case com.android.internal.R.styleable.TextView_fontFamily:
                        fontFamily = a.getString(attr);
                        break;

                    case com.android.internal.R.styleable.TextView_textStyle:
                        styleIndex = a.getInt(attr, styleIndex);
                        break;
                }
            }
            a.recycle();
        }

        setTypefaceFromAttrs(fontFamily, styleIndex);
    }

    private void setTypefaceFromAttrs(String familyName, int styleIndex) {
        Typeface tf = null;
        if (familyName != null) {
            Typeface[] typefaces = sLatoRegularTypeface;
            if (familyName.contains(CONDENSED)) {
                typefaces = sLatoCondensedTypeface;
            } else if (familyName.contains(LIGHT)) {
                typefaces = sLatoLightTypeface;
            }
            tf = typefaces[styleIndex];
            if (tf != null) {
                setTypeface(tf);
                return;
            }
        }
        setTypeface(tf, styleIndex);
    }
}
