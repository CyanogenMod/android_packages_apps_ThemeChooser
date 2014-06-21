/*
 * Copyright (c) 2012 Wireless Designs, LLC
 * Portions copyright (C) 2014, The CyanogenMod Project
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.cyanogenmod.theme.chooserv2;

import android.content.Context;
import android.graphics.Point;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

/**
 * PagerContainer: A layout that displays a ViewPager with its children that are outside
 * the typical pager bounds.
 */
public class PagerContainer extends FrameLayout implements ViewPager.OnPageChangeListener {
    private static final int ANIMATE_OUT_DURATION = 300;
    private static final int ANIMATE_OUT_INTERPOLATE_FACTOR = 1;
    private static final int ANIMATE_IN_DURATION = 300;
    private static final int ANIMATE_IN_INTERPOLATE_FACTOR = 2;

    private ThemeViewPager mPager;
    private Point mCenter = new Point();
    private Point mInitialTouch = new Point();

    boolean mNeedsRedraw = false;

    public PagerContainer(Context context) {
        super(context);
        init();
    }

    public PagerContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PagerContainer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        //Disable clipping of children so non-selected pages are visible
        setClipChildren(false);

        //Child clipping doesn't work with hardware acceleration in Android 3.x/4.x
        //You need to set this value here if using hardware acceleration in an
        // application targeted at these releases.
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onFinishInflate() {
        try {
            mPager = (ThemeViewPager) getChildAt(0);
            mPager.setOnPageChangeListener(this);
        } catch (Exception e) {
            throw new IllegalStateException("The root child of PagerContainer must be a ViewPager");
        }
    }

    public ThemeViewPager getViewPager() {
        return mPager;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mCenter.x = w / 2;
        mCenter.y = h / 2;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        //We capture any touches not already handled by the ViewPager
        // to implement scrolling from a touch outside the pager bounds.
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mInitialTouch.x = (int)ev.getX();
                mInitialTouch.y = (int)ev.getY();
            default:
                ev.offsetLocation(mCenter.x - mInitialTouch.x, mCenter.y - mInitialTouch.y);
                break;
        }

        return mPager.dispatchTouchEvent(ev);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        //Force the container to redraw on scrolling.
        //Without this the outer pages render initially and then stay static
        if (mNeedsRedraw) invalidate();
    }

    @Override
    public void onPageSelected(int position) { }

    @Override
    public void onPageScrollStateChanged(int state) {
        mNeedsRedraw = (state != ThemeViewPager.SCROLL_STATE_IDLE);
    }

    public void expand() {
        mPager.setExpanded(true);
        int current = mPager.getCurrentItem();

        if (current != 0) {
            View lchild = mPager.getChildAt(current - 1);
            animateChildOut(lchild, lchild.getX() - getWidth());
        }

        if (current < mPager.getAdapter().getCount() - 1) {
            View rchild = mPager.getChildAt(current + 1);
            animateChildOut(rchild, rchild.getX() + getWidth());
        }
    }

    public void collapse() {
        mPager.setExpanded(false);
        int current = mPager.getCurrentItem();

        if (current != 0) {
            View lchild = mPager.getChildAt(current - 1);
            animateChildIn(lchild);
        }

        if (current < mPager.getAdapter().getCount() - 1) {
            View rchild = mPager.getChildAt(current + 1);
            animateChildIn(rchild);
        }
    }

    private void animateChildOut(final View v, float endX) {
        v.animate()
                .translationX(endX)
                .setDuration(ANIMATE_OUT_DURATION)
                .setInterpolator(new AccelerateInterpolator(ANIMATE_OUT_INTERPOLATE_FACTOR));
    }

    private void animateChildIn(final View v) {
        v.animate()
                .translationX(0)
                .setDuration(ANIMATE_IN_DURATION)
                .setInterpolator(new DecelerateInterpolator(ANIMATE_IN_INTERPOLATE_FACTOR));
    }
}