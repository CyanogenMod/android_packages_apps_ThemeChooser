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
import android.content.res.ThemeConfig;
import android.content.res.ThemeManager;
import android.database.Cursor;
import android.os.AsyncTask;
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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import org.cyanogenmod.theme.chooser.R;
import org.cyanogenmod.theme.chooserv2.ComponentSelector.OnOpenCloseListener;
import org.cyanogenmod.theme.util.TypefaceHelperCache;
import org.cyanogenmod.theme.util.Utils;

import java.util.Map;

import static android.provider.ThemesContract.ThemesColumns.MODIFIES_ALARMS;
import static android.provider.ThemesContract.ThemesColumns.MODIFIES_BOOT_ANIM;
import static android.provider.ThemesContract.ThemesColumns.MODIFIES_NOTIFICATIONS;
import static android.provider.ThemesContract.ThemesColumns.MODIFIES_RINGTONES;

public class ChooserActivity extends FragmentActivity
        implements LoaderManager.LoaderCallbacks<Cursor>, ThemeManager.ThemeChangeListener {
    public static final String DEFAULT = ThemeConfig.HOLO_DEFAULT;

    private static final long SLIDE_CONTENT_ANIM_DURATION = 300L;

    private PagerContainer mContainer;
    private ThemeViewPager mPager;
    private Button mEdit;
    private ThemesAdapter mAdapter;
    private ThemeManager mService;
    private boolean mExpanded = false;
    private ComponentSelector mSelector;
    private View mSaveApplyLayout;
    private int mContainerYOffset = 0;
    private TypefaceHelperCache mTypefaceHelperCache;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.v2_activity_main);

        mContainer = (PagerContainer) findViewById(R.id.pager_container);
        mPager = (ThemeViewPager) findViewById(R.id.viewpager);
        mEdit = (Button) findViewById(R.id.edit);

        mPager.setOnClickListener(mPagerClickListener);
        mAdapter = new ThemesAdapter(this);
        mPager.setAdapter(mAdapter);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, dm);
        mPager.setPageMargin(-margin / 2);
        mPager.setOffscreenPageLimit(3);

        mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageSelected(int position) {
            }

            public void onPageScrolled(int position,
                                       float positionOffset,
                                       int positionOffsetPixels) {
            }

            public void onPageScrollStateChanged(int state) {
            }
        });

        mEdit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mExpanded = true;
                mContainer.expand();
                ThemeFragment f = getCurrentFragment();
                f.expand();
            }
        });

        mSelector = (ComponentSelector) findViewById(R.id.component_selector);
        mSelector.setOnOpenCloseListener(mOpenCloseListener);

        mService = (ThemeManager) getSystemService(Context.THEME_SERVICE);
        getSupportLoaderManager().initLoader(0, null, this);

        mSaveApplyLayout = findViewById(R.id.save_apply_layout);
        if (!Utils.hasNavigationBar(this)) {
            mSaveApplyLayout.findViewById(R.id.navbar_padding).setVisibility(View.GONE);
        }
        mSaveApplyLayout.findViewById(R.id.save_apply_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hideSaveApplyButton();
                        mExpanded = false;
                        final ThemeFragment f = getCurrentFragment();
                        f.fadeOutCards(new Runnable() {
                            public void run() {
                                mContainer.collapse();
                                f.collapse();
                            }
                        });
                    }
                });
        mTypefaceHelperCache = TypefaceHelperCache.getInstance();
    }

    public void hideSaveApplyButton() {
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.component_selection_animate_out);
        mSaveApplyLayout.startAnimation(anim);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mSaveApplyLayout.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    public ComponentSelector getComponentSelector() {
        return mSelector;
    }

    public void showComponentSelector(String component, View v) {
        if (component != null) {
            final Resources res = getResources();
            int itemsPerPage = res.getInteger(R.integer.default_items_per_page);
            int height = res.getDimensionPixelSize(R.dimen.component_selection_cell_height);
            if (MODIFIES_BOOT_ANIM.equals(component)) {
                itemsPerPage = res.getInteger(R.integer.bootani_items_per_page);
                height = res.getDimensionPixelSize(
                        R.dimen.component_selection_cell_height_boot_anim);
            } else if (MODIFIES_ALARMS.equals(component) ||
                    MODIFIES_NOTIFICATIONS.equals(component) ||
                    MODIFIES_RINGTONES.equals(component)) {
                itemsPerPage = 2;
                height = res.getDimensionPixelSize(
                        R.dimen.component_selection_cell_height_sounds);
            }
            if (mSaveApplyLayout.getVisibility() == View.VISIBLE) hideSaveApplyButton();
            mSelector.show(component, itemsPerPage, height);

            // determine if we need to shift the cards up
            int[] coordinates = new int[2];
            v.getLocationOnScreen(coordinates);
            coordinates[1] += v.getHeight();
            int top = getWindowManager().getDefaultDisplay().getHeight() - height;
            if (coordinates[1] > top) {
                slideContentUp(top - coordinates[1]);
            }
        }
    }

    private void slideContentUp(int yDelta) {
        yDelta -= getResources().getDimensionPixelSize(R.dimen.content_offset_padding);
        mContainerYOffset = yDelta;
        mContainer.animate().translationYBy(yDelta).setDuration(SLIDE_CONTENT_ANIM_DURATION);
    }

    private void slideContentDown(final int yDelta) {
        mContainer.animate().translationYBy(-yDelta).setDuration(SLIDE_CONTENT_ANIM_DURATION);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mService.onClientResumed(this);
    }

    @Override
    public void onBackPressed() {
        if (mSelector.getVisibility() == View.VISIBLE) {
            mSelector.hide();
            if (mContainerYOffset != 0) {
                slideContentDown(mContainerYOffset);
                mContainerYOffset = 0;
            }
            final ThemeFragment f = getCurrentFragment();
            f.fadeInCards();
        } else if (mExpanded) {
            if (mSaveApplyLayout.getVisibility() == View.VISIBLE) {
                hideSaveApplyButton();
                getCurrentFragment().clearChanges();
            }
            mExpanded = false;
            final ThemeFragment f = getCurrentFragment();
            f.fadeOutCards(new Runnable() {
                public void run() {
                    mContainer.collapse();
                    f.collapse();
                }
            });
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mService.onClientPaused(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mService.onClientDestroyed(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mTypefaceHelperCache.getTypefaceCount() <= 0) {
            new TypefacePreloadTask().execute();
        }
    }

    private View.OnClickListener mPagerClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!mExpanded) {
                mExpanded = true;
                mContainer.expand();
                ThemeFragment f = getCurrentFragment();
                f.expand();
            }
        }
    };

    private OnOpenCloseListener mOpenCloseListener = new OnOpenCloseListener() {
        @Override
        public void onSelectorOpened() {
        }

        @Override
        public void onSelectorClosed() {
            ThemeFragment f = getCurrentFragment();
            if (f.componentsChanged()) {
                mSaveApplyLayout.setVisibility(View.VISIBLE);
                mSaveApplyLayout.startAnimation(AnimationUtils.loadAnimation(ChooserActivity.this,
                        R.anim.component_selection_animate_in));
            }
        }
    };

    private ThemeFragment getCurrentFragment() {
        ThemeFragment f = (ThemeFragment) getSupportFragmentManager()
                .findFragmentByTag(getFragmentTag(mPager.getCurrentItem()));
        return f;
    }

    private String getFragmentTag(int pos){
        return "android:switcher:"+R.id.viewpager+":"+pos;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);
        mAdapter.notifyDataSetChanged();
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
            String pkgName;
            if (position == 0) {
                pkgName = ThemeFragment.CURRENTLY_APPLIED_THEME;
            } else {
                mCursor.moveToPosition(position - 1);
                int pkgIdx = mCursor.getColumnIndex(ThemesColumns.PKG_NAME);
                pkgName = mCursor.getString(pkgIdx);
            }
            return ThemeFragment.newInstance(pkgName);
        }

        /**
         * The first card should be the user's currently applied theme components so we
         * will always return at least 1 or mCursor.getCount() + 1
         * @return
         */
        public int getCount() {
            return mCursor == null ? 1 : mCursor.getCount() + 1;
        }

        public String getItemPkgName(int position) {
            if (position == 0) {
                return ThemeFragment.CURRENTLY_APPLIED_THEME;
            }
            mCursor.moveToPosition(position - 1);
            int pkgIdx = mCursor.getColumnIndex(ThemesContract.ThemesColumns.PKG_NAME);
            return mCursor.getString(pkgIdx);
        }

        public void swapCursor(Cursor c) {
            mCursor = c;
        }
    }

    private class TypefacePreloadTask extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] params) {
            String[] projection = { ThemesColumns.PKG_NAME };
            String selection = ThemesColumns.MODIFIES_FONTS + "=?";
            String[] selectionArgs = { "1" };
            Cursor c = getContentResolver().query(ThemesColumns.CONTENT_URI, projection, selection,
                    selectionArgs, null);
            if (c != null) {
                while (c.moveToNext()) {
                    mTypefaceHelperCache.getHelperForTheme(ChooserActivity.this, c.getString(0));
                }
                c.close();
            }
            return null;
        }
    }
}
