package org.cyanogenmod.theme.chooserv2;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.cyanogenmod.theme.chooser.R;

public class WallpaperCardView extends ComponentCardView {
    protected ImageView mImage;

    public WallpaperCardView(Context context) {
        this(context, null);
    }

    public WallpaperCardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WallpaperCardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setOrientation(LinearLayout.VERTICAL);

        setBackgroundResource(R.drawable.card_bg);

        // Wallpaper Image
        mImage = new ImageView(context);
        mImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        addView(mImage);

        // Wallpaper Label - inflated because programmatic styles is hard
        mLabel = (TextView) inflate(context, R.layout.v2card_label, null);
        addView(mLabel);
    }

    public void setWallpaper(Drawable drawable) {
        mImage.setImageDrawable(drawable);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childLeft = mPaddingLeft;
        int childRight = r - mPaddingRight;
        int childTop = mPaddingTop;
        int childBottom = b - mPaddingBottom;

        mImage.layout(childLeft, childTop, childRight, childBottom);
        mLabel.layout(childLeft, childTop, childRight, childTop + mLabel.getMeasuredHeight());
    }
}
