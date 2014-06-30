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
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.ScrollView;

import org.cyanogenmod.theme.chooser.R;

import java.util.ArrayList;
import java.util.List;

public class ThemeFragment extends Fragment {
    public static final int ANIMATE_START_DELAY = 75;
    public static final int ANIMATE_DURATION = 500;
    public static final int ANIMATE_INTERPOLATE_FACTOR = 3;
    private ScrollView mScrollView;
    private ViewGroup mScrollContent;

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
    }

    /**
     * The Fragment's UI is just a simple text view showing its
     * instance number.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ViewPager.LayoutParams params = new ViewPager.LayoutParams();
        params.width = ViewPager.LayoutParams.MATCH_PARENT;
        params.height = ViewPager.LayoutParams.MATCH_PARENT;

        View v = inflater.inflate(R.layout.v2_fragment_pager_list, container, false);
        v.setLayoutParams(params);

        mScrollView = (ScrollView) v.findViewById(android.R.id.list);
        mScrollContent = (ViewGroup) mScrollView.getChildAt(0);

        return v;
    }

    public void expand() {
        for (int i = 0; i < mScrollContent.getChildCount(); i++) {
            View child = mScrollContent.getChildAt(i);
            ViewGroup.LayoutParams layout = child.getLayoutParams();
            layout.height = 400;
            child.setLayoutParams(layout);
        }
        mScrollContent.requestLayout();
        animateChildren();
    }


    public void collapse() {
        for (int i = 0; i < mScrollContent.getChildCount(); i++) {
            View child = mScrollContent.getChildAt(i);
            ViewGroup.LayoutParams layout = child.getLayoutParams();
            layout.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            child.setLayoutParams(layout);
        }
        mScrollContent.requestLayout();

        animateChildren();
    }

    // This will animate the children's vertical value between the existing and
    // new layout changes
    private void animateChildren() {
        final ViewGroup root = (ViewGroup) getActivity().getWindow()
                .getDecorView().findViewById(android.R.id.content);

        // Get the child's current location
        final List<Float> prevYs = new ArrayList<Float>();
        for (int i = 0; i < mScrollContent.getChildCount(); i++) {
            final View v = mScrollContent.getChildAt(i);
            int[] pos = new int[2];
            v.getLocationInWindow(pos);
            prevYs.add((float) pos[1]);
        }

        // Grab the child's new location and animate from prev to current loc.
        final ViewTreeObserver observer = mScrollContent.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                observer.removeOnPreDrawListener(this);

                for (int i = mScrollContent.getChildCount() - 1; i >= 0; i--) {
                    final View v = mScrollContent.getChildAt(i);

                    float prevY;
                    float endY;
                    if (i >= prevYs.size()) {
                        // View is being created
                        prevY = mScrollContent.getTop() + mScrollContent.getHeight();
                        endY = v.getY();
                    } else {
                        prevY = prevYs.get(i);
                        int[] endPos = new int[2];
                        v.getLocationInWindow(endPos);
                        endY = endPos[1];
                    }

                    v.setTranslationY((prevY - endY));
                    root.getOverlay().add(v);

                    v.animate()
                            .setStartDelay(ANIMATE_START_DELAY)
                            .translationY(0)
                            .setDuration(ANIMATE_DURATION)
                            .setInterpolator(
                                    new DecelerateInterpolator(ANIMATE_INTERPOLATE_FACTOR))
                            .withEndAction(new Runnable() {
                                public void run() {
                                    root.getOverlay().remove(v);
                                    mScrollContent.addView(v, 0);
                                }
                            });


                }
                return false;
            }
        });
    }
}
