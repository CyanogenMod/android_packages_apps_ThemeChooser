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

package android.support.v4.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import org.cyanogenmod.theme.chooser2.R;

public class ThemeViewPager extends ViewPager {
    private static final String TAG = ThemeViewPager.class.getSimpleName();
    private boolean mExpanded;
    private boolean mIsAnimating;

    private boolean mIsDragging = false;
    private float mSlop;
    private float mLastX;
    private float mLastY;
    private boolean mScrollingEnabled = true;
    private boolean mClickedContent = false;

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

    public void setAnimating(boolean isAnimating) {
        mIsAnimating = isAnimating;
    }

    public boolean isClickedOnContent() {
        return mClickedContent;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean intercept = false;

        if (!mExpanded && isEnabled()  && !mIsAnimating)  {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    intercept = getChildCount() > 0 && !isTouching(R.id.customize, ev)
                            && !isTouching(R.id.overflow, ev)
                            && !isTouching(R.id.reset, ev)
                            && !isTouching(R.id.delete, ev)
                            && !isTouching(R.id.confirm_cancel_overlay, ev)
                            && !isTouching(R.id.customize_reset_theme_layout, ev);
                    break;
            }
        }

        return intercept;
    }

    private boolean isTouching(int viewId, MotionEvent ev) {
        int x = (int) ev.getRawX();
        int y = (int) ev.getRawY();
        View v = getViewForPosition(getCurrentItem());
        if (v == null) return false;
        View touchedView = v.findViewById(viewId);
        if (touchedView == null ||
                touchedView.getVisibility() != View.VISIBLE ||
                !touchedView.isEnabled()) {
            return false;
        }

        int location[] = new int[2];
        touchedView.getLocationOnScreen(location);
        int viewX = location[0];
        int viewY = location[1];

        if((x > viewX && x < (viewX + touchedView.getWidth())) &&
                ( y > viewY && y < (viewY + touchedView.getHeight()))){
            return true;
        } else {
            return false;
        }
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
                    mClickedContent = isTouching(R.id.clickable_view, ev);

                    View clickableView = getViewForPosition(getCurrentItem(), R.id.clickable_view);
                    if (clickableView != null) {
                        // only play the click sound when we click on the content :)
                        setSoundEffectsEnabled(mClickedContent &&
                                clickableView.isSoundEffectsEnabled());
                    }
                    performClick();
                }
                mIsDragging = false;
                break;
        }

        if (mExpanded || (!mScrollingEnabled && mIsDragging)) {
            return false;
        }

        /**
         * Work around AOSP issue #18990
         * https://code.google.com/p/android/issues/detail?id=18990
         */
        try {
            return super.onTouchEvent(ev);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Failed while calling super.onTouchEvent()", e);
        }
        return false;
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
            if (info != null && position == info.position) {
                view = v;
                break;
            }
        }
        return view;
    }

    /**
     * Like getViewForPosition(int position), but will return a specific child view with
     * id viewId.
     */
    public View getViewForPosition(int position, int id) {
        View parent = getViewForPosition(position);
        if (parent != null) {
            return parent.findViewById(id);
        }
        return null;
    }

    public void setScrollingEnabled(boolean enabled) {
        mScrollingEnabled = enabled;
    }
}
