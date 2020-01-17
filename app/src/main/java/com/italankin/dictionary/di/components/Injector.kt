package com.italankin.dictionary.di.components

import android.content.ClipboardManager
import android.view.inputmethod.InputMethodManager
import com.italankin.dictionary.di.scopes.InjectorScope
import com.italankin.dictionary.ui.main.MainActivity
import com.italankin.dictionary.utils.SharedPrefs
import com.italankin.dictionary2.ui.main.DictionaryActivity
import dagger.Component

/**
 * Dagger component used for injection.
 */
@InjectorScope
@Component(dependencies = [Injector.Dependencies::class])
interface Injector {

    fun inject(target: MainActivity)

    fun inject(target: DictionaryActivity)

    interface Dependencies {
        val clipboardManager: ClipboardManager

        val inputMethodManager: InputMethodManager

        val sharedPrefs: SharedPrefs
    }
}
