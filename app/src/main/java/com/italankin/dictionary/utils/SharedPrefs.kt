package com.italankin.dictionary.utils

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.italankin.dictionary.api.FILTER_FAMILY
import com.italankin.dictionary.api.FILTER_MORPHO
import com.italankin.dictionary.api.FILTER_POS_FILTER
import com.italankin.dictionary.api.FILTER_SHORT_POS
import com.italankin.dictionary.api.dto.Language
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Wrapper around [SharedPreferences] for this application purposes.
 */
class SharedPrefs(
        context: Context,
        private val gson: Gson
) {

    companion object {
        private const val LANGS_FILE_NAME = "langs.json"
        private val LANGS_TIMESTAMP = SimpleDateFormat("yyyy-MM-dd'T'HH:MM:ssZ", Locale.US)

        private const val PREF_SOURCE = "source"
        private const val PREF_DEST = "dest"
        private const val PREF_LANGS_LOCALE = "langs_locale"
        private const val PREF_LANGS_TIMESTAMP = "langs_timestamp"
        private const val PREF_LOOKUP_REVERSE = "lookup_reverse"
        private const val PREF_BACK_FOCUS = "back_focus"
        private const val PREF_CLOSE_ON_SHARE = "close_on_share"
        private const val PREF_INCLUDE_TRANSCRIPTION = "include_transcription"
        private const val PREF_FILTER_FAMILY = "filter_family"
        private const val PREF_FILTER_SHORT_POS = "filter_short_pos"
        private const val PREF_FILTER_MORPHO = "filter_morpho"
        private const val PREF_FILTER_POS_FILTER = "filter_pos_filter"
        private const val PREF_SHOW_SHARE_FAB = "show_share_fab"
    }

    private val filesDir: File = context.filesDir
    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private var saveLanguagesDisposable: Disposable? = null

    fun setDestLang(lang: Language) {
        destLang = lang.code
    }

    var destLang: String?
        get() = preferences.getString(PREF_DEST, null)
        set(code) = preferences.edit().putString(PREF_DEST, code).apply()

    fun setSourceLang(lang: Language) {
        sourceLang = lang.code
    }

    var sourceLang: String?
        get() = preferences.getString(PREF_SOURCE, null)
        set(code) = preferences.edit().putString(PREF_SOURCE, code).apply()

    val languagesList: Single<List<Language>>
        get() = Single
                .fromCallable {
                    val file = getLanguagesFile()
                    val fs = FileReader(file)
                    val collectionType = object : TypeToken<List<Language>>() {}.type
                    gson.fromJson<List<Language>>(fs, collectionType)
                }
                .doOnError {
                    Timber.e(it, "getLanguagesList:")
                    purgeLanguages()
                }

    fun setLanguagesTimestamp(date: Date?) {
        val timestamp = LANGS_TIMESTAMP.format(date)
        preferences.edit().putString(PREF_LANGS_TIMESTAMP, timestamp).apply()
    }

    val shouldUpdateLanguages: Boolean
        get() {
            var updatedLastTwoWeeks = false
            if (preferences.contains(PREF_LANGS_TIMESTAMP)) {
                try {
                    val timestamp = preferences.getString(PREF_LANGS_TIMESTAMP, null)
                    val timestampDate = LANGS_TIMESTAMP.parse(timestamp)
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.DAY_OF_MONTH, -14)
                    updatedLastTwoWeeks = timestampDate.after(calendar.time)
                } catch (e: ParseException) { // failed to parse date
                    Timber.e(e, "shouldUpdateLangs:")
                }
            }
            val savedLocale = preferences.getString(PREF_LANGS_LOCALE, null)
            val currentLocale = Locale.getDefault().language
            return !updatedLastTwoWeeks || currentLocale != savedLocale || !getLanguagesFile().exists()
        }

    fun saveLanguagesList(list: List<Language>?) {
        if (list == null) {
            return
        }
        val disposable = saveLanguagesDisposable
        if (disposable == null || disposable.isDisposed) {
            saveLanguagesDisposable = Completable
                    .fromRunnable {
                        val file = getLanguagesFile()
                        FileOutputStream(file).use {
                            val sorted = list.sorted()
                            val json = gson.toJson(sorted)
                            it.write(json.toByteArray())
                        }
                        val locale = Locale.getDefault().language
                        preferences.edit().putString(PREF_LANGS_LOCALE, locale).apply()
                    }
                    .subscribeOn(Schedulers.io())
                    .subscribeBy(
                            onComplete = { Timber.d("saveLanguagesList: success") },
                            onError = { throwable -> Timber.e(throwable, "saveLanguagesList:") }
                    )
        }
    }

    val showShareFab: Boolean
        get() = preferences.getBoolean(PREF_SHOW_SHARE_FAB, true)

    val lookupReverse: Boolean
        get() = preferences.getBoolean(PREF_LOOKUP_REVERSE, true)

    val backFocusSearch: Boolean
        get() = preferences.getBoolean(PREF_BACK_FOCUS, false)

    val closeOnShare: Boolean
        get() = preferences.getBoolean(PREF_CLOSE_ON_SHARE, false)

    val shareIncludeTranscription: Boolean
        get() = preferences.getBoolean(PREF_INCLUDE_TRANSCRIPTION, false)

    val searchFilter: Int
        get() {
            val family = if (preferences.getBoolean(PREF_FILTER_FAMILY, true)) FILTER_FAMILY else 0
            val shortPos = if (preferences.getBoolean(PREF_FILTER_SHORT_POS, false)) FILTER_SHORT_POS else 0
            val morpho = if (preferences.getBoolean(PREF_FILTER_MORPHO, false)) FILTER_MORPHO else 0
            val pos = if (preferences.getBoolean(PREF_FILTER_POS_FILTER, false)) FILTER_POS_FILTER else 0
            return family or shortPos or morpho or pos
        }

    private fun getLanguagesFile(): File = File(filesDir, LANGS_FILE_NAME)

    private fun purgeLanguages() {
        if (saveLanguagesDisposable != null && !saveLanguagesDisposable!!.isDisposed) {
            saveLanguagesDisposable!!.dispose()
        }
        val file = getLanguagesFile()
        if (file.exists()) {
            Timber.d("purgeLanguages: %s", file.delete())
        }
    }
}
