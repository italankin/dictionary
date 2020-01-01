/*
 * Copyright 2016 Igor Talankin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
