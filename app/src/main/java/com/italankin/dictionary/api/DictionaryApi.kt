package com.italankin.dictionary.api

import com.italankin.dictionary.api.dto.DictionaryResult

import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

interface DictionaryApi {

    @GET("getLangs")
    fun getLangs(
            @Query("key") key: String
    ): Single<Array<String>>

    @GET("lookup")
    fun lookup(
            @Query("key") key: String,
            @Query("lang") lang: String,
            @Query("text") text: String,
            @Query("ui") ui: String,
            @Query("flags") flags: Int
    ): Single<DictionaryResult>
}
