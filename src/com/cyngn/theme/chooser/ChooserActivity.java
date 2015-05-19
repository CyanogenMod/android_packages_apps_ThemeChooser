/*
 * Copyright (C) 2014 Cyanogen, Inc.
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
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.ThemeConfig;
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
import android.os.RemoteException;
import android.provider.ThemesContract;
import android.provider.ThemesContract.ThemesColumns;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ThemeViewPager;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.MutableLong;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import android.widget.ImageView;
import com.cyngn.theme.perapptheming.PerAppThemingWindow;
import com.cyngn.theme.util.NotificationHelper;
import com.cyngn.theme.util.PreferenceUtils;
import com.cyngn.theme.util.TypefaceHelperCache;
import com.cyngn.theme.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.provider.ThemesContract.ThemesColumns.MODIFIES_ALARMS;
import static android.provider.ThemesContract.ThemesColumns.MODIFIES_BOOT_ANIM;
import static android.provider.ThemesContract.ThemesColumns.MODIFIES_NOTIFICATIONS;
import static android.provider.ThemesContract.ThemesColumns.MODIFIES_RINGTONES;

import static com.cyngn.theme.chooser.ComponentSelector.DEFAULT_COMPONENT_ID;

public class ChooserActivity extends FragmentActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String THEME_STORE_PACKAGE = "com.cyngn.themestore";
    private static final String TAG = ChooserActivity.class.getSimpleName();

    public static final String DEFAULT = ThemeConfig.SYSTEM_DEFAULT;
    public static final String EXTRA_PKGNAME = "pkgName";
    public static final String EXTRA_COMPONENTS = "components";

    private static final int OFFSCREEN_PAGE_LIMIT = 3;

    private static final int LOADER_ID_INSTALLED_THEMES = 1000;
    private static final int LOADER_ID_APPLIED = 1001;


    private static final String THEME_STORE_ACTIVITY = THEME_STORE_PACKAGE + ".ui.StoreActivity";
    private static final String ACTION_APPLY_THEME = "android.intent.action.APPLY_THEME";
    private static final String PERMISSION_WRITE_THEME = "android.permission.WRITE_THEMES";

    private static final String TYPE_IMAGE = "image/*";

    private static final String CYNGN_THEMES_PERMISSION =
            "com.cyngn.themes.permission.THEMES_APP";
    private static final String ACTION_CHOOSER_OPENED =
            "com.cyngn.themes.action.CHOOSER_OPENED";
    private static final String ACTION_THEME_REMOVED =
            "com.cyngn.themes.action.THEME_REMOVED_FROM_CHOOSER";
    private static final String EXTRA_PACKAGE = "package";

    /**
     * Request code for picking an external wallpaper
     */
    public static final int REQUEST_PICK_WALLPAPER_IMAGE = 2;
    /**
     * Request code for picking an external lockscreen wallpaper
     */
    public static final int REQUEST_PICK_LOCKSCREEN_IMAGE = 3;

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
    private boolean mExpanded = false;
    private ComponentSelector mSelector;
    private View mSaveApplyLayout;
    private int mContainerYOffset = 0;
    private TypefaceHelperCache mTypefaceHelperCache;
    private boolean mIsAnimating;
    private Handler mHandler;
    private View mBottomActionsLayout;

    private String mSelectedTheme;
    private String mAppliedBaseTheme;
    private boolean mThemeChanging = false;
    private boolean mAnimateContentIn = false;
    private long mAnimateContentInDelay;
    private String mThemeToApply;
    private ArrayList mComponentsToApply;

    ImageView mCustomBackground;

    // Current system theme configuration as component -> pkgName
    private Map<String, String> mCurrentTheme = new HashMap<String, String>();
    private MutableLong mCurrentWallpaperCmpntId = new MutableLong(DEFAULT_COMPONENT_ID);

    private boolean mIsPickingImage = false;
    private boolean mRestartLoaderOnCollapse = false;
    private boolean mActivityResuming = false;

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
        mAdapter = new ThemesAdapter();
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

        mBottomActionsLayout = findViewById(R.id.bottom_actions_layout);

        mSaveApplyLayout = findViewById(R.id.save_apply_layout);
        mSaveApplyLayout.findViewById(R.id.save_apply_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mIsAnimating) return;
                        hideSaveApplyButton();
                        mContainer.setClickable(false);
                        collapse(true);
                    }
                });

        mBottomActionsLayout.findViewById(R.id.shop_themes)
                            .setOnClickListener(mOnShopThemesClicked);

        mTypefaceHelperCache = TypefaceHelperCache.getInstance();
        mHandler = new Handler();
        mCustomBackground = (ImageView) findViewById(R.id.custom_bg);
        mAnimateContentIn = true;
        mAnimateContentInDelay = 0;

        mBottomActionsLayout.findViewById(R.id.per_app_theming).setOnClickListener(
                new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PreferenceUtils.setShowPerAppThemeNewTag(ChooserActivity.this, false);
                Intent intent = new Intent(ChooserActivity.this, PerAppThemingWindow.class);
                startService(intent);
                finish();
            }
        });

        if (shouldHideShopThemes()) {
            mBottomActionsLayout.findViewById(R.id.shop_themes).setVisibility(View.GONE);
        }
        if (PreferenceUtils.getShowPerAppThemeNewTag(this)) {
            View tag = mBottomActionsLayout.findViewById(R.id.new_tag);
            if (tag != null) {
                tag.setVisibility(View.VISIBLE);
            }
        }
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

    private void hideBottomActionsLayout() {
        final ViewPropertyAnimator anim = mBottomActionsLayout.animate();
        anim.alpha(0f).setDuration(ANIMATE_SHOP_THEMES_HIDE_DURATION);
        anim.setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mBottomActionsLayout.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
    }

    private void showBottomActionsLayout() {
        mBottomActionsLayout.setVisibility(View.VISIBLE);
        final ViewPropertyAnimator anim = mBottomActionsLayout.animate();
        anim.setListener(null);
        anim.alpha(1f).setStartDelay(ThemeFragment.ANIMATE_DURATION)
                .setDuration(ANIMATE_SHOP_THEMES_SHOW_DURATION);
    }

    private boolean shouldHideShopThemes() {
        boolean hasThemeStore = false;
        try {
            if (getPackageManager().getPackageInfo(THEME_STORE_PACKAGE, 0) != null) {
                hasThemeStore = true;
            }
        } catch (PackageManager.NameNotFoundException e) {

        }
        return !hasThemeStore || Utils.isRecentTaskThemeStore(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        if (Utils.isRecentTaskHome(this)) {
            mContainer.setAlpha(0f);
            mBottomActionsLayout.setAlpha(0f);
            mAnimateContentIn = true;
            mAnimateContentInDelay = ANIMATE_CONTENT_DELAY;
        }
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if ((Intent.ACTION_MAIN.equals(action) || ACTION_APPLY_THEME.equals(action))
                && intent.hasExtra(EXTRA_PKGNAME)) {
            if (intent.hasExtra(EXTRA_COMPONENTS)) {
                mComponentsToApply = intent.getStringArrayListExtra(EXTRA_COMPONENTS);
            } else {
                mComponentsToApply = null;
            }
            mSelectedTheme = mComponentsToApply != null ?
                             PreferenceUtils.getAppliedBaseTheme(this) :
                             getSelectedTheme(intent.getStringExtra(EXTRA_PKGNAME));
            if (mPager != null) {
                startLoader(LOADER_ID_INSTALLED_THEMES);
                if (mExpanded) {
                    int collapseDelay = ThemeFragment.ANIMATE_START_DELAY;
                    if (mSelector.isEnabled()) {
                        // onBackPressed() has all the necessary logic for collapsing the
                        // component selector, so we call it here.
                        onBackPressed();
                        collapseDelay += ThemeFragment.ANIMATE_DURATION;
                    }
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            collapse(false);
                        }
                    }, collapseDelay);
                }
            }

            if (ACTION_APPLY_THEME.equals(action) &&
                    getCallingPackage() != null &&
                    PackageManager.PERMISSION_GRANTED ==
                            getPackageManager()
                                    .checkPermission(PERMISSION_WRITE_THEME,
                                            getCallingPackage())) {
                mThemeToApply = intent.getStringExtra(EXTRA_PKGNAME);
            }
        }
    }

    private String getSelectedTheme(String requestedTheme) {
        String[] projection = { ThemesColumns.PRESENT_AS_THEME };
        String selection = ThemesColumns.PKG_NAME + "=?";
        String[] selectionArgs = { requestedTheme };

        String selectedTheme = PreferenceUtils.getAppliedBaseTheme(this);

        Cursor cursor = getContentResolver().query(ThemesColumns.CONTENT_URI,
                projection, selection, selectionArgs, null);
        if (cursor != null) {
            if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                if (cursor.getInt(0) == 1) {
                    selectedTheme = requestedTheme;
                }
            }
            cursor.close();
        }
        return selectedTheme;
    }

    private void setAnimatingStateAndScheduleFinish() {
        mIsAnimating = true;
        mContainer.setIsAnimating(true);
        mHandler.postDelayed(new Runnable() {
            public void run() {
                mIsAnimating = false;
                mContainer.setIsAnimating(false);
                if (mRestartLoaderOnCollapse) {
                    mRestartLoaderOnCollapse = false;
                    startLoader(LOADER_ID_INSTALLED_THEMES);
                }
            }
        }, FINISH_ANIMATION_DELAY);
        if (mExpanded) {
            hideBottomActionsLayout();
        } else {
            showBottomActionsLayout();
        }
    }

    private void setCustomBackground(final ImageView iv, final boolean animate) {
        final Context context = ChooserActivity.this;
        iv.post(new Runnable() {
            @Override
            public void run() {
                Bitmap tmpBmp;
                try {
                    tmpBmp = Utils.getRegularWallpaperBitmap(context);
                } catch (Throwable e) {
                    Log.w(TAG, "Failed to retrieve wallpaper", e);
                    tmpBmp = null;
                }
                // Show the grid background if no wallpaper is set.
                // Note: no wallpaper is actually a 1x1 pixel black bitmap
                if (tmpBmp == null || tmpBmp.getWidth() <= 1 || tmpBmp.getHeight() <= 1) {
                    iv.setImageResource(R.drawable.bg_grid);
                    // We need to change the ScaleType to FIT_XY otherwise the background is cut
                    // off a bit at the bottom.
                    iv.setScaleType(ImageView.ScaleType.FIT_XY);
                    return;
                }

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
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                if (!animate) {
                    iv.setImageDrawable(layers[1]);
                } else {
                    iv.setImageDrawable(d);
                }
            }
        });
    }

    /**
     * Disable the ViewPager while a theme change is occuring
     */
    public void themeChangeStart() {
        lockPager();
        mThemeChanging = true;
        ThemeFragment f = getCurrentFragment();
        if (f != null) {
            mAppliedBaseTheme = f.getThemePackageName();
            PreferenceUtils.setAppliedBaseTheme(this, mAppliedBaseTheme);
        }
    }

    /**
     * Re-enable the ViewPager and update the "My theme" fragment if available
     */
    public void themeChangeEnd(boolean isSuccess) {
        mThemeChanging = false;
        ThemeFragment f = getCurrentFragment();
        if (f != null) {
            // We currently need to recreate the adapter in order to load
            // the changes otherwise the adapter returns the original fragments
            // TODO: We'll need a better way to handle this to provide a good UX
            if (!(f instanceof MyThemeFragment)) {
                mAdapter = new ThemesAdapter();
                mPager.setAdapter(mAdapter);
            }
            if (!isSuccess) {
                mAppliedBaseTheme = null;
            }
            startLoader(LOADER_ID_APPLIED);
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
        showComponentSelector(component, null, DEFAULT_COMPONENT_ID, v);
    }

    public void showComponentSelector(String component, String selectedPkgName,
            long selectedCmpntId, View v) {
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
            mSelector.show(component, selectedPkgName, selectedCmpntId, itemsPerPage, height);

            // determine if we need to shift the cards up
            int[] coordinates = new int[2];
            v.getLocationOnScreen(coordinates);
            final int top = coordinates[1];
            final int bottom = coordinates[1] + v.getHeight();
            final int statusBarHeight = res.getDimensionPixelSize(R.dimen.status_bar_height);
            int selectorTop = getWindowManager().getDefaultDisplay().getHeight() - height;
            if (bottom > selectorTop) {
                slideContentIntoView(bottom - selectorTop, height);
            } else if (top < statusBarHeight) {
                slideContentIntoView(top - statusBarHeight, height);
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

    public void collapse(final boolean applyTheme) {
        mExpanded = false;
        final ThemeFragment f = getCurrentFragment();
        if (f != null) {
            f.fadeOutCards(new Runnable() {
                public void run() {
                    mContainer.collapse();
                    f.collapse(applyTheme);
                }
            });
        }
        setAnimatingStateAndScheduleFinish();
    }

    public void pickExternalWallpaper() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(TYPE_IMAGE);
        startActivityForResult(intent, REQUEST_PICK_WALLPAPER_IMAGE);
        mIsPickingImage = true;
    }

    public void pickExternalLockscreen() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(TYPE_IMAGE);
        startActivityForResult(intent, REQUEST_PICK_LOCKSCREEN_IMAGE);
        mIsPickingImage = true;
    }

    public void uninstallTheme(String pkgName) {
        PackageManager pm = getPackageManager();
        pm.deletePackage(pkgName, new PackageDeleteObserver(), PackageManager.DELETE_ALL_USERS);
        sendThemeRemovedBroadcast(pkgName);
    }

    private void slideContentIntoView(int yDelta, int selectorHeight) {
        ThemeFragment f = getCurrentFragment();
        if (f != null) {
            final int offset = getResources().getDimensionPixelSize(R.dimen.content_offset_padding);
            if (yDelta > 0) {
                yDelta += offset;
            } else {
                yDelta -= offset;
            }
            f.slideContentIntoView(yDelta, selectorHeight);
            mContainerYOffset = yDelta;
        }
    }

    private void slideContentBack(final int yDelta) {
        ThemeFragment f = getCurrentFragment();
        if (f != null) {
            f.slideContentBack(yDelta);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setCustomBackground(mCustomBackground, mAnimateContentIn);
        // clear out any notifications that are being displayed.
        NotificationHelper.cancelNotifications(this);

        mThemeChanging = false;

        if (!mIsPickingImage) {
            startLoader(LOADER_ID_APPLIED);
        } else {
            mIsPickingImage = false;
        }

        IntentFilter filter = new IntentFilter(Intent.ACTION_WALLPAPER_CHANGED);
        registerReceiver(mWallpaperChangeReceiver, filter);
    }

    @Override
    public void onBackPressed() {
        final ThemeFragment f = getCurrentFragment();
        if (mSelector.isEnabled()) {
            mSelector.hide();
            if (mContainerYOffset != 0) {
                slideContentBack(-mContainerYOffset);
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
            collapse(false);
        } else {
            if (f != null && f.isShowingConfirmCancelOverlay()) {
                f.hideConfirmCancelOverlay();
            } else if (f != null && f.isShowingCustomizeResetLayout()) {
                f.hideCustomizeResetLayout();
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mWallpaperChangeReceiver);
        ThemeFragment f = getCurrentFragment();
        if (f != null) {
            mSelectedTheme = f.getThemePackageName();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mTypefaceHelperCache.getTypefaceCount() <= 0) {
            new TypefacePreloadTask().execute();
        }
        sendChooserOpenedBroadcast();
        mAnimateContentInDelay = ANIMATE_CONTENT_DELAY;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == REQUEST_PICK_WALLPAPER_IMAGE) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                ThemeFragment f = getCurrentFragment();
                if (f != null) {
                    f.setWallpaperImageUri(uri);
                }
            }
        } else if (resultCode == RESULT_OK && requestCode == REQUEST_PICK_LOCKSCREEN_IMAGE) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                ThemeFragment f = getCurrentFragment();
                if (f != null) {
                    f.setLockscreenImageUri(uri);
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void sendChooserOpenedBroadcast() {
        sendBroadcast(new Intent(ACTION_CHOOSER_OPENED), CYNGN_THEMES_PERMISSION);
    }

    private void sendThemeRemovedBroadcast(String pkgName) {
        Intent intent = new Intent(ACTION_THEME_REMOVED);
        intent.putExtra(EXTRA_PACKAGE, pkgName);
        sendBroadcast(intent, CYNGN_THEMES_PERMISSION);
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
        mBottomActionsLayout.setAlpha(0f);
        mBottomActionsLayout.animate().alpha(1f).setStartDelay(mAnimateContentInDelay)
                .setDuration(ANIMATE_CONTENT_IN_ALPHA_DURATION);
        mAnimateContentIn = false;
    }

    private View.OnClickListener mPagerClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ThemeFragment f = getCurrentFragment();
            if (f != null && !mThemeChanging) {
                f.performClick(mPager.isClickedOnContent());
            }
        }
    };

    private BroadcastReceiver mWallpaperChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mCustomBackground != null) setCustomBackground(mCustomBackground, false);
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
        ThemeFragment f;
        try {
            f = (mAdapter == null || mPager == null || mAdapter.getCount() <= 0) ? null :
                    (ThemeFragment) mAdapter.instantiateItem(mPager, mPager.getCurrentItem());
        } catch (Exception e) {
            f = null;
            Log.e(TAG, "Unable to get current fragment", e);
        }
        return f;
    }

    private void populateCurrentTheme(Cursor c) {
        c.moveToPosition(-1);
        //Default to first wallpaper
        mCurrentWallpaperCmpntId.value = DEFAULT_COMPONENT_ID;
        while(c.moveToNext()) {
            int mixkeyIdx = c.getColumnIndex(ThemesContract.MixnMatchColumns.COL_KEY);
            int pkgIdx = c.getColumnIndex(ThemesContract.MixnMatchColumns.COL_VALUE);
            int cmpntIdIdx = c.getColumnIndex(ThemesContract.MixnMatchColumns.COL_COMPONENT_ID);
            String mixkey = c.getString(mixkeyIdx);
            String component = ThemesContract.MixnMatchColumns.mixNMatchKeyToComponent(mixkey);
            String pkg = c.getString(pkgIdx);
            mCurrentTheme.put(component, pkg);
            if (cmpntIdIdx >= 0 && TextUtils.equals(component, ThemesColumns.MODIFIES_LAUNCHER)) {
                mCurrentWallpaperCmpntId.value = c.getLong(cmpntIdIdx);
            }
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
                Log.e(TAG, "Unable to launch Theme Store", e);
            }
        }
    };

    private <T> void startLoader(int loaderId) {
        final LoaderManager manager = getSupportLoaderManager();
        final Loader<T> loader = manager.getLoader(loaderId);
        if (loader != null) {
            manager.restartLoader(loaderId, null, this);
        } else {
            manager.initLoader(loaderId, null, this);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (mThemeChanging) return;

        if (mExpanded && !mActivityResuming) {
            mRestartLoaderOnCollapse = true;
            return;
        }

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

                    if (mThemeToApply != null) {
                        ThemeFragment f = getCurrentFragment();
                        f.applyThemeWhenPopulated(mThemeToApply, mComponentsToApply);
                        mThemeToApply = null;
                    }
                }
                if (mAnimateContentIn) animateContentIn();
                mActivityResuming = true;
                break;
            case LOADER_ID_APPLIED:
                startLoader(LOADER_ID_INSTALLED_THEMES);
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
                selection = ThemesColumns.PRESENT_AS_THEME + "=? AND " +
                        ThemesColumns.INSTALL_STATE + "=?";
                selectionArgs = new String[] { "1", "" + ThemesColumns.InstallState.INSTALLED};
                // sort in ascending order but make sure the "default" theme is always first
                sortOrder = "(" + ThemesColumns.IS_DEFAULT_THEME + "=1) DESC, "
                        + "(" + ThemesColumns.PKG_NAME + "='" + mAppliedBaseTheme + "') DESC, "
                        + ThemesColumns.INSTALL_TIME + " DESC";
                contentUri = ThemesColumns.CONTENT_URI;
                projection = new String[] {ThemesColumns.PKG_NAME, ThemesColumns.TITLE,
                        ThemesColumns.AUTHOR};
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

    public class ThemesAdapter extends NewFragmentStatePagerAdapter {
        private ArrayList<String> mInstalledThemes;
        private String mAppliedThemeTitle;
        private String mAppliedThemeAuthor;
        private HashMap<String, Integer> mRepositionedFragments;

        public ThemesAdapter() {
            super(getSupportFragmentManager());
            mRepositionedFragments = new HashMap<String, Integer>();
        }

        @Override
        public Fragment getItem(int position) {
            ThemeFragment f = null;
            MutableLong wallpaperCmpntId;
            if (mInstalledThemes != null) {
                final String pkgName = mInstalledThemes.get(position);
                if (pkgName.equals(mAppliedBaseTheme)) {
                    f = MyThemeFragment.newInstance(mAppliedBaseTheme, mAppliedThemeTitle,
                            mAppliedThemeAuthor, mAnimateContentIn);
                    wallpaperCmpntId = mCurrentWallpaperCmpntId;
                } else {
                    f = ThemeFragment.newInstance(pkgName, mAnimateContentIn);
                    wallpaperCmpntId = new MutableLong(DEFAULT_COMPONENT_ID);
                }
                f.setCurrentTheme(mCurrentTheme, wallpaperCmpntId);
            }
            return f;
        }

        @Override
        public long getItemId(int position) {
            if (mInstalledThemes != null) {
                final String pkgName = mInstalledThemes.get(position);
                return pkgName.hashCode();
            }
            return 0;
        }

        @Override
        public int getItemPosition(Object object) {
            final ThemeFragment f = (ThemeFragment) object;
            final String pkgName = f != null ? f.getThemePackageName() : null;
            if (pkgName != null && mRepositionedFragments.containsKey(pkgName)) {
                final int position = mRepositionedFragments.get(pkgName);
                mRepositionedFragments.remove(pkgName);
                return position;
            }
            return super.getItemPosition(object);
        }

        /**
         * The first card should be the user's currently applied theme components so we
         * will always return at least 1 or mCursor.getCount() + 1
         * @return
         */
        public int getCount() {
            return mInstalledThemes == null ? 0 : mInstalledThemes.size();
        }

        public void swapCursor(Cursor c) {
            if (c != null && c.getCount() != 0) {
                ArrayList<String> previousOrder = mInstalledThemes == null ? null
                        : new ArrayList<String>(mInstalledThemes);
                mInstalledThemes = new ArrayList<String>(c.getCount());
                mRepositionedFragments.clear();
                c.moveToPosition(-1);
                while (c.moveToNext()) {
                    final int pkgIdx = c.getColumnIndex(ThemesColumns.PKG_NAME);
                    final String pkgName = c.getString(pkgIdx);
                    if (pkgName.equals(mAppliedBaseTheme)) {
                        final int titleIdx = c.getColumnIndex(ThemesColumns.TITLE);
                        final int authorIdx = c.getColumnIndex(ThemesColumns.AUTHOR);
                        mAppliedThemeTitle = c.getString(titleIdx);
                        mAppliedThemeAuthor = c.getString(authorIdx);
                    }
                    mInstalledThemes.add(pkgName);

                    // track any themes that may have changed position
                    if (previousOrder != null && previousOrder.contains(pkgName)) {
                        int index = previousOrder.indexOf(pkgName);
                        if (index != c.getPosition()) {
                            mRepositionedFragments.put(pkgName, c.getPosition());
                        }
                    } else {
                        mRepositionedFragments.put(pkgName, c.getPosition());
                    }
                }
                // check if any themes are no longer in the new list
                if (previousOrder != null) {
                    for (String pkgName : previousOrder) {
                        if (!mInstalledThemes.contains(pkgName)) {
                            mRepositionedFragments.put(pkgName, POSITION_NONE);
                        }
                    }
                }
            } else {
                mInstalledThemes = null;
            }
        }

        public void removeTheme(String pkgName) {
            if (mInstalledThemes == null) return;

            if (mInstalledThemes.contains(pkgName)) {
                final int count = mInstalledThemes.size();
                final int index = mInstalledThemes.indexOf(pkgName);
                // reposition all the fragments after the one being removed
                for (int i = index + 1; i < count; i++) {
                    mRepositionedFragments.put(mInstalledThemes.get(i), i - 1);
                }
                // Now remove this theme and add it to mRepositionedFragments with POSITION_NONE
                mInstalledThemes.remove(pkgName);
                mRepositionedFragments.put(pkgName, POSITION_NONE);
                // now we can call notifyDataSetChanged()
                notifyDataSetChanged();
            }
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

    /**
     * Internal delete callback from the system
     */
    class PackageDeleteObserver extends IPackageDeleteObserver.Stub {
        public void packageDeleted(final String packageName, int returnCode) throws RemoteException {
            if (returnCode == PackageManager.DELETE_SUCCEEDED) {
                Log.d(TAG, "Delete succeeded");
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.removeTheme(packageName);
                    }
                });
            } else {
                Log.e(TAG, "Delete failed with returnCode " + returnCode);
            }
        }
    }
}
