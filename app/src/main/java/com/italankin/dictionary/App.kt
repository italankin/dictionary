package com.italankin.dictionary

import android.app.Application
import com.italankin.dictionary.di.components.*
import timber.log.Timber
import timber.log.Timber.DebugTree

class App : Application() {

    override fun onCreate() {
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }

        super.onCreate()

        mainComponent = DaggerMainComponent.builder()
                .context(this)
                .build()
        injector = DaggerInjector.builder()
                .dependencies(mainComponent)
                .build()
        presenters = DaggerPresenters.builder()
                .dependencies(mainComponent)
                .build()
    }

    companion object {
        private lateinit var mainComponent: MainComponent
        private lateinit var injector: Injector
        private lateinit var presenters: Presenters

        @JvmStatic
        fun injector(): Injector = injector

        @JvmStatic
        fun presenters(): Presenters = presenters
    }
}
