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

package org.cyanogenmod.theme.perapptheming;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ListView;

import org.cyanogenmod.theme.chooser2.R;
import org.cyanogenmod.theme.util.Utils;

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
        final Resources res = getResources();
        TypedArray a = context.obtainStyledAttributes(attrs,
                Utils.getResourceDeclareStyleableIntArray("com.android.internal", "View"));
        int resId = res.getIdentifier("View_minHeight", "styleable", "android");
        mMinHeight = a.getDimensionPixelSize(resId, 0);
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
