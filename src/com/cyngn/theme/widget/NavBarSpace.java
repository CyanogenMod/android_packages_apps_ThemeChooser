/*
 * Copyright (C) 2014 The CyanogenMod, Inc.
 */
package com.cyngn.theme.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import com.cyngn.theme.util.Utils;

/**
 * A simple view used to pad layouts so that content floats above the
 * navigation bar.  This is best used with transparent or translucent
 * navigation bars where the content can go behind them.
 */
public class NavBarSpace extends View {

    public NavBarSpace(Context context) {
        this(context, null);
    }

    public NavBarSpace(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NavBarSpace(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (!Utils.hasNavigationBar(mContext)) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            setMeasuredDimension(width, 0);
        }
    }
}
