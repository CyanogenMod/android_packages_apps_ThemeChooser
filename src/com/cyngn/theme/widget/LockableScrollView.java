/*
 * Copyright (C) 2014 The Cyanogen, Inc
 */
package com.cyngn.theme.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

public class LockableScrollView extends ScrollView {
    private boolean mScrollingEnabled = true;

    public LockableScrollView(Context context) {
        this(context, null);
    }

    public LockableScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LockableScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setScrollingEnabled(boolean enabled) {
        mScrollingEnabled = enabled;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mScrollingEnabled && super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return mScrollingEnabled && super.onTouchEvent(ev);
            default:
                return super.onTouchEvent(ev);
        }
    }

    @Override
    public void setOverScrollMode(int mode) {
        // Some themes can cause theme chooser to crash when creating the EdgeEffects for
        // the scroll view.  If an exception occurs we fallback to no overscroll
        try {
            super.setOverScrollMode(mode);
        } catch (Exception e) {
            super.setOverScrollMode(OVER_SCROLL_NEVER);
        }
    }
}
