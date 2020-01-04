package com.italankin.dictionary.api;

import androidx.annotation.IntDef;

import com.italankin.dictionary.dto.Definition;
import com.italankin.dictionary.dto.Language;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Client class for API usage
 */
public class ApiClient {

    public static final int FILTER_NONE = 0x0;
    public static final int FILTER_FAMILY = 0x1;
    public static final int FILTER_SHORT_POS = 0x2;
    public static final int FILTER_MORPHO = 0x4;
    public static final int FILTER_POS_FILTER = 0x8;

    private static Language languageFromCode(String code) {
        Locale locale = new Locale(code);
        return new Language(code, capitalize(locale.getDisplayName()));
    }

    private static String capitalize(String name) {
        return name.substring(0, 1).toUpperCase(Locale.getDefault()) + name.substring(1);
    }

    private final ApiService mService;

    public ApiClient(OkHttpClient client, String endpoint) {
        GsonConverterFactory converter = GsonConverterFactory.create();
        RxJava2CallAdapterFactory adapter = RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io());
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(endpoint)
                .addCallAdapterFactory(adapter)
                .addConverterFactory(converter)
                .client(client)
                .build();
        mService = retrofit.create(ApiService.class);
    }

    /**
     * Fetch languages list from the server.
     *
     * @param key API key
     * @return list of available languages
     */
    public Single<List<Language>> getLangs(String key) {
        return mService.getLangs(key)
                .map(entries -> {
                    List<Language> list = new ArrayList<>(entries.length);
                    Set<String> set = new HashSet<>(entries.length);
                    String l1, l2;
                    for (String s : entries) {
                        int i = s.indexOf("-");
                        if (i == -1) {
                            continue;
                        }
                        l1 = s.substring(0, i);
                        l2 = s.substring(i + 1);

                        // source language
                        if (set.add(l1)) {
                            list.add(languageFromCode(l1));
                        }

                        // destination language
                        if (set.add(l2)) {
                            list.add(languageFromCode(l2));
                        }
                    }
                    return list;
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
     *              <li>{@link #FILTER_NONE} - disable all options</li>
     *              <li>{@link #FILTER_FAMILY} - apply the family search filter.</li>
     *              <li>{@link #FILTER_SHORT_POS} - parts of speech will be presented in short forms.</li>
     *              <li>{@link #FILTER_MORPHO} - enable searching by word form.</li>
     *              <li>{@link #FILTER_POS_FILTER} - enable a filter that requires matching parts of speech
     *              for the search word and translation.</li>
     *              </ul>
     * @return {@link List} of {@link Definition}s
     */
    public Maybe<List<Definition>> lookup(String key, String lang, String text,
                                          String ui, @LookupFlags int flags) {
        return Single.fromCallable(() -> URLEncoder.encode(text, "UTF-8"))
                .flatMap(s -> mService.lookup(key, lang, s, ui, flags))
                .flatMapMaybe(dicResult -> {
                    List<Definition> result = dicResult.def;
                    return result != null ? Maybe.just(result) : Maybe.empty();
                });
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {FILTER_NONE, FILTER_FAMILY, FILTER_SHORT_POS, FILTER_MORPHO, FILTER_POS_FILTER})
    public @interface LookupFlags {

    }
}
