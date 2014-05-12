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
package org.cyanogenmod.theme.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import org.cyanogenmod.theme.util.BootAnimationHelper;

import java.io.IOException;
import java.util.List;
import java.util.zip.ZipFile;

public class BootAniImageView extends ImageView {
    private static final String TAG = BootAniImageView.class.getName();

    private static final int MAX_BUFFERS = 2;

    private Bitmap[] mBuffers = new Bitmap[MAX_BUFFERS];
    private int mReadBufferIndex = 0;
    private int mWriteBufferIndex = 0;
    private ZipFile mBootAniZip;

    private List<BootAnimationHelper.AnimationPart> mAnimationParts;
    private int mCurrentPart;
    private int mCurrentFrame;
    private int mCurrentPartPlayCount;
    private int mFrameDuration;

    private boolean mActive = false;

    public BootAniImageView(Context context) {
        this(context, null);
    }

    public BootAniImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BootAniImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mActive = false;
        removeCallbacks(mUpdateAnimationRunnable);

        if (mBootAniZip != null) {
            try {
                mBootAniZip.close();
            } catch (IOException e) {
            }
        }

    }

    public boolean setBootAnimation(ZipFile bootAni) {
        mBootAniZip = bootAni;
        try {
            mAnimationParts = BootAnimationHelper.parseAnimation(mContext, mBootAniZip);
        } catch (IOException e) {
            return false;
        }

        // pre-allocate bitmap buffers
        final BootAnimationHelper.AnimationPart part = mAnimationParts.get(0);
        for (int i = 0; i < mBuffers.length; i++) {
            mBuffers[i] = Bitmap.createBitmap(part.width, part.height, Bitmap.Config.RGB_565);
        }
        mCurrentPart = 0;
        mCurrentPartPlayCount = part.playCount;
        mFrameDuration = part.frameRateMillis;

        getNextFrame();

        return true;
    }

    private void getNextFrame() {
        synchronized (mBuffers[mWriteBufferIndex]) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inBitmap = mBuffers[mWriteBufferIndex];
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            final BootAnimationHelper.AnimationPart part = mAnimationParts.get(mCurrentPart);
            try {
                BitmapFactory.decodeStream(mBootAniZip.getInputStream(mBootAniZip.getEntry(
                        part.frames.get(mCurrentFrame++))), null, opts);
            } catch (Exception e) {
                Log.w(TAG, "Unable to get next frame", e);
            }
            mWriteBufferIndex = (mWriteBufferIndex + 1) % MAX_BUFFERS;
            if (mCurrentFrame >= part.frames.size()) {
                if (mCurrentPartPlayCount > 0) {
                    if (--mCurrentPartPlayCount == 0) {
                        mCurrentPart++;
                        mCurrentFrame = 0;
                        mCurrentPartPlayCount = mAnimationParts.get(mCurrentPart).playCount;
                    }
                } else {
                    mCurrentFrame = 0;
                }
            }
        }
    }

    public void start() {
        mActive = true;
        post(mUpdateAnimationRunnable);
    }

    private Runnable mUpdateAnimationRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mActive) return;
            synchronized (mBuffers[mReadBufferIndex]) {
                BootAniImageView.this.postDelayed(mUpdateAnimationRunnable, mFrameDuration);
                BootAniImageView.this.post(mUpdateImageRunnable);
                mReadBufferIndex = (mReadBufferIndex + 1) % MAX_BUFFERS;
                getNextFrame();
            }
        }
    };

    private Runnable mUpdateImageRunnable = new Runnable() {
        @Override
        public void run() {
            setImageBitmap(mBuffers[mReadBufferIndex]);
        }
    };
}
