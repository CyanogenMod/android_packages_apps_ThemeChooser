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

package android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

public class ComponentSelectorLinearLayout extends LinearLayout {

    private int mDividerHeight;

    public ComponentSelectorLinearLayout(Context context) {
        this(context, null);
    }

    public ComponentSelectorLinearLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ComponentSelectorLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mDividerHeight = -1;
    }

    public void setDividerHeight(int height) {
        mDividerHeight = height;
    }

    public int getDividerHeight() {
        return mDividerHeight;
    }

    @Override
    void drawVerticalDivider(Canvas canvas, int left) {
        final Drawable divider = getDividerDrawable();
        final int dividerWidth = getDividerWidth();
        final int dividerPadding = getDividerPadding();
        final int dividerHeight = mDividerHeight >= 0 ?
                                  getPaddingTop() + dividerPadding + mDividerHeight :
                                  getHeight() - getPaddingBottom() - dividerPadding;
        divider.setBounds(left, getPaddingTop() + dividerPadding, left + dividerWidth,
                          dividerHeight);
        divider.draw(canvas);
    }
}
