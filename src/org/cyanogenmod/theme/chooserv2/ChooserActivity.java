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
import android.database.Cursor;
import android.os.Bundle;
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
import android.widget.TextView;

import org.cyanogenmod.theme.chooser.R;

public class ChooserActivity extends FragmentActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String DEFAULT = ThemeConfig.HOLO_DEFAULT;

    private PagerContainer mContainer;
    private ThemeViewPager mPager;
    private TextView mThemeName;
    private ThemesAdapter mAdapter;
    private boolean mExpanded = false;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.v2_activity_main);

        mContainer = (PagerContainer) findViewById(R.id.pager_container);
        mPager = (ThemeViewPager) findViewById(R.id.viewpager);
        mThemeName = (TextView) findViewById(R.id.theme_name);

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

        getSupportLoaderManager().initLoader(0, null, this);
    }

    private void updateThemeName() {
        int position = mPager.getCurrentItem();
        String name = mAdapter.getItemName(position);
        mThemeName.setText(name);
    }

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

        public void swapCursor(Cursor c) {
            mCursor = c;
        }
    }
}
