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

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOverlay;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ComponentCardView extends LinearLayout {
    public static final int CARD_FADE_DURATION = 300;

    private static final float SEMI_OPAQUE_ALPHA = 0.4f;

    protected TextView mLabel;
    protected View mEmptyView;
    protected View mContentView;

    // Expanded Padding
    int mExpandPadLeft;
    int mExpandPadTop;
    int mExpandPadRight;
    int mExpandPadBottom;

    // The background drawable is returning an alpha of 0 regardless of what we set it to
    // so this will help us keep track of what the drawable's alpha is at.
    private int mBackgroundAlpha = 255;

    public ComponentCardView(Context context) {
        this(context, null);
    }

    public ComponentCardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ComponentCardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setEnabled(false);
    }

    @Override
    protected void onFinishInflate() {
        mLabel = (TextView) findViewById(R.id.label);

        Resources r = getContext().getResources();
        mExpandPadLeft =
                (int) r.getDimension(R.dimen.card_padding_left_right) + getPaddingLeft();
        mExpandPadTop =
                (int) r.getDimension(R.dimen.card_padding_top) + getPaddingTop();
        mExpandPadRight =
                (int) r.getDimension(R.dimen.card_padding_left_right) + getPaddingRight();
        mExpandPadBottom =
                (int) r.getDimension(R.dimen.card_padding_bottom) + getPaddingBottom();
        mEmptyView = findViewById(R.id.empty);
        mContentView = findViewById(R.id.content);
    }

    public void expand(boolean showLabel) {
        setEnabled(true);
        TransitionDrawable bg = null;
        if (getBackground() instanceof TransitionDrawable) {
            bg = (TransitionDrawable) getBackground();
        }
        if (bg != null) {
            Rect paddingRect = new Rect();
            bg.getPadding(paddingRect);
        }

        setPadding(mExpandPadLeft, 0, mExpandPadRight, mExpandPadBottom);

        if (mLabel != null) {
            mLabel.setAlpha(showLabel ? 1f : 0f);
            mLabel.setVisibility(View.VISIBLE);
        }
    }

    public void animateExpand() {
        if (getBackground() instanceof TransitionDrawable) {
            TransitionDrawable background = (TransitionDrawable) getBackground();
            if (mLabel != null) {
                mLabel.setVisibility(View.VISIBLE);
                mLabel.setAlpha(0f);
                mLabel.animate().alpha(1f).setDuration(CARD_FADE_DURATION).start();
            }
            background.startTransition(CARD_FADE_DURATION);
        }
    }

    public void collapse() {
        setEnabled(false);
        if (mLabel != null) {
            mLabel.setVisibility(View.GONE);
        }
        setPadding(0, 0, 0, 0);
    }

    public void animateFadeOut() {
        if (mLabel != null) {
            mLabel.animate().alpha(0f).setDuration(CARD_FADE_DURATION);
        }

        if (getBackground() instanceof TransitionDrawable) {
            TransitionDrawable background = (TransitionDrawable) getBackground();
            background.reverseTransition(CARD_FADE_DURATION);
        }
    }

    /**
     * Animates the card background and the title to 20% opacity.
     */
    public void animateCardFadeOut() {
        this.animate().alpha(SEMI_OPAQUE_ALPHA).setDuration(CARD_FADE_DURATION);
    }

    /**
     * Animates the card background and the title back to full opacity.
     */
    public void animateCardFadeIn() {
        this.animate().alpha(1f).setDuration(CARD_FADE_DURATION);
    }

    /**
     * Animates a change in the content of the card
     * @param v View in card to animate
     * @param overlay Drawable to animate as a ViewOverlay
     * @param duration Duration of animation
     */
    public void animateContentChange(View v, final Drawable overlay, long duration) {
        final ViewOverlay viewOverlay = this.getOverlay();
        viewOverlay.add(overlay);
        final int x = getRelativeLeft(v);
        final int y = getRelativeTop(v);
        final int width = v.getWidth();
        final int height = v.getHeight();
        overlay.setBounds(x, y, x + v.getWidth(), y + v.getHeight());

        final ValueAnimator overlayAnimator = ValueAnimator.ofFloat(1f, 0f);
        overlayAnimator.setDuration(duration);
        overlayAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (Float) animation.getAnimatedValue();
                overlay.setAlpha((int) (255 * value));
                int newWidth = (int) (value * width);
                int newHeight = (int) (value * height);
                int dw = (width - newWidth) / 2;
                int dh = (height - newHeight) / 2;
                overlay.setBounds(x + dw, y + dh, x + dw + newWidth, y + dh + newHeight);
            }
        });
        overlayAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {}

            @Override
            public void onAnimationEnd(Animator animation) {
                // Clear out the ViewOverlay now that we are done animating
                viewOverlay.clear();
            }

            @Override
            public void onAnimationCancel(Animator animation) {}

            @Override
            public void onAnimationRepeat(Animator animation) {}
        });

        AnimatorSet set = new AnimatorSet();
        set.play(ObjectAnimator.ofFloat(v, "alpha", 0f, 1f))
                .with(ObjectAnimator.ofFloat(v, "scaleX", 0f, 1f))
                .with(ObjectAnimator.ofFloat(v, "scaleY", 0f, 1f));
        set.setDuration(duration);

        set.start();
        overlayAnimator.start();
    }

    public void setEmptyViewEnabled(boolean enabled) {
        if (mEmptyView != null) {
            mEmptyView.setVisibility(enabled ? View.VISIBLE : View.GONE);
        }
        if (mContentView != null) {
            mContentView.setVisibility(enabled ? View.INVISIBLE : View.VISIBLE);
        }
    }

    public boolean isShowingEmptyView() {
        return mEmptyView != null && mEmptyView.getVisibility() == View.VISIBLE;
    }

    private int getRelativeTop(View v) {
        if (v.getParent() == this) {
            return v.getTop();
        } else {
            return v.getTop() + ((View) v.getParent()).getTop();
        }
    }

    private int getRelativeLeft(View v) {
        if (v.getParent() == this) {
            return v.getLeft();
        } else {
            return v.getLeft() + ((View) v.getParent()).getLeft();
        }
    }
}
