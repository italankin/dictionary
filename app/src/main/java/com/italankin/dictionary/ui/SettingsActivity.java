/*
 * Copyright 2016 Igor Talankin
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
package com.italankin.dictionary.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.italankin.dictionary.R;
import com.italankin.dictionary.utils.SharedPrefs;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.psdev.licensesdialog.LicensesDialog;

/**
 * Activity for displaying and manipulating user preferences.
 */
public class SettingsActivity extends AppCompatActivity {

    public static final String TAG_ROOT = "root";

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction t = fragmentManager.beginTransaction();
        Fragment f = fragmentManager.findFragmentByTag(TAG_ROOT);
        if (f == null) {
            f = new RootFragment();
        }
        t.replace(R.id.container, f, (TAG_ROOT));
        t.commit();
    }

    public static class RootFragment extends PreferenceFragment {

        private SharedPrefs mPrefs;

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setRetainInstance(true);

            mPrefs = SharedPrefs.getInstance(getActivity());

            addPreferencesFromResource(R.xml.preferences);

            SwitchPreference lookup = (SwitchPreference) findPreference(SharedPrefs.PREF_LOOKUP_REVERSE);
            lookup.setChecked(mPrefs.lookupReverse());
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            switch (preference.getKey()) {
                case "open_source_libs":
                    LicensesDialog dialog = new LicensesDialog.Builder(getActivity())
                            .setNotices(R.raw.notices)
                            .setTitle(R.string.pref_open_source_libs)
                            .build();
                    dialog.showAppCompat();
                    return true;
            }
            return false;
        }

    }

}
