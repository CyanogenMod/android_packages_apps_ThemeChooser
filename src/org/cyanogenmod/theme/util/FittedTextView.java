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
