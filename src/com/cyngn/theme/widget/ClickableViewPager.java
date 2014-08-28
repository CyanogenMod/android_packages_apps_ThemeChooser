/*
 * Copyright (C) 2014 The CyanogenMod, Inc.
 */
package com.cyngn.theme.widget;

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