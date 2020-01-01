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

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.italankin.dictionary.R;

/**
 * Activity for displaying and manipulating user preferences.
 */
public class SettingsActivity extends AppCompatActivity implements SettingsRootFragment.Callbacks {

    public static final String TAG_ROOT = "root";
    public static final String TAG_SEARCH_OPTIONS = "search_options";

    private FragmentManager mManager;

    public static Intent getStartIntent(Context context) {
        return new Intent(context, SettingsActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        mManager = getFragmentManager();

        if (savedInstanceState == null) {
            FragmentTransaction t = mManager.beginTransaction();
            t.replace(R.id.container, new SettingsRootFragment(), TAG_ROOT);
            t.commit();
        }
    }

    @Override
    public void onBackPressed() {
        if (!mManager.popBackStackImmediate()) {
            super.onBackPressed();
        }
    }

    @Override
    public void onSearchFiltersClick() {
        FragmentTransaction t = mManager.beginTransaction();
        t.setCustomAnimations(R.animator.fragment_in, R.animator.fragment_out,
                R.animator.fragment_in, R.animator.fragment_out);
        t.replace(R.id.container, new SettingsSearchOptionsFragment(), TAG_SEARCH_OPTIONS);
        t.addToBackStack(null);
        t.commit();
    }

}
