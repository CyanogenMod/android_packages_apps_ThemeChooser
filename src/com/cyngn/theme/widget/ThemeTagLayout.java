/*
 * Copyright (C) 2014 The Cyanogen, Inc
 */
package com.cyngn.theme.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.cyngn.theme.chooser.R;

public class ThemeTagLayout extends LinearLayout {
    private ImageView mAppliedTag;
    private ImageView mCustomizedTag;
    private ImageView mUpdatedTag;
    private TextView mDefaultTag;

    public ThemeTagLayout(Context context) {
        this(context, null);
    }

    public ThemeTagLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ThemeTagLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        LayoutInflater inflater =
                (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mAppliedTag = (ImageView) inflater.inflate(R.layout.tag_applied, this, false);
        mCustomizedTag = (ImageView) inflater.inflate(R.layout.tag_customized, this, false);
        mUpdatedTag = (ImageView) inflater.inflate(R.layout.tag_updated, this, false);
        mDefaultTag = (TextView) inflater.inflate(R.layout.tag_default, this, false);
    }

    public void setAppliedTagEnabled(boolean enabled) {
        if (enabled) {
            if (findViewById(R.id.tag_applied) != null) return;
            addView(mAppliedTag, 0);
        } else {
            if (findViewById(R.id.tag_applied) == null) return;
            removeView(mAppliedTag);
        }
    }

    public void setCustomizedTagEnabled(boolean enabled) {
        if (enabled) {
            if (findViewById(R.id.tag_customized) != null) return;
            final int childCount = getChildCount();
            if (childCount > 1) {
                for (int i = 0; i < childCount; i++) {
                    final View child = getChildAt(i);
                    if (child != mAppliedTag) {
                        addView(mCustomizedTag, i);
                        break;
                    }
                }
            } else {
                addView(mCustomizedTag);
            }
        } else {
            if (findViewById(R.id.tag_customized) == null) return;
            removeView(mCustomizedTag);
        }
    }

    public void setUpdatedTagEnabled(boolean enabled) {
        if (enabled) {
            if (findViewById(R.id.tag_updated) != null) return;
            final int childCount = getChildCount();
            if (childCount > 2) {
                for (int i = 0; i < childCount; i++) {
                    final View child = getChildAt(i);
                    if (child != mAppliedTag && child != mCustomizedTag) {
                        addView(mUpdatedTag, i);
                        break;
                    }
                }
            } else {
                addView(mUpdatedTag);
            }
        } else {
            if (findViewById(R.id.tag_updated) == null) return;
            removeView(mUpdatedTag);
        }
    }

    public boolean isUpdatedTagEnabled() {
        return findViewById(R.id.tag_updated) != null;
    }

    public void setDefaultTagEnabled(boolean enabled) {
        if (enabled) {
            if (findViewById(R.id.tag_default) != null) return;
            addView(mDefaultTag);
        } else {
            if (findViewById(R.id.tag_default) == null) return;
            removeView(mDefaultTag);
        }
    }
}
