package com.italankin.dictionary2.ui.main

import com.arellomobile.mvp.InjectViewState
import com.italankin.dictionary2.base.AppPresenter
import com.italankin.dictionary2.ui.main.DictionaryView.ContextMenuAction

@InjectViewState
class DictionaryPresenter(

): AppPresenter<DictionaryView>() {

    override fun onFirstViewAttach() {
        loadLanguages()
    }

    fun loadLanguages() {
        TODO()
    }

    fun showTranslation(position: Int) {
        TODO()
    }

    fun lookup(text: String) {
        TODO()
    }

    fun lookupByTranslation(position: Int) {
        TODO()
    }

    fun copyClip(position: Int, contextMenuAction: ContextMenuAction) {
        TODO()
    }

    fun saveLanguages() {
        TODO()
    }

    fun shareLastResult() {
        TODO()
    }

    fun setSourceLanguage(newPosition: Int) {
        TODO()
    }

    fun setDestLanguage(newPosition: Int) {
        TODO()
    }

    fun swapLanguages() {
        TODO()
    }
}
