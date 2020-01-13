package com.italankin.dictionary.di.components

import com.italankin.dictionary.api.DictionaryApiClient
import com.italankin.dictionary.di.scopes.PresenterScope
import com.italankin.dictionary.ui.main.MainPresenter
import com.italankin.dictionary.utils.SharedPrefs
import dagger.Component

@PresenterScope
@Component(dependencies = [Presenters.Dependencies::class])
interface Presenters {

    val mainPresenter: MainPresenter

    interface Dependencies {
        val dictionaryApiClient: DictionaryApiClient

        val sharedPrefs: SharedPrefs
    }
}
