package com.italankin.dictionary.di.components

import android.content.Context
import com.italankin.dictionary.di.modules.MainModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [MainModule::class])
interface MainComponent : Injector.Dependencies, Presenters.Dependencies {

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun context(context: Context): Builder

        fun build(): MainComponent
    }
}
