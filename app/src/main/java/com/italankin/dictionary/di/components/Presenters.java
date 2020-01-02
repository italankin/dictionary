package com.italankin.dictionary.di.components;

import com.italankin.dictionary.api.ApiClient;
import com.italankin.dictionary.di.PresenterScope;
import com.italankin.dictionary.ui.main.MainPresenter;
import com.italankin.dictionary.utils.SharedPrefs;

import dagger.Component;

@PresenterScope
@Component(dependencies = Presenters.Dependencies.class)
public interface Presenters {

    MainPresenter mainPresenter();

    interface Dependencies {
        ApiClient apiClient();

        SharedPrefs sharedPrefs();
    }
}
