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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.cyanogenmod.theme.util.BootAnimationHelper;
import org.cyanogenmod.theme.widget.PartAnimationDrawable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class BootAniPreviewFragment extends Fragment {
    private static final String TAG = "ThemeChooser";
    private static final String PKG_EXTRA = "pkg_extra";

    private String mPkgName;
    private ImageView mPreview;
    private ProgressBar mLoadingProgress;
    private TextView mNoPreviewTextView;
    private boolean mPreviewLoaded = false;
    private boolean mIsVisibileToUser = false;
    private boolean mAnimationStarted = false;
    private List<PartAnimationDrawable> mAnimationParts;
    private int mCurrentAnimationPartIndex;
    private PartAnimationDrawable mCurrentAnimationPart;
    private Timer mTimer;
    private Object mAnimationLock;

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
        mAnimationLock = new Object();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_boot_animation_preview, container, false);
        mPreview = (ImageView) view.findViewById(R.id.animated_preview);
        mLoadingProgress = (ProgressBar) view.findViewById(R.id.loading_progress);
        mNoPreviewTextView = (TextView) view.findViewById(R.id.no_preview);
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        destroyAnimation();
        mPreviewLoaded = false;
        if (mTimer != null) mTimer.cancel();
    }

    @Override
    public void onResume() {
        super.onResume();
        (new AnimationLoader(getActivity(), mPkgName)).execute();
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

    public void destroyAnimation() {
        mPreview.setImageDrawable(null);
        synchronized (mAnimationLock) {
            if (mAnimationParts == null) return;
            for (PartAnimationDrawable anim : mAnimationParts) {
                final int numFrames = anim.getNumberOfFrames();
                for (int i = 0; i < numFrames; i++) {
                    Drawable d = anim.getFrame(i);
                    if (d instanceof BitmapDrawable) {
                        ((BitmapDrawable) d).getBitmap().recycle();
                    }
                }
            }
            mAnimationParts.clear();
        }
        mCurrentAnimationPart = null;
        mCurrentAnimationPartIndex = 0;
        mAnimationStarted = false;
    }

    private void startAnimation() {
        if (mIsVisibileToUser) {
            mTimer = new Timer();
            long startTime = 100;
            for (PartAnimationDrawable anim : mAnimationParts) {
                if (anim.isOneShot()) {
                    for (int i = 0; i < anim.getPlayCount(); i++) {
                        mTimer.schedule(new AnimationUpdateTask(anim), startTime);
                        startTime += anim.getAnimationDuration();
                    }
                } else {
                    mTimer.schedule(new AnimationUpdateTask(anim), startTime);
                }
            }
            mAnimationStarted = true;
        } else {
            if (mAnimationParts != null && mAnimationParts.size() > 0)
                mPreview.setImageDrawable(mAnimationParts.get(0).getFrame(0));
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
            InputStream is;
            if ("default".equals(mPkgName)) {
                try {
                    is = new ZipInputStream(
                            new FileInputStream(BootAnimationHelper.SYSTEM_BOOT_ANI_PATH));
                } catch (FileNotFoundException e) {
                    return Boolean.FALSE;
                }
            } else {
                PackageManager pm = mContext.getPackageManager();
                try {
                    ApplicationInfo ai = pm.getApplicationInfo(mPkgName, 0);
                    ZipFile zip = new ZipFile(new File(ai.sourceDir));
                    is = zip.getInputStream(zip.getEntry(
                            BootAnimationHelper.THEME_INTERNAL_BOOT_ANI_PATH));
                } catch (PackageManager.NameNotFoundException e) {
                    return Boolean.FALSE;
                } catch (ZipException e) {
                    return Boolean.FALSE;
                } catch (IOException e) {
                    return Boolean.FALSE;
                }
            }
            if (is != null) {
                try {
                    synchronized (mAnimationLock) {
                        mAnimationParts = BootAnimationHelper.loadAnimation(mContext, is);
                    }
                } catch (IOException e) {
                    return Boolean.FALSE;
                }
            }
            return Boolean.TRUE;
        }

        @Override
        protected void onPostExecute(Boolean isSuccess) {
            super.onPostExecute(isSuccess);
            mLoadingProgress.setVisibility(View.INVISIBLE);
            if (Boolean.TRUE.equals(isSuccess) && mAnimationParts != null) {
                mPreviewLoaded = true;
                startAnimation();
            } else {
                mNoPreviewTextView.setVisibility(View.VISIBLE);
                Log.e(TAG, "Unable to load boot animation for preview.");
            }
        }
    }

    class AnimationUpdateTask extends TimerTask {
        private PartAnimationDrawable mAnimation;
        public AnimationUpdateTask(PartAnimationDrawable anim) {
            mAnimation = anim;
        }

        @Override
        public void run() {
            mPreview.post(new Runnable() {
                @Override
                public void run() {
                    mPreview.setImageDrawable(mAnimation);
                    mAnimation.stop();
                    mAnimation.start();
                }
            });
        }
    }
}