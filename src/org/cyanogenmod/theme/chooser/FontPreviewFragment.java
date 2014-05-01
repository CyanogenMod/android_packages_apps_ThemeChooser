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

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.cyanogenmod.theme.util.ThemedTypefaceHelper;


public class FontPreviewFragment extends Fragment {
    private static final String PKG_EXTRA = "pkg_extra";
    private String mPkgName;

    private Typeface mTypefaceNormal;
    private Typeface mTypefaceBold;
    private Typeface mTypefaceItalic;
    private Typeface mTypefaceBoldItalic;

    private TextView mTv1;
    private TextView mTv2;
    private TextView mTv3;
    private TextView mTv4;

    static FontPreviewFragment newInstance(String pkgName) {
        final FontPreviewFragment f = new FontPreviewFragment();
        final Bundle args = new Bundle();
        args.putString(PKG_EXTRA, pkgName);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPkgName = getArguments().getString(PKG_EXTRA);

        ThemedTypefaceHelper helper = new ThemedTypefaceHelper();
        helper.load(getActivity(), mPkgName);
        mTypefaceNormal = helper.getTypeface(Typeface.NORMAL);
        mTypefaceBold = helper.getTypeface(Typeface.BOLD);
        mTypefaceItalic = helper.getTypeface(Typeface.ITALIC);
        mTypefaceBoldItalic = helper.getTypeface(Typeface.BOLD_ITALIC);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.font_preview_item, container, false);
        mTv1 = (TextView) view.findViewById(R.id.text1);
        mTv2 = (TextView) view.findViewById(R.id.text2);
        mTv3 = (TextView) view.findViewById(R.id.text3);
        mTv4 = (TextView) view.findViewById(R.id.text4);

        mTv1.setTypeface(mTypefaceNormal);
        mTv3.setTypeface(mTypefaceItalic);
        mTv2.setTypeface(mTypefaceBold);
        mTv4.setTypeface(mTypefaceBoldItalic);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }
}
