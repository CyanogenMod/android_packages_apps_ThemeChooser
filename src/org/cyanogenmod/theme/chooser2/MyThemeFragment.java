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

import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.util.Log;
import android.util.MutableLong;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.cyanogenmod.theme.util.AudioUtils;
import org.cyanogenmod.theme.util.CursorLoaderHelper;
import org.cyanogenmod.theme.util.PreferenceUtils;
import org.cyanogenmod.theme.util.ThemedTypefaceHelper;
import org.cyanogenmod.theme.util.TypefaceHelperCache;
import org.cyanogenmod.theme.util.Utils;

import cyanogenmod.providers.ThemesContract;
import cyanogenmod.providers.ThemesContract.PreviewColumns;
import cyanogenmod.providers.ThemesContract.ThemesColumns;
import cyanogenmod.themes.ThemeChangeRequest;
import cyanogenmod.themes.ThemeChangeRequest.RequestType;
import cyanogenmod.themes.ThemeManager;

import org.cyanogenmod.internal.util.ThemeUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.cyanogenmod.theme.util.CursorLoaderHelper.LOADER_ID_ALL;

public class MyThemeFragment extends ThemeFragment {
    private static final String TAG = MyThemeFragment.class.getSimpleName();

    private static final String ARG_BASE_THEME_PACKAGE_NAME = "baseThemePkgName";
    private static final String ARG_BASE_THEME_NAME = "baseThemeName";
    private static final String ARG_BASE_THEME_AUTHOR = "baseThemeAuthor";

    private String mBaseThemeName;
    private String mBaseThemeAuthor;

    private SurfaceView mSurfaceView;

    static MyThemeFragment newInstance(String baseThemePkgName, String baseThemeName,
                                       String baseThemeAuthor, boolean skipLoadingAnim,
                                       boolean animateToLockScreenCard) {
        MyThemeFragment f = new MyThemeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PACKAGE_NAME, CURRENTLY_APPLIED_THEME);
        args.putString(ARG_BASE_THEME_PACKAGE_NAME, baseThemePkgName);
        args.putString(ARG_BASE_THEME_NAME, baseThemeName);
        args.putString(ARG_BASE_THEME_AUTHOR, baseThemeAuthor);
        args.putBoolean(ARG_SKIP_LOADING_ANIM, skipLoadingAnim);
        args.putBoolean(ARG_ANIMATE_TO_LOCK_SCREEN_CARD, animateToLockScreenCard);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context context = getActivity();
        ThemedTypefaceHelper helper = sTypefaceHelperCache.getHelperForTheme(context,
                getAppliedFontPackageName());
        mTypefaceNormal = helper.getTypeface(Typeface.NORMAL);
        mBaseThemePkgName = getArguments().getString(ARG_BASE_THEME_PACKAGE_NAME);
        mBaseThemeName = getArguments().getString(ARG_BASE_THEME_NAME);
        mBaseThemeAuthor = getArguments().getString(ARG_BASE_THEME_AUTHOR);
        mShowLockScreenSelectorAfterContentLoaded = getArguments().getBoolean(
                ARG_ANIMATE_TO_LOCK_SCREEN_CARD);
        mSurfaceView = createSurfaceView();
        populateBaseThemeSupportedComponents(mBaseThemePkgName);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        mThemeTagLayout.setAppliedTagEnabled(true);
        if (mBaseThemePkgName.equals(Utils.getDefaultThemePackageName(getActivity()))) {
            mThemeTagLayout.setDefaultTagEnabled(true);
        }
        if (PreferenceUtils.hasThemeBeenUpdated(getActivity(), mBaseThemePkgName)) {
            mThemeTagLayout.setUpdatedTagEnabled(true);
        }
        setCustomized(isThemeCustomized());
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mExpanded && getLoaderManager().getLoader(0) != null) {
            getLoaderManager().restartLoader(0, null, this);
        }

        IntentFilter filter = new IntentFilter(Intent.ACTION_WALLPAPER_CHANGED);
        getActivity().registerReceiver(mWallpaperChangeReceiver, filter);
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(mWallpaperChangeReceiver);
        super.onPause();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (mThemeTagLayout == null) return;

        if (!isVisibleToUser) {
            if (PreferenceUtils.hasThemeBeenUpdated(getActivity(), mBaseThemePkgName)) {
                mThemeTagLayout.setUpdatedTagEnabled(true);
            }
        } else {
            if (PreferenceUtils.hasThemeBeenUpdated(getActivity(), mBaseThemePkgName)) {
                PreferenceUtils.removeUpdatedTheme(getActivity(), mBaseThemePkgName);
            }
        }
    }

    @Override
    public void collapse(boolean applyTheme) {
        super.collapse(applyTheme);
        if (mSurfaceView != null) mSurfaceView.setVisibility(View.VISIBLE);
    }

    @Override
    public void expand() {
        super.expand();
        if (mSurfaceView != null && mShadowFrame.indexOfChild(mSurfaceView) >= 0) {
            mSurfaceView.setVisibility(View.GONE);
            mWallpaper.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void performClick(boolean clickedOnContent) {
        if (clickedOnContent) {
            showCustomizeResetLayout();
        } else {
            if (isShowingCustomizeResetLayout()) {
                hideCustomizeResetLayout();
            } else {
                super.performClick(clickedOnContent);
            }
        }
    }

    @Override
    public void setCurrentTheme(Map<String, String> currentTheme,
            MutableLong currentWallpaperComponentId) {
        super.setCurrentTheme(currentTheme, currentWallpaperComponentId);
        for (String key : currentTheme.keySet()) {
            mSelectedComponentsMap.put(key, currentTheme.get(key));
        }
        mSelectedWallpaperComponentId = currentWallpaperComponentId.value;
    }

    @Override
    public boolean componentsChanged() {
        // If an external wallpaper/ls are set then something changed!
        if (mExternalWallpaperUri != null || mExternalLockscreenUri != null) return true;

        for (String key : mSelectedComponentsMap.keySet()) {
            String current = mCurrentTheme.get(key);
            if (current == null || !current.equals(mSelectedComponentsMap.get(key))) {
                return true;
            }
            if (ThemesColumns.MODIFIES_LAUNCHER.equals(key) &&
                    mCurrentWallpaperComponentId.value != mSelectedWallpaperComponentId) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void applyThemeWhenPopulated(String pkgName, List<String> components) {
        super.applyThemeWhenPopulated(pkgName, components);
        populateComponentsToApply(pkgName, components);
    }

    private void populateComponentsToApply(String pkgName, List<String> components) {
        String selection = ThemesColumns.PKG_NAME + "=?";
        String[] selectionArgs = { pkgName };
        Cursor c = getActivity().getContentResolver().query(ThemesColumns.CONTENT_URI,
                null, selection, selectionArgs, null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                mSelectedComponentsMap.clear();
                if (c.getInt(c.getColumnIndex(ThemesColumns.MODIFIES_ALARMS)) == 1) {
                    mSelectedComponentsMap.put(ThemesColumns.MODIFIES_ALARMS, pkgName);
                }
                if (c.getInt(c.getColumnIndex(ThemesColumns.MODIFIES_BOOT_ANIM)) == 1) {
                    mSelectedComponentsMap.put(ThemesColumns.MODIFIES_BOOT_ANIM, pkgName);
                }
                if (c.getInt(c.getColumnIndex(ThemesColumns.MODIFIES_FONTS)) == 1) {
                    mSelectedComponentsMap.put(ThemesColumns.MODIFIES_FONTS, pkgName);
                }
                if (c.getInt(c.getColumnIndex(ThemesColumns.MODIFIES_ICONS)) == 1) {
                    mSelectedComponentsMap.put(ThemesColumns.MODIFIES_ICONS, pkgName);
                }
                if (c.getInt(c.getColumnIndex(ThemesColumns.MODIFIES_LAUNCHER)) == 1) {
                    mSelectedComponentsMap.put(ThemesColumns.MODIFIES_LAUNCHER, pkgName);
                }
                if (c.getInt(c.getColumnIndex(ThemesColumns.MODIFIES_LOCKSCREEN)) == 1) {
                    mSelectedComponentsMap.put(ThemesColumns.MODIFIES_LOCKSCREEN, pkgName);
                }
                if (c.getInt(c.getColumnIndex(ThemesColumns.MODIFIES_NAVIGATION_BAR)) == 1) {
                    mSelectedComponentsMap.put(ThemesColumns.MODIFIES_NAVIGATION_BAR, pkgName);
                }
                if (c.getInt(c.getColumnIndex(ThemesColumns.MODIFIES_NOTIFICATIONS)) == 1) {
                    mSelectedComponentsMap.put(ThemesColumns.MODIFIES_NOTIFICATIONS, pkgName);
                }
                if (c.getInt(c.getColumnIndex(ThemesColumns.MODIFIES_OVERLAYS)) == 1) {
                    mSelectedComponentsMap.put(ThemesColumns.MODIFIES_OVERLAYS, pkgName);
                }
                if (c.getInt(c.getColumnIndex(ThemesColumns.MODIFIES_RINGTONES)) == 1) {
                    mSelectedComponentsMap.put(ThemesColumns.MODIFIES_RINGTONES, pkgName);
                }
                if (c.getInt(c.getColumnIndex(ThemesColumns.MODIFIES_STATUS_BAR)) == 1) {
                    mSelectedComponentsMap.put(ThemesColumns.MODIFIES_STATUS_BAR, pkgName);
                }
                if (c.getInt(c.getColumnIndex(ThemesColumns.MODIFIES_LIVE_LOCK_SCREEN)) == 1) {
                    mSelectedComponentsMap.put(ThemesColumns.MODIFIES_LIVE_LOCK_SCREEN, pkgName);
                }
            }
            c.close();
        }

        // strip out any components that are not in the components list
        if (components != null) {
            Iterator<Map.Entry<String, String>> iterator =
                    mSelectedComponentsMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                if (!components.contains(entry.getKey())) {
                    iterator.remove();
                }
            }
        }
    }

    private void loadComponentsToApply() {
        for (String component : mSelectedComponentsMap.keySet()) {
            loadComponentFromPackage(mSelectedComponentsMap.get(component), component,
                    mSelectedWallpaperComponentId);
        }
    }

    private BroadcastReceiver mWallpaperChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // only update if we are the current visible fragment or if there is no theme
            // being applied.
            ThemeManager tm = getThemeManager();
            if (!tm.isThemeApplying() || getUserVisibleHint()) {
                final WallpaperManager wm = WallpaperManager.getInstance(context);
                if (wm.getWallpaperInfo() != null) {
                    addSurfaceView(mSurfaceView);
                } else {
                    removeSurfaceView(mSurfaceView);
                }

                Drawable wp = context == null ? null : wm.getDrawable();
                if (wp != null) {
                    mWallpaper.setImageDrawable(wp);
                    mWallpaperCard.setWallpaper(wp);
                }
            }
        }
    };

    private void setCustomized(boolean customized) {
        mThemeTagLayout.setCustomizedTagEnabled(customized);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_ALL:
                if (args != null) {
                    String pkgName = args.getString(ARG_PACKAGE_NAME);
                    if (pkgName != null) {
                        return super.onCreateLoader(id, args);
                    }
                }
                return CursorLoaderHelper.myThemeFragmentCursorLoader(getActivity(), id);
            default:
                // Only LOADER_ID_ALL differs for MyThemeFragment
                return super.onCreateLoader(id, args);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
        super.onLoadFinished(loader, c);
        // if the theme is resetting, we need to apply these changes now that the supported
        // theme components have been properly set.
        if (loader.getId() == LOADER_ID_ALL) {
            if (mThemeResetting) {
                applyTheme();
            } else if (mApplyThemeOnPopulated) {
                loadComponentsToApply();
                applyTheme();
            } else if (mSelectedComponentsMap.size() == 0) {
                //Re-populates selected components with current theme. Why?
                //We got here because the cursor was reloaded after the user pressed back and no
                //changes were applied, causing the selected components map to be wiped out
                mSelectedComponentsMap.putAll(mCurrentTheme);
            }
        }
    }

    @Override
    protected Map<String, String> fillMissingComponentsWithDefault(
            Map<String, String> originalMap) {
        // Only the ThemeFragment should be altering this, for the MyThemeFragment this is not
        // desirable as it changes components the user did not even touch.
        return originalMap;
    }

    @Override
    protected ThemeChangeRequest getThemeChangeRequestForComponents(
            Map<String, String> componentMap) {
        return getThemeChangeRequestForComponents(componentMap, RequestType.USER_REQUEST_MIXNMATCH);
    }

    @Override
    protected Map<String, String> getComponentsToApply() {
        Map<String, String> componentsToApply = mThemeResetting
                ? getEmptyComponentsMap()
                : new HashMap<String, String>();
        if (mThemeResetting) {
            final String pkgName = getThemePackageName();
            for (String component : mBaseThemeSupportedComponents) {
                componentsToApply.put(component, pkgName);
            }
        } else {
            // Only apply components that actually changed
            for (String component : mSelectedComponentsMap.keySet()) {
                String currentPkg = mCurrentTheme.get(component);
                String selectedPkg = mSelectedComponentsMap.get(component);
                if (currentPkg == null || mThemeResetting || !currentPkg.equals(selectedPkg) ||
                        mCurrentWallpaperComponentId.value != mSelectedWallpaperComponentId) {
                    componentsToApply.put(component, selectedPkg);
                }
            }
            if (mExternalLockscreenUri != null) {
                if (mCurrentTheme.containsKey(ThemesColumns.MODIFIES_LIVE_LOCK_SCREEN)) {
                    componentsToApply.put(ThemesColumns.MODIFIES_LIVE_LOCK_SCREEN, LOCKSCREEN_NONE);
                }
                if (mCurrentTheme.containsKey(ThemesColumns.MODIFIES_LOCKSCREEN)) {
                    componentsToApply.put(ThemesColumns.MODIFIES_LOCKSCREEN, LOCKSCREEN_NONE);
                }
            }
        }
        return componentsToApply;
    }

    @Override
    protected void populateSupportedComponents(Cursor c) {
    }

    @Override
    protected Boolean shouldShowComponentCard(String component) {
        return true;
    }

    @Override
    protected void loadTitle(Cursor c) {
        mTitle.setText(mBaseThemeName);
        mAuthor.setText(mBaseThemeAuthor);
    }

    @Override
    protected void loadWallpaper(Cursor c, boolean animate) {
        mExternalWallpaperUri = null;
        int pkgNameIdx = c.getColumnIndex(ThemesContract.ThemesColumns.PKG_NAME);
        if (pkgNameIdx > -1) {
            super.loadWallpaper(c, animate);
            return;
        }
        Drawable overlay = null;
        if (animate) {
            overlay = getOverlayDrawable(mWallpaperCard, true);
        }

        int wpIdx = c.getColumnIndex(PreviewColumns.WALLPAPER_PREVIEW);
        final Resources res = getResources();
        final Context context = getActivity();
        final WallpaperManager wm = WallpaperManager.getInstance(context);
        if (wm.getWallpaperInfo() != null) {
            addSurfaceView(mSurfaceView);
        } else {
            removeSurfaceView(mSurfaceView);
        }

        Drawable wp = context == null ? null : wm.getDrawable();
        if (wp == null) {
            Bitmap bmp = Utils.loadBitmapBlob(c, wpIdx);
            if (bmp != null) wp = new BitmapDrawable(res, bmp);
        }
        if (wp != null) {
            mWallpaper.setImageDrawable(wp);
            mWallpaperCard.setWallpaper(wp);
            setCardTitle(mWallpaperCard, mCurrentTheme.get(ThemesColumns.MODIFIES_LAUNCHER),
                    getString(R.string.wallpaper_label));
        } else {
            mWallpaperCard.clearWallpaper();
            mWallpaperCard.setEmptyViewEnabled(true);
            setAddComponentTitle(mWallpaperCard, getString(R.string.wallpaper_label));
        }

        if (animate) {
            animateContentChange(R.id.wallpaper_card, mWallpaperCard, overlay);
        }
    }

    @Override
    protected void loadLockScreen(Cursor c, boolean animate) {
        mExternalLockscreenUri = null;
        int pkgNameIdx = c.getColumnIndex(ThemesContract.ThemesColumns.PKG_NAME);
        if (pkgNameIdx > -1) {
            super.loadLockScreen(c, animate);
            return;
        }
        Drawable overlay = null;
        if (animate) {
            overlay = getOverlayDrawable(mLockScreenCard, true);
        }

        //If the current theme includes a lock wallpaper, the WallpaperMgr will
        //return a valid Drawable we can display in the card. However, if the user
        //picked a LLS, we need to get the path from the provider and manually load the bitmap
        int wpIdx = c.getColumnIndex(PreviewColumns.LIVE_LOCK_SCREEN_PREVIEW);
        Drawable wp = null;
        if (wpIdx >= 0) {
            final Resources res = getResources();
            Bitmap bmp = Utils.loadBitmapBlob(c, wpIdx);
            if (bmp != null) wp = new BitmapDrawable(res, bmp);
        } else {
            final Context context = getActivity();
            wp = context == null ? null :
                    WallpaperManager.getInstance(context).getFastKeyguardDrawable();
        }
        if (wp != null) {
            mLockScreenCard.setWallpaper(wp);
        } else if (!mSelectedComponentsMap.containsKey(ThemesColumns.MODIFIES_LOCKSCREEN)) {
            mLockScreenCard.clearWallpaper();
            mLockScreenCard.setEmptyViewEnabled(true);
            setAddComponentTitle(mLockScreenCard, getString(R.string.lockscreen_label));
        }

        if (animate) {
            animateContentChange(R.id.lockscreen_card, mLockScreenCard, overlay);
        }
    }

    @Override
    protected void loadFont(Cursor c, boolean animate) {
        int pkgNameIdx = c.getColumnIndex(ThemesContract.ThemesColumns.PKG_NAME);
        if (pkgNameIdx > -1) {
            super.loadFont(c, animate);
            return;
        }
        Drawable overlay = null;
        if (animate) {
            overlay = getOverlayDrawable(mFontPreview, true);
        }
        setCardTitle(mFontCard, mCurrentTheme.get(ThemesColumns.MODIFIES_FONTS),
                getString(R.string.font_label));

        TypefaceHelperCache cache = TypefaceHelperCache.getInstance();
        ThemedTypefaceHelper helper = cache.getHelperForTheme(getActivity(),
                getAppliedFontPackageName());
        mTypefaceNormal = helper.getTypeface(Typeface.NORMAL);
        mFontPreview.setTypeface(mTypefaceNormal);
        if (animate) {
            animateContentChange(R.id.font_preview_container, mFontPreview, overlay);
        }
    }

    @Override
    protected void loadAudible(int type, Cursor c, boolean animate) {
        int pkgNameIdx = c.getColumnIndex(ThemesContract.ThemesColumns.PKG_NAME);
        if (pkgNameIdx > -1) {
            super.loadAudible(type, c, animate);
            return;
        }
        ComponentCardView audibleContainer = null;
        ImageView playPause = null;
        String modsComponent = "";
        switch (type) {
            case RingtoneManager.TYPE_RINGTONE:
                audibleContainer = mRingtoneCard;
                playPause = mRingtonePlayPause;
                modsComponent = ThemesColumns.MODIFIES_RINGTONES;
                break;
            case RingtoneManager.TYPE_NOTIFICATION:
                audibleContainer = mNotificationCard;
                playPause = mNotificationPlayPause;
                modsComponent = ThemesColumns.MODIFIES_NOTIFICATIONS;
                break;
            case RingtoneManager.TYPE_ALARM:
                audibleContainer = mAlarmCard;
                playPause = mAlarmPlayPause;
                modsComponent = ThemesColumns.MODIFIES_ALARMS;
                break;
        }
        if (audibleContainer == null) return;

        if (playPause == null) {
            playPause =
                    (ImageView) audibleContainer.findViewById(R.id.play_pause);
        }
        TextView title = (TextView) audibleContainer.findViewById(R.id.audible_name);
        MediaPlayer mp = mMediaPlayers.get(playPause);
        if (mp == null) {
            mp = new MediaPlayer();
        }

        final Context context = getActivity();
        Ringtone ringtone = null;
        try {
            Uri ringtoneUri = AudioUtils.loadDefaultAudible(context, type, mp);
            if (ringtoneUri != null) ringtone = RingtoneManager.getRingtone(context, ringtoneUri);
        } catch (IOException e) {
            Log.w(TAG, "Unable to load default sound ", e);
        }

        if (ringtone != null) {
            title.setText(ringtone.getTitle(context));
            setCardTitle(audibleContainer, mCurrentTheme.get(modsComponent),
                    getAudibleLabel(type));
        } else {
            title.setText(getString(R.string.audible_title_none));
            setAddComponentTitle(audibleContainer, getAudibleLabel(type));
            playPause.setVisibility(View.INVISIBLE);
            audibleContainer.setEmptyViewEnabled(true);
        }

        playPause.setTag(mp);
        mMediaPlayers.put(playPause, mp);
        playPause.setOnClickListener(mPlayPauseClickListener);
        mp.setOnCompletionListener(mPlayCompletionListener);
    }

    @Override
    protected void loadStatusBar(Cursor c, boolean animate) {
        super.loadStatusBar(c, animate);
        setCardTitle(mStatusBarCard, mCurrentTheme.get(ThemesColumns.MODIFIES_STATUS_BAR),
                getString(R.string.statusbar_label));
    }

    @Override
    protected void loadIcons(Cursor c, boolean animate) {
        super.loadIcons(c, animate);
        setCardTitle(mIconCard, mCurrentTheme.get(ThemesColumns.MODIFIES_ICONS),
                getString(R.string.icon_label));
    }

    @Override
    protected void loadNavBar(Cursor c, boolean animate) {
        super.loadNavBar(c, animate);
        setCardTitle(mNavBarCard, mCurrentTheme.get(ThemesColumns.MODIFIES_NAVIGATION_BAR),
                getString(R.string.navbar_label));
    }

    @Override
    protected void loadStyle(Cursor c, boolean animate) {
        super.loadStyle(c, animate);
        setCardTitle(mStyleCard, mCurrentTheme.get(ThemesColumns.MODIFIES_OVERLAYS),
                getString(R.string.style_label));
    }

    @Override
    protected void loadBootAnimation(Cursor c) {
        super.loadBootAnimation(c);
        setCardTitle(mBootAnimationCard, mCurrentTheme.get(ThemesColumns.MODIFIES_BOOT_ANIM),
                getString(R.string.boot_animation_label));
    }

    @Override
    public String getThemePackageName() {
        if (mBaseThemePkgName == null) {
            // check if the package name is defined in the arguments bundle
            Bundle bundle = getArguments();
            if (bundle != null) {
                mBaseThemePkgName = bundle.getString(ARG_BASE_THEME_PACKAGE_NAME);
            }
        }
        return mBaseThemePkgName;
    }

    private SurfaceView createSurfaceView() {
        final Context context = getActivity();
        if (context == null) return null;

        SurfaceView sv = new SurfaceView(context);
        final Resources res = context.getResources();
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                res.getDimensionPixelSize(R.dimen.wallpaper_preview_width),
                res.getDimensionPixelSize(R.dimen.theme_preview_height),
                Gravity.CENTER_HORIZONTAL);
        sv.setLayoutParams(params);

        return sv;
    }

    private void addSurfaceView(SurfaceView sv) {
        if (mShadowFrame.indexOfChild(mSurfaceView) < 0) {
            int idx = mShadowFrame.indexOfChild(mWallpaper);
            mShadowFrame.addView(sv, idx + 1);
        }
    }

    private void removeSurfaceView(SurfaceView sv) {
        if (mShadowFrame.indexOfChild(mSurfaceView) >= 0) {
            mShadowFrame.removeView(sv);
        }
    }

    /**
     * Populates mBaseThemeSupportedComponents.
     * @param pkgName Package name of the base theme used
     */
    private void populateBaseThemeSupportedComponents(String pkgName) {
        String selection = ThemesColumns.PKG_NAME + "=?";
        String[] selectionArgs = { pkgName };
        Cursor c = getActivity().getContentResolver().query(ThemesColumns.CONTENT_URI,
                null, selection, selectionArgs, null);
        if (c != null) {
            if (c.moveToFirst()) {
                List<String> components = ThemeUtils.getAllComponents();
                final String baseThemePackageName = getThemePackageName();
                for (String component : components) {
                    int pkgIdx = c.getColumnIndex(ThemesColumns.PKG_NAME);
                    int modifiesCompIdx = c.getColumnIndex(component);

                    String pkg = pkgIdx >= 0 ? c.getString(pkgIdx) : null;
                    boolean supported = (modifiesCompIdx >= 0) && (c.getInt(modifiesCompIdx) == 1);
                    if (supported && baseThemePackageName.equals(pkg)) {
                        mBaseThemeSupportedComponents.add(component);
                    }
                }
            }
            c.close();
        }
    }
}
