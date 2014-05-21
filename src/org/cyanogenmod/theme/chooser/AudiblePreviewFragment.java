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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ThemeUtils;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.CustomTheme;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;


public class AudiblePreviewFragment extends Fragment {
    private static final String PKG_EXTRA = "pkg_extra";

    private static final LinearLayout.LayoutParams ITEM_LAYOUT_PARAMS =
            new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);

    private String mPkgName;
    private LinearLayout mContent;
    private SparseArray<MediaPlayer> mMediaPlayers;

    static AudiblePreviewFragment newInstance(String pkgName) {
        final AudiblePreviewFragment f = new AudiblePreviewFragment();
        final Bundle args = new Bundle();
        args.putString(PKG_EXTRA, pkgName);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPkgName = getArguments().getString(PKG_EXTRA);
        mMediaPlayers = new SparseArray<MediaPlayer>(3);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.audibles_preview, container, false);
        mContent = (LinearLayout) v.findViewById(R.id.audibles_layout);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAudibles();
    }

    @Override
    public void onPause() {
        super.onPause();
        freeMediaPlayers();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (!isVisibleToUser) {
            stopMediaPlayers();
        }
    }

    private void freeMediaPlayers() {
        final int N = mMediaPlayers.size();
        for (int i = 0; i < N; i++) {
            MediaPlayer mp = mMediaPlayers.get(mMediaPlayers.keyAt(i));
            if (mp != null) {
                mp.stop();
                mp.release();
            }
        }
        mMediaPlayers.clear();
    }

    private View.OnClickListener mPlayPauseClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MediaPlayer mp = (MediaPlayer) v.getTag();
            if (mp != null) {
                if (mp.isPlaying()) {
                    ((ImageView) v).setImageResource(android.R.drawable.ic_media_play);
                    mp.pause();
                    mp.seekTo(0);
                } else {
                    stopMediaPlayers();
                    ((ImageView) v).setImageResource(android.R.drawable.ic_media_pause);
                    mp.start();
                }
            }
        }
    };

    private MediaPlayer.OnCompletionListener mPlayCompletionListener
            = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            final int numChildern = mContent.getChildCount();
            for (int i = 0; i < numChildern; i++) {
                ((ImageView) mContent.getChildAt(i).findViewById(R.id.btn_play_pause))
                        .setImageResource(android.R.drawable.ic_media_play);
            }
        }
    };

    private void stopMediaPlayers() {
        if (mContent == null) return;
        final int numChildern = mContent.getChildCount();
        for (int i = 0; i < numChildern; i++) {
            ImageView iv = (ImageView) mContent.getChildAt(i).findViewById(R.id.btn_play_pause);
            if (iv != null) {
                iv.setImageResource(android.R.drawable.ic_media_play);
                MediaPlayer mp = (MediaPlayer) iv.getTag();
                if (mp != null && mp.isPlaying()) {
                    mp.pause();
                    mp.seekTo(0);
                }
            }
        }
    }

    private void loadAudibles() {
        mContent.removeAllViews();
        if (CustomTheme.HOLO_DEFAULT.equals(mPkgName)) {
            loadSystemAudible(RingtoneManager.TYPE_ALARM);
            loadSystemAudible(RingtoneManager.TYPE_NOTIFICATION);
            loadSystemAudible(RingtoneManager.TYPE_RINGTONE);
        } else {
            try {
                final Context themeCtx = getActivity().createPackageContext(mPkgName, 0);
                PackageInfo pi = getActivity().getPackageManager().getPackageInfo(mPkgName, 0);
                loadThemeAudible(themeCtx, RingtoneManager.TYPE_ALARM, pi);
                loadThemeAudible(themeCtx, RingtoneManager.TYPE_NOTIFICATION, pi);
                loadThemeAudible(themeCtx, RingtoneManager.TYPE_RINGTONE, pi);
            } catch (PackageManager.NameNotFoundException e) {
                return;
            }
        }
    }

    private void loadThemeAudible(Context themeCtx, int type, PackageInfo pi) {
        if (pi.isLegacyThemeApk) {
            loadLegacyThemeAudible(themeCtx, type, pi);
            return;
        }
        AssetManager assetManager = themeCtx.getAssets();
        String assetPath;
        switch (type) {
            case RingtoneManager.TYPE_ALARM:
                assetPath = "alarms";
                break;
            case RingtoneManager.TYPE_NOTIFICATION:
                assetPath = "notifications";
                break;
            case RingtoneManager.TYPE_RINGTONE:
                assetPath = "ringtones";
                break;
            default:
                assetPath = null;
                break;
        }
        if (assetPath != null) {
            try {
                String[] assetList = assetManager.list(assetPath);
                if (assetList != null && assetList.length > 0) {
                    AssetFileDescriptor afd = assetManager.openFd(assetPath
                            + File.separator + assetList[0]);
                    MediaPlayer mp = initAudibleMediaPlayer(afd, type);
                    if (mp != null) {
                        addAudibleToLayout(type, mp);
                    }
                }
            } catch (IOException e) {
                mMediaPlayers.put(type, null);
            }
        }
    }

    private void loadLegacyThemeAudible(Context themeCtx, int type, PackageInfo pi) {
        if (pi.legacyThemeInfos == null || pi.legacyThemeInfos.length == 0)
            return;
        AssetManager assetManager = themeCtx.getAssets();
        String assetPath;
        switch (type) {
            case RingtoneManager.TYPE_NOTIFICATION:
                assetPath = pi.legacyThemeInfos[0].notificationFileName;
                break;
            case RingtoneManager.TYPE_RINGTONE:
                assetPath = pi.legacyThemeInfos[0].ringtoneFileName;
                break;
            default:
                assetPath = null;
                break;
        }
        if (assetPath != null) {
            try {
                AssetFileDescriptor afd = assetManager.openFd(assetPath);
                MediaPlayer mp = initAudibleMediaPlayer(afd, type);
                if (mp != null) {
                    addAudibleToLayout(type, mp);
                }
            } catch (IOException e) {
                mMediaPlayers.put(type, null);
            }
        }
    }

    private void loadSystemAudible(int type) {
        final String audiblePath = ThemeUtils.getDefaultAudiblePath(type);
        if (audiblePath != null && (new File(audiblePath)).exists()) {
            try {
                MediaPlayer mp = initAudibleMediaPlayer(audiblePath, type);
                addAudibleToLayout(type, mp);
            } catch (IOException e) {
                mMediaPlayers.put(type, null);
            }
        }
    }

    private MediaPlayer initAudibleMediaPlayer(String audiblePath, int type) throws IOException {
        MediaPlayer mp = mMediaPlayers.get(type);
        if (mp == null) {
            mp = new MediaPlayer();
            mMediaPlayers.put(type, mp);
        } else {
            mp.reset();
        }
        mp.setDataSource(audiblePath);
        mp.prepare();
        mp.setOnCompletionListener(mPlayCompletionListener);
        return mp;
    }

    private MediaPlayer initAudibleMediaPlayer(AssetFileDescriptor afd, int type) throws IOException {
        MediaPlayer mp = mMediaPlayers.get(type);
        if (mp == null) {
            mp = new MediaPlayer();
            mMediaPlayers.put(type, mp);
        } else {
            mp.reset();
        }
        mp.setDataSource(afd.getFileDescriptor(),
                afd.getStartOffset(), afd.getLength());
        mp.prepare();
        mp.setOnCompletionListener(mPlayCompletionListener);
        return mp;
    }

    private void addAudibleToLayout(int type, MediaPlayer mp) {
        View view = View.inflate(getActivity(), R.layout.audible_preview_item, null);
        TextView tv = (TextView) view.findViewById(R.id.audible_name);
        switch (type) {
            case RingtoneManager.TYPE_ALARM:
                tv.setText(R.string.alarm_label);
                break;
            case RingtoneManager.TYPE_NOTIFICATION:
                tv.setText(R.string.notification_label);
                break;
            case RingtoneManager.TYPE_RINGTONE:
                tv.setText(R.string.ringtone_label);
                break;
        }
        ImageView iv = (ImageView) view.findViewById(R.id.btn_play_pause);
        iv.setTag(mp);
        iv.setOnClickListener(mPlayPauseClickListener);
        mContent.addView(view, ITEM_LAYOUT_PARAMS);
    }
}
