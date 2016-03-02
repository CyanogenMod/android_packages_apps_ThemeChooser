/*
 * Copyright (C) 2016 Cyanogen, Inc.
 * Copyright (C) 2016 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cyanogenmod.theme.chooser;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
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
import android.provider.Settings;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.v4.app.*;
import android.support.v4.content.Loader;
import android.support.v4.view.ThemeViewPager;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.MutableLong;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.Toast;

import org.cyanogenmod.theme.perapptheming.PerAppThemingWindow;
import org.cyanogenmod.theme.util.CursorLoaderHelper;
import org.cyanogenmod.theme.util.NotificationHelper;
import org.cyanogenmod.theme.util.PreferenceUtils;
import org.cyanogenmod.theme.util.TypefaceHelperCache;
import org.cyanogenmod.theme.util.Utils;

import cyanogenmod.platform.Manifest;
import cyanogenmod.providers.ThemesContract;
import cyanogenmod.providers.ThemesContract.ThemesColumns;
import cyanogenmod.themes.ThemeManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static cyanogenmod.providers.ThemesContract.ThemesColumns.MODIFIES_ALARMS;
import static cyanogenmod.providers.ThemesContract.ThemesColumns.MODIFIES_BOOT_ANIM;
import static cyanogenmod.providers.ThemesContract.ThemesColumns.MODIFIES_NOTIFICATIONS;
import static cyanogenmod.providers.ThemesContract.ThemesColumns.MODIFIES_RINGTONES;
import static org.cyanogenmod.theme.chooser.ComponentSelector.DEFAULT_COMPONENT_ID;
import static org.cyanogenmod.theme.util.CursorLoaderHelper.LOADER_ID_INSTALLED_THEMES;
import static org.cyanogenmod.theme.util.CursorLoaderHelper.LOADER_ID_APPLIED;
import static org.cyanogenmod.theme.util.CursorLoaderHelper.LOADER_ID_THEME_MIXES;

public class ChooserActivity extends FragmentActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String THEME_STORE_PACKAGE = "com.cyngn.theme.tore";
    private static final String TAG = ChooserActivity.class.getSimpleName();

    public static final String DEFAULT = ThemeConfig.SYSTEM_DEFAULT;
    public static final String EXTRA_PKGNAME = "pkgName";
    public static final String EXTRA_COMPONENTS = "components";

    private static final int OFFSCREEN_PAGE_LIMIT = 3;

    private static final String THEME_STORE_ACTIVITY = THEME_STORE_PACKAGE + ".ui.StoreActivity";
    private static final String ACTION_APPLY_THEME = "android.intent.action.APPLY_THEME";

    private static final String TYPE_IMAGE = "image/*";

    private static final String ACTION_PICK_LOCK_SCREEN_WALLPAPER =
            "com.cyngn.intent.action.PICK_LOCK_SCREEN_WALLPAPER";

    /**
     * Request code for picking an external wallpaper
     */
    public static final int REQUEST_PICK_WALLPAPER_IMAGE = 2;
    /**
     * Request code for picking an external lockscreen wallpaper
     */
    public static final int REQUEST_PICK_LOCKSCREEN_IMAGE = 3;

    /**
     * Request code for enabling system alert window permission
     */
    private static final int REQUEST_SYSTEM_WINDOW_PERMISSION = 4;

    private static final long ANIMATE_CONTENT_IN_SCALE_DURATION = 500;
    private static final long ANIMATE_CONTENT_IN_ALPHA_DURATION = 750;
    private static final long ANIMATE_CONTENT_IN_BLUR_DURATION = 250;
    private static final long ANIMATE_CONTENT_DELAY = 250;
    private static final long ANIMATE_SHOP_THEMES_HIDE_DURATION = 250;
    private static final long ANIMATE_SHOP_THEMES_SHOW_DURATION = 500;
    private static final long FINISH_ANIMATION_DELAY = ThemeFragment.ANIMATE_DURATION
            + ThemeFragment.ANIMATE_START_DELAY + 250;

    private static final long ANIMATE_CARDS_IN_DURATION = 250;
    private static final long ANIMATE_SAVE_APPLY_LAYOUT_DURATION = 300;
    private static final float ANIMATE_SAVE_APPLY_DECELERATE_INTERPOLATOR_FACTOR = 3;
    private static final long ONCLICK_SAVE_APPLY_FINISH_ANIMATION_DELAY = 400;

    private static int prev_coun = 0;
    private boolean is_to_change = false;
    private static ThemesAdapter.ThemeItemInfo applied_info = null;

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

    private int mAppliedThemeIndex = -1;

    ImageView mCustomBackground;

    // Current system theme configuration as component -> pkgName
    private Map<String, String> mCurrentTheme = new HashMap<String, String>();
    private MutableLong mCurrentWallpaperCmpntId = new MutableLong(DEFAULT_COMPONENT_ID);

    private boolean mIsPickingImage = false;
    private boolean mRestartLoaderOnCollapse = false;
    private boolean mActivityResuming = false;
    private boolean mShowLockScreenWallpaper = false;

    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"on create called");
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
        mPager.setClipChildren(false);

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
                        final ThemeFragment f = getCurrentFragment();
                        if (mSelector.isEnabled()) {
                            mSelector.hide();
                            if (mContainerYOffset != 0) {
                                slideContentBack(-mContainerYOffset);
                                mContainerYOffset = 0;
                            }
                            if (f != null) f.fadeInCards();
                            if (mShowLockScreenWallpaper) {
                                mShowLockScreenWallpaper = false;
                                mSelector.resetComponentType();
                            }
                        }

                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                collapse(true);
                            }
                        }, ONCLICK_SAVE_APPLY_FINISH_ANIMATION_DELAY);
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
                if (Settings.canDrawOverlays(ChooserActivity.this)) {
                    launchAppThemer();
                } else {
                    requestSystemWindowPermission();
                }
            }
        });

        if (PreferenceUtils.getShowPerAppThemeNewTag(this)) {
            View tag = mBottomActionsLayout.findViewById(R.id.new_tag);
            if (tag != null) {
                tag.setVisibility(View.VISIBLE);
            }
        }
    }

    public void showSaveApplyButton() {
        if (mSaveApplyLayout != null && mSaveApplyLayout.getVisibility() != View.VISIBLE) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    int navBarHeight = 0;
                    if (Utils.hasNavigationBar(ChooserActivity.this.getApplicationContext())) {
                        navBarHeight = ChooserActivity.this.getResources()
                                .getDimensionPixelSize(R.dimen.navigation_bar_height);
                    }
                    mSaveApplyLayout.setTranslationY(mSaveApplyLayout.getMeasuredHeight());
                    mSaveApplyLayout.setVisibility(View.VISIBLE);
                    mSaveApplyLayout.animate()
                            .setDuration(ANIMATE_SAVE_APPLY_LAYOUT_DURATION)
                            .setInterpolator(
                                    new DecelerateInterpolator(
                                            ANIMATE_SAVE_APPLY_DECELERATE_INTERPOLATOR_FACTOR))
                            .translationY(-mSelector.getMeasuredHeight()
                                    + navBarHeight);
                }
            });
        }
    }

    public void hideSaveApplyButton() {
        if (mSaveApplyLayout.getVisibility() != View.GONE) {
            Animation anim = AnimationUtils.loadAnimation(this,
                    R.anim.component_selection_animate_out);
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

    private void lauchGetThemes() {
        String playStoreUrl = getString(R.string.play_store_url);
        String wikiUrl = getString(R.string.wiki_url);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(playStoreUrl));

        // Try to launch play store
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            // If no play store, try to open wiki url
            intent.setData(Uri.parse(wikiUrl));
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, R.string.get_more_app_not_available,
                        Toast.LENGTH_LONG).show();
            }
        }
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
                                    .checkPermission(Manifest.permission.WRITE_THEMES,
                                            getCallingPackage())) {
                mThemeToApply = intent.getStringExtra(EXTRA_PKGNAME);
            }
        } else if (ACTION_PICK_LOCK_SCREEN_WALLPAPER.equals(action)) {
            mShowLockScreenWallpaper = true;
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
                //Before the FragmentManager used to retain some previous fragments
                //which was causing some wierd behaviour like different tags being
                //applied to different theme fragments. This code removes any
                //fragments retained by the FragmentManager before creating the adapter.
                FragmentManager fm = getSupportFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                if(fm.getFragments()!=null) {
                    for (Fragment frag : fm.getFragments()) {
                        if(ft.show(frag)!=null) {
                            ft.remove(frag);
                        }
                    }
                    ft.commit();
                }
                mAdapter = new ThemesAdapter();
                mPager.setAdapter(mAdapter);
            }
            else {
                FragmentManager fm = getSupportFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                if(fm.getFragments()!=null) {
                    for (Fragment frag : fm.getFragments()) {
                        if(ft.show(frag)!=null) {
                            ft.remove(frag);
                        }
                    }
                    ft.commit();
                }
//                mAdapter = new ThemesAdapter();
//                mPager.setAdapter(mAdapter);
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
            if (mSaveApplyLayout.getVisibility() == View.VISIBLE) {
                if (mSaveApplyLayout.getTranslationY() + height != 0) {
                    mSaveApplyLayout.animate()
                            .translationY(-height)
                            .setInterpolator(
                                    new DecelerateInterpolator(
                                            ANIMATE_SAVE_APPLY_DECELERATE_INTERPOLATOR_FACTOR))
                            .setDuration(ANIMATE_SAVE_APPLY_LAYOUT_DURATION);
                }
            }
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
    }

    public void deleteThemeMix(int id) {
        mAdapter.removeThemeMix(id);
        ContentResolver resolver = getContentResolver();
        String selection = ThemesContract.ThemeMixColumns._ID + "=?";
        String[] selectionArgs = {Integer.toString(id)};
        resolver.delete(ThemesContract.ThemeMixColumns.CONTENT_URI, selection, selectionArgs);
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

        ThemeManager tm = ThemeManager.getInstance(this);
        mThemeChanging = tm.isThemeApplying();

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
            if (mShowLockScreenWallpaper) {
                mShowLockScreenWallpaper = false;
                mSelector.resetComponentType();
            }
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
                    showSaveApplyButton();
                }
            }
        } else if (resultCode == RESULT_OK && requestCode == REQUEST_PICK_LOCKSCREEN_IMAGE) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                ThemeFragment f = getCurrentFragment();
                if (f != null) {
                    f.setLockscreenImageUri(uri);
                    showSaveApplyButton();
                }
            }
        } else if (requestCode == REQUEST_SYSTEM_WINDOW_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {
                launchAppThemer();
            }
        }
    }

    private void animateContentIn() {
        Drawable d = mCustomBackground.getDrawable();
        if (d instanceof TransitionDrawable) {
            ((TransitionDrawable) d).startTransition((int) ANIMATE_CONTENT_IN_BLUR_DURATION);
        }

        if (!mShowLockScreenWallpaper) {
            AnimatorSet set = new AnimatorSet();
            set.play(ObjectAnimator.ofFloat(mContainer, "alpha", 0f, 1f)
                    .setDuration(ANIMATE_CONTENT_IN_ALPHA_DURATION))
                    .with(ObjectAnimator.ofFloat(mContainer, "scaleX", 2f, 1f)
                    .setDuration(ANIMATE_CONTENT_IN_SCALE_DURATION))
                    .with(ObjectAnimator.ofFloat(mContainer, "scaleY", 2f, 1f)
                    .setDuration(ANIMATE_CONTENT_IN_SCALE_DURATION));
            set.setStartDelay(mAnimateContentInDelay);
            set.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mAnimateContentIn = false;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
            set.start();
            mBottomActionsLayout.setAlpha(0f);
            mBottomActionsLayout.animate().alpha(1f).setStartDelay(mAnimateContentInDelay)
                    .setDuration(ANIMATE_CONTENT_IN_ALPHA_DURATION);
        } else {
            mContainer.setAlpha(0f);
            mContainer.setVisibility(View.GONE);
        }
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

    private ComponentSelector.OnOpenCloseListener mOpenCloseListener =
            new ComponentSelector.OnOpenCloseListener() {
        @Override
        public void onSelectorOpened() {
        }

        @Override
        public void onSelectorClosed() {
        }

        @Override
        public void onSelectorClosing() {
            ThemeFragment f = getCurrentFragment();
            if (f != null && f.componentsChanged()
                    && mSaveApplyLayout.getVisibility() == View.VISIBLE) {
                mSaveApplyLayout.animate()
                        .translationY(0)
                        .setInterpolator(new DecelerateInterpolator())
                        .setDuration(ANIMATE_SAVE_APPLY_LAYOUT_DURATION);
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
        // clear out the previous map
        mCurrentTheme.clear();
        while(c.moveToNext()) {
            int mixkeyIdx = c.getColumnIndex(ThemesContract.MixnMatchColumns.COL_KEY);
            int pkgIdx = c.getColumnIndex(ThemesContract.MixnMatchColumns.COL_VALUE);
            int cmpntIdIdx = c.getColumnIndex(ThemesContract.MixnMatchColumns.COL_COMPONENT_ID);
            String mixkey = c.getString(mixkeyIdx);
            String component = ThemesContract.MixnMatchColumns.mixNMatchKeyToComponent(mixkey);
            String pkg = c.getString(pkgIdx);
            mCurrentTheme.put(component, pkg);
            if (TextUtils.equals(component, ThemesColumns.MODIFIES_LIVE_LOCK_SCREEN)) {
                mCurrentTheme.remove(ThemesColumns.MODIFIES_LOCKSCREEN);
            }
            if (TextUtils.equals(component, ThemesColumns.MODIFIES_LOCKSCREEN)) {
                mCurrentTheme.remove(ThemesColumns.MODIFIES_LIVE_LOCK_SCREEN);
            }
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
                lauchGetThemes();
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
                if (TextUtils.isEmpty(mSelectedTheme)) mSelectedTheme = mAppliedBaseTheme;
                while(data.moveToNext()) {
                    if (mSelectedTheme.equals(data.getString(
                            data.getColumnIndex(ThemesColumns.PKG_NAME)))) {
                        // we need to add one here since the first card is "My theme"
                        mAppliedThemeIndex = data.getPosition();
                        mPager.setCurrentItem(mAppliedThemeIndex);
                        mSelectedTheme = null;
                        break;
                    }
                }
                data.moveToFirst();
                mAdapter.setLoading(true);
                mAdapter.updateInstalledThemes(data);
                startLoader(LOADER_ID_THEME_MIXES);
                break;
            case LOADER_ID_THEME_MIXES:
                mAdapter.updateThemeMixes(data);
                mAdapter.setLoading(false);
                mAdapter.notifyDataSetChanged();
                if (mAppliedThemeIndex >= 0) {
                    mPager.setCurrentItem(mAppliedThemeIndex, false);

                    if (mThemeToApply != null) {
                        ThemeFragment f = getCurrentFragment();
                        f.applyThemeWhenPopulated(mThemeToApply, mComponentsToApply);
                        mThemeToApply = null;
                    }
                    mAppliedThemeIndex = -1;
                }
                //mAnimateContentIn = true;//hard coded is it correct?
                if (mAnimateContentIn) {
                    animateContentIn();
                    mAnimateContentIn = false;
                }
                mAdapter.notifyDataSetChanged();
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
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_INSTALLED_THEMES:
                mAppliedBaseTheme = PreferenceUtils.getAppliedBaseTheme(this);
                break;
            case LOADER_ID_APPLIED:
                //TODO: Mix n match query should only be done once
                break;
        }
        return CursorLoaderHelper.chooserActivityCursorLoader(this, id, mAppliedBaseTheme);
    }

    public Map<String, String> getSelectedComponentsMap() {
        return getCurrentFragment().getSelectedComponentsMap();
    }

    public class ThemesAdapter extends FragmentPagerAdapter {
        private ArrayList<ThemeItemInfo> mInstalledThemes;
        private ArrayList<ThemeItemInfo> mThemeMixes;
        private ArrayList<ThemeItemInfo> mOrderedThemes;
        private ArrayMap<Long, ThemeFragment> mFragments;
        private String mAppliedThemeTitle;
        private String mAppliedThemeAuthor;
        private boolean mLoading = true;

        public ThemesAdapter() {
            super(getSupportFragmentManager());
            mInstalledThemes = new ArrayList<>();
            mThemeMixes = new ArrayList<>();
            mOrderedThemes = new ArrayList<>();
            mFragments = new ArrayMap<>();
        }

        @Override
        public Fragment getItem(int position) {
            if(!is_to_change) {
                ThemeFragment f = null;
                MutableLong wallpaperCmpntId;
                if (mOrderedThemes != null) {
                    long id = getItemId(position);
                    if (mFragments.get(id) != null) {
                        f = mFragments.get(id);
                    } else {
                        final ThemeItemInfo themeItemInfo = mOrderedThemes.get(position);
                        if (!themeItemInfo.isThemeMix) {
                            if (themeItemInfo.packageName.equals(mAppliedBaseTheme)) {
                                f = MyThemeFragment.newInstance(mAppliedBaseTheme, mAppliedThemeTitle,
                                        mAppliedThemeAuthor, mAnimateContentIn,
                                        mShowLockScreenWallpaper);
                                wallpaperCmpntId = mCurrentWallpaperCmpntId;
                            } else {
                                f = ThemeFragment.newInstance(themeItemInfo.packageName,
                                        mAnimateContentIn);
                                wallpaperCmpntId = new MutableLong(DEFAULT_COMPONENT_ID);
                            }
                        } else {
                            if (themeItemInfo.packageName.equals(mAppliedBaseTheme)) {
                                f = MyThemeFragment.newInstance(mAppliedBaseTheme, mAppliedThemeTitle,
                                        "Theme Mix", mAnimateContentIn,
                                        mShowLockScreenWallpaper);
                                wallpaperCmpntId = mCurrentWallpaperCmpntId;
                            } else {
                                f = ThemeMixFragment.newInstance(themeItemInfo.themeName,
                                        themeItemInfo.packageName, themeItemInfo.id, true);
                                wallpaperCmpntId = new MutableLong(DEFAULT_COMPONENT_ID);
                            }
//                        f = ThemeMixFragment.newInstance(themeItemInfo.themeName,
//                                    themeItemInfo.packageName, themeItemInfo.id, true);
//                                wallpaperCmpntId = mCurrentWallpaperCmpntId;
                        }
                        f.setCurrentTheme(mCurrentTheme, wallpaperCmpntId);
                        mFragments.put(id, f);
                    }
                }
                return f;
            }
            else {
                ThemeFragment f = null;
                MutableLong wallpaperCmpntId;
                if (mOrderedThemes != null) {
                    long id = getItemId(position);
                    if (mFragments.get(id) != null) {
                        f = mFragments.get(id);
                    } else {
                        final ThemeItemInfo themeItemInfo = mOrderedThemes.get(position);
                        if (!themeItemInfo.isThemeMix) {
                                f = ThemeFragment.newInstance(themeItemInfo.packageName,
                                        mAnimateContentIn);
                                wallpaperCmpntId = new MutableLong(DEFAULT_COMPONENT_ID);
                            }
                        else {
                            if (themeItemInfo.packageName.equals(mAppliedBaseTheme) ||
                                    themeItemInfo.is_first_theme_applied) {
                                f = MyThemeFragment.newInstance(mAppliedBaseTheme, mAppliedThemeTitle,
                                        "Theme Mix", mAnimateContentIn,
                                        mShowLockScreenWallpaper);
                                wallpaperCmpntId = mCurrentWallpaperCmpntId;
                            } else {
                                f = ThemeMixFragment.newInstance(themeItemInfo.themeName,
                                        themeItemInfo.packageName, themeItemInfo.id, true);
                                wallpaperCmpntId = new MutableLong(DEFAULT_COMPONENT_ID);
                            }
//                        f = ThemeMixFragment.newInstance(themeItemInfo.themeName,
//                                    themeItemInfo.packageName, themeItemInfo.id, true);
//                                wallpaperCmpntId = mCurrentWallpaperCmpntId;
                        }
                        f.setCurrentTheme(mCurrentTheme, wallpaperCmpntId);
                        mFragments.put(id, f);
                    }
                }
                return f;
            }
        }

        @Override
        public int getItemPosition(Object object) {
            // Check if the fragment is still in our dataset and if so, return it's index
            ThemeFragment f = (ThemeFragment) object;
            for (int i = 0; i < getCount(); i++) {
                ThemeFragment item = (ThemeFragment) getItem(i);
                if (item.equals(f)) {
                    // item still exists in dataset so return position
                    return i;
                }
            }

            // if we arrive here then the item is no longer in the dataset so remove fragment
            for (Map.Entry<Long, ThemeFragment> entry : mFragments.entrySet()) {
                if (entry.getValue().equals(f)) {
                    mFragments.remove(entry.getKey());
                    break;
                }
            }

            return POSITION_NONE;
        }

        @Override
        public long getItemId(int position) {
            return mOrderedThemes.get(position).hashCode();
        }


        @Override
        public int getCount() {
            return (mLoading || mOrderedThemes == null) ? 0 : mOrderedThemes.size();
        }

        public void updateInstalledThemes(Cursor c) {
            mInstalledThemes.clear();
            //mAppliedThemeIndex = 1;

            if (c != null && c.getCount() != 0) {
                c.moveToPosition(-1);
                while (c.moveToNext()) {
                    final int pkgIdx = c.getColumnIndex(ThemesColumns.PKG_NAME);
                    final int titleIdx = c.getColumnIndex(ThemesColumns.TITLE);
                    final int authorIdx = c.getColumnIndex(ThemesColumns.AUTHOR);
                    final String pkgName = c.getString(pkgIdx);
                    final String title = c.getString(titleIdx);
                    final String author = c.getString(authorIdx);
                    mInstalledThemes.add(new ThemeItemInfo(pkgName, title, author, false));
                }
            }
            orderThemes();
            notifyDataSetChanged();
        }

        public void updateThemeMixes(Cursor c) {
            mThemeMixes.clear();
            //mAppliedThemeIndex = 1;
            boolean is_set = false;
            if (c.getCount() - prev_coun == 1) {
                while (c.moveToLast()) {
                    final int titleIdx = c.getColumnIndex(ThemesContract.ThemeMixColumns.TITLE);
                    final int idIdx = c.getColumnIndex(ThemesContract.ThemeMixColumns._ID);
                    final String title = c.getString(titleIdx);
                    mThemeMixes.add(new ThemeItemInfo(title, title, null, true, c.getInt(idIdx), true));
                    applied_info = new ThemeItemInfo(title, title, null, true, c.getInt(idIdx), true);
                    is_set = true;
                    break;

                }
            }

            if (c != null && c.getCount() != 0) {
                c.moveToPosition(-1);
                while (c.moveToNext()) {
                    final int titleIdx = c.getColumnIndex(ThemesContract.ThemeMixColumns.TITLE);
                    final int idIdx = c.getColumnIndex(ThemesContract.ThemeMixColumns._ID);
                    final String title = c.getString(titleIdx);
                    if(!applied_info.themeName.equals(title) || getCurrentFragment() instanceof ThemeMixFragment) {
                        mThemeMixes.add(new ThemeItemInfo(title, title, null, true, c.getInt(idIdx)));
                    }
                    else if(applied_info.themeName.equals(title) && !(getCurrentFragment() instanceof ThemeMixFragment) &&
                            !is_set) {
                        mThemeMixes.add(new ThemeItemInfo(title, title, null, true, c.getInt(idIdx)));
                    }
                    else if (applied_info.themeName.equals(title) && !is_set) {
                        mThemeMixes.add(applied_info);
                    }
                }
                orderThemes();
                notifyDataSetChanged();
                prev_coun = c.getCount();
            }
        }

        public void removeInstalledTheme(String pkgName) {
            if (pkgName == null) return;

            for (ThemeItemInfo info : mOrderedThemes) {
                if (pkgName.equals(info.packageName)) {
                    mOrderedThemes.remove(info);
                    mInstalledThemes.remove(info);
                    // now we can call notifyDataSetChanged()
                    notifyDataSetChanged();
                    break;
                }
            }
        }

        public void removeThemeMix(int id) {
            if (id < 0) return;

            for (ThemeItemInfo info : mOrderedThemes) {
                if (id == info.id) {
                    mOrderedThemes.remove(info);
                    mThemeMixes.remove(info);
                    // now we can call notifyDataSetChanged()
                    notifyDataSetChanged();
                    break;
                }
                if(info.equals(applied_info)) {
                    applied_info = null;
                }
            }
            prev_coun--;
        }

        public void setLoading(boolean loading) {
            mLoading = loading;
        }

        private void orderThemes() {
            is_to_change = false;
            for(int i=0;i<mThemeMixes.size();i++) {
                ThemeItemInfo theme_info = mThemeMixes.get(i);
                if(theme_info.is_first_theme_applied) {
                    is_to_change = true;
                }
            }
            if(!is_to_change) {
                mOrderedThemes.clear();
                //int appliedThemeIndex =-1;
                mAppliedThemeIndex = 1;
                int appliedThemeIndex = 0;
                //First add theme mixes
                int N = mThemeMixes.size();
                for (int i = 0; i < N; i++) {
                    ThemeItemInfo themeItemInfo = mThemeMixes.get(i);
                    if (themeItemInfo.packageName.equals(mAppliedBaseTheme)) {
                        mAppliedThemeTitle = themeItemInfo.themeName;
                        mAppliedThemeAuthor = themeItemInfo.authorName;
                        appliedThemeIndex = i;
                        mOrderedThemes.add(0, themeItemInfo);
                    } else {
                        mOrderedThemes.add(themeItemInfo);
                    }
                }
                // Then add installed themes
                N = mInstalledThemes.size();
                for (int i = 0; i < N; i++) {
                    ThemeItemInfo themeItemInfo = mInstalledThemes.get(i);
                    if (themeItemInfo.packageName.equals(mAppliedBaseTheme) &&
                            themeItemInfo.packageName.equals(ThemeConfig.SYSTEM_DEFAULT)) {
                        mAppliedThemeTitle = themeItemInfo.themeName;
                        mAppliedThemeAuthor = themeItemInfo.authorName;
                        appliedThemeIndex = i;
                        mAppliedThemeIndex = 0;
                        mOrderedThemes.add(0, themeItemInfo);
                    } else if (themeItemInfo.packageName.equals(ThemeConfig.SYSTEM_DEFAULT)) {
                        mOrderedThemes.add(0, themeItemInfo);
                    } else if (themeItemInfo.packageName.equals(mAppliedBaseTheme)) {
                        mAppliedThemeTitle = themeItemInfo.themeName;
                        mAppliedThemeAuthor = themeItemInfo.authorName;
                        mOrderedThemes.add(1, themeItemInfo);
                    } else {
                        mOrderedThemes.add(themeItemInfo);
                    }
                }
            }
            else {
                mOrderedThemes.clear();
                //int appliedThemeIndex =-1;
                mAppliedThemeIndex = 1;
                int appliedThemeIndex = 0;
                //First add theme mixes
                int N = mThemeMixes.size();
                for (int i = 0; i < N; i++) {
                    ThemeItemInfo themeItemInfo = mThemeMixes.get(i);
                    if (themeItemInfo.is_first_theme_applied) {
                        mAppliedThemeTitle = themeItemInfo.themeName;
                        mAppliedThemeAuthor = themeItemInfo.authorName;
                        appliedThemeIndex = i;
                        mOrderedThemes.add(0, themeItemInfo);
                    } else {
                        mOrderedThemes.add(themeItemInfo);
                    }
                }
                // Then add installed themes
                N = mInstalledThemes.size();
                for (int i = 0; i < N; i++) {
                    ThemeItemInfo themeItemInfo = mInstalledThemes.get(i);
                    if (themeItemInfo.packageName.equals(mAppliedBaseTheme) &&
                            themeItemInfo.packageName.equals(ThemeConfig.SYSTEM_DEFAULT)) {
                        mAppliedThemeTitle = themeItemInfo.themeName;
                        mAppliedThemeAuthor = themeItemInfo.authorName;
                        appliedThemeIndex = i;
                        mAppliedThemeIndex = 0;
                        mOrderedThemes.add(0, themeItemInfo);
                    } else if (themeItemInfo.packageName.equals(ThemeConfig.SYSTEM_DEFAULT)) {
                        mOrderedThemes.add(0, themeItemInfo);
                    }  else {
                        mOrderedThemes.add(themeItemInfo);
                    }
                }
            }
        }

        private class ThemeItemInfo {
            String packageName;
            String themeName;
            String authorName;
            boolean isThemeMix;
            boolean is_first_theme_applied;
            int id;

            public ThemeItemInfo(String packageName, String themeName, String authorName,
                                 boolean isThemeMix) {
                this(packageName, themeName, authorName, isThemeMix, -1);
            }

            public ThemeItemInfo(String packageName, String themeName, String authorName,
                                 boolean isThemeMix, int id) {
                this.packageName = packageName;
                this.themeName = themeName;
                this.authorName = authorName;
                this.isThemeMix = isThemeMix;
                this.id = id;
            }

            public ThemeItemInfo(String packageName, String themeName, String authorName,
                                 boolean isThemeMix, int id, boolean is_first_theme_applied) {
                this.packageName = packageName;
                this.themeName = themeName;
                this.authorName = authorName;
                this.isThemeMix = isThemeMix;
                this.id = id;
                this.is_first_theme_applied = is_first_theme_applied;
            }

            @Override
            public int hashCode() {
                int hash = 17;
                hash = 31 * hash + (packageName != null ? packageName.hashCode() : 0);
                hash = 31 * hash + (themeName != null ? themeName.hashCode() : 0);
                hash = 31 * hash + (authorName != null ? authorName.hashCode() : 0);
                hash = 31 * hash + (isThemeMix ? 1 : 0);
                hash = 31 * hash + (id > 0 ? id : 0);
                return hash;
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
        public void packageDeleted(final String packageName, int returnCode)
                throws RemoteException {
            if (returnCode == PackageManager.DELETE_SUCCEEDED) {
                Log.d(TAG, "Delete succeeded");
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.removeInstalledTheme(packageName);
                    }
                });
            } else {
                Log.e(TAG, "Delete failed with returnCode " + returnCode);
            }
        }
    }

    public void expandContentAndAnimateLockScreenCardIn() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                expand();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        AnimatorSet set = new AnimatorSet();
                        set.play(ObjectAnimator.ofFloat(mContainer, "alpha", 0f, 1f)
                                .setDuration(ANIMATE_CARDS_IN_DURATION));
                        set.setStartDelay(mAnimateContentInDelay);
                        set.start();
                        mContainer.setVisibility(View.VISIBLE);
                        getCurrentFragment().showLockScreenCard();
                    }
                }, ANIMATE_CARDS_IN_DURATION);
            }
        });
    }

    private void launchAppThemer() {
        PreferenceUtils.setShowPerAppThemeNewTag(ChooserActivity.this, false);
        Intent intent = new Intent(ChooserActivity.this, PerAppThemingWindow.class);
        startService(intent);
        finish();
    }

    private void requestSystemWindowPermission() {
        Intent intent = new Intent (Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, REQUEST_SYSTEM_WINDOW_PERMISSION);
    }
}
