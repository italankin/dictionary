/*
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

import com.italankin.dictionary.BuildConfig;
import com.italankin.dictionary.dto.Definition;
import com.italankin.dictionary.dto.DicResult;
import com.italankin.dictionary.dto.Language;
import com.italankin.dictionary.utils.NetworkInterceptor;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Client class for API usage
 */
public class ApiClient {

    private static ApiClient INSTANCE;

    private final ApiService mService;

    public static synchronized ApiClient getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ApiClient();
        }
        return INSTANCE;
    }

    private ApiClient() {
        OkHttpClient okHttp = new OkHttpClient.Builder()
                .addInterceptor(new NetworkInterceptor())
                .build();

        GsonConverterFactory converter = GsonConverterFactory.create();
        RxJavaCallAdapterFactory adapter = RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io());
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .addCallAdapterFactory(adapter)
                .addConverterFactory(converter)
                .client(okHttp)
                .build();
        mService = retrofit.create(ApiService.class);
    }

    /**
     * Fetch languages list from the server.
     *
     * @param key API key
     * @return list of available languages
     */
    public Observable<List<Language>> getLangs(String key) {
        return mService.getLangs(key)
                .map(new Func1<String[], List<Language>>() {
                    @Override
                    public List<Language> call(String[] entries) {
                        List<Language> list = new ArrayList<>(entries.length);
                        Set<String> set = new HashSet<>(entries.length);
                        String defaultCode = Locale.getDefault().getLanguage();
                        Locale locale;
                        String[] a;
                        Language lang;
                        for (String s : entries) {
                            a = s.toLowerCase().split("-");

                            // source language
                            String code = a[0];
                            if (!set.contains(code)) {
                                locale = new Locale(code);
                                lang = new Language(code, locale.getDisplayName());
                                lang.setFavorite(defaultCode.equals(code));
                                list.add(lang);
                                set.add(code);
                            }

                            // destination language
                            if (!set.contains(code)) {
                                code = a[1];
                                locale = new Locale(code);
                                lang = new Language(code, locale.getDisplayName());
                                lang.setFavorite(defaultCode.equals(code));
                                list.add(lang);
                                set.add(code);
                            }
                        }

                        return list;
                    }
                });
    }

    /**
     * Searches for a word or phrase in the dictionary and returns an automatically generated
     * dictionary entry
     *
     * @param key   API key
     * @param lang  translation direction (pair of language codes separated by hyphen ex. "en-en")
     * @param text  the word or phrase to find in the dictionary
     * @param ui    the language of the user's interface for displaying names of parts of speech in
     *              the dictionary entry
     * @param flags search options (bitmask of flags). Possible values:
     *              <ul>
     *              <li>FAMILY = 0x0001 - Apply the family search filter.</li>
     *              <li>MORPHO = 0x0004 - Enable searching by word form.</li>
     *              <li>POS_FILTER = 0x0008 - Enable a filter that requires matching parts of speech
     *              for the search word and translation.</li>
     *              </ul>
     * @return {@link List} of {@link Definition}s
     */
    public Observable<List<Definition>> lookup(String key, String lang, String text,
                                               String ui, int flags) {
        try {
            text = URLDecoder.decode(text, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return Observable.error(e);
        }
        return mService.lookup(key, lang, text, ui, flags)
                .map(new Func1<DicResult, List<Definition>>() {
                    @Override
                    public List<Definition> call(DicResult dicResult) {
                        return dicResult.def;
                    }
                });
    }

}
