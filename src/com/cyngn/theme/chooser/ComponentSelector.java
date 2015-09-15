/*
 * Copyright (C) 2014 The Cyanogen, Inc
 */
package com.cyngn.theme.chooser;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ThemeUtils;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.ThemesContract;
import android.provider.ThemesContract.PreviewColumns;
import android.provider.ThemesContract.ThemesColumns;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ComponentSelectorLinearLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cyngn.theme.util.AudioUtils;
import com.cyngn.theme.util.CursorLoaderHelper;
import com.cyngn.theme.util.ThemedTypefaceHelper;
import com.cyngn.theme.util.TypefaceHelperCache;
import com.cyngn.theme.util.Utils;

import static android.provider.ThemesContract.ThemesColumns.MODIFIES_ALARMS;
import static android.provider.ThemesContract.ThemesColumns.MODIFIES_BOOT_ANIM;
import static android.provider.ThemesContract.ThemesColumns.MODIFIES_LAUNCHER;
import static android.provider.ThemesContract.ThemesColumns.MODIFIES_LOCKSCREEN;
import static android.provider.ThemesContract.ThemesColumns.MODIFIES_NOTIFICATIONS;
import static android.provider.ThemesContract.ThemesColumns.MODIFIES_OVERLAYS;
import static android.provider.ThemesContract.ThemesColumns.MODIFIES_RINGTONES;
import static android.provider.ThemesContract.ThemesColumns.MODIFIES_STATUS_BAR;
import static android.provider.ThemesContract.ThemesColumns.MODIFIES_NAVIGATION_BAR;
import static android.provider.ThemesContract.ThemesColumns.MODIFIES_ICONS;
import static android.provider.ThemesContract.ThemesColumns.MODIFIES_FONTS;

import static com.cyngn.theme.util.CursorLoaderHelper.LOADER_ID_STATUS_BAR;
import static com.cyngn.theme.util.CursorLoaderHelper.LOADER_ID_FONT;
import static com.cyngn.theme.util.CursorLoaderHelper.LOADER_ID_ICONS;
import static com.cyngn.theme.util.CursorLoaderHelper.LOADER_ID_WALLPAPER;
import static com.cyngn.theme.util.CursorLoaderHelper.LOADER_ID_NAVIGATION_BAR;
import static com.cyngn.theme.util.CursorLoaderHelper.LOADER_ID_LOCKSCREEN;
import static com.cyngn.theme.util.CursorLoaderHelper.LOADER_ID_STYLE;
import static com.cyngn.theme.util.CursorLoaderHelper.LOADER_ID_BOOT_ANIMATION;
import static com.cyngn.theme.util.CursorLoaderHelper.LOADER_ID_RINGTONE;
import static com.cyngn.theme.util.CursorLoaderHelper.LOADER_ID_NOTIFICATION;
import static com.cyngn.theme.util.CursorLoaderHelper.LOADER_ID_ALARM;

public class ComponentSelector extends LinearLayout
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = ComponentSelector.class.getSimpleName();

    public static final boolean DEBUG_SELECTOR = false;

    public static final String EXTERNAL_WALLPAPER = "external";

    private static final int EXTRA_WALLPAPER_COMPONENTS = 2;

    protected static final long DEFAULT_COMPONENT_ID = 0;

    private Context mContext;
    private LayoutInflater mInflater;
    private ComponentSelectorLinearLayout mContent;
    private LinearLayout.LayoutParams mItemParams;
    private LinearLayout.LayoutParams mSoundItemParams =
            new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1.0f);

    private String mComponentType;
    private int mBatteryStyle;
    private int mItemsPerPage;
    private String mAppliedComponentPkgName;
    private String mSelectedComponentPkgName;
    private long mSelectedComponentId;

    // animations for bringing selector in and out of view
    private Animation mAnimateIn;
    private Animation mAnimateOut;

    private OnItemClickedListener mListener;

    private OnOpenCloseListener mOpenCloseListener;

    private MediaPlayer mMediaPlayer;
    private ImageView mCurrentPlayPause;

    private int mCurrentLoaderId;

    private TypefaceHelperCache mTypefaceCache;

    private ThemesObserver mThemesObserver;

    public ComponentSelector(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ComponentSelector);
        mItemsPerPage = a.getInt(R.styleable.ComponentSelector_itemsPerPage,
               context.getResources().getInteger(R.integer.default_items_per_page));
        a.recycle();

        mContext = context;
        mInflater = LayoutInflater.from(context);
        mBatteryStyle = Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_STYLE, 0);

        mAnimateIn = AnimationUtils.loadAnimation(mContext,
                R.anim.component_selection_animate_in);
        mAnimateOut = AnimationUtils.loadAnimation(mContext,
                R.anim.component_selection_animate_out);
        mAnimateIn.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (mOpenCloseListener != null) mOpenCloseListener.onSelectorOpened();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        mAnimateOut.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                setVisibility(View.GONE);
                if (mOpenCloseListener != null) mOpenCloseListener.onSelectorClosed();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (mCurrentPlayPause != null) {
                    mCurrentPlayPause.setImageResource(R.drawable.media_sound_selector_preview);
                    mCurrentPlayPause = null;
                }
            }
        });
        mTypefaceCache = TypefaceHelperCache.getInstance();
        mThemesObserver = new ThemesObserver();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mContent = (ComponentSelectorLinearLayout) findViewById(R.id.content);
        setEnabled(false);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mThemesObserver.register();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mThemesObserver.unregister();
    }

    public void setComponentType(String component) {
        setComponentType(component, null, DEFAULT_COMPONENT_ID);
    }

    public void setComponentType(String component, String selectedPkgName) {
        setComponentType(component, null, DEFAULT_COMPONENT_ID);
    }

    public void setComponentType(String component, String selectedPkgName,
            long selectedComponentId) {
        mSelectedComponentPkgName = selectedPkgName;
        mSelectedComponentId = selectedComponentId;
        if (mComponentType == null || !mComponentType.equals(component)) {
            mContent.removeAllViews();
            mComponentType = component;
            ((FragmentActivity) mContext).getSupportLoaderManager().restartLoader(
                    getLoaderIdFromComponent(component), null, this);
        } else if (mComponentType != null) {
            int count = mContent.getChildCount();
            final Resources res = getResources();
            for (int i = 0; i < count; i++) {
                final View child = mContent.getChildAt(i);
                final TextView tv = (TextView) child.findViewById(R.id.title);
                // The child itself may have the tag, or in the case of sounds its the grandchild,
                // either way the parent of the textview will have the pkgName in its tag
                final View viewWithTag = (View) tv.getParent();
                final String pkgName = (String) viewWithTag.getTag(R.id.tag_key_package_name);
                Long cmpntId = (Long) viewWithTag.getTag(R.id.tag_key_component_id);
                if (cmpntId == null) {
                    cmpntId = DEFAULT_COMPONENT_ID;
                }
                if (pkgName.equals(selectedPkgName) && cmpntId == selectedComponentId) {
                    tv.setTextColor(res.getColor(R.color.component_selection_current_text_color));
                } else {
                    tv.setTextColor(res.getColor(android.R.color.white));
                }
            }
        }
    }

    public String getComponentType() {
        return mComponentType;
    }

    public void setNumItemsPerPage(int itemsPerPage) {
        if (mItemsPerPage != itemsPerPage) {
            mItemsPerPage = itemsPerPage;
        }
    }

    public void setHeight(int height) {
        ViewGroup.LayoutParams params = mContent.getLayoutParams();
        if (params.height != height) {
            params.height = height;
            mContent.setLayoutParams(params);
            requestLayout();
        }
    }

    public void show(String componentType, int itemsPerPage, int height) {
        show(componentType, null, itemsPerPage, height);
    }

    public void show(String componentType, String selectedPkgName, int itemsPerPage, int height) {
        show(componentType, selectedPkgName, DEFAULT_COMPONENT_ID, itemsPerPage, height);
    }

    public void show(String componentType, String selectedPkgName, long selectedComponentId,
            int itemsPerPage, int height) {
        setNumItemsPerPage(itemsPerPage);
        setHeight(height);
        setComponentType(componentType, selectedPkgName, selectedComponentId);
        show();
    }

    public void show() {
        if (getVisibility() == View.GONE) {
            setEnabled(true);
            setVisibility(View.VISIBLE);
            startAnimation(mAnimateIn);
        }
    }

    public void hide() {
        if (getVisibility() == View.VISIBLE && isEnabled()) {
            setEnabled(false);
            startAnimation(mAnimateOut);
        }
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) mMediaPlayer.stop();
    }

    private int getLoaderIdFromComponent(String component) {
        if (MODIFIES_STATUS_BAR.equals(component)) {
            return LOADER_ID_STATUS_BAR;
        }
        if (MODIFIES_NAVIGATION_BAR.equals(component)) {
            return LOADER_ID_NAVIGATION_BAR;
        }
        if (MODIFIES_FONTS.equals(component)) {
            return LOADER_ID_FONT;
        }
        if (MODIFIES_ICONS.equals(component)) {
            return LOADER_ID_ICONS;
        }
        if (MODIFIES_OVERLAYS.equals(component)) {
            return LOADER_ID_STYLE;
        }
        if (MODIFIES_LAUNCHER.equals(component)) {
            return LOADER_ID_WALLPAPER;
        }
        if (MODIFIES_BOOT_ANIM.equals(component)) {
            return LOADER_ID_BOOT_ANIMATION;
        }
        if (MODIFIES_RINGTONES.equals(component)) {
            return LOADER_ID_RINGTONE;
        }
        if (MODIFIES_NOTIFICATIONS.equals(component)) {
            return LOADER_ID_NOTIFICATION;
        }
        if (MODIFIES_ALARMS.equals(component)) {
            return LOADER_ID_ALARM;
        }
        if (MODIFIES_LOCKSCREEN.equals(component)) {
            return LOADER_ID_LOCKSCREEN;
        }
        return -1;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return CursorLoaderHelper.componentSelectorCursorLoader(mContext, id);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, final Cursor data) {
        mCurrentLoaderId = loader.getId();
        int count = data.getCount();
        int screenWidth = mContext.getResources().getDisplayMetrics().widthPixels;
        final Resources res = getResources();
        int dividerPadding = res.getDimensionPixelSize(R.dimen.component_divider_padding_top);
        int dividerHeight = res.getDimensionPixelSize(R.dimen.component_divider_height);
        switch (mCurrentLoaderId) {
            case LOADER_ID_ALARM:
            case LOADER_ID_NOTIFICATION:
            case LOADER_ID_RINGTONE:
                mItemParams = new LayoutParams(screenWidth,
                                               ViewGroup.LayoutParams.MATCH_PARENT);
                // Sounds are a special case where there are two items laid out
                // vertically.  This layout is added as a single item so we need to
                // adjust the count by dividing by the number of items per page and
                // rounding up so we include all items.
                count = (int) Math.ceil((double)count / mItemsPerPage);
                mContent.setShowDividers(LinearLayout.SHOW_DIVIDER_NONE);
                break;
            case LOADER_ID_BOOT_ANIMATION:
                dividerPadding = res.getDimensionPixelSize(
                        R.dimen.component_divider_padding_top_bootani);
                dividerHeight = res.getDimensionPixelSize(R.dimen.component_divider_height_bootani);
                // fall through to default
            default:
                mItemParams = new LayoutParams(screenWidth / mItemsPerPage,
                                               ViewGroup.LayoutParams.MATCH_PARENT);
                if (mCurrentLoaderId == LOADER_ID_WALLPAPER ||
                        mCurrentLoaderId == LOADER_ID_LOCKSCREEN) {
                    count += EXTRA_WALLPAPER_COMPONENTS;
                }
                mContent.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
                break;
        }

        mContent.setDividerPadding(dividerPadding);
        mContent.setDividerHeight(dividerHeight);

        new LoadItemsTask().execute(data ,count);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    public void setOnItemClickedListener(OnItemClickedListener listener) {
        mListener = listener;
    }

    public void setOnOpenCloseListener(OnOpenCloseListener listener) {
        mOpenCloseListener = listener;
    }

    private View newView(Cursor cursor, int position, ViewGroup container) {
        if (MODIFIES_STATUS_BAR.equals(mComponentType)) {
            return newStatusBarView(cursor, container, position);
        }
        if (MODIFIES_NAVIGATION_BAR.equals(mComponentType)) {
            return newNavBarView(cursor, container, position);
        }
        if (MODIFIES_FONTS.equals(mComponentType)) {
            return newFontView(cursor, container, position);
        }
        if (MODIFIES_ICONS.equals(mComponentType)) {
            return newIconView(cursor, container, position);
        }
        if (MODIFIES_OVERLAYS.equals(mComponentType)) {
            return newStyleView(cursor, container, position);
        }
        if (MODIFIES_LAUNCHER.equals(mComponentType)) {
            return newWallpapersView(cursor, container, position,
                    cursor.getColumnIndex(PreviewColumns.WALLPAPER_THUMBNAIL));
        }
        if (MODIFIES_BOOT_ANIM.equals(mComponentType)) {
            return newBootanimationView(cursor, container, position);
        }
        if (MODIFIES_RINGTONES.equals(mComponentType) ||
                MODIFIES_NOTIFICATIONS.equals(mComponentType) ||
                MODIFIES_ALARMS.equals(mComponentType)) {
            return newSoundView(cursor, container, position, mComponentType);
        }
        if (MODIFIES_LOCKSCREEN.equals(mComponentType)) {
            return newWallpapersView(cursor, container, position,
                    cursor.getColumnIndex(PreviewColumns.LOCK_WALLPAPER_THUMBNAIL));
        }
        return null;
    }

    private View newStatusBarView(Cursor cursor, ViewGroup parent, int position) {
        cursor.moveToPosition(position);
        View v = mInflater.inflate(R.layout.status_bar_component_selection_item,
                                   parent, false);
        int wifiIndex = cursor.getColumnIndex(PreviewColumns.STATUSBAR_WIFI_ICON);
        int signalIndex = cursor.getColumnIndex(PreviewColumns.STATUSBAR_SIGNAL_ICON);
        int bluetoothIndex = cursor.getColumnIndex(PreviewColumns.STATUSBAR_BLUETOOTH_ICON);
        int batteryIndex = cursor.getColumnIndex(Utils.getBatteryIndex(mBatteryStyle));
        int backgroundIndex = cursor.getColumnIndex(PreviewColumns.STATUSBAR_BACKGROUND);
        int pkgNameIndex = cursor.getColumnIndex(ThemesContract.ThemesColumns.PKG_NAME);

        ((ImageView) v.findViewById(R.id.slot1)).setImageBitmap(
                Utils.loadBitmapBlob(cursor, wifiIndex));
        ((ImageView) v.findViewById(R.id.slot2)).setImageBitmap(
                Utils.loadBitmapBlob(cursor, signalIndex));
        ((ImageView) v.findViewById(R.id.slot3)).setImageBitmap(
                Utils.loadBitmapBlob(cursor, bluetoothIndex));
        ((ImageView) v.findViewById(R.id.slot4)).setImageBitmap(
                Utils.loadBitmapBlob(cursor, batteryIndex));
        setTitle(((TextView) v.findViewById(R.id.title)), cursor);
        v.findViewById(R.id.container).setBackground(
                new BitmapDrawable(Utils.loadBitmapBlob(cursor, backgroundIndex)));
        v.setTag(R.id.tag_key_package_name, cursor.getString(pkgNameIndex));
        v.setOnClickListener(mItemClickListener);
        return v;
    }

    private View newNavBarView(Cursor cursor, ViewGroup parent, int position) {
        cursor.moveToPosition(position);
        View v = mInflater.inflate(R.layout.navigation_bar_component_selection_item, parent,
                false);
        int backIndex = cursor.getColumnIndex(PreviewColumns.NAVBAR_BACK_BUTTON);
        int backgroundIndex = cursor.getColumnIndex(PreviewColumns.STATUSBAR_BACKGROUND);
        int pkgNameIndex = cursor.getColumnIndex(ThemesContract.ThemesColumns.PKG_NAME);

        ((ImageView) v.findViewById(R.id.back)).setImageBitmap(
                Utils.loadBitmapBlob(cursor, backIndex));
        setTitle(((TextView) v.findViewById(R.id.title)), cursor);
        v.findViewById(R.id.container).setBackground(
                new BitmapDrawable(Utils.loadBitmapBlob(cursor, backgroundIndex)));
        v.setTag(R.id.tag_key_package_name, cursor.getString(pkgNameIndex));
        v.setOnClickListener(mItemClickListener);
        return v;
    }

    private View newFontView(Cursor cursor, ViewGroup parent, int position) {
        cursor.moveToPosition(position);
        View v = mInflater.inflate(R.layout.font_component_selection_item, parent, false);
        int pkgNameIndex = cursor.getColumnIndex(ThemesContract.ThemesColumns.PKG_NAME);

        TextView preview = (TextView) v.findViewById(R.id.text_preview);
        String pkgName = cursor.getString(pkgNameIndex);

        ThemedTypefaceHelper helper = mTypefaceCache.getHelperForTheme(mContext, pkgName);
        Typeface typefaceNormal = helper.getTypeface(Typeface.NORMAL);
        preview.setTypeface(typefaceNormal);

        setTitle(((TextView) v.findViewById(R.id.title)), cursor);
        v.setTag(R.id.tag_key_package_name, cursor.getString(pkgNameIndex));
        v.setOnClickListener(mItemClickListener);
        return v;
    }

    private View newIconView(Cursor cursor, ViewGroup parent, int position) {
        cursor.moveToPosition(position);
        View v = mInflater.inflate(R.layout.icon_component_selection_item, parent,
                false);
        int iconIndex = cursor.getColumnIndex(PreviewColumns.ICON_PREVIEW_1);
        int pkgNameIndex = cursor.getColumnIndex(ThemesContract.ThemesColumns.PKG_NAME);

        ((ImageView) v.findViewById(R.id.icon)).setImageBitmap(
                Utils.loadBitmapBlob(cursor, iconIndex));
        setTitle(((TextView) v.findViewById(R.id.title)), cursor);
        v.setTag(R.id.tag_key_package_name, cursor.getString(pkgNameIndex));
        v.setOnClickListener(mItemClickListener);
        return v;
    }

    private View newStyleView(Cursor cursor, ViewGroup parent, int position) {
        cursor.moveToPosition(position);
        View v = mInflater.inflate(R.layout.icon_component_selection_item, parent,
                false);
        int styleIndex = cursor.getColumnIndex(PreviewColumns.STYLE_THUMBNAIL);
        int pkgNameIndex = cursor.getColumnIndex(ThemesContract.ThemesColumns.PKG_NAME);

        ((ImageView) v.findViewById(R.id.icon)).setImageBitmap(
                Utils.loadBitmapBlob(cursor, styleIndex));
        setTitle(((TextView) v.findViewById(R.id.title)), cursor);
        v.setTag(R.id.tag_key_package_name, cursor.getString(pkgNameIndex));
        v.setOnClickListener(mItemClickListener);
        return v;
    }

    private View newWallpapersView(Cursor cursor, ViewGroup parent, int position,
                                   int wallpaperIndex) {
        View v = mInflater.inflate(R.layout.wallpaper_component_selection_item, parent,
                false);
        ImageView iv = (ImageView) v.findViewById(R.id.icon);
        if (position == 0) {
            iv.setImageResource(R.drawable.img_wallpaper_none);
            v.setTag(R.id.tag_key_package_name, "");
            ((TextView) v.findViewById(R.id.title)).setText(R.string.wallpaper_none_title);
        } else if (position == 1) {
            iv.setImageResource(R.drawable.img_wallpaper_external);
            v.setTag(R.id.tag_key_package_name, EXTERNAL_WALLPAPER);
            ((TextView) v.findViewById(R.id.title))
                    .setText(R.string.wallpaper_external_title);
        } else {
            cursor.moveToPosition(position - EXTRA_WALLPAPER_COMPONENTS);
            int pkgNameIndex = cursor.getColumnIndex(ThemesContract.ThemesColumns.PKG_NAME);
            int cmpntIdIndex = cursor.getColumnIndex(PreviewColumns.COMPONENT_ID);
            long cmpntId = (cmpntIdIndex >= 0) ?
                    cursor.getLong(cmpntIdIndex) : DEFAULT_COMPONENT_ID;
            iv.setImageBitmap(
                    Utils.loadBitmapBlob(cursor, wallpaperIndex));
            setTitle(((TextView) v.findViewById(R.id.title)), cursor);
            v.setTag(R.id.tag_key_package_name, cursor.getString(pkgNameIndex));
            v.setTag(R.id.tag_key_component_id, cmpntId);
        }
        v.setOnClickListener(mItemClickListener);
        return v;
    }

    private View newBootanimationView(Cursor cursor, ViewGroup parent, int position) {
        cursor.moveToPosition(position);
        View v = mInflater.inflate(R.layout.bootani_component_selection_item, parent,
                false);
        int wallpaperIndex = cursor.getColumnIndex(PreviewColumns.BOOTANIMATION_THUMBNAIL);
        int pkgNameIndex = cursor.getColumnIndex(ThemesContract.ThemesColumns.PKG_NAME);

        ((ImageView) v.findViewById(R.id.preview)).setImageBitmap(
                Utils.loadBitmapBlob(cursor, wallpaperIndex));
        setTitle(((TextView) v.findViewById(R.id.title)), cursor);
        v.setTag(R.id.tag_key_package_name, cursor.getString(pkgNameIndex));
        v.setOnClickListener(mItemClickListener);
        return v;
    }

    private View newSoundView(Cursor cursor, ViewGroup parent, int position,
                              final String component) {
        LinearLayout container = (LinearLayout) mInflater.inflate(
                R.layout.component_selection_sounds_pager_item, parent, false);
        container.setWeightSum(mItemsPerPage);
        for (int i = 0; i < mItemsPerPage; i++) {
            int index = position * mItemsPerPage + i;
            if (cursor.getCount() <= index) continue;
            cursor.moveToPosition(index);
            View v = mInflater.inflate(R.layout.sound_component_selection_item, parent,
                    false);
            final int pkgNameIndex = cursor.getColumnIndex(ThemesContract.ThemesColumns.PKG_NAME);

            setTitle(((TextView) v.findViewById(R.id.title)), cursor);
            v.setTag(R.id.tag_key_package_name, cursor.getString(pkgNameIndex));
            v.setOnClickListener(mItemClickListener);
            container.addView(v, mSoundItemParams);
            final View playButton = v.findViewById(R.id.play_button);
            playButton.setTag(cursor.getString(pkgNameIndex));
            playButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    int type;
                    String pkgName = (String) v.getTag();
                    if (component.equals(MODIFIES_RINGTONES)) {
                        type = RingtoneManager.TYPE_RINGTONE;
                    } else if (component.equals(MODIFIES_NOTIFICATIONS)) {
                        type = RingtoneManager.TYPE_NOTIFICATION;
                    } else {
                        type = RingtoneManager.TYPE_ALARM;
                    }
                    boolean shouldStop = playButton == mCurrentPlayPause;
                    try {
                        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                            mMediaPlayer.stop();
                            if (mCurrentPlayPause != null) {
                                mCurrentPlayPause.setImageResource(
                                        R.drawable.media_sound_selector_preview);
                            }
                            mCurrentPlayPause = null;
                        }
                        if (mCurrentPlayPause != playButton && !shouldStop) {
                            AudioUtils.loadThemeAudible(mContext, type, pkgName,
                                    mMediaPlayer);
                            mMediaPlayer.start();
                            mCurrentPlayPause = (ImageView) playButton;
                            mCurrentPlayPause.setImageResource(
                                    R.drawable.media_sound_selector_stop);
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.w(TAG, "Unable to play preview sound", e);
                    }
                }
            });
        }

        return container;
    }

    private class LoadItemsTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            Cursor c = (Cursor) params[0];
            int count = (Integer) params[1];
            for (int i = 0; i < count && !isCancelled(); i++) {
                final View v = newView(c, i, mContent);
                mContent.post(new Runnable() {
                    @Override
                    public void run() {
                        mContent.addView(v, mItemParams);
                    }
                });
            }
            // destroy the loader now that we are done with it
            ComponentSelector.this.post(new Runnable() {
                @Override
                public void run() {
                    ((FragmentActivity)mContext).getSupportLoaderManager().destroyLoader(
                            getLoaderIdFromComponent(mComponentType));
                }
            });
            return null;
        }
    }

    private void setTitle(TextView titleView, Cursor cursor) {
        String pkgName = cursor.getString(cursor.getColumnIndex(ThemesColumns.PKG_NAME));
        int cmpntIdIndex = cursor.getColumnIndex(PreviewColumns.COMPONENT_ID);
        long cmpntId = DEFAULT_COMPONENT_ID;
        if (cmpntIdIndex >= 0) {
            cmpntId = cursor.getLong(cmpntIdIndex);
        }
        if (ThemeUtils.getDefaultThemePackageName(mContext).equals(pkgName)) {
            titleView.setText(mContext.getString(R.string.default_tag_text));
            titleView.setTypeface(titleView.getTypeface(), Typeface.BOLD);
        } else {
            titleView.setText(cursor.getString(cursor.getColumnIndex(ThemesColumns.TITLE)));
        }
        if (pkgName.equals(mSelectedComponentPkgName) && cmpntId == mSelectedComponentId) {
            titleView.setTextColor(getResources().getColor(
                    R.color.component_selection_current_text_color));
        }
    }

    private OnClickListener mItemClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            long cmpntId = DEFAULT_COMPONENT_ID;
            String pkgName = (String) v.getTag(R.id.tag_key_package_name);
            Long cmpntIdTag = (Long) v.getTag(R.id.tag_key_component_id);
            if (cmpntIdTag != null) {
                cmpntId = cmpntIdTag;
            }
            if (DEBUG_SELECTOR) Toast.makeText(mContext, pkgName, Toast.LENGTH_SHORT).show();
            if (mListener != null && (!pkgName.equals(mSelectedComponentPkgName) ||
                    pkgName.equals(EXTERNAL_WALLPAPER) || cmpntId != mSelectedComponentId)) {
                mSelectedComponentPkgName = pkgName;
                mSelectedComponentId = cmpntId;
                mListener.onItemClicked(pkgName, cmpntId);
                final int count = mContent.getChildCount();
                final Resources res = getResources();
                for (int i = 0; i < count; i++) {
                    final View child = mContent.getChildAt(i);
                    final TextView tv = (TextView) child.findViewById(R.id.title);
                    if (tv != null) {
                        if (child == v) {
                            tv.setTextColor(
                                    res.getColor(R.color.component_selection_current_text_color));
                        } else {
                            tv.setTextColor(res.getColor(android.R.color.white));
                        }
                    }
                }
            }
        }
    };

    private class ThemesObserver extends ContentObserver {
        public ThemesObserver() {
            super(null);
        }

        public void register() {
            mContext.getContentResolver().registerContentObserver(
                    ThemesColumns.CONTENT_URI, false, this);
        }

        public void unregister() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            // reload items by calling setComponentType()
            final String componentType = mComponentType;
            mComponentType = null;
            setComponentType(componentType, mSelectedComponentPkgName);
        }
    }

    public interface OnItemClickedListener {
        public void onItemClicked(String pkgName, long componentId);
    }

    public interface OnOpenCloseListener {
        public void onSelectorOpened();
        public void onSelectorClosed();
    }
}