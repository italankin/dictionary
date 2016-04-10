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
