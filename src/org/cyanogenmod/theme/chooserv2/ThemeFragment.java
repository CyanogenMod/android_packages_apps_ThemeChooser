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

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import org.cyanogenmod.theme.chooser.R;

import java.util.ArrayList;
import java.util.List;

public class ThemeFragment extends Fragment {
    public static final int ANIMATE_START_DELAY = 75;
    public static final int ANIMATE_DURATION = 500;
    public static final int ANIMATE_INTERPOLATE_FACTOR = 3;
    private ListView mListView;
    private ListAdapter mListAdapter;

    static ThemeFragment newInstance() {
        ThemeFragment f = new ThemeFragment();
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

        mListView = (ListView) v.findViewById(android.R.id.list);

        return v;
    }

    public void expand() {
        ((ThemePreviewAdapter) mListAdapter).setExpanded(true);
        ((ThemePreviewAdapter) mListAdapter).notifyDataSetChanged();
        animateChildren();
    }


    public void collapse() {
        ((ThemePreviewAdapter) mListAdapter).setExpanded(false);
        ((ThemePreviewAdapter) mListAdapter).notifyDataSetChanged();
        animateChildren();
    }

    // This will animate the children's vertical value between the existing and
    // new layout changes
    private void animateChildren() {
        // Animate each child in the listview
        final List<Float> prevYs = new ArrayList<Float>();
        for (int i = 0; i < mListView.getChildCount(); i++) {
            final View v = mListView.getChildAt(i);
            prevYs.add(v.getY());

            final ViewTreeObserver observer = mListView.getViewTreeObserver();
            observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                public boolean onPreDraw() {
                    observer.removeOnPreDrawListener(this);
                    for (int i = 0; i < mListView.getChildCount(); i++) {
                        View v = mListView.getChildAt(i);
                        float prevY = prevYs.get(i);
                        final float endY = v.getY();
                          v.setTranslationY(prevY - endY);
                          v.animate()
                               .setStartDelay(ANIMATE_START_DELAY)
                               .translationY(0)
                               .setDuration(ANIMATE_DURATION)
                               .setInterpolator(
                                       new DecelerateInterpolator(ANIMATE_INTERPOLATE_FACTOR));
                    }
                    return false;
                }
            });
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mListAdapter = new ThemePreviewAdapter(getActivity(), null);
        mListView.setAdapter(mListAdapter);
    }

    public static class ThemePreviewAdapter extends BaseAdapter {
        public static final String PLACEHOLDER_TAG = "placeholder";
        private static final int PLACEHOLDER_POSITION = 0;

        int[] layouts = {0,
                R.layout.v2item_statusbar,
                R.layout.v2item_font,
                R.layout.v2item_icon,
                R.layout.v2item_navbar};

        private Context mContext;
        private LayoutInflater mInflater;
        private boolean mIsExpanded;
        private List<String> mList;

        public ThemePreviewAdapter(Context context, List<String> list) {
            mContext = context;
            mInflater = LayoutInflater.from(context);
            mList = list;
        }

        public void setExpanded(boolean isExpanded) {
            mIsExpanded = isExpanded;
        }

        @Override
        public int getCount() {
            return layouts.length;
        }

        @Override
        public Object getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // This is just prototype code, needs to actually use convertView and
            // view holder patterns
            View view;
            if (position == PLACEHOLDER_POSITION) {
                view = new View(mContext);
                view.setTag(PLACEHOLDER_TAG);

                int height = 0;
                if (mIsExpanded) {
                    //TODO: There is probably a better way than hardcoding a height value
                    //Maybe seperate expanded/collapsed layouts for our child views,
                    //Or pass the expand mode through onMeasure.
                    height = 200;
                }

                ListView.LayoutParams layout =
                        new ListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
                view.setLayoutParams(layout);
            } else {
                view = mInflater.inflate(layouts[position], null, false);
                if (mIsExpanded) {
                    AbsListView.LayoutParams layout =
                            (AbsListView.LayoutParams) view.getLayoutParams();
                    if (layout == null) {
                        layout = new AbsListView.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, 400);
                    }
                    view.setLayoutParams(layout);
                }
            }

            return view;
        }
    }
}
