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

package org.cyanogenmod.theme.chooser2;

import android.content.Context;
import android.graphics.Point;
import android.support.v4.view.ThemeViewPager;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

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
    private int mCollapsedHeight;
    private boolean mIsAnimating = false;

    boolean mNeedsRedraw = false;

    public PagerContainer(Context context) {
        this(context, null, 0);
    }

    public PagerContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PagerContainer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mCollapsedHeight = generateLayoutParams(attrs).height;

        //Disable clipping of children so non-selected pages are visible
        setClipChildren(false);
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
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mIsAnimating) return true;
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Do not allow touch events to propagate if we are animating
        if (mIsAnimating) return true;

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

    public void setIsAnimating(boolean isAnimating) {
        mIsAnimating = isAnimating;
    }

    public void expand() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(getLayoutParams());
        params.height = LinearLayout.LayoutParams.MATCH_PARENT;
        setLayoutParams(params);

        mPager.setExpanded(true);

        final int current = mPager.getCurrentItem();
        final int prevY = (int) getY();

        //Since our viewpager's width is changing to fill the screen
        //we must start the left/right children of the current page inwards on first draw
        final int lChildPrevXf;
        final int rChildPrevXf;

        if (current != 0) {
            final View lchild = mPager.getViewForPosition(current - 1);
            lChildPrevXf = (int) lchild.getX();
        } else {
            lChildPrevXf = 0;
        }

        if (current < mPager.getAdapter().getCount() - 1) {
            View rchild =  mPager.getViewForPosition(current + 1);
            rChildPrevXf = (int) rchild.getX();
        } else {
            rChildPrevXf = 0;
        }


        final ViewTreeObserver observer = mPager.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                observer.removeOnPreDrawListener(this);
                if (current != 0) {
                    View lchild = mPager.getViewForPosition(current - 1);
                    lchild.setTranslationY(prevY - getY());
                    lchild.setX(lChildPrevXf);
                    animateChildOut(lchild, -getWidth());
                }

                if (current < mPager.getAdapter().getCount() - 1) {
                    View rchild =  mPager.getViewForPosition(current + 1);
                    rchild.setX(rChildPrevXf);
                    rchild.setTranslationY(prevY - getY());
                    animateChildOut(rchild, getWidth());
                }
                return false;
            }
        });
    }

    public void collapse() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(getLayoutParams());
        params.height = mCollapsedHeight;
        setLayoutParams(params);

        mPager.setExpanded(false);
        int current = mPager.getCurrentItem();
        final int prevY = (int) getY();

        if (current != 0) {
            View lchild = mPager.getViewForPosition(current - 1);
            lchild.setTranslationY(0);
            animateChildIn(lchild);
        }

        if (current < mPager.getAdapter().getCount() - 1) {
            View rchild = mPager.getViewForPosition(current + 1);
            rchild.setTranslationY(0);
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