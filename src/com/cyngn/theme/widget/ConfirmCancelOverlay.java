/*
 * Copyright (C) 2014 Cyanogen, Inc.
 */
package com.cyngn.theme.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.cyngn.theme.chooser.R;

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
