package com.italankin.dictionary;

import android.app.Application;

import com.italankin.dictionary.di.components.DaggerInjector;
import com.italankin.dictionary.di.components.DaggerPresenters;
import com.italankin.dictionary.di.components.Injector;
import com.italankin.dictionary.di.components.Presenters;
import com.italankin.dictionary.di.modules.MainModule;

import timber.log.Timber;

public class App extends Application {

    private static Injector injector;
    private static Presenters presenters;

    @Override
    public void onCreate() {
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        super.onCreate();

        MainModule mainModule = new MainModule(this);
        injector = DaggerInjector.builder()
                .mainModule(mainModule)
                .build();
        presenters = DaggerPresenters.builder()
                .dependencies(injector)
                .build();
    }

    public static Injector injector() {
        return injector;
    }

    public static Presenters presenters() {
        return presenters;
    }
}
