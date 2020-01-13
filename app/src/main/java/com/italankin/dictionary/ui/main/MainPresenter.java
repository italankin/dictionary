package com.italankin.dictionary.ui.main;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.StringRes;

import com.arellomobile.mvp.InjectViewState;
import com.arellomobile.mvp.MvpPresenter;
import com.italankin.dictionary.R;
import com.italankin.dictionary.api.DictionaryApiClient;
import com.italankin.dictionary.api.dto.Language;
import com.italankin.dictionary.api.dto.Result;
import com.italankin.dictionary.utils.SharedPrefs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
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
    private static final String SANITIZE_PATTERN = "[^\\p{L}\\w -']";

    /**
     * Api client for making requests
     */
    private final DictionaryApiClient apiClient;
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

    @Inject
    public MainPresenter(DictionaryApiClient client, SharedPrefs prefs) {
        apiClient = client;
        this.prefs = prefs;
        uiLanguage = Locale.getDefault().getLanguage();
    }

    @Override
    protected void onFirstViewAttach() {
        eventsDisposable = events
                .subscribeOn(Schedulers.computation())
                .map(MainPresenter::sanitizeInput)
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
        if (text != null && !text.isEmpty()) {
            events.onNext(text);
        }
    }

    /**
     * Lookup text.
     *
     * @param text string to lookup
     */
    private void lookupInternal(String text) {
        if (lookupDisposable != null && !lookupDisposable.isDisposed()) {
            // cancel existing sent request
            lookupDisposable.dispose();
            lookupDisposable = null;
        }
        int flags = prefs.getSearchFilter();
        lookupDisposable = apiClient.lookup(getLangParam(false), text, uiLanguage, flags)
                .switchIfEmpty(Maybe.defer(() -> {
                    if (prefs.getLookupReverse()) {
                        return apiClient.lookup(getLangParam(true),
                                text, uiLanguage, flags);
                    } else {
                        return Maybe.empty();
                    }
                }))
                .map(Result::new)
                .defaultIfEmpty(Result.EMPTY)
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
                }, throwable -> {
                    Timber.e(throwable, "lookupInternal:");
                    getViewState().showError(getErrorMessage(throwable));
                });
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
        if (prefs.getShareIncludeTranscription() && lastResult.transcription != null &&
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

        if (prefs.getShouldUpdateLanguages()) {
            langsDisposable = loadLanguagesFromRemote()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(ignored -> {
                        Timber.d("loadLanguages: %s", langs);
                        getViewState().onLanguagesResult(langs, getDestLanguageIndex(), getSourceLanguageIndex());
                    }, throwable -> {
                        Timber.e(throwable, "loadLanguages:");
                        getViewState().onLanguagesError();
                    });
        } else {
            langsDisposable = prefs.getLanguagesList()
                    .map(this::updateLanguages)
                    .onErrorResumeNext(throwable -> loadLanguagesFromRemote())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(ignored -> {
                        Timber.d("loadLanguages: %s", langs);
                        getViewState().onLanguagesResult(langs, getDestLanguageIndex(), getSourceLanguageIndex());
                    }, throwable -> {
                        Timber.e(throwable, "loadLanguages:");
                        getViewState().onLanguagesError();
                    });
        }
    }

    @NonNull
    private Single<List<Language>> loadLanguagesFromRemote() {
        return apiClient.getLanguages()
                .doOnSuccess(list -> {
                    prefs.setLanguagesTimestamp(new Date());
                    updateLanguages(list);
                    saveLanguages();
                });
    }

    /**
     * Setup presenter fields for manipulating with languages.
     *
     * @param list list of the languages
     */
    private List<Language> updateLanguages(List<Language> list) {
        langs = list;
        if (!list.isEmpty()) {
            setSourceLanguageByCode(prefs.getSourceLang());
            setDestLanguageByCode(prefs.getDestLang());
            sortLanguages();
        }
        return langs;
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

    /**
     * Swap languages.
     *
     * @return {@code true}, if languages were swapped, {@code false} otherwise
     * (ex. {@link #source} == {@link #dest})
     */
    public boolean onSwapLanguages() {
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

    @StringRes
    private int getErrorMessage(Throwable throwable) {
        if (throwable instanceof IOException) {
            return R.string.error_no_connection;
        } else if (throwable instanceof HttpException) {
            HttpException e = (HttpException) throwable;
            switch (e.code()) {
                case 400:
                case 501:
                    return R.string.error_lang_not_supported;
                case 401:
                case 402:
                case 403:
                case 502:
                    return R.string.error;
                case 413:
                    return R.string.error_long_request;
                default:
                    return R.string.error_no_results;
            }
        }
        return R.string.error;
    }

    public void onSwitchLanguages() {
        getViewState().switchLanguages(getDestLanguageIndex(), getSourceLanguageIndex());
    }

    private int getSourceLanguageIndex() {
        return langs.indexOf(source);
    }

    private int getDestLanguageIndex() {
        return langs.indexOf(dest);
    }

    private static String sanitizeInput(String s) {
        return s.replaceAll(SANITIZE_PATTERN, "").trim();
    }
}
