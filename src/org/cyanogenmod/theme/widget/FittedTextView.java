/*
 * Copyright (C) 2016 Cyanogen, Inc.
 * Copyright (C) 2016 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cyanogenmod.theme.widget;

import android.content.Context;
import android.graphics.Paint;
import android.text.method.TransformationMethod;
import android.text.method.AllCapsTransformationMethod;
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
    //If set to true, the text will be resized to fit the view.
    private boolean mAutoFitText = true;
    //Used to instruct whether the text should be expanded to fill out the view, even if the text
    //fits without being resized
    private boolean mAutoExpand = true;

    public FittedTextView(Context context) {
        this(context, null);
    }

    public FittedTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FittedTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mPaint = new Paint();
    }

    protected void setAutoFitText(boolean autoFit) {
        mAutoFitText = autoFit;
    }

    protected boolean getAutoFitText() {
        return mAutoFitText;
    }

    protected void setAutoExpand(boolean autoExpand) {
        mAutoExpand = autoExpand;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (!mAutoFitText) return;

        final float THRESHOLD = 0.5f;
        final float TARGET_WIDTH = getMeasuredWidth();
        String text = getText().toString();
        TransformationMethod tm = getTransformationMethod();
        if (tm != null && tm instanceof AllCapsTransformationMethod) {
            text = getText().toString().toUpperCase();
        }
        mPaint.set(getPaint());

        if (mPaint.measureText(text) <= TARGET_WIDTH && !mAutoExpand) return;

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
