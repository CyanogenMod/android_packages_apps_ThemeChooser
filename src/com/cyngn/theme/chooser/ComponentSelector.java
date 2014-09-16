/*
 * Copyright (C) 2014 The Cyanogen, Inc
 */
package com.cyngn.theme.chooser;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ThemeUtils;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.ThemesContract;
import android.provider.ThemesContract.MixnMatchColumns;
import android.provider.ThemesContract.PreviewColumns;
import android.provider.ThemesContract.ThemesColumns;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.PagerAdapter;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import au.com.glassechidna.velocityviewpager.VelocityViewPager;
import com.viewpagerindicator.PageIndicator;
import com.cyngn.theme.util.AudioUtils;
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

public class ComponentSelector extends LinearLayout
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = ComponentSelector.class.getSimpleName();

    public static final boolean DEBUG_SELECTOR = false;

    public static final String EXTERNAL_WALLPAPER = "external";

    private static final int LOADER_ID_STATUS_BAR = 100;
    private static final int LOADER_ID_NAVIGATION_BAR = 101;
    private static final int LOADER_ID_FONT = 102;
    private static final int LOADER_ID_ICON = 103;
    private static final int LOADER_ID_STYLE = 104;
    private static final int LOADER_ID_WALLPAPER = 105;
    private static final int LOADER_ID_BOOTANIMATIONS = 106;
    private static final int LOADER_ID_RINGTONE = 107;
    private static final int LOADER_ID_NOTIFICATION = 108;
    private static final int LOADER_ID_ALARM = 109;
    private static final int LOADER_ID_LOCKSCREEN = 110;

    private static final int EXTRA_WALLPAPER_COMPONENTS = 2;

    private Context mContext;
    private LayoutInflater mInflater;
    private VelocityViewPager mPager;

    private String mComponentType;
    private CursorPagerAdapter mAdapter;
    private int mBatteryStyle;
    private int mItemsPerPage;
    private String mAppliedComponentPkgName;

    // animations for bringing selector in and out of view
    private Animation mAnimateIn;
    private Animation mAnimateOut;

    private OnItemClickedListener mListener;

    private OnOpenCloseListener mOpenCloseListener;

    private MediaPlayer mMediaPlayer;
    private ImageView mCurrentPlayPause;

    private int mCurrentLoaderId;

    public ComponentSelector(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ComponentSelector);
        mItemsPerPage = a.getInt(R.styleable.ComponentSelector_itemsPerPage,
               context.getResources().getInteger(R.integer.default_items_per_page));
        a.recycle();

        mContext = context;
        mInflater = LayoutInflater.from(context);
        mBatteryStyle = Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY, 0);

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
                    mCurrentPlayPause.setImageResource(R.drawable.media_sound__selector_preview);
                    mCurrentPlayPause = null;
                }
            }
        });
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mPager = (VelocityViewPager) findViewById(R.id.pager);
        mAdapter = new CursorPagerAdapter<View>(null, mItemsPerPage);
        mPager.setAdapter(mAdapter);
        PageIndicator indicator = (PageIndicator) findViewById(R.id.page_indicator);
        indicator.setViewPager(mPager);

        setEnabled(false);
    }

    public void setComponentType(String component) {
        // Find out which theme is currently applied for this component
        String selection = MixnMatchColumns.COL_KEY + "=?";
        String[] selectionArgs = {MixnMatchColumns.componentToMixNMatchKey(component)};
        Cursor c = mContext.getContentResolver().query(MixnMatchColumns.CONTENT_URI,
                null, selection, selectionArgs, null);
        if (c != null) {
            if (c.moveToFirst()) {
                mAppliedComponentPkgName = c.getString(
                        c.getColumnIndex(MixnMatchColumns.COL_VALUE));
            }
            c.close();
        } else {
            mAppliedComponentPkgName = null;
        }
        if (mComponentType == null || !mComponentType.equals(component)){
            mAdapter.swapCursor(null);
        }
        mComponentType = component;
        ((FragmentActivity) mContext).getSupportLoaderManager().restartLoader(
                getLoaderIdFromComponent(component), null, this);
    }

    public String getComponentType() {
        return mComponentType;
    }

    public void setNumItemsPerPage(int itemsPerPage) {
        if (mItemsPerPage != itemsPerPage) {
            mItemsPerPage = itemsPerPage;
            mAdapter.setNumItemsPerPage(mItemsPerPage);
        }
    }

    public void setHeight(int height) {
        ViewGroup.LayoutParams params = mPager.getLayoutParams();
        if (params.height != height) {
            params.height = height;
            mPager.setLayoutParams(params);
            requestLayout();
        }
    }

    public void show(String componentType, int itemsPerPage, int height) {
        setNumItemsPerPage(itemsPerPage);
        setHeight(height);
        setComponentType(componentType);
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
            return LOADER_ID_ICON;
        }
        if (MODIFIES_OVERLAYS.equals(component)) {
            return LOADER_ID_STYLE;
        }
        if (MODIFIES_LAUNCHER.equals(component)) {
            return LOADER_ID_WALLPAPER;
        }
        if (MODIFIES_BOOT_ANIM.equals(component)) {
            return LOADER_ID_BOOTANIMATIONS;
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
        String selection;
        String[] selectionArgs = { "1" };
        String[] projection = { ThemesColumns.TITLE, ThemesColumns.PKG_NAME };
        switch(id) {
            case LOADER_ID_STATUS_BAR:
                selection = MODIFIES_STATUS_BAR + "=?";
                projection = new String[] {
                        PreviewColumns.STATUSBAR_WIFI_ICON,
                        PreviewColumns.STATUSBAR_SIGNAL_ICON,
                        PreviewColumns.STATUSBAR_BLUETOOTH_ICON,
                        PreviewColumns.STATUSBAR_BACKGROUND,
                        PreviewColumns.STATUSBAR_BATTERY_CIRCLE,
                        PreviewColumns.STATUSBAR_BATTERY_LANDSCAPE,
                        PreviewColumns.STATUSBAR_BATTERY_PORTRAIT,
                        ThemesColumns.TITLE,
                        ThemesColumns.PKG_NAME
                };
                break;
            case LOADER_ID_NAVIGATION_BAR:
                selection = MODIFIES_NAVIGATION_BAR + "=?";
                projection = new String[] {
                        PreviewColumns.NAVBAR_BACK_BUTTON,
                        PreviewColumns.STATUSBAR_BACKGROUND,
                        ThemesColumns.TITLE,
                        ThemesColumns.PKG_NAME,
                };
                break;
            case LOADER_ID_FONT:
                selection = MODIFIES_FONTS + "=?";
                break;
            case LOADER_ID_ICON:
                selection = MODIFIES_ICONS + "=?";
                projection = new String[] {
                        PreviewColumns.ICON_PREVIEW_1,
                        ThemesColumns.TITLE,
                        ThemesColumns.PKG_NAME
                };
                break;
            case LOADER_ID_STYLE:
                selection = MODIFIES_OVERLAYS + "=?";
                projection = new String[] {
                        PreviewColumns.STYLE_THUMBNAIL,
                        ThemesColumns.TITLE,
                        ThemesColumns.PKG_NAME
                };
                break;
            case LOADER_ID_WALLPAPER:
                selection = MODIFIES_LAUNCHER + "=?";
                projection = new String[] {
                        PreviewColumns.WALLPAPER_THUMBNAIL,
                        ThemesColumns.TITLE,
                        ThemesColumns.PKG_NAME
                };
                break;
            case LOADER_ID_BOOTANIMATIONS:
                selection = MODIFIES_BOOT_ANIM + "=?";
                projection = new String[] {
                        PreviewColumns.BOOTANIMATION_THUMBNAIL,
                        ThemesColumns.TITLE,
                        ThemesColumns.PKG_NAME
                };
                break;
            case LOADER_ID_RINGTONE:
                selection = MODIFIES_RINGTONES + "=?";
                break;
            case LOADER_ID_NOTIFICATION:
                selection = MODIFIES_NOTIFICATIONS + "=?";
                break;
            case LOADER_ID_ALARM:
                selection = MODIFIES_ALARMS + "=?";
                break;
            case LOADER_ID_LOCKSCREEN:
                selection = MODIFIES_LOCKSCREEN + "=?";
                projection = new String[] {
                        PreviewColumns.LOCK_WALLPAPER_THUMBNAIL,
                        ThemesColumns.TITLE,
                        ThemesColumns.PKG_NAME
                };
                break;
            default:
                return null;
        }
        // sort in ascending order but make sure the "default" theme is always first
        String sortOrder = "(" + ThemesContract.ThemesColumns.IS_DEFAULT_THEME + "=1) DESC, "
                + ThemesContract.ThemesColumns.TITLE + " ASC";
        return new CursorLoader(mContext, PreviewColumns.CONTENT_URI,
                projection, selection, selectionArgs, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, final Cursor data) {
        mCurrentLoaderId = loader.getId();
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    public void setOnItemClickedListener(OnItemClickedListener listener) {
        mListener = listener;
    }

    public void setOnOpenCloseListener(OnOpenCloseListener listener) {
        mOpenCloseListener = listener;
    }

    public class CursorPagerAdapter<T extends View> extends PagerAdapter {
        LinearLayout.LayoutParams mItemParams =
                new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f);
        LinearLayout.LayoutParams mSoundItemParams =
                new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1.0f);
        private Cursor mCursor;
        private int mItemsPerPage;
        private TypefaceHelperCache mTypefaceCache;

        public CursorPagerAdapter(Cursor cursor, int itemsPerPage) {
            super();
            mCursor = cursor;
            mItemsPerPage = itemsPerPage;
            mTypefaceCache = TypefaceHelperCache.getInstance();
        }

        public void setNumItemsPerPage(int itemsPerPage) {
            mItemsPerPage = itemsPerPage;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            ViewGroup v = (ViewGroup) mInflater.inflate(R.layout.component_selection_pager_item,
                    container, false);
            if (v instanceof LinearLayout) {
                ((LinearLayout) v).setWeightSum(mItemsPerPage);
            }
            if (MODIFIES_STATUS_BAR.equals(mComponentType)) {
                newStatusBarView(mCursor, v, position);
            }
            if (MODIFIES_NAVIGATION_BAR.equals(mComponentType)) {
                newNavBarView(mCursor, v, position);
            }
            if (MODIFIES_FONTS.equals(mComponentType)) {
                newFontView(mCursor, v, position);
            }
            if (MODIFIES_ICONS.equals(mComponentType)) {
                newIconView(mCursor, v, position);
            }
            if (MODIFIES_OVERLAYS.equals(mComponentType)) {
                newStyleView(mCursor, v, position);
            }
            if (MODIFIES_LAUNCHER.equals(mComponentType)) {
                newWallpapersView(mCursor, v, position,
                        mCursor.getColumnIndex(PreviewColumns.WALLPAPER_THUMBNAIL));
            }
            if (MODIFIES_BOOT_ANIM.equals(mComponentType)) {
                newBootanimationView(mCursor, v, position);
            }
            if (MODIFIES_RINGTONES.equals(mComponentType) ||
                    MODIFIES_NOTIFICATIONS.equals(mComponentType) ||
                    MODIFIES_ALARMS.equals(mComponentType)) {
                v = (ViewGroup) mInflater.inflate(R.layout.component_selection_sounds_pager_item,
                        container, false);
                if (v instanceof LinearLayout) {
                    ((LinearLayout) v).setWeightSum(mItemsPerPage);
                }
                newSoundView(mCursor, v, position, mComponentType);
            }
            if (MODIFIES_LOCKSCREEN.equals(mComponentType)) {
                newWallpapersView(mCursor, v, position,
                        mCursor.getColumnIndex(PreviewColumns.LOCK_WALLPAPER_THUMBNAIL));
            }
            container.addView(v);
            return v;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            if (object instanceof View) {
                container.removeView((View) object);
            }
        }

        @Override
        public int getCount() {
            if (mCursor == null) return 0;

            int count = mCursor.getCount();
            if (mCurrentLoaderId == LOADER_ID_WALLPAPER ||
                    mCurrentLoaderId == LOADER_ID_LOCKSCREEN) {
                // Wallpaper and lockscreen have additional options (none, and pick image).
                count += EXTRA_WALLPAPER_COMPONENTS;
            }
            return (int) Math.ceil((float)count / mItemsPerPage);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        public void swapCursor(Cursor c) {
            mCursor = c;
            notifyDataSetChanged();
            mPager.setCurrentItem(0, false);
        }

        public Cursor getCursor() {
            return mCursor;
        }

        private OnClickListener mItemClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                String pkgName = (String) v.getTag();
                if (DEBUG_SELECTOR) Toast.makeText(mContext, pkgName, Toast.LENGTH_SHORT).show();
                if (mListener != null) {
                    mListener.onItemClicked(pkgName);
                }
            }
        };

        private void newStatusBarView(Cursor cursor, ViewGroup parent, int position) {
            for (int i = 0; i < mItemsPerPage; i++) {
                int index = position * mItemsPerPage + i;
                if (cursor.getCount() <= index) continue;
                cursor.moveToPosition(index);
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
                v.setTag(cursor.getString(pkgNameIndex));
                v.setOnClickListener(mItemClickListener);
                parent.addView(v, mItemParams);
                addDividerIfNeeded(parent, i, index, cursor);
            }
        }

        private void newNavBarView(Cursor cursor, ViewGroup parent, int position) {
            for (int i = 0; i < mItemsPerPage; i++) {
                int index = position * mItemsPerPage + i;
                if (cursor.getCount() <= index) continue;
                cursor.moveToPosition(index);
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
                v.setTag(cursor.getString(pkgNameIndex));
                v.setOnClickListener(mItemClickListener);
                parent.addView(v, mItemParams);
                addDividerIfNeeded(parent, i, index, cursor);
            }
        }

        private void newFontView(Cursor cursor, ViewGroup parent, int position) {
            for (int i = 0; i < mItemsPerPage; i++) {
                int index = position * mItemsPerPage + i;
                if (cursor.getCount() <= index) continue;
                cursor.moveToPosition(index);
                View v = mInflater.inflate(R.layout.font_component_selection_item, parent,
                        false);
                int pkgNameIndex = cursor.getColumnIndex(ThemesContract.ThemesColumns.PKG_NAME);

                TextView preview = (TextView) v.findViewById(R.id.text_preview);
                String pkgName = cursor.getString(pkgNameIndex);

                ThemedTypefaceHelper helper = mTypefaceCache.getHelperForTheme(mContext, pkgName);
                Typeface typefaceNormal = helper.getTypeface(Typeface.NORMAL);
                preview.setTypeface(typefaceNormal);

                setTitle(((TextView) v.findViewById(R.id.title)), cursor);
                v.setTag(cursor.getString(pkgNameIndex));
                v.setOnClickListener(mItemClickListener);
                parent.addView(v, mItemParams);
                addDividerIfNeeded(parent, i, index, cursor);
            }
        }

        private void newIconView(Cursor cursor, ViewGroup parent, int position) {
            for (int i = 0; i < mItemsPerPage; i++) {
                int index = position * mItemsPerPage + i;
                if (cursor.getCount() <= index) continue;
                cursor.moveToPosition(index);
                View v = mInflater.inflate(R.layout.icon_component_selection_item, parent,
                        false);
                int iconIndex = cursor.getColumnIndex(PreviewColumns.ICON_PREVIEW_1);
                int pkgNameIndex = cursor.getColumnIndex(ThemesContract.ThemesColumns.PKG_NAME);

                ((ImageView) v.findViewById(R.id.icon)).setImageBitmap(
                        Utils.loadBitmapBlob(cursor, iconIndex));
                setTitle(((TextView) v.findViewById(R.id.title)), cursor);
                v.setTag(cursor.getString(pkgNameIndex));
                v.setOnClickListener(mItemClickListener);
                parent.addView(v, mItemParams);
                addDividerIfNeeded(parent, i, index, cursor);
            }
        }

        private void newStyleView(Cursor cursor, ViewGroup parent, int position) {
            for (int i = 0; i < mItemsPerPage; i++) {
                int index = position * mItemsPerPage + i;
                if (cursor.getCount() <= index) continue;
                cursor.moveToPosition(index);
                View v = mInflater.inflate(R.layout.icon_component_selection_item, parent,
                        false);
                int styleIndex = cursor.getColumnIndex(PreviewColumns.STYLE_THUMBNAIL);
                int pkgNameIndex = cursor.getColumnIndex(ThemesContract.ThemesColumns.PKG_NAME);

                ((ImageView) v.findViewById(R.id.icon)).setImageBitmap(
                        Utils.loadBitmapBlob(cursor, styleIndex));
                setTitle(((TextView) v.findViewById(R.id.title)), cursor);
                v.setTag(cursor.getString(pkgNameIndex));
                v.setOnClickListener(mItemClickListener);
                parent.addView(v, mItemParams);
                addDividerIfNeeded(parent, i, index, cursor);
            }
        }

        private void newWallpapersView(Cursor cursor, ViewGroup parent, int position,
                int wallpaperIndex) {
            for (int i = 0; i < mItemsPerPage; i++) {
                int index = position * mItemsPerPage + i;
                if (cursor.getCount() < (index - EXTRA_WALLPAPER_COMPONENTS)) continue;
                View v = mInflater.inflate(R.layout.wallpaper_component_selection_item, parent,
                        false);
                ImageView iv = (ImageView) v.findViewById(R.id.icon);
                if (index == 0) {
                    iv.setImageResource(R.drawable.img_wallpaper_none);
                    v.setTag("");
                    ((TextView) v.findViewById(R.id.title)).setText(R.string.wallpaper_none_title);
                } else if (index == 1) {
                    iv.setImageResource(R.drawable.img_wallpaper_external);
                    v.setTag(EXTERNAL_WALLPAPER);
                    ((TextView) v.findViewById(R.id.title))
                            .setText(R.string.wallpaper_external_title);
                } else {
                    cursor.moveToPosition(index - EXTRA_WALLPAPER_COMPONENTS);
                    int pkgNameIndex = cursor.getColumnIndex(ThemesContract.ThemesColumns.PKG_NAME);
                    iv.setImageBitmap(
                            Utils.loadBitmapBlob(cursor, wallpaperIndex));
                    setTitle(((TextView) v.findViewById(R.id.title)), cursor);
                    v.setTag(cursor.getString(pkgNameIndex));
                }
                v.setOnClickListener(mItemClickListener);
                parent.addView(v, mItemParams);
                addDividerIfNeeded(parent, i, index, cursor);
            }
        }

        private void newBootanimationView(Cursor cursor, ViewGroup parent, int position) {
            for (int i = 0; i < mItemsPerPage; i++) {
                int index = position * mItemsPerPage + i;
                if (cursor.getCount() <= index) continue;
                cursor.moveToPosition(index);
                View v = mInflater.inflate(R.layout.bootani_component_selection_item, parent,
                        false);
                int wallpaperIndex = cursor.getColumnIndex(PreviewColumns.BOOTANIMATION_THUMBNAIL);
                int pkgNameIndex = cursor.getColumnIndex(ThemesContract.ThemesColumns.PKG_NAME);

                ((ImageView) v.findViewById(R.id.preview)).setImageBitmap(
                        Utils.loadBitmapBlob(cursor, wallpaperIndex));
                setTitle(((TextView) v.findViewById(R.id.title)), cursor);
                v.setTag(cursor.getString(pkgNameIndex));
                v.setOnClickListener(mItemClickListener);
                parent.addView(v, mItemParams);
                addDividerIfNeeded(parent, i, index, cursor);
            }
        }

        private void newSoundView(Cursor cursor, ViewGroup parent, int position, String component) {
            for (int i = 0; i < mItemsPerPage; i++) {
                int index = position * mItemsPerPage + i;
                if (cursor.getCount() <= index) continue;
                cursor.moveToPosition(index);
                View v = mInflater.inflate(R.layout.sound_component_selection_item, parent,
                        false);
                final int pkgNameIndex = cursor.getColumnIndex(ThemesContract.ThemesColumns.PKG_NAME);

                setTitle(((TextView) v.findViewById(R.id.title)), cursor);
                v.setTag(cursor.getString(pkgNameIndex));
                v.setOnClickListener(mItemClickListener);
                parent.addView(v, mSoundItemParams);
                final View playButton = v.findViewById(R.id.play_button);
                playButton.setTag(cursor.getString(pkgNameIndex));
                playButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int type;
                        String pkgName = (String) v.getTag();
                        if (mComponentType.equals(MODIFIES_RINGTONES)) {
                            type = RingtoneManager.TYPE_RINGTONE;
                        } else if (mComponentType.equals(MODIFIES_NOTIFICATIONS)) {
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
                                            R.drawable.media_sound__selector_preview);
                                }
                                mCurrentPlayPause = null;
                            }
                            if (mCurrentPlayPause != playButton && !shouldStop) {
                                AudioUtils.loadThemeAudible(mContext, type, pkgName,
                                        mMediaPlayer);
                                mMediaPlayer.start();
                                mCurrentPlayPause = (ImageView) playButton;
                                mCurrentPlayPause.setImageResource(
                                        R.drawable.media_sound__selector_stop);
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            Log.w(TAG, "Unable to play preview sound", e);
                        }
                    }
                });
            }
        }

        private void addDivider(ViewGroup parent) {
            final Resources res = getResources();
            View v = mInflater.inflate(R.layout.component_divider, parent, false);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) v.getLayoutParams();
            // Boot animations require a taller divider so adjust accordingly
            if (ThemesColumns.MODIFIES_BOOT_ANIM.equals(mComponentType)) {
                params.topMargin = res.getDimensionPixelSize(
                        R.dimen.component_divider_margin_top_bootani);
                params.height = res.getDimensionPixelSize(R.dimen.component_divider_height_bootani);
            }
            v.setLayoutParams(params);
            parent.addView(v);
        }

        private void addDividerIfNeeded(ViewGroup parent, int position, int cursorIndex,
                Cursor cursor) {
            if (position < mItemsPerPage - 1 && cursorIndex < cursor.getCount() - 1) {
                addDivider(parent);
            }
        }

        private void setTitle(TextView titleView, Cursor cursor) {
            String pkgName = cursor.getString(cursor.getColumnIndex(ThemesColumns.PKG_NAME));
            if (ThemeUtils.getDefaultThemePackageName(mContext).equals(pkgName)) {
                titleView.setText(mContext.getString(R.string.default_tag_text));
                titleView.setTypeface(null, Typeface.BOLD);
            } else {
                titleView.setText(cursor.getString(cursor.getColumnIndex(ThemesColumns.TITLE)));
            }
            if (pkgName.equals(mAppliedComponentPkgName)) {
                titleView.setTextColor(getResources().getColor(
                        R.color.component_selection_current_text_color));
            }
        }
    }

    public interface OnItemClickedListener {
        public void onItemClicked(String pkgName);
    }

    public interface OnOpenCloseListener {
        public void onSelectorOpened();
        public void onSelectorClosed();
    }
}