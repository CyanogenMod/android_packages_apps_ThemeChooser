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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;

import org.cyanogenmod.theme.chooser.R;

public class ChooserActivity extends FragmentActivity {

    private PagerContainer mContainer;
    private ThemeViewPager mPager;
    private PagerAdapter mAdapter;
    private boolean mExpanded = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.v2_activity_main);

        mContainer = (PagerContainer) findViewById(R.id.pager_container);
        mPager = (ThemeViewPager) findViewById(R.id.viewpager);

        mAdapter = new MyAdapter(getSupportFragmentManager());
        mPager.setOnClickListener(mPagerClickListener);
        mPager.setAdapter(mAdapter);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, dm);
        mPager.setPageMargin(margin);
        mPager.setOffscreenPageLimit(3);
    }

    private View.OnClickListener mPagerClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final FrameLayout.LayoutParams layout =
                    (FrameLayout.LayoutParams) mPager.getLayoutParams();
            mExpanded = !mExpanded;
            if (mExpanded) {
                mContainer.expand();
                ThemeFragment f = (ThemeFragment) getSupportFragmentManager()
                        .findFragmentByTag(getFragmentTag(mPager.getCurrentItem()));
                f.expand();
            } else {
                mContainer.collapse();
                ThemeFragment f = (ThemeFragment) getSupportFragmentManager()
                        .findFragmentByTag(getFragmentTag(mPager.getCurrentItem()));
                f.collapse();
            }
        }
    };

    private String getFragmentTag(int pos){
        return "android:switcher:"+R.id.viewpager+":"+pos;
    }

    public static class MyAdapter extends FragmentPagerAdapter {
        public MyAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public Fragment getItem(int position) {
            return ThemeFragment.newInstance();
        }
    }
}
