/*
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

import android.app.Activity;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.ArrayMap;
import android.util.MutableLong;
import android.view.*;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import cyanogenmod.providers.ThemesContract;
import cyanogenmod.providers.ThemesContract.ThemesColumns;
import cyanogenmod.themes.ThemeChangeRequest;
import cyanogenmod.themes.ThemeManager;
import org.cyanogenmod.theme.chooser2.R;
import org.cyanogenmod.theme.util.CursorLoaderHelper;
import org.cyanogenmod.theme.util.ThemedTypefaceHelper;
import org.cyanogenmod.theme.util.TypefaceHelperCache;
import org.cyanogenmod.theme.util.Utils;
import org.cyanogenmod.theme.widget.ThemeTagLayout;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;

import static android.view.View.LAYER_TYPE_HARDWARE;
import static android.view.View.LAYER_TYPE_SOFTWARE;
import static cyanogenmod.providers.ThemesContract.ThemesColumns.*;
import static org.cyanogenmod.theme.chooser2.ComponentSelector.DEFAULT_COMPONENT_ID;
import static org.cyanogenmod.theme.util.CursorLoaderHelper.LOADER_ID_ALL;
import static org.cyanogenmod.theme.util.CursorLoaderHelper.LOADER_ID_THEME_MIX_ENTRIES;

public class ThemeMixFragment extends ThemeFragment {

    private static final String ARG_THEME_MIX_NAME = "theme_mix_name";
    private static final String ARG_THEME_MIX_ID = "theme_mix_id";

    private String mThemeMixName;
    private int mThemeMixId;
    private static String mThemeMixAuthor;

    private Set<String> uninstalled_themes;
    private Map<String, String> mThemeNamesMap;
    private View mMissingTheme;

    static ThemeMixFragment newInstance(String mixName, String baseThemePkgName,
                                        String authorName, int id,
                                        boolean skipLoadingAnim) {
        ThemeMixFragment f = new ThemeMixFragment();
        Bundle args = new Bundle();
        args.putString(ARG_THEME_MIX_NAME, mixName);
        args.putBoolean(ARG_SKIP_LOADING_ANIM, skipLoadingAnim);
        args.putLong(ARG_COMPONENT_ID, DEFAULT_COMPONENT_ID);
        args.putInt(ARG_THEME_MIX_ID, id);
        args.putString(ARG_PACKAGE_NAME, baseThemePkgName);
        mThemeMixAuthor = authorName;
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mThemeMixName = args.getString(ARG_THEME_MIX_NAME);
        mBaseThemePkgName = args.getString(ARG_PACKAGE_NAME);

        mThemeMixId = args.getInt(ARG_THEME_MIX_ID);
        mThemeNamesMap = new ArrayMap<>();
        uninstalled_themes = new HashSet<>();
        getLoaderManager().initLoader(LOADER_ID_THEME_MIX_ENTRIES, null, this);
    }

    private void setMixed(boolean mixed) {
        mThemeTagLayout.setMixedTagEnabled(mixed);
    }

    private void setLegacy(boolean legacy) {
        mThemeTagLayout.setLegacyTagEnabled(legacy);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        int id = 0;
        mMissingTheme = v.findViewById(R.id.missing_theme_overlay);

        String selection = ThemesContract.ThemeMixColumns.TITLE + "=?";
        String[] selection_Args = {getThemePackageName()};
        Cursor cur = getActivity().getContentResolver().query
                (ThemesContract.ThemeMixColumns.CONTENT_URI,
                        null, selection, selection_Args, null);
        if (cur.getCount() == 1) {
            cur.moveToPosition(-1);
            while (cur.moveToNext()) {
                id = cur.getInt(0);
            }
        }
        cur.close();

        String[] projection = {"component_type", "package_name", "theme_name", "installed"};
        String selection_theme = "theme_mix_id=?";
        String[] selectionArgs_theme = {Integer.toString(id)};

        Cursor cu = getActivity().getContentResolver().query(ThemesContract.
                        ThemeMixEntryColumns.CONTENT_URI,
                 projection, selection_theme, selectionArgs_theme, null);
        cu.moveToPosition(-1);
        uninstalled_themes.clear();
        while (cu.moveToNext()) {
            int isInstalled = cu.getInt(cu.getColumnIndex
                    (ThemesContract.ThemeMixEntryColumns.IS_INSTALLED));
            String missing_theme_name = cu.getString(cu.getColumnIndex
                    (ThemesContract.ThemeMixEntryColumns.THEME_NAME));
            if (isInstalled == 0) {
                uninstalled_themes.add(missing_theme_name);
                View v1 = v.findViewById(R.id.shadow_frame);
//                v1.setOnClickListener(mMissingThemes);

                Paint p = new Paint();
                ColorMatrix cm = new ColorMatrix();
                cm.setSaturation(0);
                p.setColorFilter(new ColorMatrixColorFilter(cm));
                v1.setLayerType(v1.isHardwareAccelerated() ? LAYER_TYPE_HARDWARE : LAYER_TYPE_SOFTWARE, p);
                //v.setEnabled(false);
//                v1.setOnClickListener(mMissingThemes);
                mOverflow.setEnabled(false);
                if (mThemeMixName != null) mTitle.setText(mThemeMixName);
                mAuthor.setText(mThemeMixAuthor);
                setLegacy(true);
                //return v;
            }
        }
        cu.close();

        if (mThemeMixName != null) mTitle.setText(mThemeMixName);
        mAuthor.setText(mThemeMixAuthor);
        setMixed(true);

        return v;
    }

    @Override
    protected void showApplyThemeOverlay() {
        if(!mThemeTagLayout.isLegacyTagEnabled()) {
            super.showApplyThemeOverlay();
            return;
        }
        mMissingTheme.setVisibility(View.VISIBLE);
        TextView tv1 = (TextView) mMissingTheme.findViewById(R.id.missing_theme_overlay_title);
        tv1.setVisibility(View.VISIBLE);
        String setText = " ";
        Iterator<String> it = uninstalled_themes.iterator();
        while(it.hasNext()) {
            setText = setText + it.next() + "\n";
        }
        TextView tv = (TextView) mMissingTheme.findViewById(R.id.missing_theme_names);
        tv.setText(setText);
        tv.setVisibility(View.VISIBLE);
        tv.setMovementMethod(new ScrollingMovementMethod());
    }

    @Override
    protected boolean isThemeProcessing() {
        return false;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_ALL:
                return CursorLoaderHelper.themeMixFragmentCursorLoader(getActivity(), mThemeMixId);
            case LOADER_ID_THEME_MIX_ENTRIES:
                return CursorLoaderHelper.themeMixEntriesCursorLoader(getActivity(), mThemeMixId);
            default:
                // Only LOADER_ID_ALL differs for ThemeMixFragment
                return super.onCreateLoader(id, args);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
        switch (loader.getId()) {
            case LOADER_ID_THEME_MIX_ENTRIES:
                populateSupportedComponents(c);
                break;
            default:
                super.onLoadFinished(loader, c);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        super.onLoaderReset(loader);
    }

    @Override
    protected void populateSupportedComponents(Cursor c) {
        if (c != null && c.getCount() > 1) {
            while(c.moveToNext()) {
                int pkgIdx = c.getColumnIndex(ThemesContract.ThemeMixEntryColumns.PACKAGE_NAME);
                int compIdx = c.getColumnIndex(ThemesContract.ThemeMixEntryColumns.COMPONENT_TYPE);
                int themeNameIdx = c.getColumnIndex(ThemesContract.ThemeMixEntryColumns.THEME_NAME);
                if (pkgIdx >= 0 && compIdx >= 0 && themeNameIdx >= 0) {
                    mSelectedComponentsMap.put(c.getString(compIdx), c.getString(pkgIdx));
                    mThemeNamesMap.put(c.getString(compIdx), c.getString(themeNameIdx));
                    mBaseThemeSupportedComponents.put(c.getString(compIdx), c.getString(pkgIdx));
                }
            }
        }
    }

    @Override
    protected Boolean shouldShowComponentCard(String component) {
        return true;
    }

    @Override
    protected void loadLegacyThemeInfo(Cursor c) {
        super.loadLegacyThemeInfo(c);
    }

    @Override
    protected void loadTitle(Cursor c) {
    }

    @Override
    protected void loadWallpaper(Cursor c, boolean animate) {
        super.loadWallpaper(c, animate);
    }

    @Override
    protected void loadLockScreen(Cursor c, boolean animate) {
        if (mCurrentLoaderId != LOADER_ID_ALL) {
            super.loadLockScreen(c, animate);
            return;
        }

        mExternalLockscreenUri = null;
        Drawable overlay = null;
        if (animate) {
            overlay = getOverlayDrawable(mLockScreenCard, true);
        }
        if (mLockScreenCard.isShowingEmptyView()) mLockScreenCard.setEmptyViewEnabled(false);

        int liveLockIndex = c.getColumnIndex(ThemesContract.PreviewColumns.LIVE_LOCK_SCREEN_PREVIEW);
        boolean isLiveLockScreen = liveLockIndex >= 0;

        int wpIdx = isLiveLockScreen
                ? c.getColumnIndex(ThemesContract.PreviewColumns.LIVE_LOCK_SCREEN_PREVIEW)
                : c.getColumnIndex(ThemesContract.PreviewColumns.LOCK_WALLPAPER_PREVIEW);
        final Resources res = getResources();
        Bitmap bitmap = Utils.loadBitmapBlob(c, wpIdx);
        if (bitmap != null) {
            mLockScreenCard.setWallpaper(new BitmapDrawable(res, bitmap));
            String pkgName = isLiveLockScreen ? mSelectedComponentsMap.get
                    (ThemesColumns.MODIFIES_LIVE_LOCK_SCREEN)
                    : mSelectedComponentsMap.get(ThemesColumns.MODIFIES_LOCKSCREEN);
            if (!mPkgName.equals(pkgName) || (mPkgName.equals(pkgName)
                    && (mBaseThemeSupportedComponents.containsKey
                    (ThemesColumns.MODIFIES_LOCKSCREEN) ||
                    mBaseThemeSupportedComponents.containsKey
                            (ThemesColumns.MODIFIES_LIVE_LOCK_SCREEN)))) {
                if (isLiveLockScreen) {
                    if (mSelectedComponentsMap.containsKey(ThemesColumns.MODIFIES_LOCKSCREEN)) {
                        mSelectedComponentsMap.put(ThemesColumns.MODIFIES_LOCKSCREEN, "");
                    }
                    mSelectedComponentsMap.put(ThemesColumns.MODIFIES_LIVE_LOCK_SCREEN, pkgName);
                    setCardTitle(mLockScreenCard, pkgName,
                          getString(org.cyanogenmod.theme.chooser2.R.string.live_lock_screen_label));
                } else {
                    mSelectedComponentsMap.put(ThemesColumns.MODIFIES_LOCKSCREEN, pkgName);
                    if (mSelectedComponentsMap.containsKey(ThemesColumns.MODIFIES_LIVE_LOCK_SCREEN))
                    {
                        mSelectedComponentsMap.put(ThemesColumns.MODIFIES_LIVE_LOCK_SCREEN, "");
                    }
                    setCardTitle(mLockScreenCard, pkgName,
                            getString(org.cyanogenmod.theme.chooser2.R.string.lockscreen_label));
                }
            }
        } else {
            // Set the lockscreen wallpaper to "None"
            mLockScreenCard.setWallpaper(null);
            setCardTitle(mLockScreenCard, WALLPAPER_NONE,
                    getString(org.cyanogenmod.theme.chooser2.R.string.lockscreen_label));
        }

        if (animate) {
            animateContentChange(org.cyanogenmod.theme.chooser2.R.id.lockscreen_card,
                    mLockScreenCard, overlay);
        }
    }

    @Override
    protected void loadStatusBar(Cursor c, boolean animate) {
        super.loadStatusBar(c, animate);
    }

    @Override
    protected void loadIcons(Cursor c, boolean animate) {
        super.loadIcons(c, animate);
    }

    @Override
    protected void loadNavBar(Cursor c, boolean animate) {
        super.loadNavBar(c, animate);
    }

    @Override
    protected void loadFont(Cursor c, boolean animate) {
        if (mCurrentLoaderId != LOADER_ID_ALL) {
            super.loadFont(c, animate);
            return;
        }

        Drawable overlay = null;
        if (animate) {
            overlay = getOverlayDrawable(mFontPreview, true);
        }
        if (mFontCard.isShowingEmptyView()) mFontCard.setEmptyViewEnabled(false);

        String pkgName = mSelectedComponentsMap.get(MODIFIES_FONTS);
        TypefaceHelperCache cache = TypefaceHelperCache.getInstance();
        ThemedTypefaceHelper helper = cache.getHelperForTheme(getActivity(), pkgName);
        mTypefaceNormal = helper.getTypeface(Typeface.NORMAL);
        mFontPreview.setTypeface(mTypefaceNormal);
        if (!mPkgName.equals(pkgName) || (mPkgName.equals(pkgName)
                && mBaseThemeSupportedComponents.containsKey(MODIFIES_FONTS))) {
            mSelectedComponentsMap.put(MODIFIES_FONTS, pkgName);
            setCardTitle(mFontCard, pkgName,
                    getString(org.cyanogenmod.theme.chooser2.R.string.font_label));
        }

        if (animate) {
            animateContentChange(org.cyanogenmod.theme.chooser2.R.id.font_preview_container,
                    mFontPreview, overlay);
        }
    }

    @Override
    public void performClick(boolean clickedOnContent) {
        if (!getView().isEnabled()) return;
        super.performClick(clickedOnContent);
    }

    @Override
    protected void loadStyle(Cursor c, boolean animate) {
        super.loadStyle(c, animate);
    }

    @Override
    protected void loadBootAnimation(Cursor c) {
        super.loadBootAnimation(c);
    }

    @Override
    protected void loadAudible(int type, Cursor c, boolean animate) {
        if (mCurrentLoaderId == LOADER_ID_ALL) {
            ComponentCardView audibleContainer = null;
            ImageView playPause = null;
            String component = null;
            int parentResId = 0;
            switch (type) {
                case RingtoneManager.TYPE_RINGTONE:
                    audibleContainer = mRingtoneCard;
                    playPause = mRingtonePlayPause;
                    component = MODIFIES_RINGTONES;
                    parentResId = org.cyanogenmod.theme.chooser2.R.id.ringtone_preview_container;
                    break;
                case RingtoneManager.TYPE_NOTIFICATION:
                    audibleContainer = mNotificationCard;
                    playPause = mNotificationPlayPause;
                    component = MODIFIES_NOTIFICATIONS;
                    parentResId = org.cyanogenmod.theme.chooser2.R.id.notification_preview_container;
                    break;
                case RingtoneManager.TYPE_ALARM:
                    audibleContainer = mAlarmCard;
                    playPause = mAlarmPlayPause;
                    component = MODIFIES_ALARMS;
                    parentResId = org.cyanogenmod.theme.chooser2.R.id.alarm_preview_container;
                    break;
            }
            String pkgName = mSelectedComponentsMap.get(component);
            if (audibleContainer == null || TextUtils.isEmpty(pkgName)) return;

            View content = audibleContainer.findViewById(org.cyanogenmod.theme.chooser2.R.id.content);
            Drawable overlay = null;
            if (animate) {
                overlay = getOverlayDrawable(content, true);
            }
            if (audibleContainer.isShowingEmptyView()) {
                audibleContainer.setEmptyViewEnabled(false);
            }

            if (playPause == null) {
                playPause = (ImageView) audibleContainer.findViewById
                        (org.cyanogenmod.theme.chooser2.R.id.play_pause);
            }
            TextView title = (TextView) audibleContainer.findViewById(R.id.audible_name);
            title.setText(mThemeNamesMap.get(component));
            MediaPlayer mp = mMediaPlayers.get(playPause);
            if (mp == null) {
                mp = new MediaPlayer();
            }
            setCardTitle(audibleContainer, pkgName, getAudibleLabel(type));
            AudibleLoadingThread thread = new AudibleLoadingThread(getActivity(), type, pkgName, mp);

            playPause.setVisibility(View.VISIBLE);
            playPause.setTag(mp);
            mMediaPlayers.put(playPause, mp);
            playPause.setOnClickListener(mPlayPauseClickListener);
            mp.setOnCompletionListener(mPlayCompletionListener);
            if (animate) {
                animateContentChange(parentResId, content, overlay);
            }
            thread.start();
        } else {
            super.loadAudible(type, c, animate);
        }
    }

    @Override
    protected boolean isThemeCustomized() {
        return false;
    }

    @Override
    public String getThemePackageName() {
        return mBaseThemePkgName;
    }

    @Override
    protected void uninstallTheme() {
        getChooserActivity().deleteThemeMix(mThemeMixId);
    }
}
