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

import android.content.Context;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.FileUtils;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.cyanogenmod.theme.util.BootAnimationHelper;
import org.cyanogenmod.theme.widget.BootAniImageView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipFile;

public class BootAniPreviewFragment extends Fragment {
    private static final String TAG = "ThemeChooser";
    private static final String PKG_EXTRA = "pkg_extra";
    private static final String CACHED_SUFFIX = "_bootanimation.zip";

    private String mPkgName;
    private BootAniImageView mPreview;
    private ProgressBar mLoadingProgress;
    private TextView mNoPreviewTextView;
    private boolean mPreviewLoaded = false;
    private boolean mIsVisibileToUser = false;
    private boolean mAnimationStarted = false;

    public static BootAniPreviewFragment newInstance(String pkgName) {
        BootAniPreviewFragment fragment = new BootAniPreviewFragment();
        Bundle args = new Bundle();
        args.putString(PKG_EXTRA, pkgName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPkgName = getArguments().getString(PKG_EXTRA);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_boot_animation_preview, container, false);
        mPreview = (BootAniImageView) view.findViewById(R.id.animated_preview);
        mLoadingProgress = (ProgressBar) view.findViewById(R.id.loading_progress);
        mNoPreviewTextView = (TextView) view.findViewById(R.id.no_preview);
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        mPreviewLoaded = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        new AnimationLoader(getActivity(), mPkgName).execute();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        mIsVisibileToUser = isVisibleToUser;
        if (isVisibleToUser) {
            if (mPreviewLoaded && !mAnimationStarted) {
                startAnimation();
            }
        }
    }

    private void startAnimation() {
        if (mIsVisibileToUser) {
            mPreview.start();
            mAnimationStarted = true;
        }
    }

    class AnimationLoader extends AsyncTask<Void, Void, Boolean> {
        Context mContext;
        String mPkgName;

        public AnimationLoader(Context context, String pkgName) {
            mContext = context;
            mPkgName = pkgName;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mPreview.setImageDrawable(null);
            mLoadingProgress.setVisibility(View.VISIBLE);
            mNoPreviewTextView.setVisibility(View.INVISIBLE);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (mContext == null) {
                return Boolean.FALSE;
            }
            ZipFile zip = null;
            if ("default".equals(mPkgName)) {
                try {
                    zip = new ZipFile(new File(BootAnimationHelper.SYSTEM_BOOT_ANI_PATH));
                } catch (Exception e) {
                    Log.w(TAG, "Unable to load boot animation", e);
                    return Boolean.FALSE;
                }
            } else {
                // check if the bootanimation is cached
                File f = new File(mContext.getCacheDir(), mPkgName + CACHED_SUFFIX);
                if (!f.exists()) {
                    // go easy on cache storage and clear out any previous boot animations
                    clearBootAnimationCache();
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
                mPreview.setBootAnimation(zip);
            } else {
                return Boolean.FALSE;
            }
            return Boolean.TRUE;
        }

        @Override
        protected void onPostExecute(Boolean isSuccess) {
            super.onPostExecute(isSuccess);
            mLoadingProgress.setVisibility(View.INVISIBLE);
            if (Boolean.TRUE.equals(isSuccess)) {
                mPreviewLoaded = true;
                startAnimation();
            } else {
                mNoPreviewTextView.setVisibility(View.VISIBLE);
                Log.e(TAG, "Unable to load boot animation for preview.");
            }
        }
    }

    private void clearBootAnimationCache() {
        File cache = getActivity().getCacheDir();
        if (cache.exists()) {
            for(File f : cache.listFiles()) {
                // volley stores stuff in cache so don't delete the volley directory
                if(!f.isDirectory() && f.getName().endsWith(CACHED_SUFFIX)) f.delete();
            }
        }
    }
}