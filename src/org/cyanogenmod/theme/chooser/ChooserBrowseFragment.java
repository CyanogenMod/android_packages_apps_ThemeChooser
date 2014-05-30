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

import android.content.res.CustomTheme;

import org.cyanogenmod.theme.chooser.WallpaperAndIconPreviewFragment.IconInfo;
import org.cyanogenmod.theme.util.BootAnimationHelper;
import org.cyanogenmod.theme.util.IconPreviewHelper;
import org.cyanogenmod.theme.util.ThemedTypefaceHelper;
import org.cyanogenmod.theme.util.Utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ThemesContract.ThemesColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class ChooserBrowseFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String TAG = ChooserBrowseFragment.class.getCanonicalName();
    public static final String DEFAULT = CustomTheme.HOLO_DEFAULT;

    public ListView mListView;
    public LocalPagerAdapter mAdapter;
    public ArrayList<String> mComponentFilters;

    private Point mMaxImageSize = new Point(); //Size of preview image in listview

    public static ChooserBrowseFragment newInstance(ArrayList<String> componentFilters) {
        ChooserBrowseFragment fragment = new ChooserBrowseFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(ChooserActivity.EXTRA_COMPONENT_FILTER, componentFilters);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_chooser_browse, container, false);
        ArrayList<String> filters = getArguments().getStringArrayList(ChooserActivity.EXTRA_COMPONENT_FILTER);
        mComponentFilters = (filters != null) ? filters : new ArrayList<String>(0);
        mListView = (ListView) v.findViewById(R.id.list);
        mAdapter = new LocalPagerAdapter(getActivity(), null, mComponentFilters);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String pkgName = (String) mAdapter.getItem(position);
                ChooserDetailFragment fragment =  ChooserDetailFragment.newInstance(pkgName, mComponentFilters);
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.content, fragment, ChooserDetailFragment.class.toString());
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });
        getLoaderManager().initLoader(0, null, this);

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        display.getSize(mMaxImageSize);
        mMaxImageSize.y  = (int) getActivity().getResources().getDimension(R.dimen.item_browse_height);

        return v;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String selection;
        String selectionArgs[] = null;
        if (mComponentFilters.isEmpty()) {
            selection = ThemesColumns.PRESENT_AS_THEME + "=?";
            selectionArgs = new String[] {"1"};
        } else {
            StringBuffer sb = new StringBuffer();
            for(int i=0; i < mComponentFilters.size(); i++) {
                sb.append(mComponentFilters.get(i));
                sb.append("=1");
                if (i !=  mComponentFilters.size()-1) {
                    sb.append(" OR ");
                }
            }
            selection = sb.toString();
        }

        // sort in ascending order but make sure the "default" theme is always first
        String sortOrder = "(" + ThemesColumns.IS_DEFAULT_THEME + "=1) DESC, "
                + ThemesColumns.TITLE + " ASC";

        return new CursorLoader(getActivity(), ThemesColumns.CONTENT_URI, null, selection,
                selectionArgs, sortOrder);
    }

    public class LocalPagerAdapter extends CursorAdapter {
        List<String> mFilters;
        Context mContext;

        public LocalPagerAdapter(Context context, Cursor c, List<String> filters) {
            super(context, c, 0);
            mFilters = filters;
            mContext = context;
        }

        @Override
        public Object getItem(int position) {
            mCursor.moveToPosition(position);
            int pkgIdx = mCursor.getColumnIndex(ThemesColumns.PKG_NAME);
            String pkgName = (String) mCursor.getString(pkgIdx);
            return pkgName;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            int titleIdx = mCursor.getColumnIndex(ThemesColumns.TITLE);
            int authorIdx = mCursor.getColumnIndex(ThemesColumns.AUTHOR);
            int hsIdx = mCursor.getColumnIndex(ThemesColumns.HOMESCREEN_URI);
            int wpIdx = mCursor.getColumnIndex(ThemesColumns.WALLPAPER_URI);
            int styleIdx = mCursor.getColumnIndex(ThemesColumns.STYLE_URI);
            int pkgIdx = mCursor.getColumnIndex(ThemesColumns.PKG_NAME);
            int legacyIndex = mCursor.getColumnIndex(ThemesColumns.IS_LEGACY_THEME);
            int defaultIndex = mCursor.getColumnIndex(ThemesColumns.IS_DEFAULT_THEME);

            String pkgName = mCursor.getString(pkgIdx);
            String title = DEFAULT.equals(pkgName) ? mContext.getString(R.string.holo)
                    : mCursor.getString(titleIdx);
            String author = mCursor.getString(authorIdx);
            String hsImagePath = DEFAULT.equals(pkgName) ? mCursor.getString(hsIdx) :
                    mCursor.getString(wpIdx);
            String styleImagePath = mCursor.getString(styleIdx);
            boolean isLegacyTheme = mCursor.getInt(legacyIndex) == 1;
            boolean isDefaultTheme = mCursor.getInt(defaultIndex) == 1;

            ThemeItemHolder item = (ThemeItemHolder) view.getTag();
            item.title.setText(title + (isDefaultTheme ? " "
                    + getString(R.string.default_tag) : ""));
            item.author.setText(author);
            if (mFilters.isEmpty()) {
                bindDefaultView(item, pkgName, hsImagePath, isLegacyTheme);
            } else if (mFilters.contains(ThemesColumns.MODIFIES_BOOT_ANIM)) {
                bindBootAnimView(item, context, pkgName);
            } else if (mFilters.contains(ThemesColumns.MODIFIES_LAUNCHER)) {
                bindWallpaperView(item, pkgName, hsImagePath, isLegacyTheme);
            } else if (mFilters.contains(ThemesColumns.MODIFIES_FONTS)) {
                bindFontView(view, context, pkgName);
            } else if (mFilters.contains(ThemesColumns.MODIFIES_OVERLAYS)) {
                bindOverlayView(item, pkgName, styleImagePath, isLegacyTheme);
            } else if (mFilters.contains(ThemesColumns.MODIFIES_ICONS)) {
                bindDefaultView(item, pkgName, hsImagePath, isLegacyTheme);
                bindIconView(view, context, pkgName);
            } else {
                bindDefaultView(item, pkgName, hsImagePath, isLegacyTheme);
            }
        }

        private void bindDefaultView(ThemeItemHolder item, String pkgName,
                                     String hsImagePath, boolean isLegacyTheme) {
            //Do not load wallpaper if we preview icons
            if (mFilters.contains(ThemesColumns.MODIFIES_ICONS)) return;

            if (isLegacyTheme) {
                item.thumbnail.setTag(pkgName);
            } else {
                item.thumbnail.setTag(hsImagePath);
            }
            item.thumbnail.setImageDrawable(null);

            if (item.thumbnail.getTag() != null) {
                LoadImage loadImageTask = new LoadImage(item.thumbnail, isLegacyTheme, false, pkgName);
                loadImageTask.execute();
            }
        }

        private void bindOverlayView(ThemeItemHolder item, String pkgName,
                                     String styleImgPath, boolean isLegacyTheme) {
            if (isLegacyTheme) {
                item.thumbnail.setTag(pkgName);
            } else {
                item.thumbnail.setTag(styleImgPath);
            }
            item.thumbnail.setImageDrawable(null);

            if (item.thumbnail.getTag() != null) {
                LoadImage loadImageTask = new LoadImage(item.thumbnail, isLegacyTheme, false, pkgName);
                loadImageTask.execute();
            }
        }

        private void bindBootAnimView(ThemeItemHolder item, Context context, String pkgName) {
            (new BootAnimationHelper.LoadBootAnimationImage(item.thumbnail, context, pkgName)).execute();
        }

        private void bindWallpaperView(ThemeItemHolder item, String pkgName,
                                       String hsImagePath, boolean isLegacyTheme) {
            if (isLegacyTheme) {
                item.thumbnail.setTag(pkgName);
            } else {
                item.thumbnail.setTag(hsImagePath);
            }
            item.thumbnail.setImageDrawable(null);

            if (item.thumbnail.getTag() != null) {
                LoadImage loadImageTask = new LoadImage(item.thumbnail, isLegacyTheme, true, pkgName);
                loadImageTask.execute();
            }
        }

        public void bindFontView(View view, Context context, String pkgName) {
            FontItemHolder item = (FontItemHolder) view.getTag();
            ThemedTypefaceHelper helper = new ThemedTypefaceHelper();
            helper.load(mContext, pkgName);
            Typeface typefaceNormal = helper.getTypeface(Typeface.NORMAL);
            Typeface typefaceBold = helper.getTypeface(Typeface.BOLD);
            item.textView.setTypeface(typefaceNormal);
            item.textViewBold.setTypeface(typefaceBold);
        }

        public void bindIconView(View view, Context context, String pkgName) {
            ThemeItemHolder holder = (ThemeItemHolder) view.getTag();
            LoadIconsTask loadImageTask = new LoadIconsTask(context, pkgName, holder.mIconHolders);
            loadImageTask.execute();
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            if (mComponentFilters.isEmpty()) {
                return newDefaultView(context, cursor, parent);
            } else if (mComponentFilters.contains(ThemesColumns.MODIFIES_FONTS)) {
                return newFontView(context, cursor, parent);
            } else if (mComponentFilters.contains(ThemesColumns.MODIFIES_ICONS)) {
                return newDefaultView(context, cursor, parent);
            }
            return newDefaultView(context, cursor, parent);
        }

        private View newDefaultView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View row = inflater.inflate(R.layout.item_store_browse, parent, false);
            ThemeItemHolder item = new ThemeItemHolder();
            item.thumbnail = (ImageView) row.findViewById(R.id.image);
            item.title = (TextView) row.findViewById(R.id.title);
            item.author = (TextView) row.findViewById(R.id.author);
            item.mIconHolders = (ViewGroup) row.findViewById(R.id.icon_container);
            row.setTag(item);
            return row;
        }

        private View newFontView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View row = inflater.inflate(R.layout.item_chooser_browse_font, parent, false);
            FontItemHolder item = new FontItemHolder();
            item.textView = (TextView) row.findViewById(R.id.text1);
            item.textViewBold = (TextView) row.findViewById(R.id.text2);
            item.title = (TextView) row.findViewById(R.id.title);
            item.author = (TextView) row.findViewById(R.id.author);
            row.setTag(item);
            return row;
        }
    }

    public static class ThemeItemHolder {
        ImageView thumbnail;
        TextView title;
        TextView author;
        ViewGroup mIconHolders;
    }

    public static class FontItemHolder extends ThemeItemHolder {
        TextView textView;
        TextView textViewBold;
    }

    public class LoadImage extends AsyncTask<Object, Void, Bitmap> {
        private ImageView imv;
        private String path;
        private boolean isLegacyTheme;
        private boolean showWallpaper;
        private String pkgName;

        public LoadImage(ImageView imv, boolean isLegacyTheme, boolean showWallpaper, String pkgName) {
            this.imv = imv;
            this.path = imv.getTag().toString();
            this.isLegacyTheme = isLegacyTheme;
            this.showWallpaper = showWallpaper;
            this.pkgName = pkgName;
        }

        @Override
        protected Bitmap doInBackground(Object... params) {
            Bitmap bitmap = null;
            Context context = getActivity();
            if (context == null) {
                Log.d(TAG, "Activity was detached, skipping loadImage");
                return null;
            }

            if (!isLegacyTheme) {
                if (DEFAULT.equals(pkgName)) {
                    Resources res = context.getResources();
                    AssetManager assets = new AssetManager();
                    assets.addAssetPath(WallpaperAndIconPreviewFragment.FRAMEWORK_RES);
                    Resources frameworkRes = new Resources(assets, res.getDisplayMetrics(),
                            res.getConfiguration());
                    bitmap = Utils.decodeResource(frameworkRes,
                            com.android.internal.R.drawable.default_wallpaper,
                            mMaxImageSize.x, mMaxImageSize.y);
                } else {
                    if (URLUtil.isAssetUrl(path)) {
                        Context ctx = context;
                        try {
                            ctx = context.createPackageContext(pkgName, 0);
                        } catch (PackageManager.NameNotFoundException e) {

                        }
                        bitmap = Utils.getBitmapFromAsset(ctx, path, mMaxImageSize.x, mMaxImageSize.y);
                    } else if (path != null) {
                        bitmap = Utils.decodeFile(path, mMaxImageSize.x, mMaxImageSize.y);
                    }
                }
            } else {
                try {
                    PackageManager pm = context.getPackageManager();
                    PackageInfo pi = pm.getPackageInfo(path, 0);
                    final Context themeContext = context.createPackageContext(path,
                            Context.CONTEXT_IGNORE_SECURITY);
                    final Resources res = themeContext.getResources();
                    final int resId = showWallpaper ? pi.legacyThemeInfos[0].wallpaperResourceId :
                            pi.legacyThemeInfos[0].previewResourceId;
                    bitmap = Utils.decodeResource(res, resId, mMaxImageSize.x, mMaxImageSize.y);
                } catch (PackageManager.NameNotFoundException e) {
                    bitmap = null;
                }
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (!imv.getTag().toString().equals(path)) {
                return;
            }

            if (result != null && imv != null) {
                imv.setVisibility(View.VISIBLE);
                imv.setImageBitmap(result);
            }
        }
    }


    public static class LoadIconsTask extends AsyncTask<Void, Void, List<IconInfo>> {
        private String mPkgName;
        private Context mContext;
        private ViewGroup mIconViewGroup;

        public LoadIconsTask(Context context, String pkgName, ViewGroup iconViewGroup) {
            mPkgName = pkgName;
            mContext = context.getApplicationContext();
            mIconViewGroup = iconViewGroup;
            mIconViewGroup.setTag(pkgName);
        }

        @Override
        protected List<IconInfo> doInBackground(Void... arg0) {
            List<IconInfo> icons = new ArrayList<IconInfo>();
            IconPreviewHelper helper = new IconPreviewHelper(mContext, mPkgName);

            for (ComponentName component
                    : WallpaperAndIconPreviewFragment.getIconComponents(mContext)) {
                Drawable icon = helper.getIcon(component);
                IconInfo info = new IconInfo(null, icon);
                icons.add(info);
            }

            return icons;
        }

        @Override
        protected void onPostExecute(List<IconInfo> icons) {
            if (!mIconViewGroup.getTag().toString().equals(mPkgName) || icons == null) {
                return;
            }

            mIconViewGroup.removeAllViews();
            for (IconInfo info : icons) {
                LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(0,
                        LayoutParams.WRAP_CONTENT);
                lparams.weight = 1f / icons.size();
                ImageView imageView = new ImageView(mContext);
                int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                       8, mContext.getResources().getDisplayMetrics());
                imageView.setPadding(padding, 0, padding, 0);
                imageView.setLayoutParams(lparams);
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                imageView.setImageDrawable(info.icon);
                mIconViewGroup.addView(imageView);
            }
        }
    }
}
