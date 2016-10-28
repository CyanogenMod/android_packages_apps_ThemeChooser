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

package org.cyanogenmod.theme.chooser2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.IntEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.ThemeConfig;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.util.MutableLong;
import android.util.SparseArray;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import org.cyanogenmod.theme.chooser2.ComponentSelector.OnItemClickedListener;
import org.cyanogenmod.theme.util.AudioUtils;
import org.cyanogenmod.theme.util.BootAnimationHelper;
import org.cyanogenmod.theme.util.CursorLoaderHelper;
import org.cyanogenmod.theme.util.IconPreviewHelper;
import org.cyanogenmod.theme.util.PreferenceUtils;
import org.cyanogenmod.theme.util.ThemedTypefaceHelper;
import org.cyanogenmod.theme.util.TypefaceHelperCache;
import org.cyanogenmod.theme.util.Utils;
import org.cyanogenmod.theme.util.WallpaperUtils;
import org.cyanogenmod.theme.widget.BootAniImageView;
import org.cyanogenmod.theme.widget.ConfirmCancelOverlay;
import org.cyanogenmod.theme.widget.LockableScrollView;
import org.cyanogenmod.theme.widget.ThemeTagLayout;

import cyanogenmod.app.ThemeVersion;
import cyanogenmod.providers.CMSettings;
import cyanogenmod.providers.ThemesContract.PreviewColumns;
import cyanogenmod.providers.ThemesContract.ThemesColumns;
import cyanogenmod.themes.ThemeChangeRequest;
import cyanogenmod.themes.ThemeChangeRequest.RequestType;
import cyanogenmod.themes.ThemeManager;

import org.cyanogenmod.internal.util.CmLockPatternUtils;
import org.cyanogenmod.internal.util.ThemeUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import static cyanogenmod.providers.ThemesContract.ThemesColumns.MODIFIES_ALARMS;
import static cyanogenmod.providers.ThemesContract.ThemesColumns.MODIFIES_BOOT_ANIM;
import static cyanogenmod.providers.ThemesContract.ThemesColumns.MODIFIES_LAUNCHER;
import static cyanogenmod.providers.ThemesContract.ThemesColumns.MODIFIES_LIVE_LOCK_SCREEN;
import static cyanogenmod.providers.ThemesContract.ThemesColumns.MODIFIES_LOCKSCREEN;
import static cyanogenmod.providers.ThemesContract.ThemesColumns.MODIFIES_NOTIFICATIONS;
import static cyanogenmod.providers.ThemesContract.ThemesColumns.MODIFIES_OVERLAYS;
import static cyanogenmod.providers.ThemesContract.ThemesColumns.MODIFIES_RINGTONES;
import static cyanogenmod.providers.ThemesContract.ThemesColumns.MODIFIES_STATUS_BAR;
import static cyanogenmod.providers.ThemesContract.ThemesColumns.MODIFIES_NAVIGATION_BAR;
import static cyanogenmod.providers.ThemesContract.ThemesColumns.MODIFIES_ICONS;
import static cyanogenmod.providers.ThemesContract.ThemesColumns.MODIFIES_FONTS;

import static org.cyanogenmod.theme.chooser2.ComponentSelector.DEFAULT_COMPONENT_ID;

import static org.cyanogenmod.theme.util.CursorLoaderHelper.LOADER_ID_INVALID;
import static org.cyanogenmod.theme.util.CursorLoaderHelper.LOADER_ID_ALL;
import static org.cyanogenmod.theme.util.CursorLoaderHelper.LOADER_ID_STATUS_BAR;
import static org.cyanogenmod.theme.util.CursorLoaderHelper.LOADER_ID_FONT;
import static org.cyanogenmod.theme.util.CursorLoaderHelper.LOADER_ID_ICONS;
import static org.cyanogenmod.theme.util.CursorLoaderHelper.LOADER_ID_WALLPAPER;
import static org.cyanogenmod.theme.util.CursorLoaderHelper.LOADER_ID_NAVIGATION_BAR;
import static org.cyanogenmod.theme.util.CursorLoaderHelper.LOADER_ID_LOCKSCREEN;
import static org.cyanogenmod.theme.util.CursorLoaderHelper.LOADER_ID_STYLE;
import static org.cyanogenmod.theme.util.CursorLoaderHelper.LOADER_ID_BOOT_ANIMATION;
import static org.cyanogenmod.theme.util.CursorLoaderHelper.LOADER_ID_RINGTONE;
import static org.cyanogenmod.theme.util.CursorLoaderHelper.LOADER_ID_NOTIFICATION;
import static org.cyanogenmod.theme.util.CursorLoaderHelper.LOADER_ID_ALARM;
import static org.cyanogenmod.theme.util.CursorLoaderHelper.LOADER_ID_LIVE_LOCK_SCREEN;

import static cyanogenmod.providers.CMSettings.Secure.LIVE_LOCK_SCREEN_ENABLED;

import static org.cyanogenmod.internal.util.ThemeUtils.SYSTEM_TARGET_API;

public class ThemeFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        ThemeManager.ThemeChangeListener, ThemeManager.ThemeProcessingListener {
    private static final String TAG = ThemeFragment.class.getSimpleName();

    public static final int ANIMATE_START_DELAY = 200;
    public static final int ANIMATE_DURATION = 300;
    public static final int ANIMATE_INTERPOLATE_FACTOR = 3;
    public static final int ANIMATE_COMPONENT_CHANGE_DURATION = 200;
    public static final int ANIMATE_COMPONENT_ICON_DELAY = 50;
    public static final int ANIMATE_PROGRESS_IN_DURATION = 500;
    public static final int ANIMATE_TITLE_OUT_DURATION = 400;
    public static final int ANIMATE_PROGRESS_OUT_DURATION = 400;
    public static final int ANIMATE_TITLE_IN_DURATION = 500;
    public static final int ANIMATE_APPLY_LAYOUT_DURATION = 300;

    public static final String CURRENTLY_APPLIED_THEME = "currently_applied_theme";

    private static final ComponentName COMPONENT_DIALER =
            new ComponentName("com.android.dialer", "com.android.dialer.DialtactsActivity");
    private static final ComponentName COMPONENT_DIALERNEXT =
            new ComponentName("com.cyngn.dialer", "com.android.dialer.DialtactsActivity");
    private static final ComponentName COMPONENT_MESSAGING =
            new ComponentName("com.android.mms", "com.android.mms.ui.ConversationList");
    private static final ComponentName COMPONENT_CAMERANEXT =
            new ComponentName("com.cyngn.cameranext", "com.android.camera.CameraLauncher");
    private static final ComponentName COMPONENT_CAMERA =
            new ComponentName("com.android.camera2", "com.android.camera.CameraActivity");
    private static final ComponentName COMPONENT_BROWSER =
            new ComponentName("com.android.browser", "com.android.browser.BrowserActivity");
    private static final ComponentName COMPONENT_SETTINGS =
            new ComponentName("com.android.settings", "com.android.settings.Settings");
    private static final ComponentName COMPONENT_CALENDAR =
            new ComponentName("com.android.calendar", "com.android.calendar.AllInOneActivity");
    private static final ComponentName COMPONENT_GALERY =
            new ComponentName("com.android.gallery3d", "com.android.gallery3d.app.GalleryActivity");

    private static final String CAMERA_NEXT_PACKAGE = "com.cyngn.cameranext";
    private static final String DIALER_NEXT_PACKAGE = "com.cyngn.dialer";

    private static final int ADDITIONAL_CONTENT_SPACE_ID = 123456;
    private static final long SLIDE_CONTENT_ANIM_DURATION = 300L;
    private static final long LOCK_SCREEN_CARD_SCROLL_ANIMATION_DURATION = 400;
    private static final long SHOW_LOCK_SCREEN_CARD_DELAY = 500;

    private static final int DEFAULT_WIFI_MARGIN = 0;
    private static final int DEFAULT_CLOCK_COLOR = Color.WHITE;

    protected static final String WALLPAPER_NONE = "";
    protected static final String LOCKSCREEN_NONE = "";

    protected static final String ARG_PACKAGE_NAME = "pkgName";
    protected static final String ARG_COMPONENT_ID = "cmpntId";
    protected static final String ARG_SKIP_LOADING_ANIM = "skipLoadingAnim";
    protected static final String ARG_ANIMATE_TO_LOCK_SCREEN_CARD = "animateToLockScreenCard";

    private static final String LLS_PACKAGE_NAME = "com.cyngn.lockscreen.live";
    private static final String LLS_PROVIDER_NAME =
            "com.cyngn.lockscreen.live.LockScreenProviderService";

    private static final int PERMISSION_REQUEST = 100;

    protected static ComponentName[] sIconComponents;

    protected static TypefaceHelperCache sTypefaceHelperCache;

    /**
     * Maps the card's resource ID to a theme component
     */
    private final SparseArray<String> mCardIdsToComponentTypes = new SparseArray<String>();

    protected String mPkgName;
    protected Typeface mTypefaceNormal;
    protected int mBatteryStyle;

    protected LockableScrollView mScrollView;
    protected ViewGroup mScrollContent;
    protected ViewGroup mPreviewContent; // Contains icons, font, nav/status etc. Not wallpaper
    protected View mLoadingView;

    //Status Bar Views
    protected ComponentCardView mStatusBarCard;
    protected ImageView mBluetooth;
    protected ImageView mWifi;
    protected ImageView mSignal;
    protected ImageView mBattery;
    protected TextView mClock;

    // Other Misc Preview Views
    protected FrameLayout mShadowFrame;
    protected ImageView mWallpaper;
    protected ViewGroup mStatusBar;
    protected TextView mFontPreview;
    protected ComponentCardView mStyleCard;
    protected ComponentCardView mFontCard;
    protected ComponentCardView mIconCard;
    protected ComponentCardView mBootAnimationCard;
    protected BootAniImageView mBootAnimation;

    // Nav Bar Views
    protected ComponentCardView mNavBarCard;
    protected ViewGroup mNavBar;
    protected ImageView mBackButton;
    protected ImageView mHomeButton;
    protected ImageView mRecentButton;

    // Title Card Views
    protected ViewGroup mTitleCard;
    protected ViewGroup mTitleLayout;
    protected TextView mTitle;
    protected TextView mAuthor;
    protected ImageView mOverflow;
    protected ProgressBar mProgress;

    // Additional Card Views
    protected LinearLayout mAdditionalCards;
    protected WallpaperCardView mWallpaperCard;
    protected WallpaperCardView mLockScreenCard;

    // Style views
    protected ImageView mStylePreview;

    // Sound cards
    protected ComponentCardView mRingtoneCard;
    protected ImageView mRingtonePlayPause;
    protected ComponentCardView mNotificationCard;
    protected ImageView mNotificationPlayPause;
    protected ComponentCardView mAlarmCard;
    protected ImageView mAlarmPlayPause;
    protected Map<ImageView, MediaPlayer> mMediaPlayers;

    protected Handler mHandler;

    protected int mActiveCardId = -1;
    protected ComponentSelector mSelector;
    // Supported components for the theme this fragment represents
    protected Map<String, String> mSelectedComponentsMap = new HashMap<String, String>();
    protected Long mSelectedWallpaperComponentId;
    // Current system theme configuration as component -> pkgName
    protected Map<String, String> mCurrentTheme = new HashMap<String, String>();
    protected MutableLong mCurrentWallpaperComponentId = new MutableLong(DEFAULT_COMPONENT_ID);
    // Set of components available in the base theme
    protected HashSet<String> mBaseThemeSupportedComponents = new HashSet<String>();
    protected Cursor mCurrentCursor;
    protected int mCurrentLoaderId;
    protected boolean mThemeResetting;
    protected boolean mSkipLoadingAnim;

    // Accept/Cancel overlay
    protected ConfirmCancelOverlay mConfirmCancelOverlay;

    // Customize/Reset theme layout
    protected View mCustomizeResetLayout;
    protected View mResetButton;
    protected View mCustomizeButton;
    protected View mDismissButton;

    // Processing theme layout
    protected View mProcessingThemeLayout;

    protected ThemeTagLayout mThemeTagLayout;

    protected View mClickableView;
    protected String mBaseThemePkgName;

    protected Uri mExternalWallpaperUri;
    protected Uri mExternalLockscreenUri;

    protected boolean mExpanded;
    protected boolean mProcessingResources;
    protected boolean mApplyThemeOnPopulated;

    protected boolean mIsLegacyTheme;

    private Runnable mAfterPermissionGrantedRunnable;

    private static final int mThemeVersion = ThemeVersion.getVersion();

    protected boolean mShowLockScreenSelectorAfterContentLoaded;

    private boolean mListenerRegistered;

    protected enum CustomizeResetAction {
        Customize,
        Reset,
        Dismiss
    }

    static ThemeFragment newInstance(String pkgName, boolean skipLoadingAnim) {
        ThemeFragment f = new ThemeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PACKAGE_NAME, pkgName);
        args.putBoolean(ARG_SKIP_LOADING_ANIM, skipLoadingAnim);
        args.putLong(ARG_COMPONENT_ID, DEFAULT_COMPONENT_ID);
        f.setArguments(args);
        return f;
    }

    /**
     * When creating, retrieve this instance's number from its arguments.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context context = getActivity();
        mPkgName = getArguments().getString(ARG_PACKAGE_NAME);
        mSkipLoadingAnim = getArguments().getBoolean(ARG_SKIP_LOADING_ANIM);
        // TODO: Load from settings once available
        mBatteryStyle = 0;/*Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_STYLE, 0);*/

        getIconComponents(context);
        if (sTypefaceHelperCache == null) {
            sTypefaceHelperCache = TypefaceHelperCache.getInstance();
        }
        ThemedTypefaceHelper helper = sTypefaceHelperCache.getHelperForTheme(context, mPkgName);
        mTypefaceNormal = helper.getTypeface(Typeface.NORMAL);

        mHandler = new Handler();

        mCardIdsToComponentTypes.put(R.id.status_bar_container, MODIFIES_STATUS_BAR);
        mCardIdsToComponentTypes.put(R.id.font_preview_container, MODIFIES_FONTS);
        mCardIdsToComponentTypes.put(R.id.icon_container, MODIFIES_ICONS);
        mCardIdsToComponentTypes.put(R.id.navigation_bar_container, MODIFIES_NAVIGATION_BAR);
        mCardIdsToComponentTypes.put(R.id.wallpaper_card, MODIFIES_LAUNCHER);
        mCardIdsToComponentTypes.put(R.id.lockscreen_card, MODIFIES_LOCKSCREEN);
        mCardIdsToComponentTypes.put(R.id.style_card, MODIFIES_OVERLAYS);
        mCardIdsToComponentTypes.put(R.id.bootani_preview_container, MODIFIES_BOOT_ANIM);
        mCardIdsToComponentTypes.put(R.id.ringtone_preview_container, MODIFIES_RINGTONES);
        mCardIdsToComponentTypes.put(R.id.notification_preview_container, MODIFIES_NOTIFICATIONS);
        mCardIdsToComponentTypes.put(R.id.alarm_preview_container, MODIFIES_ALARMS);

        mMediaPlayers = new HashMap<ImageView, MediaPlayer>(3);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_pager_list, container, false);

        mScrollView = (LockableScrollView) v.findViewById(android.R.id.list);
        mScrollView.setScrollingEnabled(false);
        mScrollContent = (ViewGroup) mScrollView.getChildAt(0);
        mPreviewContent = (ViewGroup) v.findViewById(R.id.preview_container);
        mLoadingView = v.findViewById(R.id.loading_view);
        mThemeTagLayout = (ThemeTagLayout) v.findViewById(R.id.tag_layout);

        // Status Bar
        mStatusBarCard = (ComponentCardView) v.findViewById(R.id.status_bar_container);
        mStatusBar = (ViewGroup) v.findViewById(R.id.status_bar);
        mBluetooth = (ImageView) v.findViewById(R.id.bluetooth_icon);
        mWifi = (ImageView) v.findViewById(R.id.wifi_icon);
        mSignal = (ImageView) v.findViewById(R.id.signal_icon);
        mBattery = (ImageView) v.findViewById(R.id.battery);
        mClock = (TextView) v.findViewById(R.id.clock);

        // Wallpaper / Font / Icons / etc
        mWallpaper = (ImageView) v.findViewById(R.id.wallpaper);
        mFontCard = (ComponentCardView) v.findViewById(R.id.font_preview_container);
        mFontPreview = (TextView) v.findViewById(R.id.font_preview);
        mFontPreview.setTypeface(mTypefaceNormal);
        mIconCard = (ComponentCardView) v.findViewById(R.id.icon_container);
        mShadowFrame = (FrameLayout) v.findViewById(R.id.shadow_frame);
        mStyleCard = (ComponentCardView) v.findViewById(R.id.style_card);
        mStylePreview = (ImageView) v.findViewById(R.id.style_preview);
        mBootAnimationCard = (ComponentCardView) v.findViewById(R.id.bootani_preview_container);
        mBootAnimation =
                (BootAniImageView) mBootAnimationCard.findViewById(R.id.bootani_preview);
        mRingtoneCard = (ComponentCardView) v.findViewById(R.id.ringtone_preview_container);
        mRingtonePlayPause = (ImageView) mRingtoneCard.findViewById(R.id.play_pause);
        mNotificationCard = (ComponentCardView) v.findViewById(R.id.notification_preview_container);
        mNotificationPlayPause = (ImageView) mNotificationCard.findViewById(R.id.play_pause);
        mAlarmCard = (ComponentCardView) v.findViewById(R.id.alarm_preview_container);
        mAlarmPlayPause = (ImageView) mAlarmCard.findViewById(R.id.play_pause);

        // Nav Bar
        mNavBarCard = (ComponentCardView) v.findViewById(R.id.navigation_bar_container);
        mNavBar = (ViewGroup) v.findViewById(R.id.navigation_bar);
        mBackButton = (ImageView) v.findViewById(R.id.back_button);
        mHomeButton = (ImageView) v.findViewById(R.id.home_button);
        mRecentButton = (ImageView) v.findViewById(R.id.recent_button);

        // Title Card
        mTitleCard = (ViewGroup)v.findViewById(R.id.title_card);
        mTitleLayout = (ViewGroup) v.findViewById(R.id.title_layout);
        mTitle = (TextView) v.findViewById(R.id.title);
        mAuthor = (TextView) v.findViewById(R.id.author);
        mProgress = (ProgressBar) v.findViewById(R.id.apply_progress);
        mOverflow = (ImageView) v.findViewById(R.id.overflow);
        mOverflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isShowingConfirmCancelOverlay()) {
                    hideConfirmCancelOverlay();
                } else if (isShowingCustomizeResetLayout()) {
                    hideCustomizeResetLayout();
                }

                PopupMenu popupmenu = new PopupMenu(getActivity(), mTitleCard, Gravity.END);
                popupmenu.getMenuInflater().inflate(R.menu.overflow, popupmenu.getMenu());

                Menu menu = popupmenu.getMenu();
                if (CURRENTLY_APPLIED_THEME.equals(mPkgName) ||
                        mPkgName.equals(Utils.getDefaultThemePackageName(getActivity())) ||
                        mPkgName.equals(ThemeConfig.SYSTEM_DEFAULT)) {
                    menu.findItem(R.id.menu_delete).setVisible(false);
                    if(mThemeTagLayout.isCustomizedTagEnabled()) {
                        menu.findItem(R.id.menu_reset).setVisible(true);
                        menu.findItem(R.id.menu_reset).setEnabled(true);
                    }
                    else {
                        menu.findItem(R.id.menu_reset).setVisible(false);
                        menu.findItem(R.id.menu_reset).setEnabled(false);
                    }
                }
                if(!mThemeTagLayout.isAppliedTagEnabled()) {
                    menu.findItem(R.id.menu_delete).setEnabled(true);
                    menu.findItem(R.id.menu_reset).setVisible(false);
                }
                if(mThemeTagLayout.isAppliedTagEnabled()) {
                    if(mThemeTagLayout.isCustomizedTagEnabled()) {
                        menu.findItem(R.id.menu_reset).setVisible(true);
                        menu.findItem(R.id.menu_reset).setEnabled(true);
                    }
                    else {
                        menu.findItem(R.id.menu_reset).setVisible(true);
                        menu.findItem(R.id.menu_reset).setEnabled(false);
                    }

                }

                popupmenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        return onPopupMenuItemClick(item);
                    }
                });
                popupmenu.show();
            }
        });

        if (!Utils.hasNavigationBar(getActivity())) {
            adjustScrollViewPaddingTop();
            mNavBarCard.setVisibility(View.GONE);
        }

        // Additional cards which should hang out offscreen until expanded
        mAdditionalCards = (LinearLayout) v.findViewById(R.id.additional_cards);

        mWallpaperCard = (WallpaperCardView) v.findViewById(R.id.wallpaper_card);
        mLockScreenCard = (WallpaperCardView) v.findViewById(R.id.lockscreen_card);
        int translationY = getDistanceToMoveBelowScreen(mAdditionalCards);
        mAdditionalCards.setTranslationY(translationY);

        mConfirmCancelOverlay = (ConfirmCancelOverlay) v.findViewById(R.id.confirm_cancel_overlay);
        mClickableView = v.findViewById(R.id.clickable_view);

        mCustomizeResetLayout = v.findViewById(R.id.customize_reset_theme_layout);
        mDismissButton = mCustomizeResetLayout.findViewById(R.id.btn_dismiss);
        mDismissButton.setOnClickListener(mCustomizeResetClickListener);
        mResetButton = mCustomizeResetLayout.findViewById(R.id.btn_reset);
        mResetButton.setOnClickListener(mCustomizeResetClickListener);
        mCustomizeButton = mCustomizeResetLayout.findViewById(R.id.btn_customize);
        mCustomizeButton.setOnClickListener(mCustomizeResetClickListener);

        mProcessingThemeLayout = v.findViewById(R.id.processing_theme_layout);

        if (mPkgName.equals(Utils.getDefaultThemePackageName(getActivity()))) {
            mThemeTagLayout.setDefaultTagEnabled(true);
        }
        if (PreferenceUtils.hasThemeBeenUpdated(getActivity(), mPkgName)) {
            mThemeTagLayout.setUpdatedTagEnabled(true);
        }

        if (mSkipLoadingAnim) {
            mLoadingView.setVisibility(View.GONE);
            mTitleLayout.setAlpha(1f);
        }

        getLoaderManager().initLoader(LOADER_ID_ALL, null, this);

        setupCardClickListeners(v);

        return v;
    }

    @Override
    public void onPause() {
        super.onPause();
        stopMediaPlayers();
    }

    @Override
    public void onResume() {
        super.onResume();
        ThemeManager tm = getThemeManager();
        if (tm != null) {
            if (isThemeProcessing()) {
                if (!mListenerRegistered) {
                    tm.registerProcessingListener(this);
                    mListenerRegistered = true;
                }
                mProcessingThemeLayout.setVisibility(View.VISIBLE);
                mProcessingResources = true;
            } else {
                mProcessingResources = false;
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        freeMediaPlayers();
        ThemeManager tm = getThemeManager();
        if (tm != null) {
            tm.removeClient(this);
            if (mListenerRegistered) {
                tm.unregisterProcessingListener(this);
                mListenerRegistered = false;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST) {
            int N = permissions.length;
            for (int i = 0; i < N; i++) {
                if (READ_EXTERNAL_STORAGE.equals(permissions[i])) {
                    if (grantResults[i] == PERMISSION_GRANTED) {
                        // Run the runnable now that we have been granted permission
                        if (mAfterPermissionGrantedRunnable != null) {
                            mAfterPermissionGrantedRunnable.run();
                            mAfterPermissionGrantedRunnable = null;
                        }
                    } else {
                        // inform the user that they will be unable to pick an image because
                        // we were not granted permission to do so
                        Toast.makeText(getActivity(),
                                R.string.read_external_permission_denied_message,
                                Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }

    @Override
    public void onProgress(int progress) {
        mProgress.setProgress(progress);
    }

    private void setLiveLockScreenAsKeyguard(boolean setLLS) {
        ComponentName cn = null;
        if (setLLS) {
            try {
                final String[] permissions = Utils.getDangerousPermissionsNotGranted(getActivity(),
                        LLS_PACKAGE_NAME);
                if (permissions.length > 0) {
                    Intent reqIntent = Utils.buildPermissionGrantRequestIntent(getActivity(),
                            LLS_PACKAGE_NAME, permissions);
                    if (reqIntent != null) {
                        startActivity(reqIntent);
                    }
                }
                cn = new ComponentName(LLS_PACKAGE_NAME, LLS_PROVIDER_NAME);
            } catch (InvalidParameterException e) {
                Log.e(TAG, "Package Manager couldn't find package " + LLS_PACKAGE_NAME, e);
                return;
            }
        }

        CmLockPatternUtils lockPatternUtils = new CmLockPatternUtils(getActivity());
        try {
            lockPatternUtils.setThirdPartyKeyguard(cn);
        } catch (PackageManager.NameNotFoundException e) {
            // we should not be here!
        }
    }

    @Override
    public void onFinish(boolean isSuccess) {
        // We post a runnable to mHandler so the client is removed from the same thread
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                ThemeManager tm = getThemeManager();
                if (tm != null) tm.removeClient(ThemeFragment.this);
            }
        });
        if (isSuccess) {
            if (mExternalLockscreenUri != null) {
                // Handle setting an external wallpaper in a separate thread
                // Need to do this AFTER ThemeMgr is done processing our change request.
                // The external lock screen that we just applied would be removed when
                // the change request is setting/clearing the lock screen
                new Thread(mApplyExternalLockscreenRunnable).start();
            }
            Map<String, String> appliedComponents = getComponentsToApply();
            boolean modLLS = appliedComponents.containsKey(MODIFIES_LIVE_LOCK_SCREEN);
            if (modLLS) {
                String pkgName = appliedComponents.get(MODIFIES_LIVE_LOCK_SCREEN);
                if (pkgName.equals(LOCKSCREEN_NONE)) {
                    setLiveLockScreenAsKeyguard(false);
                } else {
                    setLiveLockScreenAsKeyguard(true);
                }
            }
            mProgress.setProgress(100);
            animateProgressOut();
        }
        getChooserActivity().themeChangeEnd(isSuccess);
    }

    @Override
    public void onFinishedProcessing(String pkgName) {
        if (pkgName.equals(mPkgName) || pkgName.equals(mBaseThemePkgName)) {
            ThemeManager tm = getThemeManager();
            if (tm != null && mListenerRegistered) {
                tm.unregisterProcessingListener(this);
                mListenerRegistered = false;
            }
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (mThemeTagLayout == null) return;

        if (!isVisibleToUser) {
            if (PreferenceUtils.hasThemeBeenUpdated(getActivity(), mPkgName)) {
                mThemeTagLayout.setUpdatedTagEnabled(true);
            }
        } else {
            if (PreferenceUtils.hasThemeBeenUpdated(getActivity(), mPkgName)) {
                PreferenceUtils.removeUpdatedTheme(getActivity(), mPkgName);
            }
        }
    }

    public void setWallpaperImageUri(Uri uri) {
        mExternalWallpaperUri = uri;
        final Point size = new Point(mWallpaper.getWidth(), mWallpaper.getHeight());
        final Drawable wp = getWallpaperDrawableFromUri(uri, size);
        mWallpaperCard.setWallpaper(wp);
        mWallpaper.setImageDrawable(wp);
        // remove the entry from mSelectedComponentsMap
        mSelectedComponentsMap.remove(ThemesColumns.MODIFIES_LAUNCHER);
    }

    public void setLockscreenImageUri(Uri uri) {
        mExternalLockscreenUri = uri;
        final Point size = new Point(mLockScreenCard.getWidth(), mLockScreenCard.getHeight());
        final Drawable wp = getWallpaperDrawableFromUri(uri, size);
        if (mLockScreenCard.isShowingEmptyView()) {
            mLockScreenCard.setEmptyViewEnabled(false);
        }
        mLockScreenCard.setWallpaper(wp);
        // remove the entry from mSelectedComponentsMap
        mSelectedComponentsMap.remove(ThemesColumns.MODIFIES_LIVE_LOCK_SCREEN);
        mSelectedComponentsMap.remove(ThemesColumns.MODIFIES_LOCKSCREEN);
    }

    protected Drawable getWallpaperDrawableFromUri(Uri uri, Point size) {
        final Context context = getActivity();
        final Resources res = context.getResources();
        Bitmap bmp = WallpaperUtils.createPreview(size, context, uri, null, res, 0, 0, false);
        if (bmp != null) {
            return new BitmapDrawable(res, bmp);
        }
        return null;
    }

    protected ChooserActivity getChooserActivity() {
        return (ChooserActivity) getActivity();
    }

    private void adjustScrollViewPaddingTop() {
        Resources res = getResources();
        int extraPadding =
                res.getDimensionPixelSize(R.dimen.navigation_bar_height) / 2;
        mScrollView.setPadding(mScrollView.getPaddingLeft(),
                mScrollView.getPaddingTop() + extraPadding, mScrollView.getPaddingRight(),
                mScrollView.getPaddingBottom());
    }

    protected boolean isThemeProcessing() {
        ThemeManager tm = getThemeManager();
        if (tm != null) {
            final String pkgName = mBaseThemePkgName != null ? mBaseThemePkgName : mPkgName;
            return tm.isThemeBeingProcessed(pkgName);
        }
        return false;
    }

    protected boolean onPopupMenuItemClick(MenuItem item) {
        int id = item.getItemId();
        switch(item.getItemId()) {
            case R.id.menu_delete:
                showDeleteThemeOverlay();
                break;
            case R.id.menu_customize:
                //showResetThemeOverlay();
                if (!isShowingConfirmCancelOverlay() && !isShowingCustomizeResetLayout()) {
                    getChooserActivity().expand();
                }
                break;
            case R.id.menu_reset:
                showResetThemeOverlay();
                break;
        }

        return true;
    }

    public void expand() {
        if (mCurrentLoaderId == LOADER_ID_ALL && mCurrentCursor != null) {
            loadAdditionalCards(mCurrentCursor);
            // we don't need this now that the additional cards are loaded, and
            // we don't want to re-load these cards if the we expand again.
            mCurrentCursor = null;
        }
        mClickableView.setVisibility(View.GONE);
        mScrollView.setScrollingEnabled(true);
        // Full width and height!
        ViewGroup content = (ViewGroup) mScrollView.getParent();
        content.setPadding(0, 0, 0, 0);
        ViewGroup.LayoutParams  layoutParams = mPreviewContent.getLayoutParams();
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        mPreviewContent.setLayoutParams(layoutParams);
        mScrollView.setPadding(0,0,0,0);

        // The parent of the wallpaper squishes the wp slightly because of padding from the 9 patch
        // When the parent expands, the wallpaper returns to regular size which creates an
        // undesireable effect.
        Rect padding = new Rect();
        NinePatchDrawable bg = (NinePatchDrawable) mShadowFrame.getBackground();
        bg.getPadding(padding);
        mIconCard.setPadding(padding.left, padding.top, padding.right, padding.bottom);
        mShadowFrame.setBackground(null);
        mShadowFrame.setPadding(0, 0, 0, 0);

        // Off screen cards will become visible and then be animated in
        mWallpaperCard.setVisibility(View.VISIBLE);

        // Expand the children
        int top = (int) getResources()
                .getDimension(R.dimen.expanded_card_margin_top);
        for (int i = 0; i < mPreviewContent.getChildCount(); i++) {
            ComponentCardView child = (ComponentCardView) mPreviewContent.getChildAt(i);

            LinearLayout.LayoutParams lparams =
                    (LinearLayout.LayoutParams) child.getLayoutParams();
            if (child == mStatusBarCard) {
                int statusBarHeight = getResources()
                        .getDimensionPixelSize(R.dimen.status_bar_height);
                lparams.setMargins(0, top + statusBarHeight, 0, 0);
            } else {
                lparams.setMargins(0, top, 0, 0);
            }

            child.setLayoutParams(lparams);
            child.expand(false);
        }

        // Expand the additional children.
        mAdditionalCards.setVisibility(View.VISIBLE);
        for (int i = 0; i < mAdditionalCards.getChildCount(); i++) {
            View v = mAdditionalCards.getChildAt(i);
            if (v instanceof ComponentCardView) {
                ComponentCardView card = (ComponentCardView) v;
                card.setVisibility(View.VISIBLE);
                card.expand(true);
            }
        }

        // Collect the present position of all the children. The next layout/draw cycle will
        // change these bounds since we just expanded them. Then we can animate from prev location
        // to the new location. Note that the order of these calls matter as they all
        // add themselves to the root layout as overlays
        mScrollView.requestLayout();
        animateWallpaperOut();
        animateTitleCard(true, false);
        animateChildren(true, getChildrensGlobalBounds(mPreviewContent));
        animateExtras(true);
        mSelector = getChooserActivity().getComponentSelector();
        mSelector.setOnItemClickedListener(mOnComponentItemClicked);
        if (mBootAnimation != null) mBootAnimation.start();
        hideThemeTagLayout();
        mExpanded = true;
    }



    // Returns the boundaries for all the children of parent relative to the app window
    private List<Rect> getChildrensGlobalBounds(ViewGroup parent) {
        List<Rect> bounds = new ArrayList<Rect>();
        for (int i = 0; i < parent.getChildCount(); i++) {
            final View v = parent.getChildAt(i);
            int[] pos = new int[2];
            v.getLocationInWindow(pos);
            Rect boundary = new Rect(pos[0], pos[1], pos[0] + v.getWidth(), pos[1]+v.getHeight());
            bounds.add(boundary);
        }
        return bounds;
    }

    public void performClick(boolean clickedOnContent) {
        // Don't do anything if the theme is being processed
        if (mProcessingThemeLayout.getVisibility() == View.VISIBLE) return;

        if (clickedOnContent) {
            showApplyThemeOverlay();
        } else {
            if (isShowingConfirmCancelOverlay()) {
                hideConfirmCancelOverlay();
            }
        }
    }

    public void fadeOutCards(Runnable endAction) {
        for (int i = 0; i < mPreviewContent.getChildCount(); i++) {
            ComponentCardView v = (ComponentCardView) mPreviewContent.getChildAt(i);
            v.animateFadeOut();
        }
        mHandler.postDelayed(endAction, ComponentCardView.CARD_FADE_DURATION);
    }

    public void collapse(final boolean applyTheme) {
        mScrollView.setScrollingEnabled(false);

        // Pad the  view so it appears thinner
        ViewGroup content = (ViewGroup) mScrollView.getParent();
        Resources r = mScrollView.getContext().getResources();
        int leftRightPadding = (int) r.getDimension(R.dimen.collapsed_theme_page_padding);
        content.setPadding(leftRightPadding, 0, leftRightPadding, 0);

        if (applyTheme) {
            final boolean customized = isThemeCustomized();
            mThemeTagLayout.setCustomizedTagEnabled(customized);
        }

        //Move the theme preview so that it is near the center of page per spec
        int paddingTop = (int) r.getDimension(R.dimen.collapsed_theme_page_padding_top);
        if (!Utils.hasNavigationBar(getActivity())) {
            paddingTop +=
                    r.getDimensionPixelSize(R.dimen.navigation_bar_height) / 2;
        }
        mScrollView.setPadding(0, paddingTop, 0, 0);

        // During expand the wallpaper size decreases slightly to makeup for 9patch padding
        // so when we collapse we should increase it again.
        mShadowFrame.setBackgroundResource(R.drawable.bg_themepreview_shadow);
        Rect padding = new Rect();
        final NinePatchDrawable bg = (NinePatchDrawable) mShadowFrame.getBackground();
        bg.getPadding(padding);
        mShadowFrame.setPadding(padding.left, padding.top, padding.right, padding.bottom);

        // Gradually fade the drop shadow back in or else it will be out of place
        ValueAnimator shadowAnimation = ValueAnimator.ofObject(new IntEvaluator(), 0, 255);
        shadowAnimation.setDuration(ANIMATE_DURATION);
        shadowAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                bg.setAlpha((Integer) animator.getAnimatedValue());
            }

        });
        shadowAnimation.start();

        //Move the title card back in
        mTitleCard.setVisibility(View.VISIBLE);
        mTitleCard.setTranslationY(0);

        // Shrink the height
        ViewGroup.LayoutParams layoutParams = mPreviewContent.getLayoutParams();
        Resources resources = mPreviewContent.getResources();
        layoutParams.height = (int) resources.getDimension(R.dimen.theme_preview_height);

        mScrollView.requestLayout();
        List<Rect> bounds = getChildrensGlobalBounds(mPreviewContent);
        for (int i = 0; i < mPreviewContent.getChildCount(); i++) {
            ComponentCardView child = (ComponentCardView) mPreviewContent.getChildAt(i);
            LinearLayout.LayoutParams lparams =
                    (LinearLayout.LayoutParams) child.getLayoutParams();
            lparams.setMargins(0, 0, 0, 0);

            if (child.getId() == R.id.icon_container) {
                int top = (int) child.getResources()
                        .getDimension(R.dimen.collapsed_icon_card_margin_top);
                lparams.setMargins(0, top, 0, 0);
            } else if (child.getId() == R.id.font_preview_container) {
                int top = (int) child.getResources()
                        .getDimension(R.dimen.collapsed_font_card_margin_top);
                lparams.setMargins(0, top, 0, 0);
            } else if (child.getId() == R.id.navigation_bar_container) {
                int top = (int) child.getResources()
                        .getDimension(R.dimen.collapsed_navbar_card_margin_top);
                lparams.setMargins(0, top, 0, 0);
            }

            child.getLayoutParams();
            child.collapse();
        }

        // Collapse additional cards
        for (int i = 0; i < mAdditionalCards.getChildCount(); i++) {
            View v = mAdditionalCards.getChildAt(i);
            if (v instanceof ComponentCardView) {
                ComponentCardView card = (ComponentCardView) v;
                card.setVisibility(View.VISIBLE);
                card.collapse();
            }
        }

        animateChildren(false, bounds);
        animateExtras(false);
        animateWallpaperIn();
        animateTitleCard(false, applyTheme);
        if (mBootAnimation != null) mBootAnimation.stop();
        stopMediaPlayers();
        showThemeTagLayout();

        // Need to set the wallpaper background to black if the user has selected to apply
        // the "none" wallpaper
        if (applyTheme) {
            String pkgName = mSelectedComponentsMap.get(ThemesColumns.MODIFIES_LAUNCHER);
            if (pkgName != null && pkgName.length() == 0) {
                mWallpaper.setImageResource(R.drawable.wallpaper_none_bg);
            }
            // we do this here instead of in applyTheme() because this can take a bit longer
            // to propagate the change from WallpaperManager back to us
            if (mExternalWallpaperUri != null) {
                // Handle setting an external wallpaper in a separate thread
                new Thread(mApplyExternalWallpaperRunnable).start();
            }
        }
        mExpanded = false;
    }

    // This will animate the children's vertical positions between the previous bounds and the
    // new bounds which occur on the next draw
    private void animateChildren(final boolean isExpanding, final List<Rect> prevBounds) {
        final ViewGroup root = (ViewGroup) getActivity().getWindow()
                .getDecorView().findViewById(android.R.id.content);

        final Resources res = getResources();
        final float yOffset =
                res.getDimensionPixelSize(R.dimen.expand_collapse_child_offset)
                        * (isExpanding ? -1 : 1);
        // Grab the child's new location and animate from prev to current loc.
        final ViewTreeObserver observer = mScrollContent.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                observer.removeOnPreDrawListener(this);

                for (int i = mPreviewContent.getChildCount() - 1; i >= 0; i--) {
                    final ComponentCardView v = (ComponentCardView) mPreviewContent.getChildAt(i);

                    float prevY;
                    float endY;
                    float prevHeight;
                    float endHeight;
                    if (i >= prevBounds.size()) {
                        // View is being created
                        prevY = mPreviewContent.getTop() + mPreviewContent.getHeight();
                        endY = v.getY();
                        prevHeight = v.getHeight();
                        endHeight = v.getHeight();
                    } else {
                        Rect boundary = prevBounds.get(i);
                        prevY = boundary.top;
                        prevHeight = boundary.height();

                        int[] endPos = new int[2];
                        v.getLocationInWindow(endPos);
                        endY = endPos[1];
                        endHeight = v.getHeight();
                    }

                    int paddingTop = v.getPaddingTop() / 2;
                    float dy = (prevY - endY - paddingTop) + (prevHeight - endHeight) / 2;
                    dy += yOffset;
                    v.setTranslationY(dy);
                    root.getOverlay().add(v);

                    // Expanding has a delay while the wallpaper begins to fade out
                    // Collapsing is opposite of this so wallpaper will have the delay instead
                    int startDelay = isExpanding ? ANIMATE_START_DELAY : 0;

                    v.animate()
                            .setStartDelay(startDelay)
                            .translationY(0)
                            .setDuration(ANIMATE_DURATION)
                            .setInterpolator(
                                    new DecelerateInterpolator(ANIMATE_INTERPOLATE_FACTOR))
                            .withEndAction(new Runnable() {
                                public void run() {
                                    root.getOverlay().remove(v);
                                    mPreviewContent.addView(v, 0);
                                }
                            });
                    v.postDelayed(new Runnable() {
                        public void run() {
                            if (isExpanding) {
                                v.animateExpand();
                            }
                        }
                    }, ANIMATE_DURATION / 2);
                }
                return true;
            }
        });
    }

    private void animateExtras(final boolean isExpanding) {
        int[] pos = new int[2];
        mAdditionalCards.getLocationInWindow(pos);
        final ViewGroup parent = (ViewGroup) mAdditionalCards.getParent();
        final ViewGroup root = (ViewGroup) getActivity().getWindow()
                .getDecorView().findViewById(android.R.id.content);

        // During a collapse we don't want the card to shrink so add it to the overlay now
        // During an expand we want the card to expand so add it to the overlay post-layout
        if (!isExpanding) {
            root.getOverlay().add(mAdditionalCards);
        }

        // Expanding has a delay while the wallpaper begins to fade out
        // Collapsing is opposite of this so wallpaper will have the delay instead
        final int startDelay = isExpanding ? ANIMATE_START_DELAY : 0;
        final ViewTreeObserver observer = mScrollContent.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                observer.removeOnPreDrawListener(this);

                int translationY = 0;
                if (isExpanding) {
                    root.getOverlay().add(mAdditionalCards);
                } else {
                    translationY = getDistanceToMoveBelowScreen(mAdditionalCards);
                }

                int duration = isExpanding ? ANIMATE_DURATION + 100 : ANIMATE_DURATION;
                mAdditionalCards.animate()
                        .setStartDelay(startDelay)
                        .translationY(translationY)
                        .setDuration(duration)
                        .setInterpolator(
                                new DecelerateInterpolator(ANIMATE_INTERPOLATE_FACTOR))
                        .withEndAction(new Runnable() {
                            public void run() {
                                if (!isExpanding) {
                                    mAdditionalCards.setVisibility(View.INVISIBLE);
                                }
                                root.getOverlay().remove(mAdditionalCards);
                                parent.addView(mAdditionalCards);
                            }
                        });
                return false;
            }
        });
    }

    private int getDistanceToMoveBelowScreen(View v) {
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point p = new Point();
        display.getSize(p);
        int heightId = getResources()
                .getIdentifier("navigation_bar_height", "dimen", "android");
        int navbar_height = getResources().getDimensionPixelSize(heightId);
        int[] pos = new int[2];
        v.getLocationInWindow(pos);
        return p.y + navbar_height - pos[1];
    }

    private void animateTitleCard(final boolean expand, final boolean applyTheme) {
        final ViewGroup parent = (ViewGroup) mTitleCard.getParent();
        // Get current location of the title card
        int[] location = new int[2];
        mTitleCard.getLocationOnScreen(location);
        final int prevY = location[1];
        final int position = parent.indexOfChild(mTitleCard);

        final ViewTreeObserver observer = mScrollContent.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                observer.removeOnPreDrawListener(this);

                final ViewGroup root = (ViewGroup) getActivity().getWindow()
                        .getDecorView().findViewById(android.R.id.content);

                root.getOverlay().add(mTitleCard);

                //Move title card back where it was before the relayout
                float alpha = 1f;
                if (expand) {
                    int[] endPos = new int[2];
                    mTitleCard.getLocationInWindow(endPos);
                    int endY = endPos[1];
                    mTitleCard.setTranslationY(prevY - endY);
                    alpha = 0;
                } else {
                }

                // Fade the title card and move it out of the way
                mTitleCard.animate()
                        .alpha(alpha)
                        .setDuration(ANIMATE_DURATION)
                        .withEndAction(new Runnable() {
                            public void run() {
                                root.getOverlay().remove(mTitleCard);
                                parent.addView(mTitleCard, position);
                                if (expand) {
                                    mTitleCard.setVisibility(View.INVISIBLE);
                                } else {
                                    mTitleCard.setVisibility(View.VISIBLE);
                                    mClickableView.setVisibility(View.VISIBLE);
                                    if (applyTheme) {
                                        // The title card is the last animation when collapsing so
                                        // we will handle applying the theme, if applicable, here
                                        applyTheme();
                                    }
                                }
                            }
                        });
                return true;
            }
        });
    }

    private void animateWallpaperOut() {
        final ViewGroup root = (ViewGroup) getActivity().getWindow()
                .getDecorView().findViewById(android.R.id.content);

        int[] location = new int[2];
        mWallpaper.getLocationOnScreen(location);

        final int prevY = location[1];

        final ViewTreeObserver observer = mScrollContent.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                observer.removeOnPreDrawListener(this);
                root.getOverlay().add(mWallpaper);

                int[] location = new int[2];
                mWallpaper.getLocationOnScreen(location);
                final int newY = location[1];

                mWallpaper.setTranslationY(prevY - newY);
                mWallpaper.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction(new Runnable() {
                            public void run() {
                                root.getOverlay().remove(mWallpaper);
                                mShadowFrame.addView(mWallpaper, 0);
                                mWallpaper.setVisibility(View.GONE);
                            }
                        });
                return true;

            }
        });
    }

    private void animateWallpaperIn() {
                mWallpaper.setVisibility(View.VISIBLE);
                mWallpaper.setTranslationY(0);
                mWallpaper.animate()
                        .alpha(1f)
                        .setDuration(300);
    }

    protected String getAppliedFontPackageName() {
        final Configuration config = getActivity().getResources().getConfiguration();
        final ThemeConfig themeConfig = config != null ? config.themeConfig : null;
        return themeConfig != null ? themeConfig.getFontPkgName() :
                ThemeConfig.getSystemTheme().getFontPkgName();
    }

    protected ThemeManager getThemeManager() {
        return ThemeManager.getInstance(getActivity());
    }

    private void freeMediaPlayers() {
        for (MediaPlayer mp : mMediaPlayers.values()) {
            if (mp != null) {
                mp.stop();
                mp.release();
            }
        }
        mMediaPlayers.clear();
    }

    protected View.OnClickListener mPlayPauseClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MediaPlayer mp = (MediaPlayer) v.getTag();
            if (mp != null) {
                if (mp.isPlaying()) {
                    ((ImageView) v).setImageResource(R.drawable.media_sound_preview);
                    mp.pause();
                    mp.seekTo(0);
                } else {
                    stopMediaPlayers();
                    ((ImageView) v).setImageResource(R.drawable.media_sound_stop);
                    mp.start();
                }
            }
        }
    };

    protected MediaPlayer.OnCompletionListener mPlayCompletionListener
            = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            for (ImageView v : mMediaPlayers.keySet()) {
                if (mp == mMediaPlayers.get(v)) {
                    if (v != null) {
                        v.setImageResource(R.drawable.media_sound_preview);
                    }
                }
            }
        }
    };

    private void stopMediaPlayers() {
        for (ImageView v : mMediaPlayers.keySet()) {
            if (v != null) {
                v.setImageResource(R.drawable.media_sound_preview);
            }
            MediaPlayer mp = mMediaPlayers.get(v);
            if (mp != null && mp.isPlaying()) {
                mp.pause();
                mp.seekTo(0);
            }
        }
    }

    protected void resetTheme() {
        mSelectedComponentsMap.clear();
        Bundle args = new Bundle();
        args.putString(ARG_PACKAGE_NAME, mBaseThemePkgName);
        args.putLong(ARG_COMPONENT_ID, DEFAULT_COMPONENT_ID);
        getLoaderManager().restartLoader(LOADER_ID_ALL, args, this);
        mThemeResetting = true;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String pkgName = mPkgName;
        long componentId = DEFAULT_COMPONENT_ID;
        if (args != null) {
            pkgName = args.getString(ARG_PACKAGE_NAME);
            componentId = args.getLong(ARG_COMPONENT_ID, DEFAULT_COMPONENT_ID);
        }
        return CursorLoaderHelper.themeFragmentCursorLoader(getActivity(), id, pkgName,
                componentId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
        if (c.getCount() == 0) return;
        mCurrentCursor = c;
        mCurrentLoaderId = loader.getId();
        c.moveToFirst();
        boolean animate = !mApplyThemeOnPopulated;
        switch (mCurrentLoaderId) {
            case LOADER_ID_ALL:
                if (mProcessingResources && !isThemeProcessing()) {
                    mProcessingResources = false;
                    hideProcessingOverlay();
                }
                loadLegacyThemeInfo(c);
                populateSupportedComponents(c);
                loadWallpaper(c, false);
                loadStatusBar(c, false);
                loadIcons(c, false);
                loadNavBar(c, false);
                loadTitle(c);
                loadFont(c, false);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        animateContentIn();
                    }
                });
                if (mShowLockScreenSelectorAfterContentLoaded) {
                    getChooserActivity().expandContentAndAnimateLockScreenCardIn();
                    mShowLockScreenSelectorAfterContentLoaded = false;
                }
                break;
            case LOADER_ID_STATUS_BAR:
                loadStatusBar(c, animate);
                break;
            case LOADER_ID_FONT:
                loadFont(c, animate);
                break;
            case LOADER_ID_ICONS:
                loadIcons(c, animate);
                break;
            case LOADER_ID_WALLPAPER:
                loadWallpaper(c, animate);
                break;
            case LOADER_ID_NAVIGATION_BAR:
                loadNavBar(c, animate);
                break;
            case LOADER_ID_LIVE_LOCK_SCREEN:
            case LOADER_ID_LOCKSCREEN:
                loadLockScreen(c, animate);
                break;
            case LOADER_ID_STYLE:
                loadStyle(c, animate);
                break;
            case LOADER_ID_BOOT_ANIMATION:
                loadBootAnimation(c);
                break;
            case LOADER_ID_RINGTONE:
                loadAudible(RingtoneManager.TYPE_RINGTONE, c, animate);
                break;
            case LOADER_ID_NOTIFICATION:
                loadAudible(RingtoneManager.TYPE_NOTIFICATION, c, animate);
                break;
            case LOADER_ID_ALARM:
                loadAudible(RingtoneManager.TYPE_ALARM, c, animate);
                break;
        }
        if (mCurrentLoaderId != LOADER_ID_ALL) {
            if (!componentsChanged()) {
                getChooserActivity().hideSaveApplyButton();
            } else if (!mApplyThemeOnPopulated) {
                getChooserActivity().showSaveApplyButton();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {}

    private void loadAdditionalCards(Cursor c) {
        for(int i=0; i < mAdditionalCards.getChildCount(); i++) {
            View v = mAdditionalCards.getChildAt(i);
            if (v instanceof ComponentCardView) {
                String component = mCardIdsToComponentTypes.get(v.getId());
                loadAdditionalCard(c, component, shouldShowComponentCard(component));
            }
        }
    }

    private void loadAdditionalCard(Cursor c, String component, boolean hasContent) {
        if (MODIFIES_LOCKSCREEN.equals(component)) {
            if (hasContent) {
                loadLockScreen(c, false);
            } else {
                mLockScreenCard.clearWallpaper();
                mLockScreenCard.setEmptyViewEnabled(true);
                setAddComponentTitle(mLockScreenCard, getString(R.string.lockscreen_label));
            }
        } else if (MODIFIES_LAUNCHER.equals(component)) {
            // this was already loaded so no need to do this again.
        } else if (MODIFIES_OVERLAYS.equals(component)) {
            if (hasContent) {
                loadStyle(c, false);
            } else {
                mStyleCard.setEmptyViewEnabled(true);
                setAddComponentTitle(mStyleCard,
                        getString(R.string.style_label));
            }
        } else if (MODIFIES_BOOT_ANIM.equals(component)) {
            if (hasContent) {
                loadBootAnimation(c);
            } else {
                mBootAnimationCard.setEmptyViewEnabled(true);
                setAddComponentTitle(mBootAnimationCard,
                        getString(R.string.boot_animation_label));
            }
        } else if (MODIFIES_RINGTONES.equals(component)) {
            if (hasContent) {
                loadAudible(RingtoneManager.TYPE_RINGTONE, c, false);
            } else {
                mRingtoneCard.setEmptyViewEnabled(true);
                setAddComponentTitle(mRingtoneCard,
                        getAudibleLabel(RingtoneManager.TYPE_RINGTONE));
            }
        } else if (MODIFIES_NOTIFICATIONS.equals(component)) {
            if (hasContent) {
                loadAudible(RingtoneManager.TYPE_NOTIFICATION, c, false);
            } else {
                mNotificationCard.setEmptyViewEnabled(true);
                setAddComponentTitle(mNotificationCard,
                        getAudibleLabel(RingtoneManager.TYPE_NOTIFICATION));
            }
        } else if (MODIFIES_ALARMS.equals(component)) {
            if (hasContent) {
                loadAudible(RingtoneManager.TYPE_ALARM, c, false);
            } else {
                mAlarmCard.setEmptyViewEnabled(true);
                setAddComponentTitle(mAlarmCard,
                        getAudibleLabel(RingtoneManager.TYPE_ALARM));
            }
        } else {
            throw new IllegalArgumentException("Don't know how to load: " + component);
        }
    }

    protected void populateSupportedComponents(Cursor c) {
        List<String> components = ThemeUtils.getAllComponents();
        for(String component : components) {
            int pkgIdx = c.getColumnIndex(ThemesColumns.PKG_NAME);
            int modifiesCompIdx = c.getColumnIndex(component);

            String pkg = c.getString(pkgIdx);
            boolean supported = (modifiesCompIdx >= 0) && (c.getInt(modifiesCompIdx) == 1);
            if (supported) {
                mBaseThemeSupportedComponents.add(component);
                mSelectedComponentsMap.put(component, pkg);
            }
        }

        if (mApplyThemeOnPopulated) {
            applyTheme();
        }
    }

    /**
     *  Determines whether a card should be shown or not.
     *  UX Rules:
     *    1) "My Theme" always shows all cards
     *    2) Other themes only show what has been implemented in the theme
     *
     */
    protected Boolean shouldShowComponentCard(String component) {
        String pkg = mSelectedComponentsMap.get(component);
        return pkg != null && pkg.equals(mPkgName);
    }

    protected void loadLegacyThemeInfo(Cursor c) {
        int targetApiIdx = c.getColumnIndex(ThemesColumns.TARGET_API);
        // If this is being called for a MyThemeFragment the index will be -1 so set to
        // SYSTEM_TARGET_API so we don't display the tag.  If the user applied a legacy theme
        // then they should have already been warned.
        int targetApi = targetApiIdx < 0 ? SYSTEM_TARGET_API : c.getInt(targetApiIdx);
        mIsLegacyTheme = targetApi != SYSTEM_TARGET_API && targetApi <= Build.VERSION_CODES.KITKAT;
        mThemeTagLayout.setLegacyTagEnabled(mIsLegacyTheme);
    }

    protected void loadTitle(Cursor c) {
        int titleIdx = c.getColumnIndex(ThemesColumns.TITLE);
        int authorIdx = c.getColumnIndex(ThemesColumns.AUTHOR);
        mTitle.setText(c.getString(titleIdx));
        mAuthor.setText(c.getString(authorIdx));
    }

    protected void loadWallpaper(Cursor c, boolean animate) {
        mExternalWallpaperUri = null;
        Drawable overlay = null;
        if (animate) {
            overlay = getOverlayDrawable(mWallpaperCard, true);
        }
        if (mWallpaperCard.isShowingEmptyView()) mWallpaperCard.setEmptyViewEnabled(false);

        int pkgNameIdx = c.getColumnIndex(ThemesColumns.PKG_NAME);
        int wpIdx = c.getColumnIndex(PreviewColumns.WALLPAPER_PREVIEW);
        int cmpntIdIdx = c.getColumnIndex(PreviewColumns.COMPONENT_ID);
        final Resources res = getResources();
        Bitmap bitmap = Utils.loadBitmapBlob(c, wpIdx);
        if (bitmap != null) {
            mWallpaper.setImageBitmap(bitmap);
            mWallpaperCard.setWallpaper(new BitmapDrawable(res, bitmap));
            String pkgName = c.getString(pkgNameIdx);
            Long cmpntId = (cmpntIdIdx >= 0) ? c.getLong(cmpntIdIdx) : DEFAULT_COMPONENT_ID;
            if (!mPkgName.equals(pkgName) || (mPkgName.equals(pkgName)
                    && mBaseThemeSupportedComponents.contains(MODIFIES_LAUNCHER))) {
                mSelectedComponentsMap.put(MODIFIES_LAUNCHER, pkgName);
                mSelectedWallpaperComponentId = cmpntId;
                setCardTitle(mWallpaperCard, pkgName, getString(R.string.wallpaper_label));
            }
        } else {
            // Set the wallpaper to "None"
            mWallpaperCard.setWallpaper(null);
            setCardTitle(mWallpaperCard, WALLPAPER_NONE, getString(R.string.wallpaper_label));
            mSelectedComponentsMap.put(ThemesColumns.MODIFIES_LAUNCHER, WALLPAPER_NONE);
        }

        if (animate) {
            animateContentChange(R.id.wallpaper_card, mWallpaperCard, overlay);
        }
    }

    protected void loadLockScreen(Cursor c, boolean animate) {
        mExternalLockscreenUri = null;
        Drawable overlay = null;
        if (animate) {
            overlay = getOverlayDrawable(mLockScreenCard, true);
        }
        if (mLockScreenCard.isShowingEmptyView()) mLockScreenCard.setEmptyViewEnabled(false);

        int pkgNameIdx = c.getColumnIndex(ThemesColumns.PKG_NAME);
        int liveLockIndex = c.getColumnIndex(MODIFIES_LIVE_LOCK_SCREEN);
        boolean isLiveLockScreen = liveLockIndex >= 0 && c.getInt(liveLockIndex) == 1;

        int wpIdx = isLiveLockScreen
                ? c.getColumnIndex(PreviewColumns.LIVE_LOCK_SCREEN_PREVIEW)
                : c.getColumnIndex(PreviewColumns.LOCK_WALLPAPER_PREVIEW);
        final Resources res = getResources();
        Bitmap bitmap = Utils.loadBitmapBlob(c, wpIdx);
        if (bitmap != null) {
            mLockScreenCard.setWallpaper(new BitmapDrawable(res, bitmap));
            String pkgName = c.getString(pkgNameIdx);
            if (!mPkgName.equals(pkgName) || (mPkgName.equals(pkgName)
                    && (mBaseThemeSupportedComponents.contains(MODIFIES_LOCKSCREEN) ||
                    mBaseThemeSupportedComponents.contains(MODIFIES_LIVE_LOCK_SCREEN)))) {
                if (isLiveLockScreen) {
                    mSelectedComponentsMap.put(MODIFIES_LIVE_LOCK_SCREEN, pkgName);
                    if (mCurrentTheme.containsKey(MODIFIES_LOCKSCREEN)) {
                        mSelectedComponentsMap.put(MODIFIES_LOCKSCREEN, LOCKSCREEN_NONE);
                    } else {
                        mSelectedComponentsMap.remove(MODIFIES_LOCKSCREEN);
                    }
                    setCardTitle(mLockScreenCard, pkgName,
                            getString(R.string.live_lock_screen_label));
                } else {
                    mSelectedComponentsMap.put(MODIFIES_LOCKSCREEN, pkgName);
                    if (mCurrentTheme.containsKey(MODIFIES_LIVE_LOCK_SCREEN)) {
                        mSelectedComponentsMap.put(MODIFIES_LIVE_LOCK_SCREEN, LOCKSCREEN_NONE);
                    } else {
                        mSelectedComponentsMap.remove(MODIFIES_LIVE_LOCK_SCREEN);
                    }
                    setCardTitle(mLockScreenCard, pkgName, getString(R.string.lockscreen_label));
                }
            }
        } else {
            // Set the lockscreen wallpaper to "None"
            mLockScreenCard.setWallpaper(null);
            setCardTitle(mLockScreenCard, WALLPAPER_NONE, getString(R.string.lockscreen_label));
        }

        if (animate) {
            animateContentChange(R.id.lockscreen_card, mLockScreenCard, overlay);
        }
    }

    protected void loadStatusBar(Cursor c, boolean animate) {
        int backgroundIdx = c.getColumnIndex(PreviewColumns.STATUSBAR_BACKGROUND);
        int wifiIdx = c.getColumnIndex(PreviewColumns.STATUSBAR_WIFI_ICON);
        int wifiMarginIdx = c.getColumnIndex(PreviewColumns.STATUSBAR_WIFI_COMBO_MARGIN_END);
        int bluetoothIdx = c.getColumnIndex(PreviewColumns.STATUSBAR_BLUETOOTH_ICON);
        int signalIdx = c.getColumnIndex(PreviewColumns.STATUSBAR_SIGNAL_ICON);
        int batteryIdx = c.getColumnIndex(Utils.getBatteryIndex(mBatteryStyle));
        int clockColorIdx = c.getColumnIndex(PreviewColumns.STATUSBAR_CLOCK_TEXT_COLOR);
        int pkgNameIdx = c.getColumnIndex(ThemesColumns.PKG_NAME);

        Bitmap background = Utils.loadBitmapBlob(c, backgroundIdx);
        Bitmap bluetoothIcon = Utils.loadBitmapBlob(c, bluetoothIdx);
        Bitmap wifiIcon = Utils.loadBitmapBlob(c, wifiIdx);
        Bitmap signalIcon = Utils.loadBitmapBlob(c, signalIdx);
        Bitmap batteryIcon = Utils.loadBitmapBlob(c, batteryIdx);
        int wifiMargin = wifiMarginIdx != -1 ? c.getInt(wifiMarginIdx) : DEFAULT_WIFI_MARGIN;
        int clockTextColor = clockColorIdx != -1 ? c.getInt(clockColorIdx) : DEFAULT_CLOCK_COLOR;

        Drawable overlay = null;
        if (animate) {
            overlay = getOverlayDrawable(mStatusBar, false);
        }
        if (mStatusBarCard.isShowingEmptyView()) mStatusBarCard.setEmptyViewEnabled(false);

        mStatusBar.setBackground(new BitmapDrawable(getActivity().getResources(), background));
        mBluetooth.setImageBitmap(bluetoothIcon);
        mWifi.setImageBitmap(wifiIcon);
        mSignal.setImageBitmap(signalIcon);
        mBattery.setImageBitmap(batteryIcon);
        mClock.setTextColor(clockTextColor);

        ViewGroup.MarginLayoutParams params =
                (ViewGroup.MarginLayoutParams) mWifi.getLayoutParams();
        params.setMarginEnd(wifiMargin);
        mWifi.setLayoutParams(params);

        if (mBatteryStyle == 4) {
            mBattery.setVisibility(View.GONE);
        } else {
            mBattery.setVisibility(View.VISIBLE);
        }
        mStatusBar.post(new Runnable() {
            @Override
            public void run() {
                mStatusBar.invalidate();
            }
        });
        if (pkgNameIdx > -1) {
            String pkgName = c.getString(pkgNameIdx);
            if (!mPkgName.equals(pkgName) || (mPkgName.equals(pkgName)
                    && mBaseThemeSupportedComponents.contains(MODIFIES_STATUS_BAR))) {
                mSelectedComponentsMap.put(MODIFIES_STATUS_BAR, pkgName);
                setCardTitle(mStatusBarCard, pkgName,
                        getString(R.string.statusbar_label));
            }
        }
        if (animate) {
            animateContentChange(R.id.status_bar_container, mStatusBar, overlay);
        }
    }

    protected void loadIcons(Cursor c, boolean animate) {
        if (mIconCard.isShowingEmptyView()) {
            mIconCard.setEmptyViewEnabled(false);
        }
        int[] iconIdx = new int[3];
        iconIdx[0] = c.getColumnIndex(PreviewColumns.ICON_PREVIEW_1);
        iconIdx[1] = c.getColumnIndex(PreviewColumns.ICON_PREVIEW_2);
        iconIdx[2] = c.getColumnIndex(PreviewColumns.ICON_PREVIEW_3);
        int pkgNameIdx = c.getColumnIndex(ThemesColumns.PKG_NAME);

        // Set the icons. If the provider does not have an icon preview then
        // fall back to the default icon set
        IconPreviewHelper helper = new IconPreviewHelper(getActivity(), "");
        ViewGroup iconContainer =
                (ViewGroup) mIconCard.findViewById(R.id.icon_preview_container);
        int numOfChildren = iconContainer.getChildCount();

        List<ImageView> iconViews = new ArrayList<ImageView>(numOfChildren);
        for(int i=0; i < numOfChildren; i++) {
            final View view = iconContainer.getChildAt(i);
            if (!(view instanceof ImageView)) continue;
            iconViews.add((ImageView) view);
        }

        for(int i=0; i < iconViews.size() && i < iconIdx.length; i++) {
            final ImageView v = iconViews.get(i);
            Bitmap bitmap = Utils.loadBitmapBlob(c, iconIdx[i]);
            Drawable oldIcon = v.getDrawable();
            Drawable newIcon;
            if (bitmap == null) {
                ComponentName component = sIconComponents[i];
                newIcon = helper.getDefaultIcon(component.getPackageName(),
                        component.getClassName());
            } else {
                newIcon = new BitmapDrawable(getResources(), bitmap);
            }
            if (animate) {
                Drawable[] layers = new Drawable[2];
                layers[0] = oldIcon instanceof IconTransitionDrawable ?
                        ((IconTransitionDrawable) oldIcon).getDrawable(1) : oldIcon;
                layers[1] = newIcon;
                final IconTransitionDrawable itd = new IconTransitionDrawable(layers);
                v.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        itd.startTransition(ANIMATE_COMPONENT_CHANGE_DURATION);
                        v.setImageDrawable(itd);
                    }
                }, ANIMATE_COMPONENT_ICON_DELAY * i);
            } else {
                v.setImageDrawable(newIcon);
            }
        }
        if (pkgNameIdx > -1) {
            String pkgName = c.getString(pkgNameIdx);
            if (!mPkgName.equals(pkgName) || (mPkgName.equals(pkgName)
                    && mBaseThemeSupportedComponents.contains(MODIFIES_ICONS))) {
                mSelectedComponentsMap.put(MODIFIES_ICONS, pkgName);
                setCardTitle(mIconCard, pkgName,
                        getString(R.string.icon_label));
            }
        }
    }

    protected void loadNavBar(Cursor c, boolean animate) {
        int backButtonIdx = c.getColumnIndex(PreviewColumns.NAVBAR_BACK_BUTTON);
        int homeButtonIdx = c.getColumnIndex(PreviewColumns.NAVBAR_HOME_BUTTON);
        int recentButtonIdx = c.getColumnIndex(PreviewColumns.NAVBAR_RECENT_BUTTON);
        int backgroundIdx = c.getColumnIndex(PreviewColumns.NAVBAR_BACKGROUND);
        if (backgroundIdx == -1) {
            backgroundIdx = c.getColumnIndex(PreviewColumns.STATUSBAR_BACKGROUND);
        }
        int pkgNameIdx = c.getColumnIndex(ThemesColumns.PKG_NAME);

        Bitmap background = Utils.loadBitmapBlob(c, backgroundIdx);
        Bitmap backButton = Utils.loadBitmapBlob(c, backButtonIdx);
        Bitmap homeButton = Utils.loadBitmapBlob(c, homeButtonIdx);
        Bitmap recentButton = Utils.loadBitmapBlob(c, recentButtonIdx);

        Drawable overlay = null;
        if (animate) {
            overlay = getOverlayDrawable(mNavBar, false);
        }
        if (mNavBarCard.isShowingEmptyView()) mNavBarCard.setEmptyViewEnabled(false);

        mNavBar.setBackground(new BitmapDrawable(getActivity().getResources(), background));
        mBackButton.setImageBitmap(backButton);
        mHomeButton.setImageBitmap(homeButton);
        mRecentButton.setImageBitmap(recentButton);

        if (pkgNameIdx > -1) {
            String pkgName = c.getString(pkgNameIdx);
            if (!mPkgName.equals(pkgName) || (mPkgName.equals(pkgName)
                    && mBaseThemeSupportedComponents.contains(MODIFIES_NAVIGATION_BAR))) {
                mSelectedComponentsMap.put(MODIFIES_NAVIGATION_BAR, pkgName);
                setCardTitle(mNavBarCard, pkgName, getString(R.string.navbar_label));
            }
        }
        if (animate) {
            animateContentChange(R.id.navigation_bar_container, mNavBar, overlay);
        }
    }

    protected void loadFont(Cursor c, boolean animate) {
        Drawable overlay = null;
        if (animate) {
            overlay = getOverlayDrawable(mFontPreview, true);
        }
        if (mFontCard.isShowingEmptyView()) mFontCard.setEmptyViewEnabled(false);

        int pkgNameIdx = c.getColumnIndex(ThemesColumns.PKG_NAME);
        String pkgName = pkgNameIdx >= 0 ? c.getString(pkgNameIdx) : mPkgName;
        TypefaceHelperCache cache = TypefaceHelperCache.getInstance();
        ThemedTypefaceHelper helper = cache.getHelperForTheme(getActivity(), pkgName);
        mTypefaceNormal = helper.getTypeface(Typeface.NORMAL);
        mFontPreview.setTypeface(mTypefaceNormal);
        if (!mPkgName.equals(pkgName) || (mPkgName.equals(pkgName)
                && mBaseThemeSupportedComponents.contains(MODIFIES_FONTS))) {
            mSelectedComponentsMap.put(MODIFIES_FONTS, pkgName);
            setCardTitle(mFontCard, pkgName, getString(R.string.font_label));
        }

        if (animate) {
            animateContentChange(R.id.font_preview_container, mFontPreview, overlay);
        }
    }

    protected void loadStyle(Cursor c, boolean animate) {
        Drawable overlay = null;
        if (animate) {
            overlay = getOverlayDrawable(mStylePreview, true);
        }
        if (mStyleCard.isShowingEmptyView()) {
            mStyleCard.setEmptyViewEnabled(false);
        }

        int pkgNameIdx = c.getColumnIndex(ThemesColumns.PKG_NAME);
        int styleIdx = c.getColumnIndex(PreviewColumns.STYLE_PREVIEW);
        mStylePreview.setImageBitmap(Utils.loadBitmapBlob(c, styleIdx));
        if (pkgNameIdx > -1) {
            String pkgName = c.getString(pkgNameIdx);
            if (!mPkgName.equals(pkgName) || (mPkgName.equals(pkgName)
                    && mBaseThemeSupportedComponents.contains(MODIFIES_OVERLAYS))) {
                mSelectedComponentsMap.put(MODIFIES_OVERLAYS, pkgName);
                setCardTitle(mStyleCard, pkgName,
                        getString(R.string.style_label));
            }
        }
        if (animate) {
            animateContentChange(R.id.style_card, mStylePreview, overlay);
        }
    }

    protected void loadBootAnimation(Cursor c) {
        if (mBootAnimationCard.isShowingEmptyView()) {
            mBootAnimationCard.setEmptyViewEnabled(false);
        }
        int pkgNameIdx = c.getColumnIndex(ThemesColumns.PKG_NAME);
        if (mBootAnimation != null) {
            String pkgName;
            if (pkgNameIdx > -1) {
                pkgName = c.getString(pkgNameIdx);
                if (!mPkgName.equals(pkgName) || (mPkgName.equals(pkgName)
                        && mBaseThemeSupportedComponents.contains(MODIFIES_BOOT_ANIM))) {
                    mSelectedComponentsMap.put(MODIFIES_BOOT_ANIM, pkgName);
                    setCardTitle(mBootAnimationCard, pkgName,
                            getString(R.string.boot_animation_label));
                }
            } else {
                pkgName = mCurrentTheme.get(MODIFIES_BOOT_ANIM);
            }
            mBootAnimation.stop();
            new AnimationLoader(getActivity(), pkgName, mBootAnimation).execute();
        }
    }

    protected void loadAudible(int type, Cursor c, boolean animate) {
        ComponentCardView audibleContainer = null;
        ImageView playPause = null;
        String component = null;
        int parentResId = 0;
        switch (type) {
            case RingtoneManager.TYPE_RINGTONE:
                audibleContainer = mRingtoneCard;
                playPause = mRingtonePlayPause;
                component = MODIFIES_RINGTONES;
                parentResId = R.id.ringtone_preview_container;
                break;
            case RingtoneManager.TYPE_NOTIFICATION:
                audibleContainer = mNotificationCard;
                playPause = mNotificationPlayPause;
                component = MODIFIES_NOTIFICATIONS;
                parentResId = R.id.notification_preview_container;
                break;
            case RingtoneManager.TYPE_ALARM:
                audibleContainer = mAlarmCard;
                playPause = mAlarmPlayPause;
                component = MODIFIES_ALARMS;
                parentResId = R.id.alarm_preview_container;
                break;
        }
        if (audibleContainer == null) return;

        View content = audibleContainer.findViewById(R.id.content);
        Drawable overlay = null;
        if (animate) {
            overlay = getOverlayDrawable(content, true);
        }
        if (audibleContainer.isShowingEmptyView()) {
            audibleContainer.setEmptyViewEnabled(false);
        }

        int pkgNameIdx = c.getColumnIndex(ThemesColumns.PKG_NAME);
        int titleIdx = c.getColumnIndex(ThemesColumns.TITLE);
        if (playPause == null) {
            playPause = (ImageView) audibleContainer.findViewById(R.id.play_pause);
        }
        TextView title = (TextView) audibleContainer.findViewById(R.id.audible_name);
        MediaPlayer mp = mMediaPlayers.get(playPause);
        if (mp == null) {
            mp = new MediaPlayer();
        }
        String pkgName = c.getString(pkgNameIdx);
        setCardTitle(audibleContainer, pkgName, getAudibleLabel(type));
        AudibleLoadingThread thread = new AudibleLoadingThread(getActivity(), type, pkgName, mp);
        title.setText(c.getString(titleIdx));
        if (!mPkgName.equals(pkgName) || (mPkgName.equals(pkgName)
                && mBaseThemeSupportedComponents.contains(component))) {
            mSelectedComponentsMap.put(component, pkgName);
        }

        playPause.setVisibility(View.VISIBLE);
        playPause.setTag(mp);
        mMediaPlayers.put(playPause, mp);
        playPause.setOnClickListener(mPlayPauseClickListener);
        mp.setOnCompletionListener(mPlayCompletionListener);
        if (animate) {
            animateContentChange(parentResId, content, overlay);
        }
        thread.start();
    }

    protected Drawable getOverlayDrawable(View v, boolean requiresTransparency) {
        if (!v.isDrawingCacheEnabled()) v.setDrawingCacheEnabled(true);
        Bitmap cache = v.getDrawingCache(true).copy(
                requiresTransparency ? Config.ARGB_8888 : Config.RGB_565, false);
        Drawable d = cache != null ? new BitmapDrawable(getResources(), cache) : null;
        v.destroyDrawingCache();

        return d;
    }

    protected String getAudibleLabel(int type) {
        switch (type) {
            case RingtoneManager.TYPE_RINGTONE:
                return getString(R.string.ringtone_label);
            case RingtoneManager.TYPE_NOTIFICATION:
                return getString(R.string.notification_label);
            case RingtoneManager.TYPE_ALARM:
                return getString(R.string.alarm_label);
        }
        return null;
    }

    protected void setCardTitle(ComponentCardView card, String pkgName, String title) {
        TextView tv = (TextView) card.findViewById(R.id.label);
        if (Utils.getDefaultThemePackageName(getActivity()).equals(pkgName)) {
            tv.setText(getString(R.string.default_tag_text) + " " + title);
        } else {
            tv.setText(title);
        }
    }

    protected void setAddComponentTitle(ComponentCardView card, String title) {
        TextView tv = (TextView) card.findViewById(R.id.label);
        tv.setText(getString(R.string.add_component_text) + " " + title);
    }

    public static ComponentName[] getIconComponents(Context context) {
        if (sIconComponents == null || sIconComponents.length == 0) {
            sIconComponents = new ComponentName[]{COMPONENT_DIALER, COMPONENT_MESSAGING,
                    COMPONENT_CAMERA, COMPONENT_BROWSER};

            PackageManager pm = context.getPackageManager();

            // if device does not have telephony replace dialer and mms
            if (!pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
                sIconComponents[0] = COMPONENT_CALENDAR;
                sIconComponents[1] = COMPONENT_GALERY;
            } else {
                // decide on which dialer icon to use
                try {
                    if (pm.getPackageInfo(DIALER_NEXT_PACKAGE, 0) != null) {
                        sIconComponents[0] = COMPONENT_DIALERNEXT;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    // default to COMPONENT_DIALER
                }
            }

            if (!pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                sIconComponents[2] = COMPONENT_SETTINGS;
            } else {
                // decide on which camera icon to use
                try {
                    if (pm.getPackageInfo(CAMERA_NEXT_PACKAGE, 0) != null) {
                        sIconComponents[2] = COMPONENT_CAMERANEXT;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    // default to COMPONENT_CAMERA
                }
            }

        }
        return sIconComponents;
    }

    private void setupCardClickListeners(View parent) {
        for (int i = 0; i < mCardIdsToComponentTypes.size(); i++) {
            parent.findViewById(mCardIdsToComponentTypes.keyAt(i))
                    .setOnClickListener(mCardClickListener);
        }
    }

    private View.OnClickListener mCardClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (isShowingConfirmCancelOverlay() || isShowingCustomizeResetLayout()) return;
            if (mActiveCardId > 0) {
                // need to fade the newly selected card in if another was currently selected.
                ((ComponentCardView) v).animateCardFadeIn();
            }
            mActiveCardId = v.getId();
            String component = mCardIdsToComponentTypes.get(mActiveCardId);
            // Only pass on mSelectedWallpaperComponentId if dealing with mods_launcher
            long selectedComponentId = (ThemesColumns.MODIFIES_LAUNCHER.equals(component)) ?
                    mSelectedWallpaperComponentId : DEFAULT_COMPONENT_ID;
            String pkgName = mSelectedComponentsMap.get(component);
            if (component.equals(MODIFIES_LOCKSCREEN) && TextUtils.isEmpty(pkgName)) {
                String liveLockScreenPkg = mSelectedComponentsMap.get(MODIFIES_LIVE_LOCK_SCREEN);
                if (liveLockScreenPkg != null) {
                    pkgName = liveLockScreenPkg;
                }
            }
            getChooserActivity().showComponentSelector(component, pkgName, selectedComponentId, v);
            fadeOutNonSelectedCards(mActiveCardId);
            stopMediaPlayers();
        }
    };

    private ConfirmCancelOverlay.OnOverlayDismissedListener mApplyCancelListener =
            new ConfirmCancelOverlay.OnOverlayDismissedListener() {
                @Override
                public void onDismissed(boolean accepted) {
                    hideConfirmCancelOverlay(accepted);
                }
            };

    private ConfirmCancelOverlay.OnOverlayDismissedListener mDeleteConfirmationListener =
            new ConfirmCancelOverlay.OnOverlayDismissedListener() {
                @Override
                public void onDismissed(boolean accepted) {
                    if (accepted) uninstallTheme();
                    hideConfirmCancelOverlay();
                }
            };

    private ConfirmCancelOverlay.OnOverlayDismissedListener mResetConfirmationListener =
            new ConfirmCancelOverlay.OnOverlayDismissedListener() {
                @Override
                public void onDismissed(boolean accepted) {
                    if (accepted) resetTheme();
                    hideConfirmCancelOverlay();
                }
            };

    private View.OnClickListener mCustomizeResetClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == mDismissButton) {
                hideCustomizeResetLayout(CustomizeResetAction.Dismiss);
            } else if (v == mResetButton) {
                hideCustomizeResetLayout(CustomizeResetAction.Reset);
            } else if (v == mCustomizeButton) {
                hideCustomizeResetLayout(CustomizeResetAction.Customize);
            }
        }
    };

    protected void loadComponentFromPackage(String pkgName, String component, long componentId) {
        Bundle args = new Bundle();
        args.putString(ARG_PACKAGE_NAME, pkgName);
        args.putLong(ARG_COMPONENT_ID, componentId);
        int loaderId = LOADER_ID_INVALID;
        if (MODIFIES_STATUS_BAR.equals(component)) {
            loaderId = LOADER_ID_STATUS_BAR;
        } else if (MODIFIES_FONTS.equals(component)) {
            loaderId = LOADER_ID_FONT;
        } else if (MODIFIES_ICONS.equals(component)) {
            loaderId = LOADER_ID_ICONS;
        } else if (MODIFIES_NAVIGATION_BAR.equals(component)) {
            loaderId = LOADER_ID_NAVIGATION_BAR;
        } else if (MODIFIES_LAUNCHER.equals(component)) {
            if (pkgName != null) {
                if (TextUtils.isEmpty(pkgName)) {
                    mWallpaperCard.setWallpaper(null);
                    mSelectedComponentsMap.put(ThemesColumns.MODIFIES_LAUNCHER, WALLPAPER_NONE);
                    setCardTitle(mWallpaperCard, WALLPAPER_NONE,
                            getString(R.string.wallpaper_label));
                    getChooserActivity().showSaveApplyButton();
                } else if (ComponentSelector.EXTERNAL_WALLPAPER.equals(pkgName)) {
                    // Check if we have READ_EXTERNAL_STORAGE permission and if not request it,
                    // otherwise let the user pick an image
                    if (getActivity().checkSelfPermission(
                            READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
                        mAfterPermissionGrantedRunnable = new Runnable() {
                            @Override
                            public void run() {
                                getChooserActivity().pickExternalWallpaper();
                                setCardTitle(mWallpaperCard, WALLPAPER_NONE,
                                        getString(R.string.wallpaper_label));
                            }
                        };
                        requestPermissions(new String[]{READ_EXTERNAL_STORAGE},
                                PERMISSION_REQUEST);
                    } else {
                        getChooserActivity().pickExternalWallpaper();
                        setCardTitle(mWallpaperCard, WALLPAPER_NONE,
                                getString(R.string.wallpaper_label));
                    }
                } else {
                    loaderId = LOADER_ID_WALLPAPER;
                }
            }
        } else if (MODIFIES_LOCKSCREEN.equals(component)
                || MODIFIES_LIVE_LOCK_SCREEN.equals(component)) {
            if (pkgName != null && TextUtils.isEmpty(pkgName)) {
                mLockScreenCard.setWallpaper(null);
                mSelectedComponentsMap.put(ThemesColumns.MODIFIES_LOCKSCREEN, LOCKSCREEN_NONE);

                if(mSelectedComponentsMap.containsKey(MODIFIES_LIVE_LOCK_SCREEN)) {
                    mSelectedComponentsMap.put(MODIFIES_LIVE_LOCK_SCREEN, LOCKSCREEN_NONE);
                }
                setCardTitle(mLockScreenCard, WALLPAPER_NONE,
                        getString(R.string.lockscreen_label));
                if (mLockScreenCard.isShowingEmptyView()) {
                    mLockScreenCard.setEmptyViewEnabled(false);
                }
                getChooserActivity().showSaveApplyButton();
            } else if (ComponentSelector.EXTERNAL_WALLPAPER.equals(pkgName)) {
                // Check if we have READ_EXTERNAL_STORAGE permission and if not request it,
                // otherwise let the user pick an image
                if (getActivity().checkSelfPermission(
                        READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
                    mAfterPermissionGrantedRunnable = new Runnable() {
                        @Override
                        public void run() {
                            getChooserActivity().pickExternalLockscreen();
                            setCardTitle(mLockScreenCard, WALLPAPER_NONE,
                                    getString(R.string.lockscreen_label));
                        }
                    };
                    requestPermissions(new String[] {READ_EXTERNAL_STORAGE},
                            PERMISSION_REQUEST);
                } else {
                    getChooserActivity().pickExternalLockscreen();
                    setCardTitle(mLockScreenCard, WALLPAPER_NONE,
                            getString(R.string.lockscreen_label));
                }
            } else if (ComponentSelector.MOD_LOCK.equals(pkgName)) {
                startLiveLockScreenSettings();
            } else {
                if (MODIFIES_LIVE_LOCK_SCREEN.equals(component)) {
                    loaderId = LOADER_ID_LIVE_LOCK_SCREEN;
                } else {
                    loaderId = LOADER_ID_LOCKSCREEN;
                }
            }
        } else if (MODIFIES_OVERLAYS.equals(component)) {
            loaderId = LOADER_ID_STYLE;
        } else if (MODIFIES_BOOT_ANIM.equals(component)) {
            loaderId = LOADER_ID_BOOT_ANIMATION;
        } else if (MODIFIES_RINGTONES.equals(component)) {
            loaderId = LOADER_ID_RINGTONE;
        } else if (MODIFIES_NOTIFICATIONS.equals(component)) {
            loaderId = LOADER_ID_NOTIFICATION;
        } else if (MODIFIES_ALARMS.equals(component)) {
            loaderId = LOADER_ID_ALARM;
        } else {
            return;
        }

        if (loaderId != LOADER_ID_INVALID) {
            getLoaderManager().restartLoader(loaderId, args, ThemeFragment.this);
        }
    }

    private OnItemClickedListener mOnComponentItemClicked = new OnItemClickedListener() {
        @Override
        public void onItemClicked(String pkgName, long componentId, Bundle params) {
            String component = mSelector.getComponentType();
            if (MODIFIES_LOCKSCREEN.equals(component) && params != null) {
                boolean isLiveLockView = params.getBoolean(
                        ComponentSelector.IS_LIVE_LOCK_SCREEN_VIEW,false);
                if (isLiveLockView) {
                    //We got here because an live lock thubmnail view was clicked. We need to
                    //replace the component to load the proper data from the provider.
                    component = MODIFIES_LIVE_LOCK_SCREEN;
                }
            }
            loadComponentFromPackage(pkgName, component, componentId);
        }
    };

    private void fadeOutNonSelectedCards(int selectedCardId) {
        for (int i = 0; i < mCardIdsToComponentTypes.size(); i++) {
            if (mCardIdsToComponentTypes.keyAt(i) != selectedCardId) {
                ComponentCardView card = (ComponentCardView) getView().findViewById(
                        mCardIdsToComponentTypes.keyAt(i));
                if (card != null) card.animateCardFadeOut();
            }
        }
    }

    protected void animateContentChange(int parentId, View viewToAnimate, Drawable overlay) {
        ((ComponentCardView) getView().findViewById(parentId))
                .animateContentChange(viewToAnimate, overlay, ANIMATE_COMPONENT_CHANGE_DURATION);
    }

    private Runnable mApplyThemeRunnable = new Runnable() {
        @Override
        public void run() {
            final Context context = getActivity();
            if (context != null) {
                // Post this on mHandler so the client is added and removed from the same
                // thread
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        final Map<String, String> componentsToApply = getComponentsToApply();
                        if (componentsToApply != null && componentsToApply.size() > 0) {
                            final Map<String, String> fullMap
                                    = fillMissingComponentsWithDefault(componentsToApply);
                            ThemeManager tm = getThemeManager();
                            if (tm != null) {
                                try {
                                    tm.addClient(ThemeFragment.this);
                                } catch (IllegalArgumentException e) {
                                    /* ignore since this means we already have a listener added */
                                }
                                ThemeChangeRequest request =
                                        getThemeChangeRequestForComponents(fullMap);
                                boolean value = request.getReqeustType().
                                        equals(RequestType.USER_REQUEST_MIXNMATCH);

                                tm.requestThemeChange(request, !value);
                            }
                            mApplyThemeOnPopulated = false;
                        } else {
                            onFinish(true);
                        }
                    }
                });
            }
        }
    };

    protected Map<String, String> fillMissingComponentsWithDefault(
            Map<String, String> originalMap) {
        HashMap newMap = new HashMap<String, String>();
        newMap.putAll(originalMap);
        Map<String, String> defaultMap = getEmptyComponentsMap();
        for(Map.Entry<String, String> entry : defaultMap.entrySet()) {
            String component = entry.getKey();
            String defaultPkg = entry.getValue();
            if (!newMap.containsKey(component)) {
                newMap.put(component, defaultPkg);
            }
        }
        return newMap;
    }

    protected Map<String, String> getEmptyComponentsMap() {
        List<String> componentsList = ThemeUtils.getAllComponents();
        Map<String, String> defaultMap = new HashMap<>(componentsList.size());
        for (String component : componentsList) {
            defaultMap.put(component, "");
        }
        return defaultMap;
    }

    /**
     * This is the method that will be called when applying a theme and the idea is to override
     * it in MyThemeFragment and pass in a different RequestType, once we have a type that indicates
     * the user is mixing and matching instead of applying an entire theme.
     * @param componentMap
     * @return
     */
    protected ThemeChangeRequest getThemeChangeRequestForComponents(
            Map<String, String> componentMap) {
        return getThemeChangeRequestForComponents(componentMap, RequestType.USER_REQUEST);
    }

    protected ThemeChangeRequest getThemeChangeRequestForComponents(
            Map<String, String> componentMap, RequestType requestType) {
        ThemeChangeRequest.Builder builder = new ThemeChangeRequest.Builder();
        for (String component : componentMap.keySet()) {
            builder.setComponent(component, componentMap.get(component));
        }
        builder.setRequestType(requestType);
        if (mThemeVersion >= 3)  {
            builder.setWallpaperId(mSelectedWallpaperComponentId != null
                    ? mSelectedWallpaperComponentId
                    : DEFAULT_COMPONENT_ID);
        }
        return builder.build();
    }

    protected Map<String, String> getComponentsToApply() {
        return mSelectedComponentsMap;
    }

    private Runnable mApplyExternalWallpaperRunnable = new Runnable() {
        @Override
        public void run() {
            // If an external image was selected for the wallpaper, we need to
            // set that manually.
            if (mExternalWallpaperUri != null) {
                WallpaperManager wm =
                        WallpaperManager.getInstance(getActivity());
                final Context context = getActivity();
                final Resources res = context.getResources();
                final Point size = new Point(wm.getDesiredMinimumWidth(),
                        wm.getDesiredMinimumHeight());
                Bitmap bmp = WallpaperUtils.createPreview(size, context, mExternalWallpaperUri,
                        null, res, 0, 0, false);
                try {
                    wm.setBitmap(bmp);
                } catch (Exception e) {
                    Log.e(TAG, "Unable to set external wallpaper", e);
                }
            }
        }
    };

    private Runnable mApplyExternalLockscreenRunnable = new Runnable() {
        @Override
        public void run() {
            // If an external image was selected for the wallpaper, we need to
            // set that manually.
            if (mExternalLockscreenUri != null) {
                WallpaperManager wm =
                        WallpaperManager.getInstance(getActivity());
                final Context context = getActivity();
                final Resources res = context.getResources();
                final Point size = new Point();
                ((Activity) context).getWindowManager().getDefaultDisplay().getRealSize(size);
                Bitmap bmp = WallpaperUtils.createPreview(size, context, mExternalLockscreenUri,
                        null, res, 0, 0, false);
                try {
                    wm.setKeyguardBitmap(bmp);
                } catch (Exception e) {
                    Log.e(TAG, "Unable to set external lockscreen wallpaper", e);
                }
            }
        }
    };

    class RestoreLockScreenCardRunnable implements Runnable {

        final private boolean mWasShowingNone;
        private String mCurrentLockScreenPkgName;

        public RestoreLockScreenCardRunnable(boolean w, String pkgName) {
            mWasShowingNone = w;
            mCurrentLockScreenPkgName = pkgName;
        }

        @Override
        public void run() {
            if (!TextUtils.isEmpty(mCurrentLockScreenPkgName)) {
                loadComponentFromPackage(mCurrentLockScreenPkgName,
                        MODIFIES_LOCKSCREEN, 0);
            } else if (mWasShowingNone) {
                mLockScreenCard.setWallpaper(null);
                TextView none = (TextView) mLockScreenCard.findViewById(
                        R.id.none);
                if (none != null) {
                    none.setVisibility(View.VISIBLE);
                }
                mLockScreenCard.setEmptyViewEnabled(false);
            } else {
                mLockScreenCard.clearWallpaper();
                TextView none = (TextView) mLockScreenCard.findViewById(
                        R.id.none);
                if (none != null) {
                    none.setVisibility(View.GONE);
                }
                mLockScreenCard.setEmptyViewEnabled(true);
                setAddComponentTitle(mLockScreenCard, getString(R.string.lockscreen_label));
            }
        }
    }

    protected void applyTheme() {
        if (mExternalWallpaperUri == null && mExternalLockscreenUri == null &&
                (mSelectedComponentsMap == null || mSelectedComponentsMap.size() <= 0)) {
            return;
        }
        final Map<String,String> componentsToApply = getComponentsToApply();
        boolean isLLSEnabled = CMSettings.Secure.getInt(getActivity().getContentResolver(),
                LIVE_LOCK_SCREEN_ENABLED, 0) == 1;
        if (!TextUtils.isEmpty(componentsToApply.get(MODIFIES_LIVE_LOCK_SCREEN)) && !isLLSEnabled) {
            AlertDialog d = new AlertDialog.Builder(getActivity(),
                    android.R.style.Theme_Material_Dialog)
                    .setTitle(R.string.enable_live_lock_screen_dialog_title)
                    .setMessage(R.string.enable_live_lock_screen_dialog_message)
                    .setPositiveButton(R.string.enable_live_lock_screen_dialog_positive_btn_text,
                            new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            CMSettings.Secure.putInt(getActivity().getContentResolver(),
                                    LIVE_LOCK_SCREEN_ENABLED, 1);
                            getChooserActivity().themeChangeStart();
                            animateProgressIn(mApplyThemeRunnable);
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mSelectedComponentsMap.remove(MODIFIES_LIVE_LOCK_SCREEN);
                            boolean wasNone = false;
                            if (TextUtils.equals("", mSelectedComponentsMap.get(
                                    MODIFIES_LOCKSCREEN))) {
                                if (!TextUtils.isEmpty(mCurrentTheme.get(MODIFIES_LOCKSCREEN))) {
                                    //The map entry was set to empty string because there's a
                                    //lockscreen currently applied and setting this entry to empty
                                    //would instruct the ThemeManager to clear the currently applied
                                    //lockscreen, but the user decided not to enable LLS so we need
                                    //to abort this change
                                    mSelectedComponentsMap.put(MODIFIES_LOCKSCREEN,
                                            mCurrentTheme.get(MODIFIES_LOCKSCREEN));
                                } else {
                                    wasNone = true;
                                }
                            }
                            //Restore the lockscreen card to its previous state
                            mHandler.post(new RestoreLockScreenCardRunnable(wasNone,
                                    mCurrentTheme.get(MODIFIES_LOCKSCREEN)));

                            //Did the user make more changes?
                            boolean modLLS = componentsToApply.containsKey(
                                    MODIFIES_LIVE_LOCK_SCREEN);
                            boolean modLockscreen = componentsToApply.containsKey(
                                    MODIFIES_LOCKSCREEN);
                            if (modLLS && ((modLockscreen && componentsToApply.size() > 2) ||
                                    (!modLockscreen && componentsToApply.size() > 1))) {
                                getChooserActivity().themeChangeStart();
                                animateProgressIn(mApplyThemeRunnable);
                            }
                        }
                    })
                    .setCancelable(false)
                    .create();
            d.setCanceledOnTouchOutside(false);
            d.show();
        } else {
            getChooserActivity().themeChangeStart();
            animateProgressIn(mApplyThemeRunnable);
        }
    }

    /**
     * Use when applyTheme() might be too early. ie mSelectedComponentsMap is not pop. yet
     * @param pkgName Only used in MyThemeFragment to apply components on top of current theme
     * @param components Optional list of components to apply.
     */
    protected void applyThemeWhenPopulated(String pkgName, List<String> components) {
        mApplyThemeOnPopulated = true;
    }

    private void animateProgressIn(Runnable endAction) {
        mProgress.setVisibility(View.VISIBLE);
        mProgress.setProgress(0);
        float pivotX = mTitleLayout.getWidth() -
                getResources().getDimensionPixelSize(R.dimen.apply_progress_padding);
        ScaleAnimation scaleAnim = new ScaleAnimation(0f, 1f, 1f, 1f,
                pivotX, 0f);
        scaleAnim.setDuration(ANIMATE_PROGRESS_IN_DURATION);

        mTitleLayout.animate()
                .translationXBy(-(pivotX / 3))
                .alpha(0f)
                .setDuration(ANIMATE_TITLE_OUT_DURATION)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(endAction).start();
        mProgress.startAnimation(scaleAnim);
    }

    private void animateProgressOut() {
        mProgress.setVisibility(View.VISIBLE);
        float pivotX = mTitleLayout.getWidth() -
                getResources().getDimensionPixelSize(R.dimen.apply_progress_padding);
        ScaleAnimation scaleAnim = new ScaleAnimation(1f, 0f, 1f, 1f,
                pivotX, 0f);
        scaleAnim.setDuration(ANIMATE_PROGRESS_OUT_DURATION);
        scaleAnim.setFillAfter(false);
        scaleAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mProgress.setVisibility(View.GONE);
                if (mThemeResetting) {
                    mThemeResetting = false;
                    mThemeTagLayout.setCustomizedTagEnabled(false);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        mTitleLayout.animate()
                .translationXBy((pivotX / 3))
                .alpha(1f)
                .setDuration(ANIMATE_TITLE_IN_DURATION)
                .setInterpolator(new AccelerateInterpolator())
                .start();
        mProgress.startAnimation(scaleAnim);
    }

    private void animateContentIn() {
        if (mSkipLoadingAnim) {
            return;
        }
        AnimatorSet set = new AnimatorSet();
        set.setDuration(ANIMATE_TITLE_IN_DURATION);
        set.play(ObjectAnimator.ofFloat(mLoadingView, "alpha", 1f, 0f))
                .with(ObjectAnimator.ofFloat(mTitleLayout, "alpha", 0f, 1f));
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mLoadingView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        set.start();
    }

    public boolean isShowingConfirmCancelOverlay() {
        return mConfirmCancelOverlay.getVisibility() == View.VISIBLE;
    }

    public void showApplyThemeOverlay() {
        if (mConfirmCancelOverlay.getVisibility() == View.VISIBLE) return;
        mConfirmCancelOverlay.setTitle(R.string.apply_theme_overlay_title);
        mConfirmCancelOverlay.setBackgroundColor(getActivity().getResources()
                .getColor(R.color.apply_overlay_background));
        mConfirmCancelOverlay.setOnOverlayDismissedListener(mApplyCancelListener);
        getChooserActivity().lockPager();
        ViewPropertyAnimator anim = mConfirmCancelOverlay.animate();
        mConfirmCancelOverlay.setVisibility(View.VISIBLE);
        mConfirmCancelOverlay.setAlpha(0f);
        anim.setListener(null);
        anim.setDuration(ANIMATE_APPLY_LAYOUT_DURATION);
        anim.alpha(1f).start();

        if (mIsLegacyTheme) {
            // Display cm11 theme warning message
            TextView tv = (TextView) mConfirmCancelOverlay.findViewById(R.id.warning_message);
            tv.setVisibility(View.VISIBLE);
            tv.setText(String.format(getString(R.string.legacy_theme_warning), mTitle.getText()));
        } else if (Utils.hasPerAppThemesApplied(getActivity())) {
            // Display per app theme changes will be removed warning
            TextView tv = (TextView) mConfirmCancelOverlay.findViewById(R.id.warning_message);
            tv.setVisibility(View.VISIBLE);
            tv.setText(String.format(getString(R.string.per_app_theme_removal_warning),
                    mTitle.getText()));
        }
        mClickableView.setSoundEffectsEnabled(false);
    }

    public void showDeleteThemeOverlay() {
        if (mConfirmCancelOverlay.getVisibility() == View.VISIBLE) return;
        mConfirmCancelOverlay.setTitle(R.string.delete_theme_overlay_title);
        mConfirmCancelOverlay.setBackgroundColor(getActivity().getResources()
                .getColor(R.color.delete_overlay_background));
        mConfirmCancelOverlay.setOnOverlayDismissedListener(mDeleteConfirmationListener);
        getChooserActivity().lockPager();
        ViewPropertyAnimator anim = mConfirmCancelOverlay.animate();
        mConfirmCancelOverlay.setVisibility(View.VISIBLE);
        mConfirmCancelOverlay.setAlpha(0f);
        anim.setListener(null);
        anim.setDuration(ANIMATE_APPLY_LAYOUT_DURATION);
        anim.alpha(1f).start();

        mClickableView.setSoundEffectsEnabled(false);
    }

    public void showResetThemeOverlay() {
        if (mConfirmCancelOverlay.getVisibility() == View.VISIBLE) return;
        mConfirmCancelOverlay.setTitle(R.string.reset_theme_overlay_title);
        mConfirmCancelOverlay.setBackgroundColor(getActivity().getResources()
                .getColor(R.color.apply_overlay_background));
        mConfirmCancelOverlay.setOnOverlayDismissedListener(mResetConfirmationListener);
        getChooserActivity().lockPager();
        ViewPropertyAnimator anim = mConfirmCancelOverlay.animate();
        mConfirmCancelOverlay.setVisibility(View.VISIBLE);
        mConfirmCancelOverlay.setAlpha(0f);
        anim.setListener(null);
        anim.setDuration(ANIMATE_APPLY_LAYOUT_DURATION);
        anim.alpha(1f).start();

        mClickableView.setSoundEffectsEnabled(false);
    }

    public void hideConfirmCancelOverlay() {
        hideConfirmCancelOverlay(false);
    }

    /**
     * Hides the apply theme layout overlay and can apply the selected theme
     * when the animation is finished.
     * @param applyThemeWhenFinished If true, the current theme will be applied.
     */
    private void hideConfirmCancelOverlay(final boolean applyThemeWhenFinished) {
        getChooserActivity().unlockPager();
        ViewPropertyAnimator anim = mConfirmCancelOverlay.animate();
        mConfirmCancelOverlay.setVisibility(View.VISIBLE);
        anim.setDuration(ANIMATE_APPLY_LAYOUT_DURATION);
        anim.alpha(0f).start();
        anim.setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mConfirmCancelOverlay.setVisibility(View.GONE);
                if (applyThemeWhenFinished) applyTheme();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });

        mClickableView.setSoundEffectsEnabled(true);
    }

    public boolean isShowingCustomizeResetLayout() {
        return mCustomizeResetLayout.getVisibility() == View.VISIBLE;
    }

    public void showCustomizeResetLayout() {
        if (mCustomizeResetLayout.getVisibility() == View.VISIBLE) return;
        if (!mThemeTagLayout.isCustomizedTagEnabled()) {
            mResetButton.setEnabled(false);
        } else {
            mResetButton.setEnabled(true);
        }
        getChooserActivity().lockPager();
        ViewPropertyAnimator anim = mCustomizeResetLayout.animate();
        mCustomizeResetLayout.setVisibility(View.VISIBLE);
        mCustomizeResetLayout.setAlpha(0f);
        anim.setListener(null);
        anim.setDuration(ANIMATE_APPLY_LAYOUT_DURATION);
        anim.alpha(1f).start();

        mClickableView.setSoundEffectsEnabled(false);
    }

    public void hideCustomizeResetLayout() {
        hideCustomizeResetLayout(CustomizeResetAction.Dismiss);
    }

    private void hideCustomizeResetLayout(final CustomizeResetAction action) {
        getChooserActivity().unlockPager();
        ViewPropertyAnimator anim = mCustomizeResetLayout.animate();
        mCustomizeResetLayout.setVisibility(View.VISIBLE);
        anim.setDuration(ANIMATE_APPLY_LAYOUT_DURATION);
        anim.alpha(0f).start();
        anim.setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mCustomizeResetLayout.setVisibility(View.GONE);
                switch (action) {
                    case Customize:
                        getChooserActivity().expand();
                        break;
                    case Reset:
                        resetTheme();
                        break;
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });

        mClickableView.setSoundEffectsEnabled(true);
    }

    public void showThemeTagLayout() {
        mThemeTagLayout.setVisibility(View.VISIBLE);
        mThemeTagLayout.animate().alpha(1f).setStartDelay(ANIMATE_START_DELAY).start();
    }

    public void hideThemeTagLayout() {
        mThemeTagLayout.setAlpha(0f);
        mThemeTagLayout.setVisibility(View.GONE);
    }

    public void hideProcessingOverlay() {
        mProcessingThemeLayout.animate().alpha(0).withEndAction(new Runnable() {
            @Override
            public void run() {
                mProcessingThemeLayout.setVisibility(View.GONE);
            }
        }).setDuration(ANIMATE_APPLY_LAYOUT_DURATION).start();

        mClickableView.setSoundEffectsEnabled(true);
    }

    public void fadeInCards() {
        for (int i = 0; i < mCardIdsToComponentTypes.size(); i++) {
            final int key = mCardIdsToComponentTypes.keyAt(i);
            if (key != mActiveCardId) {
                ComponentCardView card = (ComponentCardView) getView().findViewById(key);
                if (card != null) card.animateCardFadeIn();
            }
        }
        mActiveCardId = -1;
    }

    public boolean componentsChanged() {
        // If an external wallpaper/ls are set then something changed!
        if (mExternalWallpaperUri != null || mExternalLockscreenUri != null) return true;

        for (String key : mSelectedComponentsMap.keySet()) {
            if (!mPkgName.equals(mSelectedComponentsMap.get(key))) {
                return true;
            }
            if (ThemesColumns.MODIFIES_LAUNCHER.equals(key) &&
                    mCurrentWallpaperComponentId.value != mSelectedWallpaperComponentId) {
                return true;
            }
        }
        return false;
    }

    protected boolean isThemeCustomized() {
        final String themePkgName = getThemePackageName();
        for (String key : mSelectedComponentsMap.keySet()) {
            final String selectedPkgName = mSelectedComponentsMap.get(key);
            if (!themePkgName.equals(selectedPkgName)) {
                return true;
            }
            if (mBaseThemeSupportedComponents.size() > 0 &&
                    !mBaseThemeSupportedComponents.contains(key)) {
                return true;
            }
        }
        // finally check if we're missing anything from mBaseThemeSupportedComponents
        for (String component : mBaseThemeSupportedComponents) {
            if (!mSelectedComponentsMap.containsKey(component)) return true;
        }
        return false;
    }

    public void clearChanges() {
        mSelectedComponentsMap.clear();
        mExternalWallpaperUri = null;
        mExternalLockscreenUri = null;
        View none = mLockScreenCard.findViewById(R.id.none);
        if (none != null && none.getVisibility() == View.VISIBLE) {
            none.setVisibility(View.GONE);
        }
        TextView tv = (TextView) mLockScreenCard.findViewById(R.id.label);
        if (tv != null) {
            tv.setAlpha(1f);
            tv.setBackgroundResource(R.drawable.wallpaper_label_bg);
        }
        getLoaderManager().restartLoader(LOADER_ID_ALL, null, ThemeFragment.this);
    }

    public String getThemePackageName() {
        if (mPkgName == null) {
            // check if the package name is defined in the arguments bundle
            Bundle bundle = getArguments();
            if (bundle != null) {
                mPkgName = bundle.getString(ARG_PACKAGE_NAME);
            }
        }
        return mPkgName;
    }

    private void uninstallTheme() {
        getChooserActivity().uninstallTheme(mPkgName);
    }

    public void setCurrentTheme(Map<String, String> currentTheme,
            MutableLong currentWallpaperComponentId) {
        mCurrentTheme = currentTheme;
        mCurrentWallpaperComponentId = currentWallpaperComponentId;
    }

    /**
     * Slides the scrollview content up and adds a space view at the bottom
     * of mAdditionalCards so all content can be visible above the selector.
     *
     * We are using a ValueAnimator here to scroll the content rather than calling
     * mScrollView.smoothScrollBy() since the speed of that animation cannot be customized.
     * @param yDelta
     * @param selectorHeight
     */
    public void slideContentIntoView(final int yDelta, int selectorHeight) {
        Space space = (Space) mAdditionalCards.findViewById(ADDITIONAL_CONTENT_SPACE_ID);
        if (space == null) {
            // No space view yet so lets create it one
            space = new Space(getActivity());
            space.setId(ADDITIONAL_CONTENT_SPACE_ID);
            mAdditionalCards.addView(space,
                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                            selectorHeight));
        } else {
            // Space view already exists so just update the LayoutParams
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) space.getLayoutParams();
            params.height = selectorHeight;
            space.setLayoutParams(params);
        }
        final int startY = mScrollView.getScrollY();
        final ValueAnimator scrollAnimator =
                ValueAnimator.ofInt(startY, startY + yDelta);
        scrollAnimator.setDuration(SLIDE_CONTENT_ANIM_DURATION);
        scrollAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (Integer) animation.getAnimatedValue();
                mScrollView.scrollTo(0, value);
            }
        });
        scrollAnimator.start();
    }

    public Map<String, String> getSelectedComponentsMap() {
        return mSelectedComponentsMap;
    }

    /**
     * Slides the scrollview content down and removes a space view at the bottom
     * of mAdditionalCards.
     *
     * We are using a ValueAnimator here to scroll the content rather than calling
     * mScrollView.smoothScrollBy() since the speed of that animation cannot be customized.
     * @param yDelta
     */
    public void slideContentBack(int yDelta) {
        final int startY = mScrollView.getScrollY();
        final ValueAnimator scrollAnimator =
                ValueAnimator.ofInt(startY, startY + yDelta);
        scrollAnimator.setDuration(SLIDE_CONTENT_ANIM_DURATION);
        scrollAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (Integer) animation.getAnimatedValue();
                mScrollView.scrollTo(0, value);
            }
        });
        scrollAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                View space = mAdditionalCards.findViewById(ADDITIONAL_CONTENT_SPACE_ID);
                if (space != null) mAdditionalCards.removeView(space);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        scrollAnimator.start();
    }

    public void showLockScreenCard() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                final int scrollToY = mStatusBarCard.getMeasuredHeight()
                        + mFontCard.getMeasuredHeight() + mIconCard.getMeasuredHeight()
                        + mNavBarCard.getMeasuredHeight() + mWallpaperCard.getMeasuredHeight() / 2;
                final ValueAnimator scrollAnimator = ValueAnimator.ofInt(0, scrollToY);
                scrollAnimator.setDuration(LOCK_SCREEN_CARD_SCROLL_ANIMATION_DURATION);
                scrollAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        int value = (Integer) animation.getAnimatedValue();
                        mScrollView.scrollTo(0, value);
                    }
                });
                scrollAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mCardClickListener.onClick(mLockScreenCard);
                    }
                });
                scrollAnimator.start();
            }
        }, SHOW_LOCK_SCREEN_CARD_DELAY);
    }

    protected void startLiveLockScreenSettings() {
        Intent intent = new Intent(cyanogenmod.content.Intent.ACTION_OPEN_LIVE_LOCKSCREEN_SETTINGS);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // TODO: inform user that this action failed (Toast?)
        }
    }

    class AnimationLoader extends AsyncTask<Void, Void, Boolean> {
        Context mContext;
        String mPkgName;
        BootAniImageView mBootAnim;

        public AnimationLoader(Context context, String pkgName, BootAniImageView bootAnim) {
            mContext = context;
            mPkgName = pkgName;
            mBootAnim = bootAnim;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (mContext == null) {
                return Boolean.FALSE;
            }
            ZipFile zip = null;
            if (ThemeConfig.SYSTEM_DEFAULT.equals(mPkgName)) {
                try {
                    zip = new ZipFile(new File(BootAnimationHelper.SYSTEM_BOOT_ANI_PATH));
                } catch (Exception e) {
                    Log.w(TAG, "Unable to load boot animation", e);
                    return Boolean.FALSE;
                }
            } else {
                // check if the bootanimation is cached
                File f = new File(mContext.getCacheDir(),
                        mPkgName + BootAnimationHelper.CACHED_SUFFIX);
                if (!f.exists()) {
                    // go easy on cache storage and clear out any previous boot animations
                    BootAnimationHelper.clearBootAnimationCache(mContext);
                    try {
                        Context themeContext = mContext.createPackageContext(mPkgName, 0);
                        AssetManager am = themeContext.getAssets();
                        InputStream is = am.open("bootanimation/bootanimation.zip");
                        FileUtils.copyToFile(is, f);
                        is.close();
                    } catch (Exception e) {
                        Log.w(TAG, "Unable to load boot animation", e);
                        return Boolean.FALSE;
                    }
                }
                try {
                    zip = new ZipFile(f);
                } catch (IOException e) {
                    Log.w(TAG, "Unable to load boot animation", e);
                    return Boolean.FALSE;
                }
            }
            if (zip != null) {
                mBootAnim.setBootAnimation(zip);
            } else {
                return Boolean.FALSE;
            }
            return Boolean.TRUE;
        }

        @Override
        protected void onPostExecute(Boolean isSuccess) {
            super.onPostExecute(isSuccess);
            if (isSuccess) {
                mBootAnim.start();
            }
        }
    }

    class AudibleLoadingThread extends Thread {
        private Context mContext;
        private int mType;
        private String mPkgName;
        private MediaPlayer mPlayer;

        public AudibleLoadingThread(Context context, int type, String pkgName, MediaPlayer mp) {
            super();
            mContext = context;
            mType = type;
            mPkgName = pkgName;
            mPlayer = mp;
        }

        @Override
        public void run() {
            try {
                AudioUtils.loadThemeAudible(mContext, mType, mPkgName, mPlayer);
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "Unable to load sound for " + mPkgName, e);
            }
        }
    }
}
