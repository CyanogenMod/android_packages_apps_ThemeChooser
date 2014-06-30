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
package android.support.v4.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;


import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ThemeViewPager extends ViewPager {
    private boolean mExpanded;

    private boolean mIsDragging = false;
    private float mSlop;
    private float mLastX;
    private float mLastY;

    public ThemeViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    private void initView(Context context) {
        mSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public void setExpanded(boolean expanded) {
        mExpanded = expanded;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastX = ev.getX();
                mLastY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float xDist = Math.abs(mLastX - ev.getX());
                float yDist = Math.abs(mLastY - ev.getY());
                if (xDist > mSlop || yDist > mSlop) {
                    mIsDragging = true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                mIsDragging = false;
                break;
            case MotionEvent.ACTION_UP:
                if (!mIsDragging) {
                    performClick();
                }
                mIsDragging = false;
                break;
        }

        if (mExpanded) {
            return true;
        }

        return super.onTouchEvent(ev);
    }

    /**
     * This method will return the view associated with a given position. This is neccessary
     * because the index value in 'getChildAt(index)' does not neccessarily associate with
     * the viewpager's position.
     *
     */
    public View getViewForPosition(int position) {
        View view = null;
        for(int i=0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            ItemInfo info = infoForChild(v);
            if (position == info.position) {
                view = v;
                break;
            }
        }
        return view;
    }
}
