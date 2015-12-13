package com.italankin.dictionary.ui;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.italankin.dictionary.BuildConfig;
import com.italankin.dictionary.R;
import com.italankin.dictionary.api.ApiClient;
import com.italankin.dictionary.api.ServerException;
import com.italankin.dictionary.dto.Definition;
import com.italankin.dictionary.dto.Language;
import com.italankin.dictionary.dto.Result;
import com.italankin.dictionary.utils.SharedPrefs;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MainPresenter {

    private static final WeakReference<MainActivity> NULL_REF = new WeakReference<>(null);

    private static MainPresenter INSTANCE;

    public static MainPresenter getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new MainPresenter(context);
        }
        return INSTANCE;
    }

    private WeakReference<MainActivity> mRef;

    private final ApiClient mClient;
    private final SharedPrefs mPrefs;
    private final String mUiLanguage;

    private List<Language> mLangs;

    private Language mSource;
    private Language mDest;

    private Subscription mSubLangs;
    private Subscription mSubLookup;

    private Result mLastResult;

    private LinkedHashSet<String> mLastQueries = new LinkedHashSet<>(0);

    private MainPresenter(Context context) {
        mPrefs = SharedPrefs.getInstance(context);
        mClient = ApiClient.getInstance();
        if (mPrefs.cacheResults()) {
            int size = mPrefs.getCacheSize();
            int age = mPrefs.getCacheAge();
            mClient.setCacheDirectory(context.getCacheDir(), size, age);
        }
        mUiLanguage = Locale.getDefault().getLanguage();
    }

    /**
     * @param activity activity for attaching presenter to
     */
    public void attach(MainActivity activity) {
        mRef = new WeakReference<>(activity);
    }

    /**
     * Called when activity is finishing.
     */
    public void detach() {
        mRef = NULL_REF;
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

        if (mPrefs.hasLangsFile()) {
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
        } else {
            mSubLangs = mClient.getLangs(BuildConfig.API_KEY)
                    .map(new Func1<List<Language>, Object>() {
                        @Override
                        public Object call(List<Language> list) {
                            try {
                                if (list == null || list.isEmpty()) {
                                    throw new NullPointerException();
                                }
                                mPrefs.putLangs(list);
                            } catch (IOException e) {
                                throw new RuntimeException(e.getMessage());
                            }
                            updateLanguages(list);
                            return null;
                        }
                    })
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

        mSubLookup = mClient.lookup(BuildConfig.API_KEY, getLangParam(false), text, mUiLanguage, 0)
                .flatMap(new Func1<List<Definition>, Observable<List<Definition>>>() {
                    @Override
                    public Observable<List<Definition>> call(List<Definition> definitions) {
                        if ((definitions == null || definitions.isEmpty()) && mPrefs.lookupReverse()) {
                            // if we got no result, attempt to lookup in reverse direction
                            return mClient.lookup(BuildConfig.API_KEY, getLangParam(true), text,
                                    mUiLanguage, 0);
                        }
                        return Observable.just(definitions);
                    }
                })
                .map(new Func1<List<Definition>, Result>() {
                    @Override
                    public Result call(List<Definition> definitions) {
                        if (definitions == null || definitions.isEmpty()) {
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
                                    mLastQueries.add(result.text);
                                }
                                MainActivity a = mRef.get();
                                if (a != null) {
                                    a.onLookupResult(result);
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
     * Dispatch last result to activity. Used when configuration changes.
     */
    public void getLastResultAsync() {
        if (mLastResult != null) {
            mSubLookup = Observable.timer(300, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Long>() {
                        @Override
                        public void call(Long aLong) {
                            MainActivity a = mRef.get();
                            if (a != null) {
                                a.onLookupResult(mLastResult);
                            }
                            if (mSubLookup != null && !mSubLookup.isUnsubscribed()) {
                                mSubLookup.unsubscribe();
                                mSubLookup = null;
                            }
                        }
                    });
        }
    }

    public String[] getLastQueries() {
        String[] result = new String[mLastQueries.size()];
        mLastQueries.toArray(result);
        return result;
    }

    public void clearLastQueries() {
        mLastQueries.clear();
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
     * @return <b>true</b>, if languages were swapped, <b>false</b> otherwise (ex. {@link #mSource}
     * == {@link #mDest})
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
                            mPrefs.putLangs(mLangs);
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
            throwable.printStackTrace();
            MainActivity a = mRef.get();
            if (a != null) {
                String message = a.getString(R.string.error);
                if (throwable instanceof IOException) {
                    message = a.getString(R.string.error_no_connection);
                }
                if (throwable instanceof ServerException) {
                    ServerException e = (ServerException) throwable;
                    switch (e.getCode()) {

                        case 501:
                            message = a.getString(R.string.error_lang_not_supported);
                            break;
                        case 401:
                        case 402:
                        case 403:
                        case 502:
                            message = a.getString(R.string.error);
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
            Log.println(priority, getClass().getSimpleName(), message);
        }
    }

}