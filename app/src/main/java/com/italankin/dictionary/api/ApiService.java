package com.italankin.dictionary.api;

import com.italankin.dictionary.dto.DicResult;

import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

public interface ApiService {

    @GET("getLangs")
    Observable<String[]> getLangs(@Query("key") String key);

    @GET("lookup")
    Observable<DicResult> lookup(
            @Query("key") String key,
            @Query("lang") String lang,
            @Query("text") String text,
            @Query("ui") String ui,
            @Query("flags") int flags);

}
