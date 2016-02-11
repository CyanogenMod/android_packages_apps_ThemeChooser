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
import com.cyngn.theme.chooser.R;
import com.cyngn.theme.util.Utils;

import java.io.File;

/**
 * A custom TextView that always uses the Lato font
 */
public class LatoTextView extends FittedTextView {
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

    // Retrieving these attributes is done via reflection so let's just do this once and share
    // it amongst the other instances of LatoTextView
    private static int[] sTextViewStyleAttributes;

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
        if (sTextViewStyleAttributes == null) {
            sTextViewStyleAttributes =
                    Utils.getResourceDeclareStyleableIntArray("com.android.internal", "TextView");
        }

        if (sTextViewStyleAttributes != null) {
            TypedArray a =
                    theme.obtainStyledAttributes(attrs, sTextViewStyleAttributes, defStyle, 0);
            String fontFamily = "sans-serif";
            int styleIndex = Typeface.NORMAL;
            if (a != null) {
                int n = a.getIndexCount();
                for (int i = 0; i < n; i++) {
                    int attr = a.getIndex(i);

                    final Resources res = getResources();
                    int attrFontFamily =
                            res.getIdentifier("TextView_fontFamily", "styleable", "android");
                    int attrTextStyle =
                            res.getIdentifier("TextView_textStyle", "styleable", "android");
                    if (attr == attrFontFamily) {
                        fontFamily = a.getString(attr);
                    } else if (attr == attrTextStyle) {
                        styleIndex = a.getInt(attr, styleIndex);
                    }
                }
                a.recycle();
            }

            setTypefaceFromAttrs(fontFamily, styleIndex);
            TypedArray styledAttrs = context.obtainStyledAttributes(attrs,
                    R.styleable.FittedTextView, 0, 0);
            try {
                //Although we extend FittedTextView, we don't want all instances to auto fit the
                //text, so we check if autoFitText has been set in the attributes. Default to false
                boolean fit = styledAttrs.getBoolean(R.styleable.FittedTextView_autoFitText, false);
                setAutoFitText(fit);
            } finally {
                styledAttrs.recycle();
            }
        }
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
