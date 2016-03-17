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
import android.widget.Toast;

import com.italankin.dictionary.R;
import com.italankin.dictionary.utils.SharedPrefs;

import butterknife.Bind;
import butterknife.ButterKnife;

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
                    Toast.makeText(getActivity(), "TODO", Toast.LENGTH_SHORT).show(); // TODO
                    return true;
            }
            return false;
        }

    }

}
