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

import android.os.Bundle;

import com.italankin.dictionary.api.ApiClient;
import com.italankin.dictionary.ui.main.MainPresenter;
import com.italankin.dictionary.utils.SharedPrefs;

import java.util.WeakHashMap;

/**
 * Class manages created presenters and delivers to recreated activities their old presenters.
 */
public final class PresenterFactory {

    private static final String KEY_PRESENTER_ID = "@@presenter_id";

    private final ApiClient api;
    private final SharedPrefs prefs;

    private int mCount = 0;
    private WeakHashMap<Integer, MainPresenter> mainPresenters = new WeakHashMap<>(0);

    public PresenterFactory(ApiClient api, SharedPrefs prefs) {
        this.api = api;
        this.prefs = prefs;
    }

    /**
     * Get instance of {@link MainPresenter}. If passed {@code bundle} does not
     * contain {@link #KEY_PRESENTER_ID} value or it's invalid, the new instance of presenter
     * will be created.
     *
     * @param bundle bundle, containing (or not) presenter indentifier
     * @return presenter
     */
    public MainPresenter getMainPresenter(Bundle bundle) {
        MainPresenter instance = null;
        if (bundle.containsKey(KEY_PRESENTER_ID)) {
            int id = bundle.getInt(KEY_PRESENTER_ID, mCount + 1);
            instance = mainPresenters.get(id);
        }
        if (instance == null) {
            mCount++;
            instance = createMainPresenter();
            mainPresenters.put(mCount, instance);
            bundle.putInt(KEY_PRESENTER_ID, mCount);
        }
        return instance;
    }

    private MainPresenter createMainPresenter() {
        return new MainPresenter(api, prefs);
    }

}
