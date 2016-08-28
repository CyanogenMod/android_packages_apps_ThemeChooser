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

package org.cyanogenmod.theme.widget;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

public class AutoSnapHorizontalScrollView extends HorizontalScrollView {
    private static final int SNAP_ON_UP_DELAY = 250;

    private int mScrollPositionOnUp;

    enum EventStates {
        SCROLLING,
        FLING
    }

    private EventStates mSystemState = EventStates.SCROLLING;

    public AutoSnapHorizontalScrollView(Context context) {
        super(context);
    }

    public AutoSnapHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoSnapHorizontalScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    Runnable mSnapRunnable = new Runnable(){
        @Override
        public void run() {
            snapItems();
            mSystemState = EventStates.SCROLLING;
        }
    };

    /**
     * Added runnable for snapping on an item when the user lifts up their finger and
     * there is no scrolling taking place (i.e. no flinging)
     */
    Runnable mSnapOnUpRunnable = new Runnable(){
        @Override
        public void run() {
            int scrollX = getScrollX();
            if (scrollX != mScrollPositionOnUp) {
                mScrollPositionOnUp = scrollX;
                postDelayed(mSnapOnUpRunnable, SNAP_ON_UP_DELAY);
            } else {
                snapItems();
                mSystemState = EventStates.SCROLLING;
            }
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            mSystemState = EventStates.FLING;
            removeCallbacks(mSnapRunnable);
            mScrollPositionOnUp = getScrollX();
            postDelayed(mSnapOnUpRunnable, SNAP_ON_UP_DELAY);
        } else if (action == MotionEvent.ACTION_DOWN) {
            mSystemState = EventStates.SCROLLING;
            removeCallbacks(mSnapRunnable);
            removeCallbacks(mSnapOnUpRunnable);
        }
        return super.onTouchEvent(ev);
    }

    private void snapItems() {
        Rect parentBounds = new Rect();
        getDrawingRect(parentBounds);
        Rect childBounds = new Rect();
        ViewGroup parent = (ViewGroup) getChildAt(0);
        for (int i = 0; i < parent.getChildCount(); i++) {
            View view = parent.getChildAt(i);
            view.getHitRect(childBounds);
            if (childBounds.right >= parentBounds.left && childBounds.left <= parentBounds.left) {
                // First partially visible child
                if ((childBounds.right - parentBounds.left) >=
                        (parentBounds.left - childBounds.left)) {
                    smoothScrollTo(Math.abs(childBounds.left), 0);
                } else {
                    /**
                     * Added code to take into account dividers so that we do not see
                     * one on the edge of the screen when items snap in place.
                     */
                    int dividerWidth = 0;
                    if (parent instanceof LinearLayout) {
                        dividerWidth = ((LinearLayout) parent).getDividerWidth();
                    }
                    smoothScrollTo(Math.abs(childBounds.right) + dividerWidth, 0);
                }
                break;
            }
        }
    }

    // Overwrite measureChildX as we want our child to be able to tell the
    // parents width but do not impose any limits (AT_MOST; AT_MAX)
    @Override
    protected void measureChild(View child, int parentWidthMeasureSpec,
            int parentHeightMeasureSpec) {
        ViewGroup.LayoutParams lp = child.getLayoutParams();

        int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec, mPaddingTop
                + mPaddingBottom, lp.height);

        int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                MeasureSpec.getSize(parentWidthMeasureSpec) - (mPaddingLeft + mPaddingRight),
                MeasureSpec.UNSPECIFIED);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed,
            int parentHeightMeasureSpec, int heightUsed) {
        MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

        int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
                mPaddingTop + mPaddingBottom + lp.topMargin + lp.bottomMargin
                        + heightUsed, lp.height);

        int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                MeasureSpec.getSize(parentWidthMeasureSpec) -
                        (mPaddingLeft + mPaddingRight + lp.leftMargin + lp.rightMargin
                        + widthUsed), MeasureSpec.UNSPECIFIED);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        if (mSystemState == EventStates.SCROLLING) {
            return;
        }
        if (Math.abs(l - oldl) <= 1 && mSystemState == EventStates.FLING) {
            removeCallbacks(mSnapRunnable);
            postDelayed(mSnapRunnable, 100);
        }
    }
}
