package com.italankin.dictionary;

import android.content.Context;
import android.util.Log;

import com.italankin.dictionary.dto.Definition;
import com.italankin.dictionary.dto.Language;
import com.italankin.dictionary.dto.Translation;
import com.italankin.dictionary.utils.SharedPrefs;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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

    private boolean mAttached = false;

    private List<Language> mLangs;

    private Language mSource;
    private Language mDest;

    private List<Definition> mLastLookup;

    private Subscription mSubLangs;
    private Subscription mSubLookup;

    private MainPresenter(Context context) {
        mPrefs = SharedPrefs.getInstance(context);
        mClient = ApiClient.getInstance();
        mClient.setCacheDirectory(context.getCacheDir());
        mUiLanguage = Locale.getDefault().getLanguage();
    }

    public void attach(MainActivity activity) {
        mRef = new WeakReference<>(activity);
        mAttached = true;
    }

    public void detach() {
        mRef = NULL_REF;
        mAttached = false;
    }

    public boolean isAttached() {
        return mAttached;
    }

    public void getLangs() {
        if (mDest != null && mSource != null) {
            mRef.get().onLangsResult();
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
                                updateData(mPrefs.getLangs());
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
                    .subscribe(onGetLangsResult, mErrorHandler);
        } else {
            mSubLangs = mClient.getLangs(BuildConfig.API_KEY)
                    .map(new Func1<List<Language>, Object>() {
                        @Override
                        public Object call(List<Language> list) {
                            try {
                                mPrefs.putLangs(list);
                            } catch (IOException e) {
                                throw new RuntimeException(e.getMessage());
                            }
                            updateData(list);
                            return null;
                        }
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(onGetLangsResult, mErrorHandler);
        }
    }

    private void updateData(List<Language> list) {
        mLangs = list;
        if (!list.isEmpty()) {
            setSourceLangByCode(mPrefs.getSourceLang());
            setDestLangByCode(mPrefs.getDestLang());
            sortLangsList();
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

    public void lookup(String text) {
        if (mSubLookup != null && !mSubLookup.isUnsubscribed()) {
            mSubLookup.unsubscribe();
        }

        mSubLookup = mClient.lookup(BuildConfig.API_KEY, getLangParam(), text, mUiLanguage, 0)
                .map(new Func1<List<Definition>, List<Translation>>() {
                    @Override
                    public List<Translation> call(List<Definition> definitions) {
                        mLastLookup = definitions;
                        List<Translation> list = new ArrayList<>(0);
                        for (Definition d : definitions) {
                            Collections.addAll(list, d.tr);
                        }
                        return list;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Action1<List<Translation>>() {
                            @Override
                            public void call(List<Translation> list) {
                                MainActivity a = mRef.get();
                                if (a != null) {
                                    a.onLookupResult(list);
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

    public void saveLangs() {
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
                                log("saveLangs: success");
                                if (mSubLangs != null && !mSubLangs.isUnsubscribed()) {
                                    mSubLangs.unsubscribe();
                                    mSubLangs = null;
                                }
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                log("saveLangs: " + throwable.toString());
                            }
                        }
                );
    }

    public void setSourceLang(int position) {
        mSource = mLangs.get(position);
        mPrefs.setSourceLang(mSource);
    }

    public void setDestLang(int position) {
        mDest = mLangs.get(position);
        mPrefs.setDestLang(mDest);
    }

    public void setSourceLangByCode(String code) {
        mSource = mLangs.get(0);
        for (Language l : mLangs) {
            if (l.getCode().equals(code)) {
                mSource = l;
                break;
            }
        }
        mPrefs.setSourceLang(mSource);
    }

    public void setDestLangByCode(String code) {
        mDest = mLangs.get(0);
        for (Language l : mLangs) {
            if (l.getCode().equals(code)) {
                mDest = l;
                break;
            }
        }
        mPrefs.setDestLang(mDest);
    }

    public List<Language> getLangsList() {
        return mLangs;
    }

    public void sortLangsList() {
        Collections.sort(mLangs);
    }

    public Language getSource() {
        return mSource;
    }

    public Language getDest() {
        return mDest;
    }

    public void swapLangs() {
        Language tmp = mSource;
        mSource = mDest;
        mDest = tmp;
    }

    private String getLangParam() {
        return mSource.getCode() + "-" + mDest.getCode();
    }

    private Action1<Throwable> mErrorHandler = new Action1<Throwable>() {
        @Override
        public void call(Throwable throwable) {
            log(throwable.getMessage());
        }
    };

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
