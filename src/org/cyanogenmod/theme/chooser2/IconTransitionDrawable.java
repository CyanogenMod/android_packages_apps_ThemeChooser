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

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.SystemClock;

/**
 * An extension of LayerDrawables that is intended to cross-fade between
 * the first and second layer. To start the transition, call {@link #startTransition(int)}. To
 * display just the first layer, call {@link #resetTransition()}.
 * <p>
 * It can be defined in an XML file with the <code>&lt;transition></code> element.
 * Each Drawable in the transition is defined in a nested <code>&lt;item></code>. For more
 * information, see the guide to <a
 * href="{@docRoot}guide/topics/resources/drawable-resource.html">Drawable Resources</a>.</p>
 *
 * @attr ref android.R.styleable#LayerDrawableItem_left
 * @attr ref android.R.styleable#LayerDrawableItem_top
 * @attr ref android.R.styleable#LayerDrawableItem_right
 * @attr ref android.R.styleable#LayerDrawableItem_bottom
 * @attr ref android.R.styleable#LayerDrawableItem_drawable
 * @attr ref android.R.styleable#LayerDrawableItem_id
 *
 */
public class IconTransitionDrawable extends LayerDrawable {

    /**
     * A transition is about to start.
     */
    private static final int TRANSITION_STARTING = 0;

    /**
     * The transition has started and the animation is in progress
     */
    private static final int TRANSITION_RUNNING = 1;

    /**
     * No transition will be applied
     */
    private static final int TRANSITION_NONE = 2;

    /**
     * The current state of the transition. One of {@link #TRANSITION_STARTING},
     * {@link #TRANSITION_RUNNING} and {@link #TRANSITION_NONE}
     */
    private int mTransitionState = TRANSITION_NONE;

    private long mStartTimeMillis;
    private int mFrom;
    private int mTo;
    private int mDuration;
    private int mAlpha = 0;
    private float mFromScale;
    private float mToScale;

    /**
     * Create a new transition drawable with the specified list of layers. At least
     * 2 layers are required for this drawable to work properly.
     */
    public IconTransitionDrawable(Drawable[] layers) {
        super(layers);
    }

    /**
     * Begin the second layer on top of the first layer.
     *
     * @param durationMillis The length of the transition in milliseconds
     */
    public void startTransition(int durationMillis) {
        mFrom = 0;
        mTo = 255;
        mAlpha = 0;
        mFromScale = 0f;
        mToScale = 1.0f;
        mDuration = durationMillis;
        mTransitionState = TRANSITION_STARTING;
        invalidateSelf();
    }

    /**
     * Show only the first layer.
     */
    public void resetTransition() {
        mAlpha = 0;
        mTransitionState = TRANSITION_NONE;
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        boolean done = true;
        float scale = 0f;

        switch (mTransitionState) {
            case TRANSITION_STARTING:
                mStartTimeMillis = SystemClock.uptimeMillis();
                done = false;
                mTransitionState = TRANSITION_RUNNING;
                break;

            case TRANSITION_RUNNING:
                if (mStartTimeMillis >= 0) {
                    float normalized = (float)
                            (SystemClock.uptimeMillis() - mStartTimeMillis) / mDuration;
                    done = normalized >= 1.0f;
                    normalized = Math.min(normalized, 1.0f);
                    mAlpha = (int) (mFrom  + (mTo - mFrom) * normalized);
                    scale = mFromScale + (mToScale - mFromScale) * normalized;
                }
                break;
        }

        final int alpha = mAlpha;

        if (done) {
            // the setAlpha() calls below trigger invalidation and redraw. If we're done, just draw
            // the appropriate drawable[s] and return
            if (alpha == 0) {
                getDrawable(0).draw(canvas);
            }
            if (alpha == 0xFF) {
                getDrawable(1).draw(canvas);

            }
            return;
        }

        Drawable d;
        d = getDrawable(0);
        d.setAlpha(255 - alpha);
        int cx = getIntrinsicWidth() / 2;
        int cy = getIntrinsicHeight() / 2;
        canvas.save();
        canvas.scale(1.0f - scale, 1.0f - scale, cx, cy);
        d.draw(canvas);
        canvas.restore();
        d.setAlpha(0xFF);

        if (alpha > 0) {
            d = getDrawable(1);
            d.setAlpha(alpha);
            canvas.save();
            canvas.scale(scale, scale, cx, cy);
            d.draw(canvas);
            canvas.restore();
            d.setAlpha(0xFF);
        }

        if (!done) {
            invalidateSelf();
        }
    }
}
