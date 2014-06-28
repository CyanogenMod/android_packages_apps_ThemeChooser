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
import android.content.res.ThemeConfig;
import android.content.res.ThemeManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ThemesContract;
import android.provider.ThemesContract.ThemesColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ThemeViewPager;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.cyanogenmod.theme.chooser.R;

public class ChooserActivity extends FragmentActivity
        implements LoaderManager.LoaderCallbacks<Cursor>, ThemeManager.ThemeChangeListener {
    public static final String DEFAULT = ThemeConfig.HOLO_DEFAULT;

    private PagerContainer mContainer;
    private ThemeViewPager mPager;
    private TextView mThemeName;
    private Button mApply;
    private Button mEdit;
    private ViewGroup mApplyEditBtns;
    private ThemesAdapter mAdapter;
    private ThemeManager mService;
    private boolean mExpanded = false;
    private Button mStatusBar;
    private Button mNavBar;
    private Button mIcons;
    private Button mFonts;
    private Button mStyles;
    private Button mWallpaper;
    private Button mBootani;
    private ComponentSelector mSelector;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.v2_activity_main);

        mContainer = (PagerContainer) findViewById(R.id.pager_container);
        mPager = (ThemeViewPager) findViewById(R.id.viewpager);
        mThemeName = (TextView) findViewById(R.id.theme_name);
        mApplyEditBtns = (ViewGroup) findViewById(R.id.apply_edit_container);
        mApply = (Button) findViewById(R.id.apply);
        mEdit = (Button) findViewById(R.id.edit);

        mPager.setOnClickListener(mPagerClickListener);
        mAdapter = new ThemesAdapter(this);
        mPager.setAdapter(mAdapter);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, dm);
        mPager.setPageMargin(margin);
        mPager.setOffscreenPageLimit(3);

        mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageSelected(int position) {
                updateThemeName();
            }

            public void onPageScrolled(int position,
                                       float positionOffset,
                                       int positionOffsetPixels) {
            }

            public void onPageScrollStateChanged(int state) {
            }
        });

        mApply.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int position = mPager.getCurrentItem();
                String pkgName = mAdapter.getItemPkgName(position);
                mService.requestThemeChange(pkgName);
            }
        });

        mEdit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mExpanded = true;
                mContainer.expand();
                ThemeFragment f = (ThemeFragment) getSupportFragmentManager()
                        .findFragmentByTag(getFragmentTag(mPager.getCurrentItem()));
                f.expand();
            }
        });

        mSelector = (ComponentSelector) findViewById(R.id.component_selector);

        if (ComponentSelector.DEBUG_SELECTOR) {
            findViewById(R.id.selector_testing).setVisibility(View.VISIBLE);
            mStatusBar = (Button) findViewById(R.id.show_status_bar);
            mNavBar = (Button) findViewById(R.id.show_nav_bar);
            mIcons = (Button) findViewById(R.id.show_icons);
            mFonts = (Button) findViewById(R.id.show_fonts);
            mStyles = (Button) findViewById(R.id.show_styles);
            mWallpaper = (Button) findViewById(R.id.show_wallpaper);
            mBootani = (Button) findViewById(R.id.show_bootani);
            mStatusBar.setOnClickListener(mButtonClickListener);
            mNavBar.setOnClickListener(mButtonClickListener);
            mIcons.setOnClickListener(mButtonClickListener);
            mFonts.setOnClickListener(mButtonClickListener);
            mStyles.setOnClickListener(mButtonClickListener);
            mWallpaper.setOnClickListener(mButtonClickListener);
            mBootani.setOnClickListener(mButtonClickListener);
        }

        mService = (ThemeManager) getSystemService(Context.THEME_SERVICE);
        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mService.onClientResumed("temp_placeholder", this);
    }

    @Override
    public void onBackPressed() {
        if (mSelector.getVisibility() == View.VISIBLE) {
            mSelector.hide();
        } else if (mExpanded) {
            mExpanded = false;
            mContainer.collapse();
            ThemeFragment f = (ThemeFragment) getSupportFragmentManager()
                    .findFragmentByTag(getFragmentTag(mPager.getCurrentItem()));
            f.collapse();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mService.onClientPaused("temp_placeholder");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mService.onClientDestroyed("temp_placeholder");
    }

    private void updateThemeName() {
        int position = mPager.getCurrentItem();
        String name = mAdapter.getItemName(position);
        mThemeName.setText(name);
    }

    private View.OnClickListener mButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == mStatusBar) {
                mSelector.setNumItemsPerPage(4);
                mSelector.setHeight(getResources().getDimensionPixelSize(
                        R.dimen.component_selection_cell_height));
                mSelector.setComponentType(ThemesColumns.MODIFIES_STATUS_BAR);
            } else if (v == mNavBar) {
                mSelector.setNumItemsPerPage(4);
                mSelector.setHeight(getResources().getDimensionPixelSize(
                        R.dimen.component_selection_cell_height));
                mSelector.setComponentType(ThemesColumns.MODIFIES_NAVIGATION_BAR);
            } else if (v == mIcons) {
                mSelector.setNumItemsPerPage(4);
                mSelector.setHeight(getResources().getDimensionPixelSize(
                        R.dimen.component_selection_cell_height));
                mSelector.setComponentType(ThemesColumns.MODIFIES_ICONS);
            } else if (v == mFonts) {
                mSelector.setNumItemsPerPage(4);
                mSelector.setHeight(getResources().getDimensionPixelSize(
                        R.dimen.component_selection_cell_height));
                mSelector.setComponentType(ThemesColumns.MODIFIES_FONTS);
            } else if (v == mStyles) {
                mSelector.setNumItemsPerPage(4);
                mSelector.setHeight(getResources().getDimensionPixelSize(
                        R.dimen.component_selection_cell_height));
                mSelector.setComponentType(ThemesColumns.MODIFIES_OVERLAYS);
            } else if (v == mWallpaper) {
                mSelector.setNumItemsPerPage(4);
                mSelector.setHeight(getResources().getDimensionPixelSize(
                        R.dimen.component_selection_cell_height));
                mSelector.setComponentType(ThemesColumns.MODIFIES_LAUNCHER);
            } else if (v == mBootani) {
                mSelector.setNumItemsPerPage(3);
                mSelector.setHeight(getResources().getDimensionPixelSize(
                        R.dimen.component_selection_cell_height_boot_anim));
                mSelector.setComponentType(ThemesColumns.MODIFIES_BOOT_ANIM);
            }
            if (mSelector.getVisibility() == View.GONE) mSelector.show();
        }
    };

    private View.OnClickListener mPagerClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mExpanded = !mExpanded;
            if (mExpanded) {
                mContainer.expand();
                ThemeFragment f = (ThemeFragment) getSupportFragmentManager()
                        .findFragmentByTag(getFragmentTag(mPager.getCurrentItem()));
                f.expand();
            } else {
                mContainer.collapse();
                ThemeFragment f = (ThemeFragment) getSupportFragmentManager()
                        .findFragmentByTag(getFragmentTag(mPager.getCurrentItem()));
                f.collapse();
            }
        }
    };

    private String getFragmentTag(int pos){
        return "android:switcher:"+R.id.viewpager+":"+pos;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);
        mAdapter.notifyDataSetChanged();

        updateThemeName();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String selection;
        String selectionArgs[] = null;
        selection = ThemesColumns.PRESENT_AS_THEME + "=?";
        selectionArgs = new String[] {"1"};

        // sort in ascending order but make sure the "default" theme is always first
        String sortOrder = "(" + ThemesColumns.IS_DEFAULT_THEME + "=1) DESC, "
                + ThemesColumns.TITLE + " ASC";

        return new CursorLoader(this, ThemesColumns.CONTENT_URI, null, selection,
                selectionArgs, sortOrder);
    }

    @Override
    public void onProgress(int progress) {

    }

    @Override
    public void onFinish(boolean isSuccess) {

    }

    public class ThemesAdapter extends FragmentPagerAdapter {
        private Cursor mCursor;
        private Context mContext;

        public ThemesAdapter(Context context) {
            super(getSupportFragmentManager());
            mContext = context;
        }

        @Override
        public Fragment getItem(int position) {
            mCursor.moveToPosition(position);
            int pkgIdx = mCursor.getColumnIndex(ThemesColumns.PKG_NAME);
            String pkgName = (String) mCursor.getString(pkgIdx);
            return ThemeFragment.newInstance(pkgName);
        }

        public int getCount() {
            return mCursor == null ? 0 : mCursor.getCount();
        }

        public String getItemName(int position) {
            mCursor.moveToPosition(position);
            int pkgIdx = mCursor.getColumnIndex(ThemesColumns.PKG_NAME);
            int titleIdx = mCursor.getColumnIndex(ThemesColumns.TITLE);
            String pkgName = mCursor.getString(pkgIdx);
            String title = DEFAULT.equals(pkgName) ? mContext.getString(R.string.holo)
                    : mCursor.getString(titleIdx);
            return title;
        }

        public String getItemPkgName(int position) {
            mCursor.moveToPosition(position);
            int pkgIdx = mCursor.getColumnIndex(ThemesContract.ThemesColumns.PKG_NAME);
            String pkgName = mCursor.getString(pkgIdx);
            return pkgName;
        }

        public void swapCursor(Cursor c) {
            mCursor = c;
        }
    }
}
