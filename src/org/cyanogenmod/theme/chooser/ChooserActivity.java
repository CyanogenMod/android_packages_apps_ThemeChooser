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
import java.util.Arrays;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import org.cyanogenmod.theme.util.NotificationHelper;

public class ChooserActivity extends FragmentActivity {
    public static final String TAG = ChooserActivity.class.getName();
    public static final String EXTRA_COMPONENT_FILTER = "component_filter";
    public static final String EXTRA_PKGNAME = "pkgName";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        NotificationHijackingService.ensureEnabled(this);

        if (savedInstanceState == null) {
            //Determine if there we need to filter by component (ex icon sets only)
            Bundle extras = (Bundle) getIntent().getExtras();
            String filter = (extras == null) ? null : extras.getString(EXTRA_COMPONENT_FILTER);

            // If activity started by wallpaper chooser then filter on wallpapers
            if (Intent.ACTION_SET_WALLPAPER.equals(getIntent().getAction())) {
                filter = "mods_homescreen";
            }

            // Support filters passed in as csv. Since XML prefs do not support
            // passing extras in as arrays.
            ArrayList<String> filtersList = new ArrayList<String>();
            if (filter != null) {
                String[] filters = filter.split(",");
                filtersList.addAll(Arrays.asList(filters));
            }

            Fragment fragment = null;
            if (Intent.ACTION_MAIN.equals(getIntent().getAction()) &&
                    getIntent().hasExtra(EXTRA_PKGNAME)) {
                // Handle case where Theme Store or some other app wishes to open
                // a detailed theme view for a given package
                // TODO: Handle if a bad pkg is provided
                String pkgName = getIntent().getStringExtra(EXTRA_PKGNAME);
                fragment = ChooserDetailFragment.newInstance(pkgName, null);
            } else {
                fragment = ChooserBrowseFragment.newInstance(filtersList);
            }
            getSupportFragmentManager().beginTransaction().replace(R.id.content, fragment, "ChooserBrowseFragment").commit();
        }
    }
}
