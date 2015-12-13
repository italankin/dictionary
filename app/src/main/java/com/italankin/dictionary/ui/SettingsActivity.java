package com.italankin.dictionary.ui;


import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.italankin.dictionary.BuildConfig;
import com.italankin.dictionary.R;
import com.italankin.dictionary.api.ApiClient;
import com.italankin.dictionary.utils.SharedPrefs;

import java.io.File;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction t = fragmentManager.beginTransaction();
        Fragment f = fragmentManager.findFragmentByTag("root");
        if (f == null) {
            f = new RootFragment();
        }
        t.replace(R.id.container, f, "root");
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

            SwitchPreference cache = (SwitchPreference) findPreference(SharedPrefs.PREF_CACHE_RESULTS);
            cache.setChecked(mPrefs.cacheResults());

            Preference cacheClear = findPreference(SharedPrefs.PREF_CACHE_CLEAR);
            cacheClear.setEnabled(mPrefs.cacheResults());
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            switch (preference.getKey()) {
                case SharedPrefs.PREF_CACHE_CLEAR:
                    clearCache();
                    return true;

                case SharedPrefs.PREF_CACHE_RESULTS:
                    boolean enabled = mPrefs.cacheResults();
                    if (enabled) {
                        ApiClient client = ApiClient.getInstance();
                        client.setCacheDirectory(getActivity().getCacheDir(), mPrefs.getCacheSize(),
                                mPrefs.getCacheAge());
                    } else {
                        clearCache();
                    }
                    findPreference(SharedPrefs.PREF_CACHE_CLEAR).setEnabled(enabled);
                    return true;
            }
            return false;
        }

        private void openYandexUrl() {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://tech.yandex.com/dictionary/"));
            try {
                startActivity(Intent.createChooser(intent, getString(R.string.open_link)));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(getActivity(), R.string.error_no_app, Toast.LENGTH_SHORT).show();
            }
        }

        private void clearCache() {
            Observable.just(getActivity().getCacheDir())
                    .map(new Func1<File, Double>() {
                        @Override
                        public Double call(File cacheDir) {
                            long size = 0;
                            long l;
                            for (File f : cacheDir.listFiles()) {
                                l = f.length();
                                if (f.delete()) {
                                    size += l;
                                }
                            }
                            return size / 1024d;
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            new Action1<Double>() {
                                @Override
                                public void call(Double i) {
                                    Log.v("Cache", String.format("cache cleared: %.3f KB", i));
                                    Toast.makeText(getActivity(), R.string.msg_cache_cleared,
                                            Toast.LENGTH_SHORT).show();
                                }
                            },
                            new Action1<Throwable>() {
                                @Override
                                public void call(Throwable throwable) {
                                    if (BuildConfig.DEBUG) {
                                        throwable.printStackTrace();
                                    }
                                    Toast.makeText(getActivity(), R.string.error,
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
        } // clear cache

    }

}
