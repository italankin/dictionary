package com.italankin.dictionary.api

import com.italankin.dictionary.api.dto.Definition
import com.italankin.dictionary.api.dto.DictionaryResult
import com.italankin.dictionary.api.dto.Language
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URLEncoder
import java.util.*

/**
 * Client class for API usage
 */
class DictionaryApiClient(
        client: OkHttpClient,
        endpoint: String,
        private val apiKey: String
) {

    private val dictionaryApi: DictionaryApi

    init {
        val retrofit = Retrofit.Builder()
                .baseUrl(endpoint)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
        dictionaryApi = retrofit.create(DictionaryApi::class.java)
    }

    fun getLanguages(): Single<List<Language>> {
        return dictionaryApi.getLangs(apiKey)
                .map { entries ->
                    entries
                            .flatMap { it.split("-", limit = 2) }
                            .asSequence()
                            .map { it.toLanguage() }
                            .distinctBy { it.code }
                            .toList()
                }
    }

    fun lookup(lang: String, text: String, ui: String, flags: FilterFlags): Maybe<List<Definition>> {
        return Single
                .fromCallable { URLEncoder.encode(text, "UTF-8") }
                .flatMap { query -> dictionaryApi.lookup(apiKey, lang, query, ui, flags) }
                .flatMapMaybe { dictionaryResult: DictionaryResult ->
                    val result = dictionaryResult.def
                    if (result != null) {
                        Maybe.just(result)
                    } else {
                        Maybe.empty()
                    }
                }
    }

    private fun String.toLanguage(): Language {
        val locale = Locale(this)
        return Language(this, locale.displayName.capitalize())
    }
}
