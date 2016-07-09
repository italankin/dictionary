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
import android.util.Log;

import com.italankin.dictionary.BuildConfig;
import com.italankin.dictionary.R;
import com.italankin.dictionary.api.ApiClient;
import com.italankin.dictionary.dto.Definition;
import com.italankin.dictionary.dto.Language;
import com.italankin.dictionary.dto.Result;
import com.italankin.dictionary.utils.SharedPrefs;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Presenter for working with {@link MainActivity}.
 */
public class MainPresenter {

    private static final String TAG = "[" + MainPresenter.class.getSimpleName() + "]";

    /**
     * Reference to attached activity
     */
    private WeakReference<MainActivity> mRef;

    /**
     * Api client for making requests
     */
    private final ApiClient mClient;
    /**
     * Application shared preferences
     */
    private final SharedPrefs mPrefs;

    /**
     * UI language for receiving results for this locale (if available)
     */
    private final String mUiLanguage;

    private List<Language> mLangs;

    private Language mSource;
    private Language mDest;

    /**
     * Languages load events.
     */
    private Subscription mSubLangs;

    /**
     * Lookup events subscription
     */
    private Subscription mSubLookup;

    private Result mLastResult;
    private LinkedList<Result> mResults = new LinkedList<>();

    public MainPresenter(ApiClient client, SharedPrefs prefs) {
        mClient = client;
        mPrefs = prefs;
        mUiLanguage = Locale.getDefault().getLanguage();
    }

    /**
     * @param activity activity for attaching presenter to
     */
    public void attach(MainActivity activity) {
        if (mRef != null && activity == mRef.get()) {
            log("already attached to %s", activity);
            return;
        }
        mRef = new WeakReference<>(activity);
    }

    /**
     * Called when activity is finishing.
     */
    public void detach(MainActivity activity) {
        if (activity != mRef.get()) {
            return;
        }
        mRef.clear();
        if (mSubLangs != null && mSubLangs.isUnsubscribed()) {
            mSubLangs.unsubscribe();
            mSubLangs = null;
        }
        if (mSubLookup != null && mSubLookup.isUnsubscribed()) {
            mSubLookup.unsubscribe();
            mSubLookup = null;
        }
    }

    /**
     * Load languages list. They will be loaded from net, if there are no cached files.
     */
    public void loadLanguages() {
        if (mDest != null && mSource != null) {
            MainActivity a = mRef.get();
            a.onLanguagesResult();
            return;
        }

        if (mSubLangs != null && !mSubLangs.isUnsubscribed()) {
            // wait for request to finish
            return;
        }

        if (mPrefs.shouldUpdateLangs()) {
            mSubLangs = mClient.getLangs(BuildConfig.API_KEY)
                    .doOnNext(new Action1<List<Language>>() {
                        @Override
                        public void call(List<Language> list) {
                            try {
                                mPrefs.putLangs(list, Locale.getDefault().getLanguage());
                            } catch (IOException e) {
                                throw new RuntimeException(e.getMessage());
                            }
                            updateLanguages(list);
                        }
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(onGetLangsResult, mGetLangsErrorHandler);
        } else {
            mSubLangs = Observable
                    .create(new Observable.OnSubscribe<Object>() {
                        @Override
                        public void call(Subscriber<? super Object> subscriber) {
                            try {
                                updateLanguages(mPrefs.getLangs());
                                if (!subscriber.isUnsubscribed()) {
                                    subscriber.onNext(null);
                                }
                            } catch (Exception e) {
                                if (!subscriber.isUnsubscribed()) {
                                    subscriber.onError(e);
                                }
                            }
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(onGetLangsResult, mGetLangsErrorHandler);
        }
    }

    /**
     * Setup presenter fields for manipulating with languages.
     *
     * @param list list of the languages
     */
    private void updateLanguages(List<Language> list) {
        mLangs = list;
        if (!list.isEmpty()) {
            setSourceLanguageByCode(mPrefs.getSourceLang());
            setDestLanguageByCode(mPrefs.getDestLang());
            sortLanguagesList();
        }
    }

    /**
     * Callback function called when receiving languages list.
     */
    private Action1<Object> onGetLangsResult = new Action1<Object>() {
        @Override
        public void call(Object o) {
            MainActivity a = mRef.get();
            if (a != null) {
                a.onLanguagesResult();
            }
            mSubLangs.unsubscribe();
        }
    };

    /**
     * Lookup text.
     *
     * @param text string to lookup
     */
    public void lookup(final String text) {
        if (mSubLookup != null && !mSubLookup.isUnsubscribed()) {
            // cancel existing sent request
            mSubLookup.unsubscribe();
        }

        @ApiClient.LookupFlags final int flags = mPrefs.getSearchFilter();

        mSubLookup = mClient.lookup(BuildConfig.API_KEY, getLangParam(false), text, mUiLanguage, flags)
                .flatMap(new Func1<List<Definition>, Observable<List<Definition>>>() {
                    @Override
                    public Observable<List<Definition>> call(List<Definition> definitions) {
                        if (definitions.isEmpty() && mPrefs.lookupReverse()) {
                            // if we got no result, attempt to lookup in reverse direction
                            //noinspection WrongConstant
                            return mClient.lookup(BuildConfig.API_KEY, getLangParam(true), text,
                                    mUiLanguage, flags);
                        }
                        return Observable.just(definitions);
                    }
                })
                .map(new Func1<List<Definition>, Result>() {
                    @Override
                    public Result call(List<Definition> definitions) {
                        if (definitions.isEmpty()) {
                            return null;
                        }
                        return new Result(definitions);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Action1<Result>() {
                            @Override
                            public void call(Result result) {
                                if (result != null) {
                                    mLastResult = result;
                                    mResults.add(result);
                                }
                                MainActivity a = mRef.get();
                                if (a != null) {
                                    if (result != null) {
                                        a.onLookupResult(result);
                                    } else {
                                        a.onNoResults();
                                    }
                                }
                                if (mSubLookup != null && !mSubLookup.isUnsubscribed()) {
                                    mSubLookup.unsubscribe();
                                    mSubLookup = null;
                                }
                            }
                        },
                        mErrorHandler
                );
    }

    /**
     * Concat languages codes for sending request to server.
     *
     * @param reverse false: SOURCE-DEST, true: DEST-SOURCE
     * @return language parameter
     */
    private String getLangParam(boolean reverse) {
        if (reverse) {
            return mDest.getCode() + "-" + mSource.getCode();
        } else {
            return mSource.getCode() + "-" + mDest.getCode();
        }
    }

    /**
     * Return last successfull result
     *
     * @return {@link Result} object
     */
    public Result getLastResult() {
        return mLastResult;
    }

    /**
     * @return list of last queries
     */
    public String[] getHistory() {
        int size = mResults.size();
        String[] result = new String[size];
        for (int i = 0; i < size; i++) {
            result[i] = mResults.get(i).text;
        }
        return result;
    }

    public void loadHistory(int position) {
        mRef.get().onLookupResult(mResults.get(position));
    }

    /**
     * @return whenever activity should close on successful share
     */
    public boolean closeOnShare() {
        return mPrefs.closeOnShare();
    }

    /**
     * @return should Back button move focus to the search field
     */
    public boolean backFocusSearchField() {
        return mPrefs.backFocusSearch();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Languages
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Set source language by its position in languages list.
     *
     * @param position language index
     */
    public void setSourceLanguage(int position) {
        mSource = mLangs.get(position);
        mPrefs.setSourceLang(mSource);
    }

    /**
     * Set destination language by its position in languages list.
     *
     * @param position language index
     */
    public void setDestLanguage(int position) {
        mDest = mLangs.get(position);
        mPrefs.setDestLang(mDest);
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
        mSource = mLangs.get(0);
        for (Language l : mLangs) {
            if (l.getCode().equals(code)) {
                mSource = l;
                break;
            }
        }
        mPrefs.setSourceLang(mSource);
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
        mDest = mLangs.get(0);
        for (Language l : mLangs) {
            if (l.getCode().equals(code)) {
                mDest = l;
                break;
            }
        }
        mPrefs.setDestLang(mDest);
    }

    /**
     * @return list of the available languages.
     */
    public List<Language> getLanguagesList() {
        return mLangs;
    }

    /**
     * Sort language list.
     *
     * @see Language#compareTo(Language)
     */
    public void sortLanguagesList() {
        Collections.sort(mLangs);
    }

    /**
     * @return currently set source language
     */
    public Language getSourceLanguage() {
        return mSource;
    }

    /**
     * @return currently set destination language
     */
    public Language getDestLanguage() {
        return mDest;
    }

    /**
     * Swap languages.
     *
     * @return {@code true}, if languages were swapped, {@code false} otherwise
     * (ex. {@link #mSource} == {@link #mDest})
     */
    public boolean swapLanguages() {
        if (!TextUtils.equals(mSource.getCode(), mDest.getCode())) {
            Language tmp = mSource;
            mSource = mDest;
            mPrefs.setSourceLang(mSource);
            mDest = tmp;
            mPrefs.setDestLang(mDest);
            return true;
        }
        return false;
    }

    /**
     * Save language list on the disk.
     */
    public void saveLanguages() {
        if (mSubLangs != null && !mSubLangs.isUnsubscribed()) {
            mSubLangs.unsubscribe();
        }

        mSubLangs = Observable
                .create(new Observable.OnSubscribe<Object>() {
                    @Override
                    public void call(Subscriber<? super Object> subscriber) {
                        try {
                            String code = Locale.getDefault().getLanguage();
                            mPrefs.putLangs(mLangs, code);
                            if (!subscriber.isUnsubscribed()) {
                                subscriber.onNext(null);
                            }
                        } catch (Exception e) {
                            if (!subscriber.isUnsubscribed()) {
                                subscriber.onError(e);
                            }
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe(
                        new Action1<Object>() {
                            @Override
                            public void call(Object o) {
                                log("saveLanguages: success");
                                if (mSubLangs != null && !mSubLangs.isUnsubscribed()) {
                                    mSubLangs.unsubscribe();
                                    mSubLangs = null;
                                }
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                log("saveLanguages: " + throwable.toString());
                            }
                        }
                );
    }

    ///////////////////////////////////////////////////////////////////////////
    // Error handlers
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Generic error handler.
     */
    private Action1<Throwable> mErrorHandler = new Action1<Throwable>() {
        @Override
        public void call(Throwable throwable) {
            if (BuildConfig.DEBUG) {
                throwable.printStackTrace();
            }
            MainActivity a = mRef.get();
            if (a != null) {
                String message = a.getString(R.string.error);
                if (throwable instanceof IOException) {
                    message = a.getString(R.string.error_no_connection);
                }
                if (throwable instanceof HttpException) {
                    HttpException e = (HttpException) throwable;
                    switch (e.code()) {
                        case 501:
                            message = a.getString(R.string.error_lang_not_supported);
                            break;
                        case 401:
                        case 402:
                        case 403:
                        case 502:
                            // just error
                            break;
                        case 413:
                            message = a.getString(R.string.error_long_request);
                            break;
                        default:
                            message = e.getMessage();
                    }
                }
                a.onError(message);
            }
        }
    };

    /**
     * Handling languages fetching errors.
     */
    private Action1<Throwable> mGetLangsErrorHandler = new Action1<Throwable>() {
        @Override
        public void call(Throwable throwable) {
            throwable.printStackTrace();
            MainActivity a = mRef.get();
            if (a != null) {
                a.onLanguagesError();
            }
        }
    };

    ///////////////////////////////////////////////////////////////////////////
    // Logging
    ///////////////////////////////////////////////////////////////////////////

    private void log(String format, Object... args) {
        log(String.format(format, args));
    }

    private void log(String message) {
        log(Log.DEBUG, message);
    }

    private void log(int priority, String message) {
        if (BuildConfig.DEBUG) {
            Log.println(priority, TAG, message);
        }
    }

}
