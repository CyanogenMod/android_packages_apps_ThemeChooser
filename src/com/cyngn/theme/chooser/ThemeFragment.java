/*
 * Copyright (C) 2014 The Cyanogen, Inc
 */
package com.cyngn.theme.chooser;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.IntEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ThemeUtils;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.ThemeConfig;
import android.content.res.ThemeManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
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
import android.os.Bundle;
import android.os.FileUtils;
import android.os.Handler;
import android.provider.Settings;
import android.provider.ThemesContract;
import android.provider.ThemesContract.PreviewColumns;
import android.provider.ThemesContract.ThemesColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
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

import com.cyngn.theme.chooser.ComponentSelector.OnItemClickedListener;
import com.cyngn.theme.util.AudioUtils;
import com.cyngn.theme.util.BootAnimationHelper;
import com.cyngn.theme.util.IconPreviewHelper;
import com.cyngn.theme.util.ThemedTypefaceHelper;
import com.cyngn.theme.util.TypefaceHelperCache;
import com.cyngn.theme.util.Utils;
import com.cyngn.theme.widget.BootAniImageView;
import com.cyngn.theme.widget.LockableScrollView;
import com.cyngn.theme.widget.ThemeTagLayout;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

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

public class ThemeFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        ThemeManager.ThemeChangeListener {
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
    public static final int REQUEST_UNINSTALL = 1; // Request code

    public static final String CURRENTLY_APPLIED_THEME = "currently_applied_theme";

    private static final ComponentName COMPONENT_DIALER =
            new ComponentName("com.android.dialer", "com.android.dialer.DialtactsActivity");
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

    private static final int ADDITIONAL_CONTENT_SPACE_ID = 123456;
    private static final long SLIDE_CONTENT_ANIM_DURATION = 300L;

    protected static final int LOADER_ID_ALL = 0;
    protected static final int LOADER_ID_STATUS_BAR = 1;
    protected static final int LOADER_ID_FONT = 2;
    protected static final int LOADER_ID_ICONS = 3;
    protected static final int LOADER_ID_WALLPAPER = 4;
    protected static final int LOADER_ID_NAVIGATION_BAR = 5;
    protected static final int LOADER_ID_LOCKSCREEN = 6;
    protected static final int LOADER_ID_STYLE = 7;
    protected static final int LOADER_ID_BOOT_ANIMATION = 8;
    protected static final int LOADER_ID_RINGTONE = 9;
    protected static final int LOADER_ID_NOTIFICATION = 10;
    protected static final int LOADER_ID_ALARM = 11;

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
    protected ViewGroup mStyleContainer;
    protected ComponentCardView mFontCard;
    protected ViewGroup mIconContainer;
    protected ViewGroup mBootAnimationContainer;
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
    protected ImageView mCustomize;
    protected ImageView mOverflow;
    protected ProgressBar mProgress;

    // Additional Card Views
    protected LinearLayout mAdditionalCards;
    protected WallpaperCardView mWallpaperCard;
    protected WallpaperCardView mLockScreenCard;

    // Style views
    protected ImageView mStylePreview;

    // Sound cards
    protected ViewGroup mRingtoneContainer;
    protected ImageView mRingtonePlayPause;
    protected ViewGroup mNotificationContainer;
    protected ImageView mNotificationPlayPause;
    protected ViewGroup mAlarmContainer;
    protected ImageView mAlarmPlayPause;
    protected Map<ImageView, MediaPlayer> mMediaPlayers;

    protected Handler mHandler;

    protected boolean mIsUninstalled;
    protected int mActiveCardId = -1;
    protected ComponentSelector mSelector;
    // Supported components for the theme this fragment represents
    protected Map<String, String> mSelectedComponentsMap = new HashMap<String, String>();
    // Current system theme configuration as component -> pkgName
    protected Map<String, String> mCurrentTheme = new HashMap<String, String>();
    protected Cursor mCurrentCursor;
    protected int mCurrentLoaderId;

    protected View mApplyThemeLayout;
    protected View mApplyButton;
    protected View mCancelButton;

    protected ThemeTagLayout mThemeTagLayout;

    static ThemeFragment newInstance(String pkgName) {
        ThemeFragment f = new ThemeFragment();
        Bundle args = new Bundle();
        args.putString("pkgName", pkgName);
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
        mPkgName = getArguments().getString("pkgName");
        mBatteryStyle = Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY, 0);

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
        mScrollContent = (ViewGroup) mScrollView.getChildAt(0);
        mPreviewContent = (ViewGroup) v.findViewById(R.id.preview_container);
        mLoadingView = v.findViewById(R.id.loading_view);

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
        mIconContainer = (ViewGroup) v.findViewById(R.id.icon_container);
        mShadowFrame = (FrameLayout) v.findViewById(R.id.shadow_frame);
        mStyleContainer = (ViewGroup) v.findViewById(R.id.style_card);
        mStylePreview = (ImageView) v.findViewById(R.id.style_preview);
        mBootAnimationContainer = (ViewGroup) v.findViewById(R.id.bootani_preview_container);
        mBootAnimation =
                (BootAniImageView) mBootAnimationContainer.findViewById(R.id.bootani_preview);
        mRingtoneContainer = (ViewGroup) v.findViewById(R.id.ringtone_preview_container);
        mRingtonePlayPause = (ImageView) mRingtoneContainer.findViewById(R.id.play_pause);
        mNotificationContainer = (ViewGroup) v.findViewById(R.id.notification_preview_container);
        mNotificationPlayPause = (ImageView) mNotificationContainer.findViewById(R.id.play_pause);
        mAlarmContainer = (ViewGroup) v.findViewById(R.id.alarm_preview_container);
        mAlarmPlayPause = (ImageView) mAlarmContainer.findViewById(R.id.play_pause);

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
        mProgress = (ProgressBar) v.findViewById(R.id.apply_progress);
        mOverflow = (ImageView) v.findViewById(R.id.overflow);
        mOverflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupmenu = new PopupMenu(getActivity(), mTitleCard, Gravity.END);
                popupmenu.getMenuInflater().inflate(R.menu.overflow, popupmenu.getMenu());

                if (CURRENTLY_APPLIED_THEME.equals(mPkgName) ||
                        mPkgName.equals(ThemeUtils.getDefaultThemePackageName(getActivity())) ||
                        mPkgName.equals(ThemeConfig.HOLO_DEFAULT)) {
                    Menu menu = popupmenu.getMenu();
                    menu.findItem(R.id.menu_delete).setEnabled(false);
                }

                popupmenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch(item.getItemId()) {
                            case R.id.menu_author:
                                Toast.makeText(getActivity(),
                                        "Not supported",
                                        Toast.LENGTH_LONG).show();
                                break;
                            case R.id.menu_delete:
                                uninstallTheme();
                                break;
                        }

                        return true;
                    }
                });
                popupmenu.show();
            }
        });
        mCustomize = (ImageView) v.findViewById(R.id.customize);
        mCustomize.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!isShowingApplyThemeLayout()) {
                    ((ChooserActivity) getActivity()).expand();
                }
            }
        });

        // Additional cards which should hang out offscreen until expanded
        mAdditionalCards = (LinearLayout) v.findViewById(R.id.additional_cards);

        mWallpaperCard = (WallpaperCardView) v.findViewById(R.id.wallpaper_card);
        mLockScreenCard = (WallpaperCardView) v.findViewById(R.id.lockscreen_card);
        int translationY = getDistanceToMoveBelowScreen(mAdditionalCards);
        mAdditionalCards.setTranslationY(translationY);

        mApplyThemeLayout = v.findViewById(R.id.apply_theme_layout);
        mApplyButton = mApplyThemeLayout.findViewById(R.id.apply_apply);
        mApplyButton.setOnClickListener(mApplyCancelClickListener);
        mCancelButton = mApplyThemeLayout.findViewById(R.id.apply_cancel);
        mCancelButton.setOnClickListener(mApplyCancelClickListener);

        mThemeTagLayout = (ThemeTagLayout) v.findViewById(R.id.tag_layout);

        if (mPkgName.equals(ThemeUtils.getDefaultThemePackageName(getActivity()))) {
            mThemeTagLayout.setDefaultTagEnabled(true);
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
    public void onDestroy() {
        super.onDestroy();
        freeMediaPlayers();
    }

    @Override
    public void onProgress(int progress) {
        mProgress.setProgress(progress);
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
            mProgress.setProgress(100);
            animateProgressOut();
            ((ChooserActivity) getActivity()).themeChangeEnded();
        }
    }

    public void expand() {
        if (mCurrentLoaderId == LOADER_ID_ALL && mCurrentCursor != null) {
            loadAndRemoveAdditionalCards(mCurrentCursor);
            // we don't need this now that the additional cards are loaded, and
            // we don't want to re-load these cards if the we expand again.
            mCurrentCursor = null;
        }
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
        mIconContainer.setPadding(padding.left, padding.top, padding.right, padding.bottom);
        mShadowFrame.setBackground(null);
        mShadowFrame.setPadding(0, 0, 0, 0);
        mAdditionalCards.setPadding(padding.left, padding.top, padding.right, padding.bottom);

        // Off screen cards will become visible and then be animated in
        mWallpaperCard.setVisibility(View.VISIBLE);

        // Expand the children
        int top = (int) getResources()
                .getDimension(R.dimen.expanded_card_margin_top);
        for (int i = 0; i < mPreviewContent.getChildCount(); i++) {
            ComponentCardView child = (ComponentCardView) mPreviewContent.getChildAt(i);

            LinearLayout.LayoutParams lparams =
                    (LinearLayout.LayoutParams) child.getLayoutParams();
            lparams.setMargins(0, top, 0, 0);

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
        mSelector = ((ChooserActivity) getActivity()).getComponentSelector();
        mSelector.setOnItemClickedListener(mOnComponentItemClicked);
        if (mBootAnimation != null) mBootAnimation.start();
        hideThemeTagLayout();
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

    public void fadeOutCards(Runnable endAction) {
        for (int i = 0; i < mPreviewContent.getChildCount(); i++) {
            ComponentCardView v = (ComponentCardView) mPreviewContent.getChildAt(i);
            v.animateFadeOut();
        }
        mHandler.postDelayed(endAction, ComponentCardView.CARD_FADE_DURATION);
    }

    public void collapse(final boolean applyTheme) {
        // Pad the  view so it appears thinner
        ViewGroup content = (ViewGroup) mScrollView.getParent();
        Resources r = mScrollView.getContext().getResources();
        int leftRightPadding = (int) r.getDimension(R.dimen.collapsed_theme_page_padding);
        content.setPadding(leftRightPadding, 0, leftRightPadding, 0);

        if (applyTheme && componentsChanged()) {
            mThemeTagLayout.setCustomizedTagEnabled(true);
        }

        //Move the theme preview so that it is near the center of page per spec
        int paddingTop = (int) r.getDimension(R.dimen.collapsed_theme_page_padding_top);
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

        mScrollView.requestLayout();
        animateChildren(false, getChildrensGlobalBounds(mPreviewContent));
        animateExtras(false);
        animateWallpaperIn();
        animateTitleCard(false, applyTheme);
        if (mBootAnimation != null) mBootAnimation.stop();
        stopMediaPlayers();
        showThemeTagLayout();
    }

    // This will animate the children's vertical positions between the previous bounds and the
    // new bounds which occur on the next draw
    private void animateChildren(final boolean isExpanding, final List<Rect> prevBounds) {
        final ViewGroup root = (ViewGroup) getActivity().getWindow()
                .getDecorView().findViewById(android.R.id.content);

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
                    v.setTranslationY((prevY - endY - paddingTop) + (prevHeight - endHeight) / 2);
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
                .getIdentifier("system_bar_height", "dimen", "android");
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

    private ThemeManager getThemeManager() {
        final Context context = getActivity();
        if (context != null) {
            return (ThemeManager) context.getSystemService(Context.THEME_SERVICE);
        }
        return null;
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

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String pkgName = mPkgName;
        if (args != null) {
            pkgName = args.getString("pkgName");
        }
        Uri uri = ThemesContract.PreviewColumns.CONTENT_URI;
        String selection = ThemesContract.ThemesColumns.PKG_NAME + "= ?";
        String[] selectionArgs = new String[] { pkgName };
        String[] projection = null;
        switch (id) {
            case LOADER_ID_ALL:
                projection = new String[] {
                        ThemesColumns.PKG_NAME,
                        ThemesColumns.TITLE,
                        ThemesColumns.WALLPAPER_URI,
                        ThemesColumns.HOMESCREEN_URI,
                        // Theme abilities
                        ThemesColumns.MODIFIES_LAUNCHER,
                        ThemesColumns.MODIFIES_LOCKSCREEN,
                        ThemesColumns.MODIFIES_ALARMS,
                        ThemesColumns.MODIFIES_BOOT_ANIM,
                        ThemesColumns.MODIFIES_FONTS,
                        ThemesColumns.MODIFIES_ICONS,
                        ThemesColumns.MODIFIES_NAVIGATION_BAR,
                        ThemesColumns.MODIFIES_OVERLAYS,
                        ThemesColumns.MODIFIES_RINGTONES,
                        ThemesColumns.MODIFIES_STATUS_BAR,
                        ThemesColumns.MODIFIES_NOTIFICATIONS,
                        //Previews
                        PreviewColumns.WALLPAPER_PREVIEW,
                        PreviewColumns.STATUSBAR_BACKGROUND,
                        PreviewColumns.STATUSBAR_WIFI_ICON,
                        PreviewColumns.STATUSBAR_WIFI_COMBO_MARGIN_END,
                        PreviewColumns.STATUSBAR_BLUETOOTH_ICON,
                        PreviewColumns.STATUSBAR_SIGNAL_ICON,
                        PreviewColumns.STATUSBAR_CLOCK_TEXT_COLOR,
                        PreviewColumns.STATUSBAR_BATTERY_CIRCLE,
                        PreviewColumns.STATUSBAR_BATTERY_LANDSCAPE,
                        PreviewColumns.STATUSBAR_BATTERY_PORTRAIT,
                        PreviewColumns.NAVBAR_BACK_BUTTON,
                        PreviewColumns.NAVBAR_HOME_BUTTON,
                        PreviewColumns.NAVBAR_RECENT_BUTTON,
                        PreviewColumns.ICON_PREVIEW_1,
                        PreviewColumns.ICON_PREVIEW_2,
                        PreviewColumns.ICON_PREVIEW_3,
                        PreviewColumns.LOCK_WALLPAPER_PREVIEW,
                        PreviewColumns.STYLE_PREVIEW
                };
                break;
            case LOADER_ID_STATUS_BAR:
                projection = new String[] {
                        ThemesColumns.PKG_NAME,
                        ThemesColumns.TITLE,
                        PreviewColumns.STATUSBAR_BACKGROUND,
                        PreviewColumns.STATUSBAR_WIFI_ICON,
                        PreviewColumns.STATUSBAR_WIFI_COMBO_MARGIN_END,
                        PreviewColumns.STATUSBAR_BLUETOOTH_ICON,
                        PreviewColumns.STATUSBAR_SIGNAL_ICON,
                        PreviewColumns.STATUSBAR_CLOCK_TEXT_COLOR,
                        PreviewColumns.STATUSBAR_BATTERY_CIRCLE,
                        PreviewColumns.STATUSBAR_BATTERY_LANDSCAPE,
                        PreviewColumns.STATUSBAR_BATTERY_PORTRAIT
                };
                break;
            case LOADER_ID_FONT:
                projection = new String[] {
                        ThemesColumns.PKG_NAME,
                        ThemesColumns.TITLE
                };
                break;
            case LOADER_ID_ICONS:
                projection = new String[] {
                        ThemesColumns.PKG_NAME,
                        ThemesColumns.TITLE,
                        PreviewColumns.ICON_PREVIEW_1,
                        PreviewColumns.ICON_PREVIEW_2,
                        PreviewColumns.ICON_PREVIEW_3,
                        PreviewColumns.ICON_PREVIEW_4
                };
                break;
            case LOADER_ID_WALLPAPER:
                projection = new String[] {
                        ThemesColumns.PKG_NAME,
                        ThemesColumns.TITLE,
                        PreviewColumns.WALLPAPER_PREVIEW
                };
                break;
            case LOADER_ID_NAVIGATION_BAR:
                projection = new String[] {
                        ThemesColumns.PKG_NAME,
                        ThemesColumns.TITLE,
                        PreviewColumns.STATUSBAR_BACKGROUND,
                        PreviewColumns.NAVBAR_BACK_BUTTON,
                        PreviewColumns.NAVBAR_HOME_BUTTON,
                        PreviewColumns.NAVBAR_RECENT_BUTTON
                };
                break;
            case LOADER_ID_LOCKSCREEN:
                projection = new String[]{
                        ThemesColumns.PKG_NAME,
                        ThemesColumns.TITLE,
                        PreviewColumns.LOCK_WALLPAPER_PREVIEW
                };
                break;
            case LOADER_ID_STYLE:
                projection = new String[] {
                        ThemesColumns.PKG_NAME,
                        ThemesColumns.TITLE,
                        PreviewColumns.STYLE_PREVIEW
                };
                break;
            case LOADER_ID_BOOT_ANIMATION:
                projection = new String[] {
                        ThemesColumns.PKG_NAME,
                        ThemesColumns.TITLE
                };
                break;
            case LOADER_ID_RINGTONE:
            case LOADER_ID_NOTIFICATION:
            case LOADER_ID_ALARM:
                projection = new String[] {
                        ThemesColumns.PKG_NAME,
                        ThemesColumns.TITLE
                };
                break;
        }
        return new CursorLoader(getActivity(), uri, projection, selection, selectionArgs, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
        if (c.getCount() == 0) return;
        mCurrentCursor = c;
        mCurrentLoaderId = loader.getId();
        c.moveToFirst();
        switch (mCurrentLoaderId) {
            case LOADER_ID_ALL:
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
                break;
            case LOADER_ID_STATUS_BAR:
                loadStatusBar(c, true);
                break;
            case LOADER_ID_FONT:
                loadFont(c, true);
                break;
            case LOADER_ID_ICONS:
                loadIcons(c, true);
                break;
            case LOADER_ID_WALLPAPER:
                loadWallpaper(c, true);
                break;
            case LOADER_ID_NAVIGATION_BAR:
                loadNavBar(c, true);
                break;
            case LOADER_ID_LOCKSCREEN:
                loadLockScreen(c, true);
                break;
            case LOADER_ID_STYLE:
                loadStyle(c, true);
                break;
            case LOADER_ID_BOOT_ANIMATION:
                loadBootAnimation(c);
                break;
            case LOADER_ID_RINGTONE:
                loadAudible(RingtoneManager.TYPE_RINGTONE, c, true);
                break;
            case LOADER_ID_NOTIFICATION:
                loadAudible(RingtoneManager.TYPE_NOTIFICATION, c, true);
                break;
            case LOADER_ID_ALARM:
                loadAudible(RingtoneManager.TYPE_ALARM, c, true);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {}

    private void loadAndRemoveAdditionalCards(Cursor c) {
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
                mLockScreenCard.setEmptyViewEnabled(true);
            }
        } else if (MODIFIES_LAUNCHER.equals(component)) {
            // this was already loaded so no need to do this again.
        } else if (MODIFIES_OVERLAYS.equals(component)) {
            if (hasContent) {
                loadStyle(c, false);
            } else {
                ((ComponentCardView) mStyleContainer).setEmptyViewEnabled(true);
                setAddComponentTitle((ComponentCardView) mStyleContainer,
                        getString(R.string.style_label));
            }
        } else if (MODIFIES_BOOT_ANIM.equals(component)) {
            if (hasContent) {
                loadBootAnimation(c);
            } else {
                ((ComponentCardView) mBootAnimationContainer).setEmptyViewEnabled(true);
                setAddComponentTitle((ComponentCardView) mStyleContainer,
                        getString(R.string.style_label));
            }
        } else if (MODIFIES_RINGTONES.equals(component)) {
            if (hasContent) {
                loadAudible(RingtoneManager.TYPE_RINGTONE, c, false);
            } else {
                ((ComponentCardView) mRingtoneContainer).setEmptyViewEnabled(true);
                setAddComponentTitle((ComponentCardView) mRingtoneContainer,
                        getAudibleLabel(RingtoneManager.TYPE_RINGTONE));
            }
        } else if (MODIFIES_NOTIFICATIONS.equals(component)) {
            if (hasContent) {
                loadAudible(RingtoneManager.TYPE_NOTIFICATION, c, false);
            } else {
                ((ComponentCardView) mNotificationContainer).setEmptyViewEnabled(true);
                setAddComponentTitle((ComponentCardView) mNotificationContainer,
                        getAudibleLabel(RingtoneManager.TYPE_NOTIFICATION));
            }
        } else if (MODIFIES_ALARMS.equals(component)) {
            if (hasContent) {
                loadAudible(RingtoneManager.TYPE_ALARM, c, false);
            } else {
                ((ComponentCardView) mAlarmContainer).setEmptyViewEnabled(true);
                setAddComponentTitle((ComponentCardView) mAlarmContainer,
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
            boolean supported = c.getInt(modifiesCompIdx) == 1;
            if (supported) {
                mSelectedComponentsMap.put(component, pkg);
            }
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

    protected void loadTitle(Cursor c) {
        int titleIdx = c.getColumnIndex(ThemesColumns.TITLE);
        String title = c.getString(titleIdx);
        mTitle.setText(title);
    }

    protected void loadWallpaper(Cursor c, boolean animate) {
        Drawable overlay = null;
        if (animate) {
            overlay = getOverlayDrawable(mWallpaperCard, true);
        }
        if (mWallpaperCard.isShowingEmptyView()) mWallpaperCard.setEmptyViewEnabled(false);

        int pkgNameIdx = c.getColumnIndex(ThemesColumns.PKG_NAME);
        int wpIdx = c.getColumnIndex(PreviewColumns.WALLPAPER_PREVIEW);
        final Resources res = getResources();
        Bitmap bitmap = Utils.loadBitmapBlob(c, wpIdx);
        mWallpaper.setImageBitmap(bitmap);
        mWallpaperCard.setWallpaper(new BitmapDrawable(res, bitmap));
        String pkgName = c.getString(pkgNameIdx);
        mSelectedComponentsMap.put(MODIFIES_LAUNCHER, pkgName);
        setCardTitle(mWallpaperCard, pkgName, getString(R.string.wallpaper_label));

        if (animate) {
            animateContentChange(R.id.wallpaper_card, mWallpaperCard, overlay);
        }
    }

    protected void loadLockScreen(Cursor c, boolean animate) {
        Drawable overlay = null;
        if (animate) {
            overlay = getOverlayDrawable(mLockScreenCard, true);
        }
        if (mLockScreenCard.isShowingEmptyView()) mLockScreenCard.setEmptyViewEnabled(false);

        int pkgNameIdx = c.getColumnIndex(ThemesColumns.PKG_NAME);
        int wpIdx = c.getColumnIndex(PreviewColumns.LOCK_WALLPAPER_PREVIEW);
        final Resources res = getResources();
        Bitmap bitmap = Utils.loadBitmapBlob(c, wpIdx);
        mLockScreenCard.setWallpaper(new BitmapDrawable(res, bitmap));
        String pkgName = c.getString(pkgNameIdx);
        mSelectedComponentsMap.put(MODIFIES_LOCKSCREEN, pkgName);
        setCardTitle(mLockScreenCard, pkgName, getString(R.string.lockscreen_label));

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
        int wifiMargin = c.getInt(wifiMarginIdx);
        int clockTextColor = c.getInt(clockColorIdx);

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
            mSelectedComponentsMap.put(MODIFIES_STATUS_BAR, pkgName);
            setCardTitle(mStatusBarCard, pkgName,
                    getString(R.string.statusbar_label));
        }
        if (animate) {
            animateContentChange(R.id.status_bar_container, mStatusBar, overlay);
        }
    }

    protected void loadIcons(Cursor c, boolean animate) {
        if (((ComponentCardView) mIconContainer).isShowingEmptyView()) {
            ((ComponentCardView) mIconContainer).setEmptyViewEnabled(false);
        }
        int[] iconIdx = new int[4];
        iconIdx[0] = c.getColumnIndex(PreviewColumns.ICON_PREVIEW_1);
        iconIdx[1] = c.getColumnIndex(PreviewColumns.ICON_PREVIEW_2);
        iconIdx[2] = c.getColumnIndex(PreviewColumns.ICON_PREVIEW_3);
        iconIdx[3] = c.getColumnIndex(PreviewColumns.ICON_PREVIEW_4);
        int pkgNameIdx = c.getColumnIndex(ThemesColumns.PKG_NAME);

        // Set the icons. If the provider does not have an icon preview then
        // fall back to the default icon set
        IconPreviewHelper helper = new IconPreviewHelper(getActivity(), "");
        ViewGroup iconContainer =
                (ViewGroup) mIconContainer.findViewById(R.id.icon_preview_container);
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
            mSelectedComponentsMap.put(MODIFIES_ICONS, pkgName);
            setCardTitle((ComponentCardView) mIconContainer, pkgName,
                    getString(R.string.icon_label));
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
            mSelectedComponentsMap.put(MODIFIES_NAVIGATION_BAR, pkgName);
            setCardTitle(mNavBarCard, pkgName, getString(R.string.navbar_label));
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
        mSelectedComponentsMap.put(MODIFIES_FONTS, pkgName);
        setCardTitle(mFontCard, pkgName, getString(R.string.font_label));

        if (animate) {
            animateContentChange(R.id.font_preview_container, mFontPreview, overlay);
        }
    }

    protected void loadStyle(Cursor c, boolean animate) {
        Drawable overlay = null;
        if (animate) {
            overlay = getOverlayDrawable(mStylePreview, true);
        }
        if (((ComponentCardView)mStyleContainer).isShowingEmptyView()) {
            ((ComponentCardView)mStyleContainer).setEmptyViewEnabled(false);
        }

        int pkgNameIdx = c.getColumnIndex(ThemesColumns.PKG_NAME);
        int styleIdx = c.getColumnIndex(PreviewColumns.STYLE_PREVIEW);
        mStylePreview.setImageBitmap(Utils.loadBitmapBlob(c, styleIdx));
        if (pkgNameIdx > -1) {
            String pkgName = c.getString(pkgNameIdx);
            mSelectedComponentsMap.put(MODIFIES_OVERLAYS, pkgName);
            setCardTitle((ComponentCardView) mStyleContainer, pkgName,
                    getString(R.string.style_label));
        }
        if (animate) {
            animateContentChange(R.id.style_card, mStylePreview, overlay);
        }
    }

    protected void loadBootAnimation(Cursor c) {
        if (((ComponentCardView) mBootAnimationContainer).isShowingEmptyView()) {
            ((ComponentCardView) mBootAnimationContainer).setEmptyViewEnabled(false);
        }
        int pkgNameIdx = c.getColumnIndex(ThemesColumns.PKG_NAME);
        if (mBootAnimation != null) {
            String pkgName;
            if (pkgNameIdx > -1) {
                pkgName = c.getString(pkgNameIdx);
                mSelectedComponentsMap.put(MODIFIES_BOOT_ANIM, pkgName);
                setCardTitle((ComponentCardView) mBootAnimationContainer, pkgName,
                        getString(R.string.boot_animation_label));
            } else {
                pkgName = mCurrentTheme.get(MODIFIES_BOOT_ANIM);
            }
            mBootAnimation.stop();
            new AnimationLoader(getActivity(), pkgName, mBootAnimation).execute();
        }
    }

    protected void loadAudible(int type, Cursor c, boolean animate) {
        View audibleContainer = null;
        ImageView playPause = null;
        String component = null;
        int parentResId = 0;
        switch (type) {
            case RingtoneManager.TYPE_RINGTONE:
                audibleContainer = mRingtoneContainer;
                playPause = mRingtonePlayPause;
                component = MODIFIES_RINGTONES;
                parentResId = R.id.ringtone_preview_container;
                break;
            case RingtoneManager.TYPE_NOTIFICATION:
                audibleContainer = mNotificationContainer;
                playPause = mNotificationPlayPause;
                component = MODIFIES_NOTIFICATIONS;
                parentResId = R.id.notification_preview_container;
                break;
            case RingtoneManager.TYPE_ALARM:
                audibleContainer = mAlarmContainer;
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
        if (((ComponentCardView) audibleContainer).isShowingEmptyView()) {
            ((ComponentCardView) audibleContainer).setEmptyViewEnabled(false);
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
        setCardTitle((ComponentCardView) audibleContainer, pkgName, getAudibleLabel(type));
        try {
            AudioUtils.loadThemeAudible(getActivity(), type, pkgName, mp);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Unable to load sound for " + pkgName, e);
            return;
        }
        title.setText(c.getString(titleIdx));
        mSelectedComponentsMap.put(component, pkgName);

        playPause.setVisibility(View.VISIBLE);
        playPause.setTag(mp);
        mMediaPlayers.put(playPause, mp);
        playPause.setOnClickListener(mPlayPauseClickListener);
        mp.setOnCompletionListener(mPlayCompletionListener);
        if (animate) {
            animateContentChange(parentResId, content, overlay);
        }
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
        if (ThemeUtils.getDefaultThemePackageName(getActivity()).equals(pkgName)) {
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
            if (isShowingApplyThemeLayout()) return;
            if (mActiveCardId > 0) {
                // need to fade the newly selected card in if another was currently selected.
                ((ComponentCardView) v).animateCardFadeIn();
            }
            mActiveCardId = v.getId();
            String component = mCardIdsToComponentTypes.get(mActiveCardId);
            ((ChooserActivity) getActivity()).showComponentSelector(component, v);
            fadeOutNonSelectedCards(mActiveCardId);
            stopMediaPlayers();
        }
    };

    private View.OnClickListener mApplyCancelClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == mApplyButton) {
                hideApplyThemeLayout(true);
            } else if (v == mCancelButton) {
                hideApplyThemeLayout();
            }
        }
    };

    private OnItemClickedListener mOnComponentItemClicked = new OnItemClickedListener() {
        @Override
        public void onItemClicked(String pkgName) {
            Bundle args = new Bundle();
            args.putString("pkgName", pkgName);
            int loaderId = -1;
            String component = mSelector.getComponentType();
            if (MODIFIES_STATUS_BAR.equals(component)) {
                loaderId = LOADER_ID_STATUS_BAR;
            } else if (MODIFIES_FONTS.equals(component)) {
                loaderId = LOADER_ID_FONT;
            } else if (MODIFIES_ICONS.equals(component)) {
                loaderId = LOADER_ID_ICONS;
            } else if (MODIFIES_NAVIGATION_BAR.equals(component)) {
                loaderId = LOADER_ID_NAVIGATION_BAR;
            } else if (MODIFIES_LAUNCHER.equals(component)) {
                loaderId = LOADER_ID_WALLPAPER;
            } else if (MODIFIES_LOCKSCREEN.equals(component)) {
                loaderId = LOADER_ID_LOCKSCREEN;
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
            getLoaderManager().restartLoader(loaderId, args, ThemeFragment.this);
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
                if (mSelectedComponentsMap != null && mSelectedComponentsMap.size() > 0) {
                    // Post this on mHandler so the client is added and removed from the same
                    // thread
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            ThemeManager tm = getThemeManager();
                            if (tm != null) {
                                tm.addClient(ThemeFragment.this);
                                tm.requestThemeChange(mSelectedComponentsMap);
                            }
                        }
                    });
                }
            }
        }
    };

    private void applyTheme() {
        if (mSelectedComponentsMap == null || mSelectedComponentsMap.size() <= 0) return;
        ((ChooserActivity) getActivity()).themeChangeStarted();
        animateProgressIn(mApplyThemeRunnable);
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
                .translationXBy(-(pivotX / 4))
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
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        mTitleLayout.animate()
                .translationXBy((pivotX / 4))
                .alpha(1f)
                .setDuration(ANIMATE_TITLE_IN_DURATION)
                .setInterpolator(new AccelerateInterpolator())
                .start();
        mProgress.startAnimation(scaleAnim);
    }

    private void animateContentIn() {
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

    public boolean isShowingApplyThemeLayout() {
        return mApplyThemeLayout.getVisibility() == View.VISIBLE;
    }

    public void showApplyThemeLayout() {
        if (mApplyThemeLayout.getVisibility() == View.VISIBLE) return;
        ((ChooserActivity) getActivity()).lockPager();
        mScrollView.setScrollingEnabled(false);
        ViewPropertyAnimator anim = mApplyThemeLayout.animate();
        mApplyThemeLayout.setVisibility(View.VISIBLE);
        mApplyThemeLayout.setAlpha(0f);
        anim.setListener(null);
        anim.setDuration(ANIMATE_APPLY_LAYOUT_DURATION);
        anim.alpha(1f).start();
    }
    public void hideApplyThemeLayout() {
        hideApplyThemeLayout(false);
    }

    /**
     * Hides the apply theme layout overlay and can apply the selected theme
     * when the animation is finished.
     * @param applyThemeWhenFinished If true, the current theme will be applied.
     */
    private void hideApplyThemeLayout(final boolean applyThemeWhenFinished) {
        ((ChooserActivity) getActivity()).unlockPager();
        mScrollView.setScrollingEnabled(true);
        ViewPropertyAnimator anim = mApplyThemeLayout.animate();
        mApplyThemeLayout.setVisibility(View.VISIBLE);
        anim.setDuration(ANIMATE_APPLY_LAYOUT_DURATION);
        anim.alpha(0f).start();
        anim.setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mApplyThemeLayout.setVisibility(View.GONE);
                if (applyThemeWhenFinished) applyTheme();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
    }

    public void showThemeTagLayout() {
        mThemeTagLayout.animate().alpha(1f).setStartDelay(ANIMATE_DURATION).start();
    }

    public void hideThemeTagLayout() {
        mThemeTagLayout.setAlpha(0f);
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
        for (String key : mSelectedComponentsMap.keySet()) {
            if (!mPkgName.equals(mSelectedComponentsMap.get(key))) {
                return true;
            }
        }
        return false;
    }

    public void clearChanges() {
        mSelectedComponentsMap.clear();
        getLoaderManager().restartLoader(LOADER_ID_ALL, null, ThemeFragment.this);
    }

    public String getThemePackageName() {
        return mPkgName;
    }

    private void uninstallTheme() {
        Uri packageURI = Uri.parse("package:" + mPkgName);
        Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageURI);
        uninstallIntent.putExtra(Intent.EXTRA_UNINSTALL_ALL_USERS, true);
        startActivityForResult(uninstallIntent,
                ChooserActivity.REQUEST_UNINSTALL);
    }

    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_UNINSTALL) {
            try {
                ApplicationInfo ainfo = getActivity()
                        .getPackageManager().getApplicationInfo(mPkgName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                mIsUninstalled = true;
            }
        }
    }

    public boolean isUninstalled() {
        return mIsUninstalled;
    }

    public void setCurrentTheme(Map<String, String> currentTheme) {
        mCurrentTheme = currentTheme;
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
    public void slideContentUp(final int yDelta, int selectorHeight) {
        Space space = new Space(getActivity());
        space.setId(ADDITIONAL_CONTENT_SPACE_ID);
        mAdditionalCards.addView(space, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, selectorHeight));
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

    /**
     * Slides the scrollview content down and removes a space view at the bottom
     * of mAdditionalCards.
     *
     * We are using a ValueAnimator here to scroll the content rather than calling
     * mScrollView.smoothScrollBy() since the speed of that animation cannot be customized.
     * @param yDelta
     */
    public void slideContentDown(int yDelta) {
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
            if (ThemeConfig.HOLO_DEFAULT.equals(mPkgName)) {
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
}
