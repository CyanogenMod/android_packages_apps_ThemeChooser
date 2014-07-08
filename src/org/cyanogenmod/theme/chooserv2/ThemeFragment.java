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

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.ThemesContract;
import android.provider.ThemesContract.PreviewColumns;
import android.provider.ThemesContract.ThemesColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.cyanogenmod.theme.chooser.R;
import org.cyanogenmod.theme.util.IconPreviewHelper;
import org.cyanogenmod.theme.util.ThemedTypefaceHelper;

import java.util.ArrayList;
import java.util.List;

public class ThemeFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final int ANIMATE_START_DELAY = 200;
    public static final int ANIMATE_DURATION = 800;
    public static final int ANIMATE_INTERPOLATE_FACTOR = 3;

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

    private static ComponentName[] sIconComponents;

    private String mPkgName;
    private Typeface mTypefaceNormal;
    private int mBatteryStyle;

    private ViewGroup mScrollView;
    private ViewGroup mScrollContent;
    private ViewGroup mPreviewContent; // Contains icons, font, nav/status etc. Not wallpaper

    //Status Bar Views
    private ImageView mBluetooth;
    private ImageView mWifi;
    private ImageView mSignal;
    private ImageView mBattery;
    private TextView mClock;

    // Other Misc Preview Views
    private ImageView mWallpaper;
    private ViewGroup mStatusBar;
    private TextView mFontPreview;
    private ViewGroup mIconContainer;

    // Nav Bar Views
    private ViewGroup mNavBar;
    private ImageView mBackButton;
    private ImageView mHomeButton;
    private ImageView mRecentButton;

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
        mPkgName = getArguments().getString("pkgName");
        mBatteryStyle = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY, 0);

        getIconComponents(getActivity());
        ThemedTypefaceHelper helper = new ThemedTypefaceHelper();
        helper.load(getActivity(), mPkgName);
        mTypefaceNormal = helper.getTypeface(Typeface.NORMAL);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.v2_fragment_pager_list, container, false);

        mScrollView = (ViewGroup) v.findViewById(android.R.id.list);
        mScrollContent = (ViewGroup) mScrollView.getChildAt(0);
        mPreviewContent = (ViewGroup) v.findViewById(R.id.preview_container);

        // Status Bar
        mStatusBar = (ViewGroup) v.findViewById(R.id.status_bar);
        mBluetooth = (ImageView) v.findViewById(R.id.bluetooth_icon);
        mWifi = (ImageView) v.findViewById(R.id.wifi_icon);
        mSignal = (ImageView) v.findViewById(R.id.signal_icon);
        mBattery = (ImageView) v.findViewById(R.id.battery);
        mClock = (TextView) v.findViewById(R.id.clock);

        // Wallpaper / Font / Icons
        mWallpaper = (ImageView) v.findViewById(R.id.wallpaper);
        mFontPreview = (TextView) v.findViewById(R.id.font_preview);
        mFontPreview.setTypeface(mTypefaceNormal);
        mIconContainer = (ViewGroup) v.findViewById(R.id.icon_container);

        // Nav Bar
        mNavBar = (ViewGroup) v.findViewById(R.id.navigation_bar);
        mBackButton = (ImageView) v.findViewById(R.id.back_button);
        mHomeButton = (ImageView) v.findViewById(R.id.home_button);
        mRecentButton = (ImageView) v.findViewById(R.id.recent_button);

        getLoaderManager().initLoader(0, null, this);

        return v;
    }

    public void expand() {
        ViewGroup.LayoutParams layoutParams = mScrollContent.getLayoutParams();
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        mScrollContent.setLayoutParams(layoutParams);

        layoutParams = mPreviewContent.getLayoutParams();
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;

        for (int i = 0; i < mPreviewContent.getChildCount(); i++) {
            View child = mPreviewContent.getChildAt(i);
            RelativeLayout.LayoutParams layout =
                    (RelativeLayout.LayoutParams) child.getLayoutParams();
            int botMargin = (int) child.getContext().getResources()
                    .getDimension(R.dimen.expanded_preview_margin);
            layout.setMargins(0, 0, 0, botMargin);
            child.setLayoutParams(layout);
        }
        mScrollContent.requestLayout();
        animateChildren(true);
        animateWallpaperOut();

    }

    public void collapse() {
        ViewGroup.LayoutParams layoutParams = mPreviewContent.getLayoutParams();
        Resources resources = mPreviewContent.getResources();
        layoutParams.height = (int) resources.getDimension(R.dimen.theme_preview_height);

        for (int i = 0; i < mPreviewContent.getChildCount(); i++) {
            View child = mPreviewContent.getChildAt(i);
            RelativeLayout.LayoutParams layout =
                    (RelativeLayout.LayoutParams) child.getLayoutParams();
            layout.setMargins(0, 0, 0, 0);
            child.setLayoutParams(layout);
        }
        mPreviewContent.requestLayout();

        animateChildren(false);
        animateWallpaperIn();
    }

    // This will animate the children's vertical value between the existing and
    // new layout changes
    private void animateChildren(final boolean isExpanding) {
        final ViewGroup root = (ViewGroup) getActivity().getWindow()
                .getDecorView().findViewById(android.R.id.content);

        // Get the child's current location
        final List<Float> prevYs = new ArrayList<Float>();
        for (int i = 0; i < mPreviewContent.getChildCount(); i++) {
            final View v = mPreviewContent.getChildAt(i);
            int[] pos = new int[2];
            v.getLocationInWindow(pos);
            prevYs.add((float) pos[1]);
        }

        // Grab the child's new location and animate from prev to current loc.
        final ViewTreeObserver observer = mScrollContent.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                observer.removeOnPreDrawListener(this);

                for (int i = mPreviewContent.getChildCount() - 1; i >= 0; i--) {
                    final View v = mPreviewContent.getChildAt(i);

                    float prevY;
                    float endY;
                    if (i >= prevYs.size()) {
                        // View is being created
                        prevY = mPreviewContent.getTop() + mPreviewContent.getHeight();
                        endY = v.getY();
                    } else {
                        prevY = prevYs.get(i);
                        int[] endPos = new int[2];
                        v.getLocationInWindow(endPos);
                        endY = endPos[1];
                    }

                    v.setTranslationY((prevY - endY));
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


                }
                return false;
            }
        });
    }

    private void animateWallpaperOut() {
        int[] location = new int[2];
        mWallpaper.getLocationOnScreen(location);

        final int prevY = location[1];

        final ViewTreeObserver observer = mScrollContent.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                observer.removeOnPreDrawListener(this);

                int[] location = new int[2];
                mWallpaper.getLocationOnScreen(location);
                final int newY = location[1];

                mWallpaper.setTranslationY(prevY - newY);
                mWallpaper.animate()
                        .alpha(0)
                        .setDuration(300)
                        .withEndAction(new Runnable() {
                            public void run() {
                                mWallpaper.setVisibility(View.GONE);
                            }
                        });
                return false;
            }
        });
    }

    private void animateWallpaperIn() {
                mWallpaper.setTranslationY(0);
                mWallpaper.animate()
                        .setStartDelay(ANIMATE_START_DELAY)
                        .alpha(1f)
                        .setDuration(300)
                        .withEndAction(new Runnable() {
                            public void run() {
                                mWallpaper.setVisibility(View.VISIBLE);
                            }
                        });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                ThemesColumns.PKG_NAME,
                ThemesColumns.TITLE,
                ThemesColumns.WALLPAPER_URI,
                ThemesColumns.HOMESCREEN_URI,
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
                PreviewColumns.ICON_PREVIEW_4
        };

        Uri uri = ThemesContract.PreviewColumns.CONTENT_URI;
        String selection = ThemesContract.ThemesColumns.PKG_NAME + "= ?";
        String[] selectionArgs = new String[]{mPkgName};
        return new CursorLoader(getActivity(), uri, projection, selection, selectionArgs, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
        c.moveToFirst();
        loadWallpaper(c);
        loadStatusBar(c);
        loadIcons(c);
        loadNavBar(c);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private void loadWallpaper(Cursor c) {
        int wpIdx = c.getColumnIndex(PreviewColumns.WALLPAPER_PREVIEW);
        Bitmap bitmap = loadBitmapBlob(c, wpIdx);
        mWallpaper.setImageBitmap(bitmap);
    }

    private void loadStatusBar(Cursor c) {
        int backgroundIdx = c.getColumnIndex(PreviewColumns.STATUSBAR_BACKGROUND);
        int wifiIdx = c.getColumnIndex(PreviewColumns.STATUSBAR_WIFI_ICON);
        int wifiMarginIdx = c.getColumnIndex(PreviewColumns.STATUSBAR_WIFI_COMBO_MARGIN_END);
        int bluetoothIdx = c.getColumnIndex(PreviewColumns.STATUSBAR_BLUETOOTH_ICON);
        int signalIdx = c.getColumnIndex(PreviewColumns.STATUSBAR_SIGNAL_ICON);
        int batteryIdx = c.getColumnIndex(getBatteryIndex(mBatteryStyle));
        int clockColorIdx = c.getColumnIndex(PreviewColumns.STATUSBAR_CLOCK_TEXT_COLOR);

        Bitmap background = loadBitmapBlob(c, backgroundIdx);
        Bitmap bluetoothIcon = loadBitmapBlob(c, bluetoothIdx);
        Bitmap wifiIcon = loadBitmapBlob(c, wifiIdx);
        Bitmap signalIcon = loadBitmapBlob(c, signalIdx);
        Bitmap batteryIcon = loadBitmapBlob(c, batteryIdx);
        int wifiMargin = c.getInt(wifiMarginIdx);
        int clockTextColor = c.getInt(clockColorIdx);

        mStatusBar.setBackground(new BitmapDrawable(getActivity().getResources(), background));
        mBluetooth.setImageBitmap(bluetoothIcon);
        mWifi.setImageBitmap(wifiIcon);
        mSignal.setImageBitmap(signalIcon);
        mBattery.setImageBitmap(batteryIcon);
        mClock.setTextColor(clockTextColor);

        ViewGroup.MarginLayoutParams params =
                (ViewGroup.MarginLayoutParams) mWifi.getLayoutParams();
        params.setMarginEnd(wifiMargin);
        mWifi.requestLayout();

        if (mBatteryStyle == 4) {
            mBattery.setVisibility(View.GONE);
        } else {
            mBattery.setVisibility(View.VISIBLE);
        }
    }

    private String getBatteryIndex(int type) {
        switch(type) {
            case 2:
                return PreviewColumns.STATUSBAR_BATTERY_CIRCLE;
            case 5:
                return PreviewColumns.STATUSBAR_BATTERY_LANDSCAPE;
            default:
                return PreviewColumns.STATUSBAR_BATTERY_PORTRAIT;
        }
    }

    private void loadIcons(Cursor c) {
        int[] iconIdx = new int[4];
        iconIdx[0] = c.getColumnIndex(PreviewColumns.ICON_PREVIEW_1);
        iconIdx[1] =  c.getColumnIndex(PreviewColumns.ICON_PREVIEW_2);
        iconIdx[2] = c.getColumnIndex(PreviewColumns.ICON_PREVIEW_3);
        iconIdx[3] = c.getColumnIndex(PreviewColumns.ICON_PREVIEW_4);

        // Set the icons. If the provider does not have an icon preview then
        // fall back to the default icon set
        IconPreviewHelper helper = new IconPreviewHelper(getActivity(), "");
        for(int i=0; i < mIconContainer.getChildCount() && i < iconIdx.length; i++) {
            ImageView v = (ImageView) mIconContainer.getChildAt(i);
            Bitmap bitmap = loadBitmapBlob(c, iconIdx[i]);
            if (bitmap == null) {
                ComponentName component = sIconComponents[i];
                Drawable icon = helper.getDefaultIcon(component.getPackageName(),
                        component.getClassName());
                v.setImageDrawable(icon);
            } else {
                v.setImageBitmap(bitmap);
            }
        }
    }

    private void loadNavBar(Cursor c) {
        int backButtonIdx = c.getColumnIndex(PreviewColumns.NAVBAR_BACK_BUTTON);
        int homeButtonIdx = c.getColumnIndex(PreviewColumns.NAVBAR_HOME_BUTTON);
        int recentButtonIdx = c.getColumnIndex(PreviewColumns.NAVBAR_RECENT_BUTTON);
        int backgroundIdx = c.getColumnIndex(PreviewColumns.STATUSBAR_BACKGROUND);

        Bitmap background = loadBitmapBlob(c, backgroundIdx);
        Bitmap backButton = loadBitmapBlob(c, backButtonIdx);
        Bitmap homeButton = loadBitmapBlob(c, homeButtonIdx);
        Bitmap recentButton = loadBitmapBlob(c, recentButtonIdx);

        mNavBar.setBackground(new BitmapDrawable(getActivity().getResources(), background));
        mBackButton.setImageBitmap(backButton);
        mHomeButton.setImageBitmap(homeButton);
        mRecentButton.setImageBitmap(recentButton);
    }

    private Bitmap loadBitmapBlob(Cursor cursor, int columnIdx) {
        byte[] blob = cursor.getBlob(columnIdx);
        if (blob == null) return null;
        return BitmapFactory.decodeByteArray(blob, 0, blob.length);
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
}
