package org.cyanogenmod.theme.chooserv2;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.cyanogenmod.theme.chooser.R;

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
        setOrientation(LinearLayout.VERTICAL);

        setBackgroundResource(R.drawable.card_bg);

        LayoutInflater inflater = LayoutInflater.from(mContext);
        FrameLayout frameLayout =
                (FrameLayout) inflater.inflate(R.layout.v2wallpaper_card, this, false);
        addView(frameLayout);
        mLabel = (TextView) frameLayout.findViewById(R.id.label);
        mImage = (ImageView) frameLayout.findViewById(R.id.image);
    }

    public void setWallpaper(Drawable drawable) {
        mImage.setImageDrawable(drawable);
    }
}
