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
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;


/**
 * This class will layout its children in a very specific way. It assumes it is a child of a
 * scroll view.
 *
 * The first two children are assumed to be the "theme preview" and the "drawer handle" which will
 * be visible. These children will be restricted in their size since they must be entirely visible.
 * The third child will be shown as the user scrolls down and can take as much space as it needs.
 */
public class ChooserDetailLinearLayout extends LinearLayout {

    // The smaller this value the greater the effect
    public final int PARALLAX_CONSTANT = 2;

    // Child indices
    public final int THEME_PREVIEW_INDEX = 0;
    public final int DRAWER_HANDLE_INDEX = 1;
    public final int DRAWER_CONTENT_INDEX = 2;
    public final int PAGE_INDICATOR_INDEX = 3;

    private ViewPager mPager;

    public ChooserDetailLinearLayout(Context context) {
        super(context);
    }

    public ChooserDetailLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ChooserDetailLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Assumes the heightMeasureSpec will be exact. This exact data will be used
     * to determine the visible page size.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        // Drawer handle is just author/title. Should get whatever space it needs
        View drawerHandle = getChildAt(DRAWER_HANDLE_INDEX);
        measureChildWithMargins(drawerHandle, widthMeasureSpec, 0, heightMeasureSpec, 0);

        // Give the theme preview the remainder
        View themePreview = getChildAt(THEME_PREVIEW_INDEX);
        int usedHeight = drawerHandle.getMeasuredHeight();
        measureChildWithMargins(themePreview, widthMeasureSpec, 0, heightMeasureSpec, usedHeight);

        // Give the drawer content as much as space as it needs.
        View drawerContent = getChildAt(DRAWER_CONTENT_INDEX);
        final MarginLayoutParams lp = (MarginLayoutParams) drawerContent.getLayoutParams();
        int childWidthSpec  = getChildMeasureSpec(widthMeasureSpec, 0, lp.width);
        int childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        drawerContent.measure(childWidthSpec, childHeightSpec);

        //Give the page indicator as much space as it needs
        View pageIndicator = getChildAt(PAGE_INDICATOR_INDEX);
        pageIndicator.measure(childWidthSpec, childHeightSpec);

        // Measure ourself
        int width = drawerHandle.getMeasuredWidth();
        int height = themePreview.getMeasuredHeight() +
                        drawerHandle.getMeasuredHeight() + drawerContent.getMeasuredHeight();
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        // Place the page indicator above the drawer handle
        View pageIndicator = getChildAt(PAGE_INDICATOR_INDEX);
        int top = getChildAt(DRAWER_HANDLE_INDEX).getTop();
        int height = pageIndicator.getMeasuredHeight();
        pageIndicator.layout(l, top-height, r, top);
    }

    public void onScrollChanged(int l, int t, int oldl, int oldt) {
        // Give a parallax effect to the theme preview by scrolling in the
        // opposite direction of the scrollview
        int yScroll = (int) (-t / PARALLAX_CONSTANT);
        getChildAt(THEME_PREVIEW_INDEX).scrollTo(0, yScroll);
    }

    View getHandle() {
        return getChildAt(DRAWER_HANDLE_INDEX);
    }
}
