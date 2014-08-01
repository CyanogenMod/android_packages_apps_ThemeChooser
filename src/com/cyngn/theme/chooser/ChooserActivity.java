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
package com.cyngn.theme.chooser;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.ThemeConfig;
import android.content.res.ThemeManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ThemesContract;
import android.provider.ThemesContract.ThemesColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ThemeViewPager;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.cyngn.theme.util.TypefaceHelperCache;
import com.cyngn.theme.util.Utils;

import java.util.HashMap;
import java.util.Map;

import static android.provider.ThemesContract.ThemesColumns.MODIFIES_ALARMS;
import static android.provider.ThemesContract.ThemesColumns.MODIFIES_BOOT_ANIM;
import static android.provider.ThemesContract.ThemesColumns.MODIFIES_NOTIFICATIONS;
import static android.provider.ThemesContract.ThemesColumns.MODIFIES_RINGTONES;

public class ChooserActivity extends FragmentActivity
        implements LoaderManager.LoaderCallbacks<Cursor>, ThemeManager.ThemeChangeListener {
    public static final String DEFAULT = ThemeConfig.HOLO_DEFAULT;
    public static final int REQUEST_UNINSTALL = 1; // Request code
    public static final String EXTRA_PKGNAME = "pkgName";

    private static final long SLIDE_CONTENT_ANIM_DURATION = 300L;
    private static final long MOVE_TO_MY_THEME_DELAY = 750L;

    private static final int OFFSCREEN_PAGE_LIMIT = 3;

    private static final int LOADER_ID_INSTALLED_THEMES = 1000;
    private static final int LOADER_ID_APPLIED = 1001;

    private PagerContainer mContainer;
    private ThemeViewPager mPager;

    private ThemesAdapter mAdapter;
    private ThemeManager mService;
    private boolean mExpanded = false;
    private ComponentSelector mSelector;
    private View mSaveApplyLayout;
    private int mContainerYOffset = 0;
    private TypefaceHelperCache mTypefaceHelperCache;
    private boolean mIsAnimating;
    private Handler mHandler;

    private String mSelectedTheme;

    // Current system theme configuration as component -> pkgName
    private Map<String, String> mCurrentTheme = new HashMap<String, String>();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        NotificationHijackingService.ensureEnabled(this);

        if (savedInstanceState == null) {
            handleIntent(getIntent());
        }

        mContainer = (PagerContainer) findViewById(R.id.pager_container);
        mPager = (ThemeViewPager) findViewById(R.id.viewpager);

        mPager.setOnClickListener(mPagerClickListener);
        mAdapter = new ThemesAdapter(this);
        mPager.setAdapter(mAdapter);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, dm);
        mPager.setPageMargin(-margin / 2);
        mPager.setOffscreenPageLimit(OFFSCREEN_PAGE_LIMIT);

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

        mSelector = (ComponentSelector) findViewById(R.id.component_selector);
        mSelector.setOnOpenCloseListener(mOpenCloseListener);

        mService = (ThemeManager) getSystemService(Context.THEME_SERVICE);
        getSupportLoaderManager().restartLoader(LOADER_ID_APPLIED, null, this);

        mSaveApplyLayout = findViewById(R.id.save_apply_layout);
        if (!Utils.hasNavigationBar(this)) {
            mSaveApplyLayout.findViewById(R.id.navbar_padding).setVisibility(View.GONE);
        }
        mSaveApplyLayout.findViewById(R.id.save_apply_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mIsAnimating) return;
                        hideSaveApplyButton();
                        mExpanded = false;
                        mContainer.setClickable(false);
                        final ThemeFragment f = getCurrentFragment();
                        f.fadeOutCards(new Runnable() {
                            public void run() {
                                mContainer.collapse();
                                f.collapse(true);
                            }
                        });
                        setAnimatingStateAndScheduleFinish();
                    }
                });
        mTypefaceHelperCache = TypefaceHelperCache.getInstance();
        mHandler = new Handler();
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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_MAIN.equals(intent.getAction()) && intent.hasExtra(EXTRA_PKGNAME)) {
            mSelectedTheme = intent.getStringExtra(EXTRA_PKGNAME);
        } else {
            mSelectedTheme = null;
        }
    }

    private void setAnimatingStateAndScheduleFinish() {
        mIsAnimating = true;
        mContainer.setIsAnimating(true);
        mHandler.postDelayed(new Runnable() {
            public void run() {
                mIsAnimating = false;
                mContainer.setIsAnimating(false);
            }
        }, ThemeFragment.ANIMATE_START_DELAY + ThemeFragment.ANIMATE_DURATION);
    }

    /**
     * Disable the ViewPager while a theme change is occuring
     */
    public void themeChangeStarted() {
        mPager.setEnabled(false);
    }

    /**
     * Re-enable the ViewPager and update the "My theme" fragment if available
     */
    public void themeChangeEnded() {
        if (mPager.getCurrentItem() != 0) {
            ThemeFragment f;
            if (mPager.getCurrentItem() <= OFFSCREEN_PAGE_LIMIT) {
                // Clear the "My theme" card so it loads the newly applied changes
                f = (ThemeFragment) mAdapter.instantiateItem(mPager, 0);
                if (f != null) f.clearChanges();
            }

            // clear the current card so it returns to it's previous state
            f = getCurrentFragment();
            if (f != null) f.clearChanges();
            mPager.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mPager.setCurrentItem(0, true);
                }
            }, MOVE_TO_MY_THEME_DELAY);
        }
        mPager.setEnabled(true);
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
        getSupportLoaderManager().restartLoader(LOADER_ID_APPLIED, null, this);
    }

    @Override
    public void onBackPressed() {
        if (mSelector.isEnabled()) {
            mSelector.hide();
            if (mContainerYOffset != 0) {
                slideContentDown(mContainerYOffset);
                mContainerYOffset = 0;
            }
            final ThemeFragment f = getCurrentFragment();
            f.fadeInCards();
        } else if (mExpanded) {
            if (mIsAnimating) {
                return;
            }

            if (mSaveApplyLayout.getVisibility() == View.VISIBLE) {
                hideSaveApplyButton();
                getCurrentFragment().clearChanges();
            }
            mExpanded = false;
            final ThemeFragment f = getCurrentFragment();
            f.fadeOutCards(new Runnable() {
                public void run() {
                    mContainer.collapse();
                    f.collapse(false);
                }
            });
            setAnimatingStateAndScheduleFinish();
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
            if (!mExpanded && !mIsAnimating) {
                mExpanded = true;
                mContainer.setClickable(false);
                mContainer.expand();
                ThemeFragment f = getCurrentFragment();
                f.expand();
                setAnimatingStateAndScheduleFinish();
            }
        }
    };

    private ComponentSelector.OnOpenCloseListener mOpenCloseListener = new ComponentSelector.OnOpenCloseListener() {
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
        // instantiateItem will return the fragment if it already exists and not instantiate it,
        // which should be the case for the current fragment.
        return (ThemeFragment) mAdapter.instantiateItem(mPager, mPager.getCurrentItem());
    }

    private void populateCurrentTheme(Cursor c) {
        c.moveToPosition(-1);
        while(c.moveToNext()) {
            int mixkeyIdx = c.getColumnIndex(ThemesContract.MixnMatchColumns.COL_KEY);
            int pkgIdx = c.getColumnIndex(ThemesContract.MixnMatchColumns.COL_VALUE);
            String mixkey = c.getString(mixkeyIdx);
            String component = ThemesContract.MixnMatchColumns.mixNMatchKeyToComponent(mixkey);
            String pkg = c.getString(pkgIdx);
            mCurrentTheme.put(component, pkg);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case LOADER_ID_INSTALLED_THEMES:
                // Swap the new cursor in. (The framework will take care of closing the
                // old cursor once we return.)
                int selectedThemeIndex = 0;
                if (!TextUtils.isEmpty(mSelectedTheme)) {
                    while(data.moveToNext()) {
                        if (mSelectedTheme.equals(data.getString(
                                data.getColumnIndex(ThemesColumns.PKG_NAME)))) {
                            // we need to add one here since the first card is "My theme"
                            selectedThemeIndex = data.getPosition() + 1;
                            mSelectedTheme = null;
                            break;
                        }
                    }
                    data.moveToFirst();
                }
                mAdapter.swapCursor(data);
                mAdapter.notifyDataSetChanged();
                if (selectedThemeIndex != 0) {
                    mPager.setCurrentItem(selectedThemeIndex, false);
                }
                break;
            case LOADER_ID_APPLIED:
                getSupportLoaderManager().restartLoader(LOADER_ID_INSTALLED_THEMES, null, this);
                populateCurrentTheme(data);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOADER_ID_INSTALLED_THEMES:
                mAdapter.swapCursor(null);
                mAdapter.notifyDataSetChanged();
                break;
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String selection = null;
        String selectionArgs[] = null;
        String sortOrder = null;
        Uri contentUri = null;

        switch (id) {
            case LOADER_ID_INSTALLED_THEMES:
                selection = ThemesColumns.PRESENT_AS_THEME + "=?";
                selectionArgs = new String[] { "1" };
                // sort in ascending order but make sure the "default" theme is always first
                sortOrder = "(" + ThemesColumns.IS_DEFAULT_THEME + "=1) DESC, "
                        + ThemesColumns.TITLE + " ASC";
                contentUri = ThemesColumns.CONTENT_URI;
                break;
            case LOADER_ID_APPLIED:
                //TODO: Mix n match query should only be done once
                contentUri = ThemesContract.MixnMatchColumns.CONTENT_URI;
                selection = null;
                selectionArgs = null;
                break;
        }


        return new CursorLoader(this, contentUri, null, selection,
                selectionArgs, sortOrder);
    }

    @Override
    public void onProgress(int progress) {

    }

    @Override
    public void onFinish(boolean isSuccess) {

    }

    public class ThemesAdapter extends FragmentStatePagerAdapter {
        private Cursor mCursor;
        private Context mContext;

        public ThemesAdapter(Context context) {
            super(getSupportFragmentManager());
            mContext = context;
        }

        @Override
        public Fragment getItem(int position) {
            ThemeFragment f;
            if (position == 0) {
                f = MyThemeFragment.newInstance();
            } else {
                mCursor.moveToPosition(position - 1);
                int pkgIdx = mCursor.getColumnIndex(ThemesColumns.PKG_NAME);
                String pkgName = mCursor.getString(pkgIdx);
                f = ThemeFragment.newInstance(pkgName);
            }
            f.setCurrentTheme(mCurrentTheme);
            return f;
        }

        @Override
        public int getItemPosition(Object object) {
            ThemeFragment fragment = (ThemeFragment) object;
            if (fragment.isUninstalled()) {
                return POSITION_NONE;
            }
            return super.getItemPosition(object);
        }

        /**
         * The first card should be the user's currently applied theme components so we
         * will always return at least 1 or mCursor.getCount() + 1
         * @return
         */
        public int getCount() {
            return mCursor == null ? 1 : mCursor.getCount() + 1;
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
