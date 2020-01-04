package com.italankin.dictionary.di.modules;

import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;

import com.italankin.dictionary.App;
import com.italankin.dictionary.BuildConfig;
import com.italankin.dictionary.api.ApiClient;
import com.italankin.dictionary.utils.SharedPrefs;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

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
    ClipboardManager provideClipboardManager() {
        return (ClipboardManager) application.getSystemService(Context.CLIPBOARD_SERVICE);
    }

    @Provides
    @Singleton
    InputMethodManager provideInputMethodManager() {
        return (InputMethodManager) application.getSystemService(Context.INPUT_METHOD_SERVICE);
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
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(s -> {
                Log.d("OkHttp", s);
            });
            builder.addInterceptor(interceptor);
        }
        return builder.build();
    }

    @Provides
    @Singleton
    ApiClient provideApiClient(OkHttpClient client) {
        return new ApiClient(client, BuildConfig.BASE_URL);
    }
}
