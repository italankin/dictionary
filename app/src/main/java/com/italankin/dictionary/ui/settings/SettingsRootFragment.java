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
import android.widget.Toast;

import com.italankin.dictionary.BuildConfig;
import com.italankin.dictionary.R;

/**
 * Root fragment for settings screen.
 */
public class SettingsRootFragment extends PreferenceFragment {

    private Callbacks mCallbacks;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        addPreferencesFromResource(R.xml.prefs_root);
        Preference version = findPreference("version");
        String title = getString(R.string.version, BuildConfig.VERSION_NAME);
        version.setTitle(title);
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
        String key = preference.getKey();
        if ("open_source_libs".equals(key)) {
            Toast.makeText(getActivity(), "TODO", Toast.LENGTH_SHORT).show();
            // TODO
            return true;
        } else if (key.equals("search_filters")) {
            mCallbacks.onSearchFiltersClick();
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public interface Callbacks {
        void onSearchFiltersClick();
    }

}
