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
package com.italankin.dictionary.ui.settings;

import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import com.italankin.dictionary.R;

import de.psdev.licensesdialog.LicensesDialog;

public class SettingsRootFragment extends PreferenceFragment {

    private Callbacks mCallbacks;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        addPreferencesFromResource(R.xml.prefs_root);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCallbacks = (Callbacks) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
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
            case "search_filters":
                mCallbacks.onSearchFiltersClick();
                return true;
        }
        return false;
    }

    public interface Callbacks {
        void onSearchFiltersClick();
    }
}
