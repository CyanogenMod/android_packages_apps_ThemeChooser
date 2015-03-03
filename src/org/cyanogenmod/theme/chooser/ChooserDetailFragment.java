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
import android.content.res.ThemeChangeRequest;
import android.content.res.ThemeChangeRequest.RequestType;
import android.content.res.ThemeManager;
import android.content.res.ThemeManager.ThemeChangeListener;
import android.database.Cursor;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ThemesContract;
import android.provider.ThemesContract.MixnMatchColumns;
import android.provider.ThemesContract.ThemesColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.viewpagerindicator.CirclePageIndicator;

import org.cyanogenmod.theme.util.ChooserDetailScrollView;
import org.cyanogenmod.theme.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static android.content.pm.ThemeUtils.SYSTEM_TARGET_API;

public class ChooserDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, ThemeChangeListener {
    public static final HashMap<String, Integer> sComponentToId = new HashMap<String, Integer>();

    private static final String TAG = ChooserDetailFragment.class.getName();
    private static final int LOADER_ID_THEME_INFO = 0;
    private static final int LOADER_ID_APPLIED_THEME = 1;

    // Drawer States
    private static final int DRAWER_CLOSED = 0;
    private static final int DRAWER_PARTIALLY_OPEN = 1;
    private static final int DRAWER_MOSTLY_OPEN = 2;
    private static final int DRAWER_OPEN = 3;

    // Threshold values in "percentage scrolled" to determine what state the drawer is in
    // ex: User opens up the drawer a little bit, taking up 10% of the visible space. The
    // drawer is now considered partially open
    // because CLOSED_THRESHOLD < 10% < PARTIAL_OPEN_THRESHOLD
    private static final int DRAWER_CLOSED_THRESHOLD = 5;
    private static final int DRAWER_PARTIALLY_OPEN_THRESHOLD = 25;
    private static final int DRAWER_MOSTLY_OPEN_THRESHOLD = 90;
    private static final int DRAWER_OPEN_THRESHOLD = 100;

    // Where to scroll when moving to a new state
    private static final int DRAWER_CLOSED_SCROLL_AMOUNT = 0;
    private static final int DRAWER_PARTIALLY_OPEN_AMOUNT = 25;
    private static final int DRAWER_MOSTLY_OPEN_AMOUNT = 75;
    private static final int DRAWER_FULLY_OPEN_AMOUNT = 100;

    private TextView mTitle;
    private TextView mAuthor;
    private TextView mDesignedFor;
    private Button mApply;
    private ViewPager mPager;
    private ThemeDetailPagerAdapter mPagerAdapter;
    private String mPkgName;
    private ChooserDetailScrollView mSlidingPanel;
    private CirclePageIndicator mIndicator;

    private Handler mHandler;
    private Cursor mAppliedThemeCursor;
    private List<String> mAppliedComponents = new ArrayList<String>();
    private HashMap<String, CheckBox> mComponentToCheckbox = new HashMap<String, CheckBox>();

    private boolean mLoadInitialCheckboxStates = true;
    private SparseArray<Boolean> mInitialCheckboxStates = new SparseArray<Boolean>();
    private SparseArray<Boolean> mCurrentCheckboxStates = new SparseArray<Boolean>();

    // allows emphasis on a particular aspect of a theme. ex "mods_icons" would
    // uncheck all components but icons and sets the first preview image to be the icon pack
    private ArrayList<String> mComponentFilters;

    private ThemeManager mService;

    static {
        sComponentToId.put(ThemesColumns.MODIFIES_OVERLAYS, R.id.chk_overlays);
        sComponentToId.put(ThemesColumns.MODIFIES_STATUS_BAR, R.id.chk_status_bar);
        sComponentToId.put(ThemesColumns.MODIFIES_NAVIGATION_BAR, R.id.chk_nav_bar);
        sComponentToId.put(ThemesColumns.MODIFIES_BOOT_ANIM, R.id.chk_boot_anims);
        sComponentToId.put(ThemesColumns.MODIFIES_FONTS, R.id.chk_fonts);
        sComponentToId.put(ThemesColumns.MODIFIES_ICONS, R.id.chk_icons);
        sComponentToId.put(ThemesColumns.MODIFIES_LAUNCHER, R.id.chk_wallpaper);
        sComponentToId.put(ThemesColumns.MODIFIES_LOCKSCREEN, R.id.chk_lockscreen);
        sComponentToId.put(ThemesColumns.MODIFIES_RINGTONES, R.id.chk_ringtones);
        sComponentToId.put(ThemesColumns.MODIFIES_NOTIFICATIONS, R.id.chk_notifications);
        sComponentToId.put(ThemesColumns.MODIFIES_ALARMS, R.id.chk_alarms);
    }

    public static ChooserDetailFragment newInstance(String pkgName, ArrayList<String> componentFilters) {
        ChooserDetailFragment fragment = new ChooserDetailFragment();
        Bundle args = new Bundle();
        args.putString("pkgName", pkgName);
        args.putStringArrayList(ChooserActivity.EXTRA_COMPONENT_FILTER, componentFilters);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mPkgName = getArguments().getString("pkgName");
        ArrayList<String> filters = getArguments().getStringArrayList(ChooserActivity.EXTRA_COMPONENT_FILTER);
        mComponentFilters = (filters != null) ? filters : new ArrayList<String>(0);
        View v = inflater.inflate(R.layout.fragment_chooser_theme_pager_item, container, false);
        mTitle = (TextView) v.findViewById(R.id.title);
        mAuthor = (TextView) v.findViewById(R.id.author);
        mDesignedFor = (TextView) v.findViewById(R.id.designed_for);

        mPager = (ViewPager) v.findViewById(R.id.pager);
        mPager.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                int state = getDrawerState();
                switch(state) {
                    case DRAWER_CLOSED:
                        smoothScrollDrawerTo(DRAWER_PARTIALLY_OPEN);
                        break;
                    case DRAWER_PARTIALLY_OPEN:
                    case DRAWER_MOSTLY_OPEN:
                        smoothScrollDrawerTo(DRAWER_OPEN);
                        break;
                    case DRAWER_OPEN:
                        smoothScrollDrawerTo(DRAWER_CLOSED);
                        break;
                }
            }
        });

        mPagerAdapter = new ThemeDetailPagerAdapter(getChildFragmentManager());
        mPager.setAdapter(mPagerAdapter);


        mIndicator = (CirclePageIndicator) v.findViewById(R.id.titles);
        mIndicator.setViewPager(mPager);

        mApply = (Button) v.findViewById(R.id.apply);
        mApply.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                ThemeChangeRequest request = getThemeChangeRequestForSelectedComponents();
                mService.requestThemeChange(request, true);
                mApply.setText(R.string.applying);
            }
        });

        mSlidingPanel = (ChooserDetailScrollView) v.findViewById(R.id.sliding_layout);

        // Find all the checkboxes for theme components (ex wallpaper)
        for (Map.Entry<String, Integer> entry : sComponentToId.entrySet()) {
            CheckBox componentCheckbox = (CheckBox) v.findViewById(entry.getValue());
            mComponentToCheckbox.put(entry.getKey(), componentCheckbox);
            componentCheckbox.setOnCheckedChangeListener(mComponentCheckChangedListener);
        }

        // Remove the nav bar checkbox if the user has hardware nav keys
        if (!Utils.hasNavigationBar(getActivity())) {
            View navBarCheck = v.findViewById(R.id.chk_nav_bar);
            if (navBarCheck != null) {
                navBarCheck.setVisibility(View.GONE);
            }
        }

        getLoaderManager().initLoader(LOADER_ID_THEME_INFO, null, this);
        getLoaderManager().initLoader(LOADER_ID_APPLIED_THEME, null, this);
        mService = (ThemeManager) getActivity().getSystemService(Context.THEME_SERVICE);
        return v;
    }

    private ThemeChangeRequest getThemeChangeRequestForSelectedComponents() {
        // Get all checked components
        ThemeChangeRequest.Builder builder = new ThemeChangeRequest.Builder();
        for (Map.Entry<String, CheckBox> entry : mComponentToCheckbox.entrySet()) {
            String component = entry.getKey();
            CheckBox checkbox = entry.getValue();
            if (checkbox.isEnabled() && checkbox.isChecked()
                    && !mAppliedComponents.contains(component)) {
                builder.setComponent(component, mPkgName);
            }
        }
        builder.setRequestType(RequestType.USER_REQUEST);
        return builder.build();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mService != null) {
            mService.onClientResumed(this);
        }
        refreshApplyButton();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mService != null) {
            mService.onClientPaused(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            mService.onClientDestroyed(this);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mSlidingPanel.post(mShowSlidingPanelRunnable);
    }

    private Runnable mShowSlidingPanelRunnable = new Runnable() {
        @Override
        public void run() {
            // Arbitrarily scroll a bit at the start
            int height = mSlidingPanel.getHeight() / 4;
            mSlidingPanel.smoothScrollTo(0, height);
        }
    };

    private OnCheckedChangeListener mComponentCheckChangedListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            mCurrentCheckboxStates.put(buttonView.getId(), isChecked);
            if (componentSelectionChanged()) {
                mApply.setEnabled(true);
            } else {
                mApply.setEnabled(false);
            }
        }
    };

    private boolean componentSelectionChanged() {
        if (mCurrentCheckboxStates.size() != mInitialCheckboxStates.size()) return false;

        int N = mInitialCheckboxStates.size();
        for (int i = 0; i < N; i++) {
            int key = mInitialCheckboxStates.keyAt(i);
            if (!mInitialCheckboxStates.get(key).equals(mCurrentCheckboxStates.get(key))) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = null;
        String selection = null;
        String[] selectionArgs = null;

        switch (id) {
        case LOADER_ID_THEME_INFO:
            uri = ThemesColumns.CONTENT_URI;
            selection = ThemesColumns.PKG_NAME + "= ?";
            selectionArgs = new String[] { mPkgName };
            break;
        case LOADER_ID_APPLIED_THEME:
            uri = MixnMatchColumns.CONTENT_URI;
            break;
        }

        return new CursorLoader(getActivity(), uri, null, selection, selectionArgs, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        int id = cursorLoader.getId();

        if (id == LOADER_ID_THEME_INFO) {
            if (cursor.getCount() == 0) {
                //Theme was deleted
                safelyPopStack();
            } else {
                loadThemeInfo(cursor);
            }
        } else if (id == LOADER_ID_APPLIED_THEME) {
            loadAppliedInfo(cursor);
        }
    }

    /**
     * Avoid IllegalStateException when popping the backstack
     * in onLoadFinished.
     */
    private void safelyPopStack() {
        Runnable r = new Runnable() {
            public void run() {
                getFragmentManager().popBackStackImmediate();
            }
        };
        mHandler.post(r);
    }

    private void loadThemeInfo(Cursor cursor) {
        cursor.moveToFirst();
        int titleIdx = cursor.getColumnIndex(ThemesColumns.TITLE);
        int authorIdx = cursor.getColumnIndex(ThemesColumns.AUTHOR);
        int hsIdx = cursor.getColumnIndex(ThemesColumns.HOMESCREEN_URI);
        int styleIdx = cursor.getColumnIndex(ThemesColumns.STYLE_URI);
        int lockIdx = cursor.getColumnIndex(ThemesColumns.LOCKSCREEN_URI);
        int defaultIdx = cursor.getColumnIndex(ThemesColumns.IS_DEFAULT_THEME);
        int targetApiIdx = cursor.getColumnIndex(ThemesColumns.TARGET_API);

        boolean isDefaultTheme = cursor.getInt(defaultIdx) == 1;
        String title = ChooserBrowseFragment.DEFAULT.equals(mPkgName)
                ? getActivity().getString(R.string.system_theme_name) : cursor.getString(titleIdx);
        String author = cursor.getString(authorIdx);
        String hsImagePath = cursor.getString(hsIdx);
        String styleImagePath = cursor.getString(styleIdx);
        String lockWallpaperImagePath = cursor.getString(lockIdx);

        mTitle.setText(title + (isDefaultTheme ? " " + getString(R.string.default_tag) : ""));
        mAuthor.setText(author);

        int targetApi = cursor.getInt(targetApiIdx);
        mDesignedFor.setVisibility(
                (targetApi == SYSTEM_TARGET_API || targetApi > Build.VERSION_CODES.KITKAT) ?
                View.GONE : View.VISIBLE);

        // Configure checkboxes for all the theme components
        List<String> supportedComponents = new LinkedList<String>();
        for (Map.Entry<String, CheckBox> entry : mComponentToCheckbox.entrySet()) {
            String componentName = entry.getKey();
            CheckBox componentCheckbox = entry.getValue();
            int idx = cursor.getColumnIndex(componentName);
            boolean componentIncludedInTheme = cursor.getInt(idx) == 1;


            if (!shouldComponentBeVisible(componentName)) {
                componentCheckbox.setVisibility(View.GONE);
            }

            if (shouldComponentBeEnabled(componentName, componentIncludedInTheme)
                    && !mAppliedComponents.contains(componentName)) {
                componentCheckbox.setEnabled(true);
            } else {
                componentCheckbox.setEnabled(false);
            }

            if (componentIncludedInTheme) {
                supportedComponents.add(componentName);
            } else {
                componentCheckbox.setVisibility(View.GONE);
            }
        }

        mPagerAdapter.setPreviewImage(hsImagePath);
        mPagerAdapter.setStyleImage(styleImagePath);
        mPagerAdapter.setLockScreenImage(lockWallpaperImagePath);
        mPagerAdapter.update(supportedComponents);
    }

    private boolean shouldComponentBeVisible(String componentName) {
        // Theme pack, so it is always visible
        if (mComponentFilters.isEmpty()) return true;
        //Not in a theme pack
        return !componentFiltered(componentName);
    }

    private boolean shouldComponentBeEnabled(String componentName, boolean componentIncludedInTheme) {
        return !componentFiltered(componentName) && componentIncludedInTheme;
    }


    private boolean componentFiltered(String componentName) {
        if (mComponentFilters.isEmpty()) return false;
        return !mComponentFilters.contains(componentName);
    }

    private void loadAppliedInfo(Cursor cursor) {
        mAppliedThemeCursor = cursor;
        refreshAppliedComponents();
        refreshChecksForCheckboxes();
        refreshApplyButton();
    }

    private void refreshAppliedComponents() {
        mAppliedComponents.clear();

        //Determine which components are applied
        if (mAppliedThemeCursor != null) {
            mAppliedThemeCursor.moveToPosition(-1);
            while (mAppliedThemeCursor.moveToNext()) {
                String mixnmatchkey = mAppliedThemeCursor.getString(mAppliedThemeCursor.getColumnIndex(MixnMatchColumns.COL_KEY));
                String component = ThemesContract.MixnMatchColumns.mixNMatchKeyToComponent(mixnmatchkey);
                String pkg = mAppliedThemeCursor.getString(mAppliedThemeCursor.getColumnIndex(MixnMatchColumns.COL_VALUE));

                if (pkg.equals(mPkgName)) {
                    mAppliedComponents.add(component);
                }
            }
        }
    }

    private void refreshChecksForCheckboxes() {
        LinearLayout ll = (LinearLayout) getActivity().findViewById(R.id.details_applied);
        ll.setVisibility(View.GONE);

        boolean allApplied = true;

        //Apply checks
        for (Map.Entry<String, CheckBox> entry : mComponentToCheckbox.entrySet()) {
            String componentName = entry.getKey();
            CheckBox componentCheckbox = entry.getValue();

            if (mAppliedComponents.contains(componentName)) {
                componentCheckbox.setChecked(true);
                componentCheckbox.setEnabled(false);
                ((LinearLayout) componentCheckbox.getParent()).removeView(componentCheckbox);
                ll.addView(componentCheckbox);
                ll.setVisibility(View.VISIBLE);
            } else {
                //Ignore unavailable components
                if (componentCheckbox.getVisibility() != View.GONE) {
                    allApplied = false;
                }
            }
            if (mLoadInitialCheckboxStates) {
                mInitialCheckboxStates.put(componentCheckbox.getId(),
                        componentCheckbox.isChecked());
            }
            mCurrentCheckboxStates.put(componentCheckbox.getId(), componentCheckbox.isChecked());
        }

        //Hide available column if it is empty
        ll = (LinearLayout) getActivity().findViewById(R.id.details);
        ll.setVisibility(allApplied ? View.GONE : View.VISIBLE);
    }

    private void refreshApplyButton() {
        //Default
        mApply.setText(R.string.apply);
        StateListDrawable d = (StateListDrawable) mApply.getBackground();
        LayerDrawable bg = (LayerDrawable) d.getStateDrawable(
                d.getStateDrawableIndex(new int[] {android.R.attr.state_enabled}));
        final ClipDrawable clip = (ClipDrawable) bg.findDrawableByLayerId(android.R.id.progress);
        clip.setLevel(0);

        //Determine whether the apply button should show "apply" or "update"
        if (mAppliedThemeCursor != null) {
            mAppliedThemeCursor.moveToPosition(-1);
            while (mAppliedThemeCursor.moveToNext()) {
                String component = mAppliedThemeCursor.getString(mAppliedThemeCursor.getColumnIndex(MixnMatchColumns.COL_KEY));
                String pkg = mAppliedThemeCursor.getString(mAppliedThemeCursor.getColumnIndex(MixnMatchColumns.COL_VALUE));

                // At least one component is set here for this theme
                if (pkg != null && mPkgName.equals(pkg)) {
                    mApply.setText(R.string.update);
                    break;
                }
            }
        }

        //Determine if the apply button's progress
        int progress = (mService == null) ? 0 : mService.getProgress();
        if (progress != 0) {
            clip.setLevel(progress * 100);
            mApply.setText(R.string.applying);
            mApply.setClickable(false);
        } else {
            mApply.setClickable(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursor) {
        mAppliedThemeCursor = null;
    }

    @Override
    public void onProgress(int progress) {
        refreshApplyButton();
    }

    @Override
    public void onFinish(boolean isSuccess) {
        Log.d(TAG, "Finished Applying Theme success=" + isSuccess);
        refreshApplyButton();
    }

    public class ThemeDetailPagerAdapter extends FragmentStatePagerAdapter {
        private List<String> mPreviewList = new LinkedList<String>();
        private List<String> mSupportedComponents = Collections.emptyList();
        private String mPreviewImagePath;
        private String mLockScreenImagePath;
        private String mStyleImagePath;

        public ThemeDetailPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public void setPreviewImage(String imagePath) {
            mPreviewImagePath = imagePath;
        }

        public void setStyleImage(String imagePath) {
            mStyleImagePath = imagePath;
        }

        public void setLockScreenImage(String lockPath) {
            mLockScreenImagePath = lockPath;
        }

        private void update(List<String> supportedComponents) {
            mSupportedComponents = supportedComponents;
            mPreviewList.clear();
            mPreviewList.addAll(supportedComponents);

            // If a particular component is being emphasized
            // then the preview image should reflect that by showing it first
            for (String component : mComponentFilters) {
                mPreviewList.remove(component);
                mPreviewList.add(0, component);
            }

            // Wallpaper and Icons are previewed together so two fragments are not needed
            if (mSupportedComponents.contains(ThemesColumns.MODIFIES_LAUNCHER) &&
                    mSupportedComponents.contains(ThemesColumns.MODIFIES_ICONS)) {
                mPreviewList.remove(ThemesColumns.MODIFIES_ICONS);
            }

            // The AudiblePreviewFragment will take care of loading all available
            // audibles so remove all but one so only one fragment instance is created
            if (mSupportedComponents.contains(ThemesColumns.MODIFIES_ALARMS)) {
                mPreviewList.remove(ThemesColumns.MODIFIES_NOTIFICATIONS);
                mPreviewList.remove(ThemesColumns.MODIFIES_RINGTONES);
            } else if (mSupportedComponents.contains(ThemesColumns.MODIFIES_NOTIFICATIONS)) {
                mPreviewList.remove(ThemesColumns.MODIFIES_RINGTONES);
            }

            // Currently no previews for status bar and navigation bar
            if (mSupportedComponents.contains(ThemesColumns.MODIFIES_STATUS_BAR)) {
                mPreviewList.remove(ThemesColumns.MODIFIES_STATUS_BAR);
            }
            if (mSupportedComponents.contains(ThemesColumns.MODIFIES_NAVIGATION_BAR)) {
                mPreviewList.remove(ThemesColumns.MODIFIES_NAVIGATION_BAR);
            }

            // Sort supported components so that the previews are more reasonable
            Collections.sort(mPreviewList, new PreviewComparator());

            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mPreviewList.size();
        }

        @Override
        public Fragment getItem(int position) {
            String component = mPreviewList.get(position);
            Fragment fragment = null;

            if (component.equals(ThemesColumns.MODIFIES_LAUNCHER)) {
                boolean showIcons = mSupportedComponents.contains(ThemesColumns.MODIFIES_ICONS);
                fragment = WallpaperAndIconPreviewFragment.newInstance(null, mPkgName,
                        showIcons);
            } else if (component.equals(ThemesColumns.MODIFIES_OVERLAYS)) {
                fragment = WallpaperAndIconPreviewFragment.newInstance(mStyleImagePath, mPkgName,
                        false);
            } else if (component.equals(ThemesColumns.MODIFIES_BOOT_ANIM)) {
                fragment = BootAniPreviewFragment.newInstance(mPkgName);
            } else if (component.equals(ThemesColumns.MODIFIES_FONTS)) {
                fragment = FontPreviewFragment.newInstance(mPkgName);
            } else if (component.equals(ThemesColumns.MODIFIES_LOCKSCREEN)) {
                fragment = WallpaperAndIconPreviewFragment.newInstance(mLockScreenImagePath,
                        mPkgName, false);
            } else if (component.equals(ThemesColumns.MODIFIES_LAUNCHER)) {
                throw new UnsupportedOperationException("Not implemented yet!");
            } else if (component.equals(ThemesColumns.MODIFIES_ICONS)) {
                fragment = WallpaperAndIconPreviewFragment.newInstance(mPreviewImagePath, mPkgName,
                        mSupportedComponents.contains(ThemesColumns.MODIFIES_ICONS));
            } else if (component.equals(ThemesColumns.MODIFIES_ALARMS)
                    || component.equals(ThemesColumns.MODIFIES_NOTIFICATIONS)
                    || component.equals(ThemesColumns.MODIFIES_RINGTONES)) {
                fragment = AudiblePreviewFragment.newInstance(mPkgName);
            } else {
                throw new UnsupportedOperationException("Cannot preview " + component);
            }
            return fragment;
        }
    }

    /**
     * Used to put components in their correct preview order
     */
    public static class PreviewComparator implements Comparator<String> {
        private static final List<String> sRank = Arrays.asList(
                ThemesColumns.MODIFIES_LAUNCHER,
                ThemesColumns.MODIFIES_OVERLAYS,
                ThemesColumns.MODIFIES_LOCKSCREEN,
                ThemesColumns.MODIFIES_FONTS,
                ThemesColumns.MODIFIES_ICONS,
                ThemesColumns.MODIFIES_BOOT_ANIM,
                ThemesColumns.MODIFIES_ALARMS,
                ThemesColumns.MODIFIES_NOTIFICATIONS,
                ThemesColumns.MODIFIES_RINGTONES
        );

        @Override
        public int compare(String lhs, String rhs) {
            Integer lhsRank = sRank.indexOf(lhs);
            Integer rhsRank = sRank.indexOf(rhs);
            return Integer.compare(lhsRank, rhsRank);
        }
    }

    private void smoothScrollDrawerTo(int drawerState) {
        int scrollPercentage = 0;
        switch(drawerState) {
            case DRAWER_CLOSED:
                scrollPercentage = DRAWER_CLOSED_SCROLL_AMOUNT;
                break;
            case DRAWER_PARTIALLY_OPEN:
                scrollPercentage = DRAWER_PARTIALLY_OPEN_AMOUNT;
                break;
            case DRAWER_MOSTLY_OPEN:
                scrollPercentage = DRAWER_MOSTLY_OPEN_AMOUNT;
                break;
            case DRAWER_OPEN:
                scrollPercentage = DRAWER_FULLY_OPEN_AMOUNT;
                break;
            default:
                throw new IllegalArgumentException("Bad drawer state: " + drawerState);
        }

        int visibleHeight = mSlidingPanel.getHeight();
        View handle = mSlidingPanel.getHandle();
        visibleHeight -= handle.getHeight();

        int scrollY = scrollPercentage * visibleHeight / 100;
        mSlidingPanel.smoothScrollTo(0, scrollY);
    }

    private int getDrawerState() {
        // Scroll between 3 different heights when pager is clicked
        int visibleHeight = mSlidingPanel.getHeight();
        View handle = mSlidingPanel.getHandle();
        visibleHeight -= handle.getHeight();
        int scrollY = mSlidingPanel.getScrollY();
        int percentageScrolled = (scrollY * 100) / (visibleHeight);

        //Check if we are bottom of scroll
        View view = (View) mSlidingPanel.getChildAt(0);
        boolean isAtBottom = (view.getBottom() - (mSlidingPanel.getHeight() + scrollY)) == 0;

        if (percentageScrolled < DRAWER_CLOSED_THRESHOLD && !isAtBottom) {
            return DRAWER_CLOSED;
        } else if (percentageScrolled < DRAWER_PARTIALLY_OPEN_THRESHOLD && !isAtBottom) {
            return DRAWER_PARTIALLY_OPEN;
        } else if (percentageScrolled < DRAWER_MOSTLY_OPEN_THRESHOLD && !isAtBottom) {
            return DRAWER_MOSTLY_OPEN;
        }

        return DRAWER_OPEN;
    }
}
