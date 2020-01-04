package com.italankin.dictionary.di.components;

import com.italankin.dictionary.di.modules.MainModule;
import com.italankin.dictionary.ui.main.MainActivity;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Dagger component used for injection.
 */
@Singleton
@Component(modules = MainModule.class)
public interface Injector extends Presenters.Dependencies {

    void inject(MainActivity target);
}
