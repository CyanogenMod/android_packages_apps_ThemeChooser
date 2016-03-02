/*
 * Copyright (C) 2016 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cyanogenmod.theme.chooser;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.ArrayMap;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import cyanogenmod.providers.ThemesContract;
import org.cyanogenmod.theme.util.PreferenceUtils;

import java.util.ArrayList;
import java.util.Map;

import static cyanogenmod.providers.ThemesContract.ThemeMixEntryColumns.*;

public class SaveThemeDialogFragment extends DialogFragment {
    private static final String TAG = SaveThemeDialogFragment.class.getSimpleName();

    private static final String ARG_NAME = "name";
    private static final String ARG_BASE_THEME_PACKAGE_NAME = "base_theme_package_name";
    private static final String ARG_COMPONENTS = "components";
    private static final String ARG_PACKAGES = "packages";
    private static final String ARG_WALLPAPER_COMPONENT_ID = "wallpaper_component_id";

    private Map<String, String> mCurrentTheme;
    private EditText mThemeName;
    private Button mSaveButton;
    private Button mCancelButton;
    private ProgressBar mSaveProgress;
    private long mWallpaperComponentId;
    private String mBaseThemePackageName;

    private ChooserActivity chact;

    public static SaveThemeDialogFragment newInstance(String name, String baseThemePackageName,
            Map<String, String> currentTheme, long wallpaperComponentId) {
        Bundle args = new Bundle();
        args.putString(ARG_NAME, name);
        args.putString(ARG_BASE_THEME_PACKAGE_NAME, baseThemePackageName);
        ArrayList<String> components = new ArrayList<>(currentTheme.size());
        ArrayList<String> packages = new ArrayList<>(currentTheme.size());
        for(Map.Entry<String, String> entry : currentTheme.entrySet()) {
            components.add(entry.getKey());
            packages.add(entry.getValue());
        }
        args.putStringArrayList(ARG_COMPONENTS, components);
        args.putStringArrayList(ARG_PACKAGES, packages);
        args.putLong(ARG_WALLPAPER_COMPONENT_ID, wallpaperComponentId);
        SaveThemeDialogFragment dialog = new SaveThemeDialogFragment();
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        String themeName = args.getString(ARG_NAME);
        mBaseThemePackageName = args.getString(ARG_BASE_THEME_PACKAGE_NAME);
        ArrayList<String> components = args.getStringArrayList(ARG_COMPONENTS);
        ArrayList<String> packages = args.getStringArrayList(ARG_PACKAGES);
        int N = components.size();
        mCurrentTheme = new ArrayMap<>(N);
        for (int i = 0; i < N; i++) {
            mCurrentTheme.put(components.get(i), packages.get(i));
        }
        mWallpaperComponentId = args.getLong(ARG_WALLPAPER_COMPONENT_ID, 0);

        // Make sure we use the material light theme for alert dialog
        ContextThemeWrapper ctx = new ContextThemeWrapper(getActivity(),
                android.R.style.Theme_Material_Light_Dialog_Alert);
        AlertDialog.Builder alert = new AlertDialog.Builder(ctx);

        chact = new ChooserActivity();

        // Inflate layout containing EditText to be added to the AlertDialog
        View v = View.inflate(ctx, R.layout.fragment_save_theme, null);
        mThemeName = (EditText) v.findViewById(R.id.theme_name);
        mThemeName.setText(themeName);
        mThemeName.requestFocus();
        mThemeName.addTextChangedListener(mTextWatcher);
        mSaveProgress = (ProgressBar) v.findViewById(R.id.save_progress_indicator);
        alert.setView(v)
                .setPositiveButton(R.string.dialog_save_theme_save_button_text, null)
                .setNegativeButton(R.string.dialog_save_theme_cancel_button_text,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setTitle(R.string.dialog_save_theme_title);
        Dialog dialog = alert.create();
        // show IME when displayed
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        final AlertDialog dialog = (AlertDialog) getDialog();
        mSaveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        mCancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        // Register an onClickListener for the save button so we can override the default
        // behavior of the dialog normally being dismissed once this button is clicked
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSaveButton.setEnabled(false);
                mCancelButton.setEnabled(false);
                mThemeName.setFocusable(false);
                mSaveProgress.setVisibility(View.VISIBLE);
                new SaveThemeAsyncTask().execute(mThemeName.getText().toString(), mCurrentTheme);
                FragmentManager fm = getFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                if(fm.getFragments()!=null) {
                    for (Fragment frag : fm.getFragments()) {
                        if(frag instanceof MyThemeFragment) {
                            ThemeFragment tf = ThemeFragment.newInstance(((MyThemeFragment) frag).getThemePackageName(),false);
                            ft.remove(frag);
                        }
                    }
                    ft.commit();
                }
                chact.setmAppliedBaseTheme(mThemeName.getText().toString());
                //chact.restartLoaderFromSaveThemeDialog();


            }
        });
    }

    private TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (mSaveButton != null) {
                // don't allow saving when text input is empty
                mSaveButton.setEnabled(count > 0);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {}
    };

    private class SaveThemeAsyncTask extends AsyncTask<Object, Void, Integer> {

        @Override
        protected Integer doInBackground(Object... params) {
            String name = (String) params[0];
            Map<String, String> theme = (Map<String, String>) params[1];
            ContentResolver resolver = getActivity().getContentResolver();
            ContentValues values = new ContentValues();
            values.put(ThemesContract.ThemeMixColumns.TITLE, name);
            //PreferenceUtils.setAppliedBaseTheme(chact.getBaseContext(),mThemeName.getText().toString());
            //chact.setmAppliedBaseTheme(mThemeName.getText().toString());
            Uri themeMixUri = resolver.insert(ThemesContract.ThemeMixColumns.CONTENT_URI, values);
            long id = -1;
            if (themeMixUri != null) {
                try {
                    id = ContentUris.parseId(themeMixUri);
                    if (id >= 0) {
                        final String[] projection = {ThemesContract.ThemesColumns.TITLE};
                        final String selection = ThemesContract.ThemesColumns.PKG_NAME + "=?";
                        final ContentValues[] mixEntryValues = new ContentValues[theme.size()];
                        int idx = 0;
                        for (Map.Entry<String, String> entry : theme.entrySet()) {
                            Cursor c = resolver.query(ThemesContract.ThemesColumns.CONTENT_URI,
                                    projection, selection, new String[] {entry.getValue()}, null);
                            if (c != null) {
                                if (c.moveToFirst()) {
                                    mixEntryValues[idx] = new ContentValues();
                                    mixEntryValues[idx].put(THEME_MIX_ID, id);
                                    mixEntryValues[idx].put(COMPONENT_TYPE, entry.getKey());
                                    mixEntryValues[idx].put(PACKAGE_NAME, entry.getValue());
                                    mixEntryValues[idx].put(THEME_NAME, c.getString(0));
                                    mixEntryValues[idx].put(COMPONENT_ID,
                                            ThemesContract.ThemesColumns.MODIFIES_LAUNCHER.equals(entry.getKey())
                                                    ? mWallpaperComponentId : 0);
                                    idx++;
                                }
                                c.close();
                            }
                        }
                        resolver.bulkInsert(CONTENT_URI, mixEntryValues);
                    }
                } catch (UnsupportedOperationException | NumberFormatException e) {
                }
            }
            return (int) id;
        }

        @Override
        protected void onPostExecute(Integer value) {
            getDialog().dismiss();
        }
    }
}
