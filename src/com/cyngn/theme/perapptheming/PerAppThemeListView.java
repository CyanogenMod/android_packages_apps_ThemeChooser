/*
 * Copyright (C) 2015 Cyanogen, Inc.
 */
package com.cyngn.theme.perapptheming;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ListView;
import com.cyngn.theme.chooser.R;

public class PerAppThemeListView extends ListView {
    private int mMinHeight;
    private int mMaxHeight;

    public PerAppThemeListView(Context context) {
        this(context, null);
    }

    public PerAppThemeListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PerAppThemeListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, com.android.internal.R.styleable.View);
        mMinHeight = a.getDimensionPixelSize(com.android.internal.R.styleable.View_minHeight, 0);
        a.recycle();

        a = context.obtainStyledAttributes(attrs, R.styleable.PerAppThemeListView);
        mMaxHeight = a.getDimensionPixelSize(R.styleable.PerAppThemeListView_maxHeight, 0);
        a.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // let the super do the heavy lifting and then we'll cap the values to any max and/or min
        // values that were defined in the layout
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();
        int newHeight = measuredHeight;
        if (mMaxHeight > 0) {
            newHeight = Math.min(measuredHeight, mMaxHeight);
        }
        if (mMinHeight > 0) {
            newHeight = Math.max(newHeight, mMinHeight);
        }
        if (newHeight != measuredHeight) {
            setMeasuredDimension(measuredWidth, newHeight);
        }
    }
}
