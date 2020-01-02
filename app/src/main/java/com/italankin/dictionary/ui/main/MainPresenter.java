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
package com.italankin.dictionary.ui.main;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

import com.arellomobile.mvp.InjectViewState;
import com.arellomobile.mvp.MvpPresenter;
import com.italankin.dictionary.BuildConfig;
import com.italankin.dictionary.R;
import com.italankin.dictionary.api.ApiClient;
import com.italankin.dictionary.dto.Language;
import com.italankin.dictionary.dto.Result;
import com.italankin.dictionary.utils.SharedPrefs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import retrofit2.HttpException;
import timber.log.Timber;

/**
 * Presenter for working with {@link MainActivity}.
 */
@InjectViewState
public class MainPresenter extends MvpPresenter<MainView> {

    private static final int LOOKUP_DEBOUNCE = 450;

    /**
     * Api client for making requests
     */
    private final ApiClient apiClient;
    /**
     * Application shared preferences
     */
    private final SharedPrefs prefs;

    /**
     * UI language for receiving results for this locale (if available)
     */
    private final String uiLanguage;
    private List<Language> langs;
    private Language source;
    private Language dest;

    /**
     * Languages load events.
     */
    private Disposable langsDisposable;

    /**
     * Lookup events subscription
     */
    private Disposable lookupDisposable;

    /**
     * {@link Subject} for filtering input events.
     */
    private Subject<String> events = PublishSubject.create();
    /**
     * A {@link Disposable} for handling emissions of {@link #events}.
     */
    private Disposable eventsDisposable;
    private Result lastResult;
    private ArrayList<String> history = new ArrayList<>(0);

    /**
     * Callback function called when receiving languages list.
     */
    private Consumer<Object> onGetLangsResult = new Consumer<Object>() {
        @Override
        public void accept(Object o) {
            getViewState().onLanguagesResult(langs, getDestLanguageIndex(), getSourceLanguageIndex());
        }
    };

    /**
     * Handling languages fetching errors.
     */
    private Consumer<Throwable> getLangsErrorHandler = throwable -> {
        Timber.e(throwable, "getLangs:");
        getViewState().onLanguagesError();
    };

    @Inject
    public MainPresenter(ApiClient client, SharedPrefs prefs) {
        apiClient = client;
        this.prefs = prefs;
        uiLanguage = Locale.getDefault().getLanguage();
    }

    @Override
    protected void onFirstViewAttach() {
        eventsDisposable = events
                .subscribeOn(Schedulers.computation())
                .map(s -> s.replaceAll("[^\\p{L}\\w -']", "").trim())
                .filter(s -> !s.isEmpty())
                .debounce(LOOKUP_DEBOUNCE, TimeUnit.MILLISECONDS)
                .subscribe(this::lookupInternal);
    }

    /**
     * Should be called when activity is finishing.
     */
    @Override
    public void onDestroy() {
        if (langsDisposable != null && !langsDisposable.isDisposed()) {
            langsDisposable.dispose();
            langsDisposable = null;
        }
        if (lookupDisposable != null && !lookupDisposable.isDisposed()) {
            lookupDisposable.dispose();
            lookupDisposable = null;
        }
        if (eventsDisposable != null && !eventsDisposable.isDisposed()) {
            eventsDisposable.dispose();
            eventsDisposable = null;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Lookup
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Queue lookup request.
     *
     * @param text string to lookup
     */
    public void lookup(String text) {
        events.onNext(text);
    }

    /**
     * Lookup text.
     *
     * @param text string to lookup
     */
    private void lookupInternal(final String text) {
        if (lookupDisposable != null && !lookupDisposable.isDisposed()) {
            // cancel existing sent request
            lookupDisposable.dispose();
            lookupDisposable = null;
        }
        int flags = prefs.getSearchFilter();
        lookupDisposable = apiClient.lookup(BuildConfig.API_KEY, getLangParam(false), text, uiLanguage, flags)
                .flatMap(definitions -> {
                    if (definitions.isEmpty() && prefs.lookupReverse()) {
                        // if we got no result, attempt to lookup in reverse direction
                        return apiClient.lookup(BuildConfig.API_KEY, getLangParam(true),
                                text, uiLanguage, flags);
                    }
                    return Single.just(definitions);
                })
                .map(definitions -> definitions.isEmpty() ? Result.EMPTY : new Result(definitions))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result != Result.EMPTY) {
                        lastResult = result;
                        if (!history.contains(result.text)) {
                            history.add(result.text);
                        }
                        getViewState().onLookupResult(result);
                    } else {
                        getViewState().onEmptyResult();
                    }
                }, errorHandler);
    }

    /**
     * Concat languages codes for sending request to server.
     *
     * @param reverse false: SOURCE-DEST, true: DEST-SOURCE
     * @return language parameter
     */
    private String getLangParam(boolean reverse) {
        if (reverse) {
            return dest.getCode() + "-" + source.getCode();
        } else {
            return source.getCode() + "-" + dest.getCode();
        }
    }

    public Result getLastResult() {
        return lastResult;
    }

    /**
     * Convert {@link Result} into "shareable" form.
     *
     * @return array of 2 strings, first is the text, second is the translations
     */
    @Size(2)
    @Nullable
    public String[] getShareResult() {
        if (lastResult == null) {
            return null;
        }
        String[] result = new String[2];
        if (prefs.shareIncludeTranscription() && lastResult.transcription != null &&
                !lastResult.transcription.isEmpty()) {
            result[0] = lastResult.text + " [" + lastResult.transcription + "]";
        } else {
            result[0] = lastResult.text;
        }
        result[1] = lastResult.toString();
        return result;
    }

    public boolean isRequestInProgress() {
        return lookupDisposable != null && !lookupDisposable.isDisposed();
    }

    public ArrayList<String> getHistory() {
        return history;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Languages
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Load languages list. They will be loaded from net, if there are no cached files.
     */
    public void loadLanguages() {
        if (langs != null && dest != null && source != null) {
            getViewState().onLanguagesResult(langs, getDestLanguageIndex(), getSourceLanguageIndex());
            return;
        }

        if (langsDisposable != null && !langsDisposable.isDisposed()) {
            // wait for request to finish
            return;
        }

        if (prefs.shouldUpdateLangs()) {
            langsDisposable = loadLanguagesFromRemote()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(onGetLangsResult, getLangsErrorHandler);
        } else {
            langsDisposable = prefs.getLanguagesList()
                    .map(languages -> {
                        updateLanguages(languages);
                        return languages;
                    })
                    .onErrorResumeNext(throwable -> loadLanguagesFromRemote())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(onGetLangsResult, getLangsErrorHandler);
        }
    }

    @NonNull
    private Single<List<Language>> loadLanguagesFromRemote() {
        return apiClient.getLangs(BuildConfig.API_KEY)
                .doOnSuccess(list -> {
                    prefs.setLangsTimestamp(new Date());
                    updateLanguages(list);
                    saveLanguages();
                });
    }

    /**
     * Setup presenter fields for manipulating with languages.
     *
     * @param list list of the languages
     */
    private void updateLanguages(List<Language> list) {
        langs = list;
        if (!list.isEmpty()) {
            setSourceLanguageByCode(prefs.getSourceLang());
            setDestLanguageByCode(prefs.getDestLang());
            sortLanguages();
        }
    }

    public void sortLanguages() {
        Collections.sort(langs);
    }

    /**
     * Set source language by its position in languages list.
     *
     * @param position language index
     */
    public boolean setSourceLanguage(int position) {
        Language l = langs.get(position);
        if (source != l) {
            source = l;
            prefs.setSourceLang(source);
            return true;
        }
        return false;
    }

    /**
     * Set destination language by its position in languages list.
     *
     * @param position language index
     */
    public boolean setDestLanguage(int position) {
        Language l = langs.get(position);
        if (dest != l) {
            dest = l;
            prefs.setDestLang(dest);
            return true;
        }
        return false;
    }

    /**
     * Set source language by its code.
     *
     * @param code language code
     */
    public void setSourceLanguageByCode(String code) {
        if (code == null) {
            code = Locale.getDefault().getLanguage();
        }
        source = langs.get(0);
        for (Language l : langs) {
            if (l.getCode().equals(code)) {
                source = l;
                break;
            }
        }
        prefs.setSourceLang(source);
    }


    /**
     * Set dest language by its code.
     *
     * @param code language code
     */
    public void setDestLanguageByCode(String code) {
        if (code == null) {
            code = Locale.getDefault().getLanguage();
        }
        dest = langs.get(0);
        for (Language l : langs) {
            if (l.getCode().equals(code)) {
                dest = l;
                break;
            }
        }
        prefs.setDestLang(dest);
    }

    public int getSourceLanguageIndex() {
        return langs.indexOf(source);
    }

    public int getDestLanguageIndex() {
        return langs.indexOf(dest);
    }

    /**
     * Swap languages.
     *
     * @return {@code true}, if languages were swapped, {@code false} otherwise
     * (ex. {@link #source} == {@link #dest})
     */
    public boolean swapLanguages() {
        if (!TextUtils.equals(source.getCode(), dest.getCode())) {
            Language tmp = source;
            source = dest;
            prefs.setSourceLang(source);
            dest = tmp;
            prefs.setDestLang(dest);
            return true;
        }
        return false;
    }

    /**
     * Save language list on the disk.
     */
    public void saveLanguages() {
        prefs.saveLanguagesList(langs);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Error handlers
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Generic error handler.
     */
    private Consumer<Throwable> errorHandler = throwable -> {
        if (BuildConfig.DEBUG) {
            throwable.printStackTrace();
        }
        int message = R.string.error;
        if (throwable instanceof IOException) {
            message = R.string.error_no_connection;
        }
        if (throwable instanceof HttpException) {
            HttpException e = (HttpException) throwable;
            switch (e.code()) {
                case 400:
                case 501:
                    message = R.string.error_lang_not_supported;
                    break;
                case 401:
                case 402:
                case 403:
                case 502:
                    // just error
                    break;
                case 413:
                    message = R.string.error_long_request;
                    break;
                default:
                    message = R.string.error_no_results;
            }
        }
        getViewState().showError(message);
    };

}
