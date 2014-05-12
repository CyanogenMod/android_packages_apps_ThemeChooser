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
import android.view.MotionEvent;
import android.view.ViewConfiguration;

/**
 * Adds click functionality for the view pager since it not normally clickable
 * and focus/clickable attributes have no impact on it.
 */
public class ClickableViewPager extends ViewPager {
    private boolean mIsDragging = false;
    private float mSlop;
    private float mLastX;
    private float mLastY;

    public ClickableViewPager(Context context) {
        super(context);
        initView(context);
    }

    public ClickableViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    private void initView(Context context) {
        mSlop = ViewConfiguration.get(context).getScaledTouchSlop();
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
        return super.onTouchEvent(ev);
    }
}