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
import com.italankin.dictionary.BuildConfig;
import com.italankin.dictionary.api.ApiClient;
import com.italankin.dictionary.ui.main.MainPresenter;
import com.italankin.dictionary.utils.NetworkInterceptor;
import com.italankin.dictionary.utils.SharedPrefs;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;

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
    OkHttpClient provideOkHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(new NetworkInterceptor());
        }
        return builder.build();
    }

    @Provides
    @Singleton
    ApiClient provideApiClient(OkHttpClient client) {
        return new ApiClient(client, BuildConfig.BASE_URL);
    }

    @Provides
    MainPresenter provideMainPresenter(ApiClient client, SharedPrefs prefs) {
        return new MainPresenter(client, prefs);
    }

}
