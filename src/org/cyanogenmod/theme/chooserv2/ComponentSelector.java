/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cyanogenmod.theme.chooserv2;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.ThemesContract;
import android.provider.ThemesContract.PreviewColumns;
import android.provider.ThemesContract.ThemesColumns;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.viewpagerindicator.PageIndicator;
import org.cyanogenmod.theme.chooser.R;
import org.cyanogenmod.theme.util.ThemedTypefaceHelper;

import java.util.HashMap;

import static android.provider.ThemesContract.ThemesColumns.MODIFIES_BOOT_ANIM;
import static android.provider.ThemesContract.ThemesColumns.MODIFIES_LAUNCHER;
import static android.provider.ThemesContract.ThemesColumns.MODIFIES_OVERLAYS;
import static android.provider.ThemesContract.ThemesColumns.MODIFIES_STATUS_BAR;
import static android.provider.ThemesContract.ThemesColumns.MODIFIES_NAVIGATION_BAR;
import static android.provider.ThemesContract.ThemesColumns.MODIFIES_ICONS;
import static android.provider.ThemesContract.ThemesColumns.MODIFIES_FONTS;

public class ComponentSelector extends LinearLayout
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = ComponentSelector.class.getSimpleName();

    public static final boolean DEBUG_SELECTOR = true;

    private static final int LOADER_ID_STATUS_BAR = 100;
    private static final int LOADER_ID_NAVIGATION_BAR = 101;
    private static final int LOADER_ID_FONT = 102;
    private static final int LOADER_ID_ICON = 103;
    private static final int LOADER_ID_STYLE = 104;
    private static final int LOADER_ID_WALLPAPER = 105;
    private static final int LOADER_ID_BOOTANIMATIONS = 106;

    private Context mContext;
    private LayoutInflater mInflater;
    private ViewPager mPager;

    private String mComponentType;
    private CursorPagerAdapter mAdapter;
    private int mBatteryStyle;
    private int mItemsPerPage;

    // animations for bringing selector in and out of view
    private Animation mAnimateIn;
    private Animation mAnimateOut;

    private OnItemClickedListener mListener;

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
        mAnimateOut.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mPager = (ViewPager) findViewById(R.id.pager);
        mAdapter = new CursorPagerAdapter<View>(null, mItemsPerPage);
        mPager.setAdapter(mAdapter);
        PageIndicator indicator = (PageIndicator) findViewById(R.id.page_indicator);
        indicator.setViewPager(mPager);

        // set navbar_padding to GONE if no on screen navigation bar is available
        if (!hasNavigationBar()) findViewById(R.id.navbar_padding).setVisibility(View.GONE);
    }

    public void setComponentType(String component) {
        mComponentType = component;
        mAdapter = new CursorPagerAdapter<View>(null, mItemsPerPage);
        mPager.setAdapter(mAdapter);
        ((FragmentActivity) mContext).getSupportLoaderManager().initLoader(
                getLoaderIdFromComponent(component), null, this);
    }

    public void setNumItemsPerPage(int itemsPerPage) {
        mItemsPerPage = itemsPerPage;
    }

    public void setHeight(int height) {
        ViewGroup.LayoutParams params = mPager.getLayoutParams();
        params.height = height;
        mPager.setLayoutParams(params);
        requestLayout();
    }

    public void show() {
        setVisibility(View.VISIBLE);
        startAnimation(mAnimateIn);
    }

    public void hide() {
        startAnimation(mAnimateOut);
    }

    private boolean hasNavigationBar() {
        return !ViewConfiguration.get(mContext).hasPermanentMenuKey();
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
        return -1;
    }

    private String getBatteryIndex(int type) {
        switch(type) {
            case 2:
                return PreviewColumns.STATUSBAR_BATTERY_CIRCLE;
            case 5:
                return PreviewColumns.STATUSBAR_BATTERY_LANDSCAPE;
            default:
                return PreviewColumns.STATUSBAR_BATTERY_PORTRAIT;
        }
    }

    private Bitmap loadBitmapBlob(Cursor cursor, int columnIdx) {
        byte[] blob = cursor.getBlob(columnIdx);
        if (blob == null) return null;
        return BitmapFactory.decodeByteArray(blob, 0, blob.length);
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
                        PreviewColumns.STYLE_PREVIEW,
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
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
        mAdapter.notifyDataSetChanged();
    }

    public void setOnItemClickedListener(OnItemClickedListener listener) {
        mListener = listener;
    }

    public class CursorPagerAdapter<T extends View> extends PagerAdapter {
        LinearLayout.LayoutParams mItemParams =
                new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f);
        private Cursor mCursor;
        private int mItemsPerPage;
        HashMap<String, ThemedTypefaceHelper> mTypefaceHelpers =
                new HashMap<String, ThemedTypefaceHelper>();

        public CursorPagerAdapter(Cursor cursor, int itemsPerPage) {
            super();
            mCursor = cursor;
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
                newWallpapersView(mCursor, v, position);
            }
            if (MODIFIES_BOOT_ANIM.equals(mComponentType)) {
                newBootanimationView(mCursor, v, position);
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
            return mCursor == null ? 0 : (int) Math.ceil((float)mCursor.getCount() / mItemsPerPage);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        public void swapCursor(Cursor c) {
            if (mCursor == c)
                return;

            mCursor = c;
            notifyDataSetChanged();
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
                int batteryIndex = cursor.getColumnIndex(getBatteryIndex(mBatteryStyle));
                int backgroundIndex = cursor.getColumnIndex(PreviewColumns.STATUSBAR_BACKGROUND);
                int titleIndex = cursor.getColumnIndex(ThemesContract.ThemesColumns.TITLE);
                int pkgNameIndex = cursor.getColumnIndex(ThemesContract.ThemesColumns.PKG_NAME);

                ((ImageView) v.findViewById(R.id.slot1)).setImageBitmap(
                        loadBitmapBlob(cursor, wifiIndex));
                ((ImageView) v.findViewById(R.id.slot2)).setImageBitmap(
                        loadBitmapBlob(cursor, signalIndex));
                ((ImageView) v.findViewById(R.id.slot3)).setImageBitmap(
                        loadBitmapBlob(cursor, bluetoothIndex));
                ((ImageView) v.findViewById(R.id.slot4)).setImageBitmap(
                        loadBitmapBlob(cursor, batteryIndex));
                ((TextView) v.findViewById(R.id.title)).setText(cursor.getString(titleIndex));
                v.findViewById(R.id.container).setBackground(
                        new BitmapDrawable(loadBitmapBlob(cursor, backgroundIndex)));
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
                int titleIndex = cursor.getColumnIndex(ThemesContract.ThemesColumns.TITLE);
                int pkgNameIndex = cursor.getColumnIndex(ThemesContract.ThemesColumns.PKG_NAME);

                ((ImageView) v.findViewById(R.id.back)).setImageBitmap(
                        loadBitmapBlob(cursor, backIndex));
                ((TextView) v.findViewById(R.id.title)).setText(cursor.getString(titleIndex));
                v.findViewById(R.id.container).setBackground(
                        new BitmapDrawable(loadBitmapBlob(cursor, backgroundIndex)));
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
                int titleIndex = cursor.getColumnIndex(ThemesContract.ThemesColumns.TITLE);
                int pkgNameIndex = cursor.getColumnIndex(ThemesContract.ThemesColumns.PKG_NAME);

                TextView preview = (TextView) v.findViewById(R.id.text_preview);
                String pkgName = cursor.getString(pkgNameIndex);

                ThemedTypefaceHelper helper;
                if (!mTypefaceHelpers.containsKey(pkgName)) {
                    helper = new ThemedTypefaceHelper();
                    helper.load(mContext, pkgName);
                    mTypefaceHelpers.put(pkgName, helper);
                } else {
                    helper = mTypefaceHelpers.get(pkgName);
                }
                Typeface typefaceNormal = helper.getTypeface(Typeface.NORMAL);
                preview.setTypeface(typefaceNormal);

                ((TextView) v.findViewById(R.id.title)).setText(cursor.getString(titleIndex));
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
                int titleIndex = cursor.getColumnIndex(ThemesContract.ThemesColumns.TITLE);
                int pkgNameIndex = cursor.getColumnIndex(ThemesContract.ThemesColumns.PKG_NAME);

                ((ImageView) v.findViewById(R.id.icon)).setImageBitmap(
                        loadBitmapBlob(cursor, iconIndex));
                ((TextView) v.findViewById(R.id.title)).setText(cursor.getString(titleIndex));
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
                int styleIndex = cursor.getColumnIndex(PreviewColumns.STYLE_PREVIEW);
                int titleIndex = cursor.getColumnIndex(ThemesContract.ThemesColumns.TITLE);
                int pkgNameIndex = cursor.getColumnIndex(ThemesContract.ThemesColumns.PKG_NAME);

                ((ImageView) v.findViewById(R.id.icon)).setImageBitmap(
                        loadBitmapBlob(cursor, styleIndex));
                ((TextView) v.findViewById(R.id.title)).setText(cursor.getString(titleIndex));
                v.setTag(cursor.getString(pkgNameIndex));
                v.setOnClickListener(mItemClickListener);
                parent.addView(v, mItemParams);
                addDividerIfNeeded(parent, i, index, cursor);
            }
        }

        private void newWallpapersView(Cursor cursor, ViewGroup parent, int position) {
            for (int i = 0; i < mItemsPerPage; i++) {
                int index = position * mItemsPerPage + i;
                if (cursor.getCount() <= index) continue;
                cursor.moveToPosition(index);
                View v = mInflater.inflate(R.layout.wallpaper_component_selection_item, parent,
                        false);
                int wallpaperIndex = cursor.getColumnIndex(PreviewColumns.WALLPAPER_THUMBNAIL);
                int titleIndex = cursor.getColumnIndex(ThemesContract.ThemesColumns.TITLE);
                int pkgNameIndex = cursor.getColumnIndex(ThemesContract.ThemesColumns.PKG_NAME);

                ((ImageView) v.findViewById(R.id.icon)).setImageBitmap(
                        loadBitmapBlob(cursor, wallpaperIndex));
                ((TextView) v.findViewById(R.id.title)).setText(cursor.getString(titleIndex));
                v.setTag(cursor.getString(pkgNameIndex));
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
                int titleIndex = cursor.getColumnIndex(ThemesContract.ThemesColumns.TITLE);
                int pkgNameIndex = cursor.getColumnIndex(ThemesContract.ThemesColumns.PKG_NAME);

                ((ImageView) v.findViewById(R.id.preview)).setImageBitmap(
                        loadBitmapBlob(cursor,wallpaperIndex));
                ((TextView) v.findViewById(R.id.title)).setText(cursor.getString(titleIndex));
                v.setTag(cursor.getString(pkgNameIndex));
                v.setOnClickListener(mItemClickListener);
                parent.addView(v, mItemParams);
                addDividerIfNeeded(parent, i, index, cursor);
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
    }

    public interface OnItemClickedListener {
        public void onItemClicked(String pkgName);
    }
}