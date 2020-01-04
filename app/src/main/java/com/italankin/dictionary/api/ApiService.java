package com.italankin.dictionary.api;

import com.italankin.dictionary.dto.DicResult;

import io.reactivex.Single;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiService {

    @GET("getLangs")
    Single<String[]> getLangs(@Query("key") String key);

    @GET("lookup")
    Single<DicResult> lookup(
            @Query("key") String key,
            @Query("lang") String lang,
            @Query("text") String text,
            @Query("ui") String ui,
            @Query("flags") int flags);
}
