package com.italankin.dictionary;

import com.google.gson.Gson;
import com.italankin.dictionary.dto.Definition;
import com.italankin.dictionary.dto.DicResult;
import com.italankin.dictionary.dto.Error;
import com.italankin.dictionary.dto.Language;
import com.italankin.dictionary.utils.LoggingInterceptor;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class ApiClient {

    private static ApiClient INSTANCE;

    private Gson mGson;
    private OkHttpClient mOkHttp;

    public static ApiClient getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ApiClient();
        }
        return INSTANCE;
    }

    public ApiClient() {
        mGson = new Gson();
        mOkHttp = new OkHttpClient();
        mOkHttp.interceptors().add(new LoggingInterceptor());
    }

    public void setCacheDirectory(File dir) {
        Cache cache = new Cache(dir, 5 * 1024 * 1024);
    }

    public Observable<List<Language>> getLangs(final String key) {
        return Observable
                .create(new Observable.OnSubscribe<String[]>() {
                    @Override
                    public void call(Subscriber<? super String[]> subscriber) {
                        try {
                            String url = BuildConfig.BASE_URL + "getLangs?key=" + key;

                            Request request = new Request.Builder().url(url).build();
                            Response response = mOkHttp.newCall(request).execute();

                            if (subscriber.isUnsubscribed()) {
                                return;
                            }

                            String body = response.body().string();
                            String[] entries = mGson.fromJson(body, String[].class);

                            subscriber.onNext(entries);
                        } catch (Exception e) {
                            if (!subscriber.isUnsubscribed()) {
                                subscriber.onError(e);
                            }
                        }
                    }
                })
                .map(new Func1<String[], List<Language>>() {
                    @Override
                    public List<Language> call(String[] entries) {
                        List<Language> list = new ArrayList<>(0);
                        Set<String> set = new HashSet<>(0);
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
                })
                .subscribeOn(Schedulers.io());
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
    public Observable<List<Definition>> lookup(final String key, final String lang, final String text,
                                               final String ui, final int flags) {
        return Observable
                .create(new Observable.OnSubscribe<DicResult>() {
                    @Override
                    public void call(Subscriber<? super DicResult> subscriber) {
                        try {
                            String query = URLDecoder.decode(text, "UTF-8");
                            String url = BuildConfig.BASE_URL + "lookup?key=" + key +
                                    "&lang=" + lang + "&text=" + query;
                            if (ui != null) {
                                url += "&ui=" + ui;
                            }
                            if (flags > 0) {
                                url += "&flags=" + flags;
                            }

                            Request request = new Request.Builder().url(url).build();
                            Response response = mOkHttp.newCall(request).execute();

                            if (subscriber.isUnsubscribed()) {
                                return;
                            }

                            String body = response.body().string();

                            if (!response.isSuccessful()) {
                                Error error = mGson.fromJson(body, Error.class);
                                throw new Exception(error.message);
                            }

                            DicResult result = mGson.fromJson(body, DicResult.class);

                            subscriber.onNext(result);
                        } catch (Exception e) {
                            if (!subscriber.isUnsubscribed()) {
                                subscriber.onError(e);
                            }
                        }
                    }
                })
                .map(new Func1<DicResult, List<Definition>>() {
                    @Override
                    public List<Definition> call(DicResult dicResult) {
                        return dicResult.def;
                    }
                })
                .subscribeOn(Schedulers.io());
    }

}
