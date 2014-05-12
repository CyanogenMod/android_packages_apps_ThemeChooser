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
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

/**
 * A custom ScrollView capable of alerting its children when a scroll has changed.
 *
 * This custom view will also measure its children differently. Normally a child in scrollview
 * receives an UNSPECIFIED MeasureSpec so that the child will size itself as large as it needs to be.
 *
 * In our case we want to have a unique layout where the first full page of the scrollview
 * has very particular content
 * ie the "drawer title/author", and the viewpager taking the remaining part of the screen", with the
 * theme component checkboxes taking as much space as it needs.
 *
 **/
public class ChooserDetailScrollView extends ScrollView {
    private float xDistance;
    private float yDistance;
    private float lastX;
    private float lastY;

    public ChooserDetailScrollView(Context context) {
        super(context);
    }

    public ChooserDetailScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ChooserDetailScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed,
                                           int parentHeightMeasureSpec, int heightUsed) {
        final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

        final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
                mPaddingLeft + mPaddingRight + lp.leftMargin + lp.rightMargin
                        + widthUsed, lp.width);
        child.measure(childWidthMeasureSpec, parentHeightMeasureSpec);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                xDistance = yDistance = 0f;
                lastX = ev.getX();
                lastY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                final float curX = ev.getX();
                final float curY = ev.getY();
                xDistance = Math.abs(curX - lastX);
                yDistance = Math.abs(curY - lastY);
                lastX = curX;
                lastY = curY;
                if(xDistance > yDistance) return false;
        }

        return super.onInterceptTouchEvent(ev);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        ((ChooserDetailLinearLayout)getChildAt(0)).onScrollChanged(l, t, oldl, oldt);
    }

    public View getHandle() {
        return ((ChooserDetailLinearLayout)getChildAt(0)).getHandle();
    }
}
