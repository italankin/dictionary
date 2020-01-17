package com.italankin.dictionary2.ui.main

import androidx.annotation.StringRes
import com.arellomobile.mvp.MvpView
import com.arellomobile.mvp.viewstate.strategy.OneExecutionStateStrategy
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType
import com.arellomobile.strategy.TagStrategy
import com.italankin.dictionary.api.dto.Language
import com.italankin.dictionary.api.dto.Result
import com.italankin.dictionary.api.dto.TranslationEx

private const val TAG_LANGUAGES = "languages"
private const val TAG_PROGRESS = "progress"
private const val TAG_RESULT = "result"

interface DictionaryView : MvpView {

    @StateStrategyType(TagStrategy::class, tag = TAG_LANGUAGES)
    fun onLanguagesResult(languages: List<Language>, sourceIndex: Int, destIndex: Int)

    @StateStrategyType(TagStrategy::class, tag = TAG_LANGUAGES)
    fun onLanguagesError()

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun switchLanguages(sourceIndex: Int, destIndex: Int)

    @StateStrategyType(TagStrategy::class, tag = TAG_RESULT)
    fun onLookupResult(result: Result)

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun onEmptyResult()

    @StateStrategyType(TagStrategy::class, tag = "")
    fun showProgress(show: Boolean)

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun startTranslationActivity(translation: TranslationEx)

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun showError(@StringRes message: Int)

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun copyClip(clip: String)

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun shareContent(subject: String?, text: String?)

    enum class ContextMenuAction {
        COPY_MEAN,
        COPY_SYNONYM,
        COPY_TRANSLATION
    }
}
