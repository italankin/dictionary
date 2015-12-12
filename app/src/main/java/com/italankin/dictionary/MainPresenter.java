package com.italankin.dictionary;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.italankin.dictionary.dto.Definition;
import com.italankin.dictionary.dto.Language;
import com.italankin.dictionary.dto.Result;
import com.italankin.dictionary.dto.Translation;
import com.italankin.dictionary.dto.TranslationEx;
import com.italankin.dictionary.utils.SharedPrefs;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
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

    private MainPresenter(Context context) {
        mPrefs = SharedPrefs.getInstance(context);
        mClient = ApiClient.getInstance();
        mClient.setCacheDirectory(context.getCacheDir());
        mUiLanguage = Locale.getDefault().getLanguage();
    }

    public void attach(MainActivity activity) {
        mRef = new WeakReference<>(activity);
    }

    public void detach() {
        mRef = NULL_REF;
        if (mSubLangs != null && mSubLangs.isUnsubscribed()) {
            mSubLangs.unsubscribe();
        }
        if (mSubLookup != null && mSubLookup.isUnsubscribed()) {
            mSubLookup.unsubscribe();
        }
    }

    public void loadLanguages() {
        if (mDest != null && mSource != null) {
            MainActivity a = mRef.get();
            a.onLangsResult();
            return;
        }

        if (mSubLangs != null && !mSubLangs.isUnsubscribed()) {
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

    private void updateLanguages(List<Language> list) {
        mLangs = list;
        if (!list.isEmpty()) {
            setSourceLanguageByCode(mPrefs.getSourceLang());
            setDestLanguageByCode(mPrefs.getDestLang());
            sortLanguagesList();
        }
    }

    private Action1<Object> onGetLangsResult = new Action1<Object>() {
        @Override
        public void call(Object o) {
            MainActivity a = mRef.get();
            if (a != null) {
                a.onLangsResult();
            }
            mSubLangs.unsubscribe();
        }
    };

    public void lookup(final String text) {
        if (mSubLookup != null && !mSubLookup.isUnsubscribed()) {
            mSubLookup.unsubscribe();
        }

        mSubLookup = mClient.lookup(BuildConfig.API_KEY, getLangParam(false), text, mUiLanguage, 0)
                .flatMap(new Func1<List<Definition>, Observable<List<Definition>>>() {
                    @Override
                    public Observable<List<Definition>> call(List<Definition> definitions) {
                        if (definitions == null || definitions.isEmpty()) {
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
                                mLastResult = result;
                                MainActivity a = mRef.get();
                                if (a != null) {
                                    a.onLookupResult(mLastResult);
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

    private String getLangParam(boolean reverse) {
        if (reverse) {
            return mDest.getCode() + "-" + mSource.getCode();
        } else {
            return mSource.getCode() + "-" + mDest.getCode();
        }
    }

    public Result getLastResult() {
        return mLastResult;
    }

    public boolean getLastResultAsync() {
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
            return true;
        } else {
            return false;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Languages
    ///////////////////////////////////////////////////////////////////////////

    public void setSourceLanguage(int position) {
        mSource = mLangs.get(position);
        mPrefs.setSourceLang(mSource);
    }

    public void setDestLanguage(int position) {
        mDest = mLangs.get(position);
        mPrefs.setDestLang(mDest);
    }

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

    public List<Language> getLanguagesList() {
        return mLangs;
    }

    public void sortLanguagesList() {
        Collections.sort(mLangs);
    }

    public Language getSourceLanguage() {
        return mSource;
    }

    public Language getDestLanguage() {
        return mDest;
    }

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

    private Action1<Throwable> mErrorHandler = new Action1<Throwable>() {
        @Override
        public void call(Throwable throwable) {
            throwable.printStackTrace();
            MainActivity a = mRef.get();
            if (a != null) {
                String message = a.getString(R.string.error);
                if (throwable instanceof UnknownHostException) {
                    message = a.getString(R.string.error_no_connection);
                }
                if (throwable instanceof ServerException) {
                    ServerException e = (ServerException) throwable;
                    message = e.getMessage();
                }
                a.onError(message);
            }
        }
    };

    private Action1<Throwable> mGetLangsErrorHandler = new Action1<Throwable>() {
        @Override
        public void call(Throwable throwable) {
            throwable.printStackTrace();
            MainActivity a = mRef.get();
            if (a != null) {
                a.onLangsError();
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
