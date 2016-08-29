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
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.cyanogenmod.theme.chooser2.R;

public class ConfirmCancelOverlay extends FrameLayout {

    private View mAcceptButton;
    private View mCancelButton;
    private TextView mTitle;

    private OnOverlayDismissedListener mListener;

    public ConfirmCancelOverlay(Context context) {
        this(context, null);
    }

    public ConfirmCancelOverlay(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ConfirmCancelOverlay(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mAcceptButton = findViewById(R.id.accept);
        mCancelButton = findViewById(R.id.cancel);
        mTitle = (TextView) findViewById(R.id.overlay_title);

        mAcceptButton.setOnClickListener(mClickListener);
        mCancelButton.setOnClickListener(mClickListener);
    }

    public void setTitle(CharSequence title) {
        mTitle.setText(title);
    }

    public void setTitle(int resId) {
        mTitle.setText(resId);
    }

    public void setOnOverlayDismissedListener(OnOverlayDismissedListener listener) {
        mListener = listener;
    }

    public void dismiss() {
        if (mListener != null) {
            mListener.onDismissed(false);
        }
    }

    private OnClickListener mClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mListener != null) {
                mListener.onDismissed(v == mAcceptButton);
            }
        }
    };

    public interface OnOverlayDismissedListener {
        public void onDismissed(boolean accepted);
    }
}
