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

import com.cyngn.theme.chooser.R;

public class ThemeViewPager extends ViewPager {
    private boolean mExpanded;
    private boolean mIsAnimating;

    private boolean mIsDragging = false;
    private float mSlop;
    private float mLastX;
    private float mLastY;
    private boolean mScrollingEnabled = true;

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

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean intercept = false;

        if (!mExpanded && isEnabled()  && !mIsAnimating)  {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    intercept = getChildCount() > 0 && !isTouching(R.id.customize, ev) && !isTouching(R.id.overflow, ev)
                            && !isTouching(R.id.apply_theme_layout, ev);
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
                touchedView.getVisibility() != View.VISIBLE) {
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
                    performClick();
                }
                mIsDragging = false;
                break;
        }

        if (mExpanded || !mScrollingEnabled) {
            return false;
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

    public void setScrollingEnabled(boolean enabled) {
        mScrollingEnabled = enabled;
    }
}
