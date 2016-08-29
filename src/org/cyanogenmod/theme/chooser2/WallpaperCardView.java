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
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewOverlay;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class WallpaperCardView extends ComponentCardView {
    protected ImageView mImage;
    protected TextView mLabel;

    public WallpaperCardView(Context context) {
        this(context, null);
    }

    public WallpaperCardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WallpaperCardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WallpaperCardView);
        String labelText = a.getString(R.styleable.WallpaperCardView_labelText);
        a.recycle();

        setOrientation(VERTICAL);

        setBackgroundResource(R.drawable.card_bg);

        LayoutInflater inflater = LayoutInflater.from(mContext);
        FrameLayout frameLayout =
                (FrameLayout) inflater.inflate(R.layout.wallpaper_card, this, false);
        addView(frameLayout);
        mLabel = (TextView) frameLayout.findViewById(R.id.label);
        mImage = (ImageView) frameLayout.findViewById(R.id.image);

        mLabel.setText(labelText);
    }

    public void setWallpaper(Drawable drawable) {
        mImage.setImageDrawable(drawable);
        View none = findViewById(R.id.none);
        if (drawable == null) {
            setBackgroundResource(R.drawable.card_wallpapertoggled_bg);
            if (none != null) {
                none.setVisibility(View.VISIBLE);
            }
            if (mLabel != null) {
                mLabel.setBackgroundResource(0);
            }
        } else {
            setBackgroundResource(0);

            if (none != null) {
                none.setVisibility(View.GONE);
            }
            if (mLabel != null) {
                mLabel.setBackgroundResource(R.drawable.wallpaper_label_bg);
            }
        }
    }

    public void clearWallpaper() {
        mImage.setImageDrawable(null);
        setBackgroundResource(R.drawable.card_bg);
    }

    public Drawable getWallpaperDrawable() {
        return mImage.getDrawable();
    }

    @Override
    public void expand(boolean showLabel) {
        setEnabled(true);
    }

    @Override
    public void collapse() {
        setEnabled(false);
    }

    /**
     * Animates a change in the content of the card
     * @param v View in card to animate
     * @param overlay Drawable to animate as a ViewOverlay
     * @param duration Duration of animation
     */
    @Override
    public void animateContentChange(View v, final Drawable overlay, long duration) {
        // Since the wallpaper IS the content, we will ignore the view passed in and animate
        // the entire card
        final ViewOverlay viewOverlay = this.getOverlay();
        viewOverlay.add(overlay);
        final int x = 0;
        final int y = 0;
        final int width = v.getWidth();
        final int height = v.getHeight();
        overlay.setBounds(x, y, x + width, y + height);

        final ValueAnimator overlayAnimator = ValueAnimator.ofFloat(1f, 0f);
        overlayAnimator.setDuration(duration);
        overlayAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (Float) animation.getAnimatedValue();
                overlay.setAlpha((int) (255 * value));
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
        set.play(ObjectAnimator.ofFloat(overlay, "alpha", 0f, 1f))
                .with(overlayAnimator);
        set.start();
    }
}
