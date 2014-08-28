/*
 * Copyright (C) 2014 The CyanogenMod, Inc.
 */
package com.cyngn.theme.chooser;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.ThemeConfig;
import android.content.res.ThemeManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ThemesContract;
import android.provider.ThemesContract.ThemesColumns;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
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
import android.view.ViewPropertyAnimator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import android.widget.ImageView;
import com.cyngn.theme.util.PreferenceUtils;
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
    private static final String TAG = ChooserActivity.class.getSimpleName();

    public static final String DEFAULT = ThemeConfig.HOLO_DEFAULT;
    public static final int REQUEST_UNINSTALL = 1; // Request code
    public static final String EXTRA_PKGNAME = "pkgName";

    private static final int OFFSCREEN_PAGE_LIMIT = 3;

    private static final int LOADER_ID_INSTALLED_THEMES = 1000;
    private static final int LOADER_ID_APPLIED = 1001;

    private static final String THEME_STORE_PACKAGE = "com.cyngn.theme.store";
    private static final String THEME_STORE_ACTIVITY = "com.cyngn.theme.store.StoreActivity";

    private static final long ANIMATE_CONTENT_IN_SCALE_DURATION = 500;
    private static final long ANIMATE_CONTENT_IN_ALPHA_DURATION = 750;
    private static final long ANIMATE_CONTENT_IN_BLUR_DURATION = 250;
    private static final long ANIMATE_CONTENT_DELAY = 250;
    private static final long ANIMATE_SHOP_THEMES_HIDE_DURATION = 250;
    private static final long ANIMATE_SHOP_THEMES_SHOW_DURATION = 500;
    private static final long FINISH_ANIMATION_DELAY = ThemeFragment.ANIMATE_DURATION
            + ThemeFragment.ANIMATE_START_DELAY + 250;

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
    private View mShopThemesLayout;

    private String mSelectedTheme;
    private String mAppliedBaseTheme;
    private boolean mThemeChanging = false;
    private boolean mAnimateContentIn = false;
    private long mAnimateContentInDelay;

    ImageView mCustomBackground;

    // Current system theme configuration as component -> pkgName
    private Map<String, String> mCurrentTheme = new HashMap<String, String>();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
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

        mShopThemesLayout = findViewById(R.id.shop_themes_layout);

        mSaveApplyLayout = findViewById(R.id.save_apply_layout);
        if (!Utils.hasNavigationBar(this)) {
            mSaveApplyLayout.findViewById(R.id.navbar_padding).setVisibility(View.GONE);
            mShopThemesLayout.findViewById(R.id.navbar_padding).setVisibility(View.GONE);
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
                        if (f != null) {
                            f.fadeOutCards(new Runnable() {
                                public void run() {
                                    mContainer.collapse();
                                    f.collapse(true);
                                }
                            });
                        }
                        setAnimatingStateAndScheduleFinish();
                    }
                });

        mShopThemesLayout.findViewById(R.id.shop_themes).setOnClickListener(mOnShopThemesClicked);

        mTypefaceHelperCache = TypefaceHelperCache.getInstance();
        mHandler = new Handler();
        mCustomBackground = (ImageView) findViewById(R.id.custom_bg);
        mAnimateContentIn = true;
        mAnimateContentInDelay = 0;
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

    private void hideShopThemesLayout() {
        final ViewPropertyAnimator anim = mShopThemesLayout.animate();
        anim.alpha(0f).setDuration(ANIMATE_SHOP_THEMES_HIDE_DURATION);
        anim.setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {}

            @Override
            public void onAnimationEnd(Animator animation) {
                mShopThemesLayout.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {}

            @Override
            public void onAnimationRepeat(Animator animation) {}
        });
    }

    private void showShopThemesLayout() {
        mShopThemesLayout.setVisibility(View.VISIBLE);
        final ViewPropertyAnimator anim = mShopThemesLayout.animate();
        anim.setListener(null);
        anim.alpha(1f).setStartDelay(ThemeFragment.ANIMATE_DURATION)
                .setDuration(ANIMATE_SHOP_THEMES_SHOW_DURATION);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_MAIN.equals(intent.getAction()) && intent.hasExtra(EXTRA_PKGNAME)) {
            mSelectedTheme = intent.getStringExtra(EXTRA_PKGNAME);
            if (mPager != null) {
                mAdapter = new ThemesAdapter(this);
                mPager.setAdapter(mAdapter);
                getSupportLoaderManager().restartLoader(LOADER_ID_INSTALLED_THEMES, null, this);
            }
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
        }, FINISH_ANIMATION_DELAY);
        if (mExpanded) {
            hideShopThemesLayout();
        } else {
            showShopThemesLayout();
        }
    }

    private void setCustomBackground(final ImageView iv) {
        final Context context = ChooserActivity.this;
        iv.post(new Runnable() {
            @Override
            public void run() {
                Bitmap tmpBmp = Utils.getRegularWallpaperBitmap(context);

                // Since we are applying a blur, we can afford to scale the bitmap down and use a
                // smaller blur radius.
                Bitmap inBmp = Bitmap.createScaledBitmap(tmpBmp, tmpBmp.getWidth() / 4,
                        tmpBmp.getHeight() / 4, false);
                Bitmap outBmp = Bitmap.createBitmap(inBmp.getWidth(), inBmp.getHeight(),
                        Bitmap.Config.ARGB_8888);

                // Blur the original bitmap
                RenderScript rs = RenderScript.create(context);
                ScriptIntrinsicBlur theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
                Allocation tmpIn = Allocation.createFromBitmap(rs, inBmp);
                Allocation tmpOut = Allocation.createFromBitmap(rs, outBmp);
                theIntrinsic.setRadius(5.0f);
                theIntrinsic.setInput(tmpIn);
                theIntrinsic.forEach(tmpOut);
                tmpOut.copyTo(outBmp);

                // Create a bitmap drawable and use a color matrix to de-saturate the image
                BitmapDrawable[] layers = new BitmapDrawable[2];
                layers[0] = new BitmapDrawable(getResources(), tmpBmp);
                layers[1] = new BitmapDrawable(getResources(), outBmp);
                ColorMatrix cm = new ColorMatrix();
                cm.setSaturation(0);
                Paint p = layers[0].getPaint();
                p.setColorFilter(new ColorMatrixColorFilter(cm));
                p = layers[1].getPaint();
                p.setColorFilter(new ColorMatrixColorFilter(cm));
                TransitionDrawable d = new TransitionDrawable(layers);

                // All done
                iv.setImageDrawable(d);
            }
        });
    }

    /**
     * Disable the ViewPager while a theme change is occuring
     */
    public void themeChangeStarted() {
        mThemeChanging = true;
        lockPager();
    }

    /**
     * Re-enable the ViewPager and update the "My theme" fragment if available
     */
    public void themeChangeEnded() {
        mThemeChanging = false;
        ThemeFragment f = getCurrentFragment();
        if (f != null)  {
            // We currently need to recreate the adapter in order to load
            // the changes otherwise the adapter returns the original fragments
            // TODO: We'll need a better way to handle this to provide a good UX
            if (!(f instanceof MyThemeFragment)) {
                mAdapter = new ThemesAdapter(this);
                mPager.setAdapter(mAdapter);
            }
            mAppliedBaseTheme = f.getThemePackageName();
            PreferenceUtils.setAppliedBaseTheme(this, mAppliedBaseTheme);
            getSupportLoaderManager().restartLoader(LOADER_ID_APPLIED, null,
                    ChooserActivity.this);
        }
        unlockPager();
    }

    public void lockPager() {
        mPager.setScrollingEnabled(false);
    }

    public void unlockPager() {
        mPager.setScrollingEnabled(true);
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
                slideContentUp(top - coordinates[1], height);
            }
        }
    }

    public void expand() {
        if (!mExpanded && !mIsAnimating) {
            mExpanded = true;
            mContainer.setClickable(false);
            mContainer.expand();
            ThemeFragment f = getCurrentFragment();
            if (f != null) {
                f.expand();
            }
            setAnimatingStateAndScheduleFinish();
        }
    }

    private void slideContentUp(int yDelta, int selectorHeight) {
        ThemeFragment f = getCurrentFragment();
        if (f != null) {
            yDelta -= getResources().getDimensionPixelSize(R.dimen.content_offset_padding);
            f.slideContentUp(-yDelta, selectorHeight);
            mContainerYOffset = yDelta;
        }
    }

    private void slideContentDown(final int yDelta) {
        ThemeFragment f = getCurrentFragment();
        if (f != null) {
            f.slideContentDown(yDelta);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mService.onClientResumed(this);
        setCustomBackground(mCustomBackground);
        getSupportLoaderManager().restartLoader(LOADER_ID_APPLIED, null, this);

        IntentFilter filter = new IntentFilter(Intent.ACTION_WALLPAPER_CHANGED);
        registerReceiver(mWallpaperChangeReceiver, filter);

        // clear out the newly installed themes count
        PreferenceUtils.setNewlyInstalledThemeCount(this, 0);
    }

    @Override
    public void onBackPressed() {
        final ThemeFragment f = getCurrentFragment();
        if (mSelector.isEnabled()) {
            mSelector.hide();
            if (mContainerYOffset != 0) {
                slideContentDown(mContainerYOffset);
                mContainerYOffset = 0;
            }
            if (f != null) f.fadeInCards();
        } else if (mExpanded) {
            if (mIsAnimating) {
                return;
            }

            if (mSaveApplyLayout.getVisibility() == View.VISIBLE) {
                hideSaveApplyButton();
                if (f != null) f.clearChanges();
            }
            mExpanded = false;
            if (f != null) {
                f.fadeOutCards(new Runnable() {
                    public void run() {
                        mContainer.collapse();
                        f.collapse(false);
                    }
                });
            }
            setAnimatingStateAndScheduleFinish();
        } else {
            if (f != null && f.isShowingApplyThemeLayout()) {
                f.hideApplyThemeLayout();
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mService.onClientPaused(this);
        unregisterReceiver(mWallpaperChangeReceiver);
        ThemeFragment f = getCurrentFragment();
        if (f != null) {
            mSelectedTheme = f.getThemePackageName();
        }
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

    private void animateContentIn() {
        Drawable d = mCustomBackground.getDrawable();
        if (d instanceof TransitionDrawable) {
            ((TransitionDrawable) d).startTransition((int) ANIMATE_CONTENT_IN_BLUR_DURATION);
        }

        AnimatorSet set = new AnimatorSet();
        set.play(ObjectAnimator.ofFloat(mContainer, "alpha", 0f, 1f)
                .setDuration(ANIMATE_CONTENT_IN_ALPHA_DURATION))
                .with(ObjectAnimator.ofFloat(mContainer, "scaleX", 2f, 1f)
                .setDuration(ANIMATE_CONTENT_IN_SCALE_DURATION))
                .with(ObjectAnimator.ofFloat(mContainer, "scaleY", 2f, 1f)
                .setDuration(ANIMATE_CONTENT_IN_SCALE_DURATION));
        set.setStartDelay(mAnimateContentInDelay);
        set.start();
        mShopThemesLayout.setAlpha(0f);
        mShopThemesLayout.animate().alpha(1f).setStartDelay(mAnimateContentInDelay)
                .setDuration(ANIMATE_CONTENT_IN_ALPHA_DURATION);
        mAnimateContentIn = false;
    }

    private View.OnClickListener mPagerClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ThemeFragment f = getCurrentFragment();
            if (f != null) {
                f.performClick(mPager.isClickedOnContent());
            }
        }
    };

    private BroadcastReceiver mWallpaperChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mCustomBackground != null) setCustomBackground(mCustomBackground);
        }
    };

    private ComponentSelector.OnOpenCloseListener mOpenCloseListener = new ComponentSelector.OnOpenCloseListener() {
        @Override
        public void onSelectorOpened() {
        }

        @Override
        public void onSelectorClosed() {
            ThemeFragment f = getCurrentFragment();
            if (f != null && f.componentsChanged()) {
                mSaveApplyLayout.setVisibility(View.VISIBLE);
                mSaveApplyLayout.startAnimation(AnimationUtils.loadAnimation(ChooserActivity.this,
                        R.anim.component_selection_animate_in));
            }
        }
    };

    private ThemeFragment getCurrentFragment() {
        // instantiateItem will return the fragment if it already exists and not instantiate it,
        // which should be the case for the current fragment.
        return (mAdapter == null || mPager == null) ? null :
                (ThemeFragment) mAdapter.instantiateItem(mPager, mPager.getCurrentItem());
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

    private View.OnClickListener mOnShopThemesClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent();
            intent.setClassName(THEME_STORE_PACKAGE, THEME_STORE_ACTIVITY);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                // Unable to launch the theme store so link the user to it
                intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(getString(R.string.themes_showcase_link)));
                startActivity(intent);
            }
        }
    };

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (mThemeChanging) return;

        switch (loader.getId()) {
            case LOADER_ID_INSTALLED_THEMES:
                // Swap the new cursor in. (The framework will take care of closing the
                // old cursor once we return.)
                int selectedThemeIndex = -1;
                if (TextUtils.isEmpty(mSelectedTheme)) mSelectedTheme = mAppliedBaseTheme;
                while(data.moveToNext()) {
                    if (mSelectedTheme.equals(data.getString(
                            data.getColumnIndex(ThemesColumns.PKG_NAME)))) {
                        // we need to add one here since the first card is "My theme"
                        selectedThemeIndex = data.getPosition();
                        mSelectedTheme = null;
                        break;
                    }
                }
                data.moveToFirst();
                mAdapter.swapCursor(data);
                mAdapter.notifyDataSetChanged();
                if (selectedThemeIndex >= 0) {
                    mPager.setCurrentItem(selectedThemeIndex, false);
                }
                if (mAnimateContentIn) animateContentIn();
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
        String[] projection = null;
        Uri contentUri = null;

        switch (id) {
            case LOADER_ID_INSTALLED_THEMES:
                mAppliedBaseTheme = PreferenceUtils.getAppliedBaseTheme(this);
                selection = ThemesColumns.PRESENT_AS_THEME + "=?";
                selectionArgs = new String[] { "1" };
                // sort in ascending order but make sure the "default" theme is always first
                sortOrder = "(" + ThemesColumns.IS_DEFAULT_THEME + "=1) DESC, "
                        + "(" + ThemesColumns.PKG_NAME + "='" + mAppliedBaseTheme + "') DESC, "
                        + ThemesColumns.INSTALL_TIME + " DESC";
                contentUri = ThemesColumns.CONTENT_URI;
                projection = new String[] {ThemesColumns.PKG_NAME, ThemesColumns.TITLE};
                break;
            case LOADER_ID_APPLIED:
                //TODO: Mix n match query should only be done once
                contentUri = ThemesContract.MixnMatchColumns.CONTENT_URI;
                selection = null;
                selectionArgs = null;
                break;
        }


        return new CursorLoader(this, contentUri, projection, selection,
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
            ThemeFragment f = null;
            if (mCursor != null) {
                mCursor.moveToPosition(position);
                int pkgIdx = mCursor.getColumnIndex(ThemesColumns.PKG_NAME);
                final String pkgName = mCursor.getString(pkgIdx);
                if (pkgName.equals(mAppliedBaseTheme)) {
                    String title = mCursor.getString(mCursor.getColumnIndex(ThemesColumns.TITLE));
                    f = MyThemeFragment.newInstance(mAppliedBaseTheme, title, mAnimateContentIn);
                } else {
                    f = ThemeFragment.newInstance(pkgName, mAnimateContentIn);
                }
                f.setCurrentTheme(mCurrentTheme);
            }
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
            return mCursor == null ? 0 : mCursor.getCount();
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
