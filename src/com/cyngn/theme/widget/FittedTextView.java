/*
 * Copyright (C) 2014 Cyanogen, Inc.
 */
package com.cyngn.theme.widget;

import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

/**
 * Change the font size to match the measured
 * textview size by width
 *
 */
public class FittedTextView extends TextView {
    private Paint mPaint;

    public FittedTextView(Context context) {
        super(context);
        mPaint = new Paint();
    }

    public FittedTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
    }

    public FittedTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mPaint = new Paint();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final float THRESHOLD = 0.5f;
        final float TARGET_WIDTH = getMeasuredWidth();
        final String text = getText().toString();
        mPaint.set(getPaint());

        float max = 200;
        float min = 2;
        while(max > min) {
            float size = (max+min) / 2;
            mPaint.setTextSize(size);
            float measuredWidth = mPaint.measureText(text);
            if (Math.abs(TARGET_WIDTH - measuredWidth) <= THRESHOLD) {
                break;
            } else if (measuredWidth > TARGET_WIDTH) {
                max = size-1;
            } else {
                min = size+1;
            }
        }
        this.setTextSize(TypedValue.COMPLEX_UNIT_PX, min-1);
    }
}
