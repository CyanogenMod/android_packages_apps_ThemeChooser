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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.cyanogenmod.theme.chooser2.R;

public class ThemeTagLayout extends LinearLayout {
    private ImageView mAppliedTag;
    private TextView mCustomizedTag;
    private TextView mUpdatedTag;
    private TextView mDefaultTag;
    private TextView mLegacyTag;

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
        mCustomizedTag = (TextView) inflater.inflate(R.layout.tag_customized, this, false);
        mUpdatedTag = (TextView) inflater.inflate(R.layout.tag_updated, this, false);
        mDefaultTag = (TextView) inflater.inflate(R.layout.tag_default, this, false);
        mLegacyTag = (TextView) inflater.inflate(R.layout.tag_legacy, this, false);
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

    public boolean isAppliedTagEnabled() {
        return findViewById(R.id.tag_applied) != null;
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

    public boolean isCustomizedTagEnabled() {
        return findViewById(R.id.tag_customized) != null;
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

    public void setLegacyTagEnabled(boolean enabled) {
        if (enabled) {
            if (findViewById(R.id.tag_legacy) != null) return;
            addView(mLegacyTag);
        } else {
            if (findViewById(R.id.tag_legacy) == null) return;
            removeView(mLegacyTag);
        }
    }
}
