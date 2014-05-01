/*
 * Author: Laurence Dawson
 * Source: http://stackoverflow.com/questions/9618835/apply-two-different-font-styles-to-a-textview
 */
package org.cyanogenmod.theme.util;


import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.TypefaceSpan;

public class CustomTypeFaceSpan extends TypefaceSpan {

    public Typeface mTf;

    public CustomTypeFaceSpan(Typeface tf) {
        super("");
        mTf = tf;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        apply(ds);

    }

    @Override
    public void updateMeasureState(TextPaint paint) {
        apply(paint);
    }

    private void apply(Paint paint) {
        int oldStyle;

        Typeface old = paint.getTypeface();
        if (old == null) {
            oldStyle = 0;
        } else {
            oldStyle = old.getStyle();
        }

        int fake = oldStyle & ~mTf.getStyle();

        if ((fake & Typeface.BOLD) != 0) {
            paint.setFakeBoldText(true);
        }

        if ((fake & Typeface.ITALIC) != 0) {
            paint.setTextSkewX(-0.25f);
        }

        paint.setTypeface(mTf);
    }
}
