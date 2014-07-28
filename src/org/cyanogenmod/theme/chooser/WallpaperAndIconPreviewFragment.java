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
package org.cyanogenmod.theme.chooser;

import java.util.ArrayList;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.ThemeConfig;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.webkit.URLUtil;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.cyanogenmod.theme.util.IconPreviewHelper;
import org.cyanogenmod.theme.util.Utils;

public class WallpaperAndIconPreviewFragment extends Fragment
{
    private static final int LOADER_ID_IMAGE = 0;
    private static final int LOADER_ID_ICONS = 1;

    private static final ComponentName COMPONENT_DIALER =
            new ComponentName("com.android.dialer", "com.android.dialer.DialtactsActivity");
    private static final ComponentName COMPONENT_MESSAGING =
            new ComponentName("com.android.mms", "com.android.mms.ui.ConversationList");
    private static final ComponentName COMPONENT_CAMERANEXT =
            new ComponentName("com.cyngn.cameranext", "com.android.camera.CameraLauncher");
    private static final ComponentName COMPONENT_CAMERA =
            new ComponentName("com.android.camera2", "com.android.camera.CameraLauncher");
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

    private static final String PKGNAME_EXTRA = "pkgname";
    private static final String IMAGE_DATA_EXTRA = "url";
    private static final String LEGACY_THEME_EXTRA = "isLegacyTheme";
    private static final String HAS_ICONS_EXTRA = "hasIcons";

    public static final String FRAMEWORK_RES = "/system/framework/framework-res.apk";

    private String mPkgName;
    private String mImageUrl;
    private boolean mIsLegacyTheme;
    private boolean mHasIcons;

    private ImageView mImageView;
    private LinearLayout mIconContainer;
    private TextView mNoPreview;

    static WallpaperAndIconPreviewFragment newInstance(String imageUrl, String pkgName, boolean isLegacyTheme, boolean hasIcons) {
        final WallpaperAndIconPreviewFragment f = new WallpaperAndIconPreviewFragment();
        final Bundle args = new Bundle();
        args.putString(IMAGE_DATA_EXTRA, imageUrl);
        args.putString(PKGNAME_EXTRA, pkgName);
        args.putBoolean(LEGACY_THEME_EXTRA, isLegacyTheme);
        args.putBoolean(HAS_ICONS_EXTRA, hasIcons);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mImageUrl = getArguments().getString(IMAGE_DATA_EXTRA);
        mIsLegacyTheme = getArguments().getBoolean(LEGACY_THEME_EXTRA);
        mHasIcons = getArguments().getBoolean(HAS_ICONS_EXTRA);
        mPkgName = getArguments().getString(PKGNAME_EXTRA);

        getIconComponents(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.image_preview_item, container, false);
        mImageView = (ImageView) view.findViewById(R.id.image);
        mIconContainer = (LinearLayout) view.findViewById(R.id.icon_container);
        mNoPreview = (TextView) view.findViewById(R.id.no_preview);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(LOADER_ID_IMAGE, null, mImageCallbacks);
        if (mHasIcons) {
            getLoaderManager().initLoader(LOADER_ID_ICONS, null, mIconCallbacks);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

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
                } catch (NameNotFoundException e) {
                    // default to COMPONENT_CAMERA
                }
            }

        }

        return sIconComponents;
    }

    private final LoaderCallbacks<Bitmap> mImageCallbacks = new LoaderCallbacks<Bitmap>() {

        @Override
        public Loader<Bitmap> onCreateLoader(int id, Bundle args) {
            return new ImageLoader(getActivity(), mIsLegacyTheme, mPkgName, mImageUrl);
        }

        @Override
        public void onLoadFinished(Loader<Bitmap> loader, Bitmap result) {
            mImageView.setImageBitmap(result);
            if (result == null && !mHasIcons) {
                mNoPreview.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onLoaderReset(Loader<Bitmap> loader) {
        }
    };

    private final LoaderCallbacks<List<IconInfo>> mIconCallbacks = new LoaderCallbacks<List<IconInfo>>() {
        @Override
        public Loader<List<IconInfo>> onCreateLoader(int id, Bundle args) {
            return new IconsLoader(getActivity(), mPkgName);
        }

        @Override
        public void onLoadFinished(Loader<List<IconInfo>> loader, List<IconInfo> infos) {
            final float SHADOW_LARGE_RADIUS = 4.0f;
            final float SHADOW_Y_OFFSET = 2.0f;
            final int SHADOW_LARGE_COLOUR = 0xDD000000;

            mIconContainer.removeAllViews();
            for (IconInfo info : infos) {
                LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(0,
                        LayoutParams.WRAP_CONTENT, 1f);
                lparams.weight = 1f / infos.size();
                TextView tv = new TextView(loader.getContext());
                tv.setShadowLayer(SHADOW_LARGE_RADIUS, 0.0f, SHADOW_Y_OFFSET, SHADOW_LARGE_COLOUR);
                tv.setTextColor(Color.WHITE);
                tv.setGravity(Gravity.CENTER_HORIZONTAL);
                tv.setLayoutParams(lparams);
                tv.setCompoundDrawables(null, info.icon, null, null);
                tv.setText(info.name);

                mIconContainer.addView(tv);
            }
        }

        @Override
        public void onLoaderReset(Loader<List<IconInfo>> loader) {
        }

    };

    public static class ImageLoader extends AsyncTaskLoader<Bitmap> {
        private String mPkgName;
        private boolean mIsLegacyTheme;
        private String mImageUrl;
        private Point mDisplaySize = new Point();

        public ImageLoader(Context context, boolean isLegacyTheme, String pkgName, String imageUrl) {
            super(context);
            mIsLegacyTheme = isLegacyTheme;
            mPkgName = pkgName;
            mImageUrl = imageUrl;
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            display.getSize(mDisplaySize);
            onContentChanged();
        }

        @Override
        protected void onStartLoading() {
            if (takeContentChanged()) {
                forceLoad();
            }
        }

        @Override
        public Bitmap loadInBackground() {
            Bitmap bitmap = null;

            if (mIsLegacyTheme) {
                return loadLegacyImage();
            }

            if (ThemeConfig.HOLO_DEFAULT.equals(mPkgName)) {
                Resources res = getContext().getResources();
                AssetManager assets = new AssetManager();
                assets.addAssetPath(FRAMEWORK_RES);
                Resources frameworkRes = new Resources(assets, res.getDisplayMetrics(),
                        res.getConfiguration());
                bitmap = BitmapFactory.decodeResource(frameworkRes,
                        com.android.internal.R.drawable.default_wallpaper);
            } else {
                if (URLUtil.isAssetUrl(mImageUrl)) {
                    bitmap = Utils.getBitmapFromAsset(getContext(), mImageUrl, mDisplaySize.x,
                            mDisplaySize.y);
                } else {
                    bitmap = BitmapFactory.decodeFile(mImageUrl);
                }
            }
            return bitmap;
        }

        private Bitmap loadLegacyImage() {
            Bitmap bitmap;
            try {
                PackageManager pm = getContext().getPackageManager();
                PackageInfo pi = pm.getPackageInfo(mPkgName, 0);
                final Context themeContext = getContext().createPackageContext(mPkgName,
                        Context.CONTEXT_IGNORE_SECURITY);
                final Resources res = themeContext.getResources();
                bitmap = BitmapFactory.decodeResource(res, pi.legacyThemeInfos[0].previewResourceId);
            } catch (PackageManager.NameNotFoundException e) {
                bitmap = null;
            }
            return bitmap;
        }
    }

    public static class IconsLoader extends AsyncTaskLoader<List<IconInfo>> {
        private String mPkgName;

        public IconsLoader(Context context, String pkgName) {
            super(context);
            mPkgName = pkgName;
            onContentChanged();
        }

        @Override
        protected void onStartLoading() {
            if (takeContentChanged()) {
                forceLoad();
            }
        }

        @Override
        public List<IconInfo> loadInBackground() {
            List<IconInfo> icons = new ArrayList<IconInfo>();
            IconPreviewHelper helper = new IconPreviewHelper(getContext(), mPkgName);

            for (ComponentName component : sIconComponents) {
                Drawable icon = helper.getIcon(component);
                String label = helper.getLabel(component);
                IconInfo info = new IconInfo(label, icon);
                icons.add(info);
            }
            return icons;
        }
    }

    public static class IconInfo {
        public String name;
        public Drawable icon;
        public IconInfo(String name, Drawable drawable) {
            this.name = name;
            this.icon = drawable;
        }
    }
}
