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
package com.italankin.dictionary.di.modules;

import android.content.Context;

import com.italankin.dictionary.App;
import com.italankin.dictionary.api.ApiClient;
import com.italankin.dictionary.di.RuntimeScope;
import com.italankin.dictionary.ui.MainPresenter;
import com.italankin.dictionary.utils.SharedPrefs;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Main application module, contains main dependencies.
 */
@Module
public class MainModule {

    private final App application;

    public MainModule(App app) {
        application = app;
    }

    @Provides
    @Singleton
    @RuntimeScope
    Context provideApplicationContext() {
        return application;
    }

    @Provides
    @Singleton
    SharedPrefs provideSharedPrefs() {
        return new SharedPrefs(application);
    }

    @Provides
    @Singleton
    ApiClient provideApiClient() {
        return new ApiClient();
    }

    @Provides
    @Singleton
    MainPresenter provideMainPresenter(ApiClient client, SharedPrefs prefs) {
        return new MainPresenter(client, prefs);
    }

}
