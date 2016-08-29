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

package org.cyanogenmod.theme.perapptheming;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;

import org.cyanogenmod.theme.chooser2.R;

public class PerAppThemeListLayout extends FrameLayout {
    private PerAppThemingWindow mWindow;
    private PointF mCenter;

    private float mMaxRadius;
    private float mTargetRadius;
    private float mStartRadius;
    private float mCurrentRadius;

    private ValueAnimator mAnimator;
    private boolean mIsAnimating;

    private Path mRevealPath;

    public PerAppThemeListLayout(Context context) {
        this(context, null);
    }

    public PerAppThemeListLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PerAppThemeListLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final Resources res = getResources();
        float width = res.getDimension(R.dimen.theme_list_width);
        float height = res.getDimension(R.dimen.theme_list_max_height);
        mMaxRadius = (float) Math.sqrt(Math.pow(width, 2) + Math.pow(height, 2));
        mRevealPath = new Path();

        mAnimator = new ValueAnimator();
        mAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mAnimator.addListener(mAnimationListener);
        mAnimator.addUpdateListener(mUpdateListener);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN &&
                event.getKeyCode() == KeyEvent.KEYCODE_BACK && mWindow != null) {
            mWindow.hideThemeList();
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isEnabled() && event.getAction() == MotionEvent.ACTION_DOWN && mWindow != null) {
            mWindow.hideThemeList();
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (!mIsAnimating) {
            super.dispatchDraw(canvas);
        } else {
            final int state = canvas.save();
            mRevealPath.reset();
            mRevealPath.addCircle(mCenter.x, mCenter.y, mCurrentRadius, Path.Direction.CW);
            canvas.clipPath(mRevealPath);
            super.dispatchDraw(canvas);
            canvas.restoreToCount(state);
        }
    }

    public void setPerAppThemingWindow(PerAppThemingWindow window) {
        mWindow = window;
    }

    /**
     * Perform a circular reveal from center cx,cy
     * @param cx X position of center
     * @param cy Y position of center
     * @param duration Duration of animation
     */
    public void circularReveal(float cx, float cy, long duration) {
        mCenter = new PointF(cx, cy);
        mIsAnimating = true;

        mStartRadius = mCurrentRadius;
        mTargetRadius = mMaxRadius;
        startAnimation(duration);
    }

    /**
     * Perform a circular hide from center cx,cy
     * @param cx X position of center
     * @param cy Y position of center
     * @param duration Duration of animation
     */
    public void circularHide(float cx, float cy, long duration) {
        mCenter = new PointF(cx, cy);
        mIsAnimating = true;

        mStartRadius = mCurrentRadius;
        mTargetRadius = 0f;
        startAnimation(duration);
    }

    private void startAnimation(long duration) {
        getChildAt(0).setVisibility(View.VISIBLE);
        mAnimator.setFloatValues(mStartRadius, mTargetRadius);
        mAnimator.setDuration(duration);
        mAnimator.start();
    }

    private ValueAnimator.AnimatorUpdateListener mUpdateListener =
            new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            Float value = (Float) animation.getAnimatedValue();
            mCurrentRadius = value.floatValue();
            invalidate();
        }
    };

    private Animator.AnimatorListener mAnimationListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {}

        @Override
        public void onAnimationEnd(Animator animation) {
            mIsAnimating = false;
            if (mCurrentRadius <= 0) {
                getChildAt(0).setVisibility(INVISIBLE);
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {}

        @Override
        public void onAnimationRepeat(Animator animation) {}
    };
}
