/*
 * Copyright (C) 2014 The Cyanogen, Inc
 */
package android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

public class ComponentSelectorLinearLayout extends LinearLayout {

    private int mDividerHeight;

    public ComponentSelectorLinearLayout(Context context) {
        this(context, null);
    }

    public ComponentSelectorLinearLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ComponentSelectorLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mDividerHeight = -1;
    }

    public void setDividerHeight(int height) {
        mDividerHeight = height;
    }

    public int getDividerHeight() {
        return mDividerHeight;
    }

    @Override
    void drawVerticalDivider(Canvas canvas, int left) {
        final Drawable divider = getDividerDrawable();
        final int dividerWidth = getDividerWidth();
        final int dividerPadding = getDividerPadding();
        final int dividerHeight = mDividerHeight >= 0 ?
                                  getPaddingTop() + dividerPadding + mDividerHeight :
                                  getHeight() - getPaddingBottom() - dividerPadding;
        divider.setBounds(left, getPaddingTop() + dividerPadding, left + dividerWidth,
                          dividerHeight);
        divider.draw(canvas);
    }
}
