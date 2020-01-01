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
package com.italankin.dictionary.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.italankin.dictionary.BuildConfig;
import com.italankin.dictionary.api.ApiClient;
import com.italankin.dictionary.dto.Language;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Wrapper around {@link SharedPreferences} for this application purposes.
 */
public class SharedPrefs {

    private static final String LANGS_FILE_NAME = "langs.json";
    private static final SimpleDateFormat LANGS_TIMESTAMP =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:MM:ssZ", Locale.US);

    private static final String PREF_SOURCE = "source";
    private static final String PREF_DEST = "dest";
    private static final String PREF_LANGS_LOCALE = "langs_locale";
    private static final String PREF_LANGS_TIMESTAMP = "langs_timestamp";
    private static final String PREF_LOOKUP_REVERSE = "lookup_reverse";
    private static final String PREF_BACK_FOCUS = "back_focus";
    private static final String PREF_CLOSE_ON_SHARE = "close_on_share";
    private static final String PREF_INCLUDE_TRANSCRIPTION = "include_transcription";
    private static final String PREF_FILTER_FAMILY = "filter_family";
    private static final String PREF_FILTER_SHORT_POS = "filter_short_pos";
    private static final String PREF_FILTER_MORPHO = "filter_morpho";
    private static final String PREF_FILTER_POS_FILTER = "filter_pos_filter";
    private static final String PREF_SHOW_SHARE_FAB = "show_share_fab";

    private final SharedPreferences mPreferences;
    private final Context mContext;
    private final Gson mGson = new Gson();

    private Observable<List<Language>> mLanguagesObservable;
    private Disposable mSaveLanguagesSub;

    public SharedPrefs(Context context) {
        mContext = context;
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Destination language
    ///////////////////////////////////////////////////////////////////////////

    public void setDestLang(String code) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(PREF_DEST, code);
        editor.apply();
    }

    public void setDestLang(Language lang) {
        setDestLang(lang.getCode());
    }

    public String getDestLang() {
        return mPreferences.getString(PREF_DEST, null);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Source language
    ///////////////////////////////////////////////////////////////////////////

    public void setSourceLang(String code) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(PREF_SOURCE, code);
        editor.apply();
    }

    public void setSourceLang(Language lang) {
        setSourceLang(lang.getCode());
    }

    public String getSourceLang() {
        return mPreferences.getString(PREF_SOURCE, null);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Languages list
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Save list of languages on disk.
     *
     * @param list list of languages
     */
    public void saveLanguagesList(List<Language> list) {
        if (list == null) {
            return;
        }
        if (mSaveLanguagesSub == null || mSaveLanguagesSub.isDisposed()) {
            mSaveLanguagesSub = Observable.just(new ArrayList<>(list))
                    .map( languages -> {
                        try {
                            Collections.sort(languages);
                            File file = getLangsFile();
                            if (!file.delete() && BuildConfig.DEBUG) {
                                Log.d("SharedPrefs", "saveLanguagesList: delete failed");
                            }
                            FileOutputStream fs = new FileOutputStream(file);
                            String json = mGson.toJson(languages);
                            fs.write(json.getBytes());
                            fs.close();
                            String locale = Locale.getDefault().getLanguage();
                            mPreferences.edit().putString(PREF_LANGS_LOCALE, locale).apply();
                            return true;
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .subscribe(result -> {
                        Log.d("SharedPrefs", "saveLanguagesList: " + result);
                    }, throwable -> {
                        Log.e("SharedPrefs", "saveLanguagesList: ", throwable);
                    });
        }
    }

    public Single<List<Language>> getLanguagesList() {
        return Single
                .fromCallable(() -> {
                    File file = getLangsFile();
                    FileReader fs = new FileReader(file);
                    Type collectionType = new TypeToken<List<Language>>() {}.getType();
                    return mGson.<List<Language>>fromJson(fs, collectionType);
                })
                .doOnError(throwable -> {
                    purgeLanguages();
                    Log.e("SharedPrefs", "getLanguagesList: ", throwable);
                });
    }

    public void setLangsTimestamp(Date date) {
        String timestamp = LANGS_TIMESTAMP.format(date);
        mPreferences.edit().putString(PREF_LANGS_TIMESTAMP, timestamp).apply();
    }

    public boolean shouldUpdateLangs() {
        boolean updatedLastTwoWeeks = false;
        if (mPreferences.contains(PREF_LANGS_TIMESTAMP)) {
            try {
                String s = mPreferences.getString(PREF_LANGS_TIMESTAMP, null);
                Date timestamp = LANGS_TIMESTAMP.parse(s);
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_MONTH, -14);
                updatedLastTwoWeeks = timestamp.after(calendar.getTime());
            } catch (ParseException e) {
                // failed to parse date
            }
        }
        String savedLocale = mPreferences.getString(PREF_LANGS_LOCALE, null);
        String currentLocale = Locale.getDefault().getLanguage();
        return !updatedLastTwoWeeks || !currentLocale.equals(savedLocale) || !hasLangsFile();
    }

    public boolean hasLangsFile() {
        return getLangsFile().exists();
    }

    public void purgeLanguages() {
        if (mSaveLanguagesSub != null && !mSaveLanguagesSub.isDisposed()) {
            mSaveLanguagesSub.dispose();
        }
        File file = getLangsFile();
        if (file.exists()) {
            boolean delete = file.delete();
            if (BuildConfig.DEBUG) {
                Log.d("SharedPrefs", "purgeLanguages: " + delete);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Other
    ///////////////////////////////////////////////////////////////////////////

    public boolean showShareFab() {
        return mPreferences.getBoolean(PREF_SHOW_SHARE_FAB, true);
    }

    public boolean lookupReverse() {
        return mPreferences.getBoolean(PREF_LOOKUP_REVERSE, true);
    }

    public boolean backFocusSearch() {
        return mPreferences.getBoolean(PREF_BACK_FOCUS, false);
    }

    public boolean closeOnShare() {
        return mPreferences.getBoolean(PREF_CLOSE_ON_SHARE, false);
    }

    public boolean shareIncludeTranscription() {
        return mPreferences.getBoolean(PREF_INCLUDE_TRANSCRIPTION, false);
    }

    public int getSearchFilter() {
        int family = mPreferences.getBoolean(PREF_FILTER_FAMILY, true) ? ApiClient.FILTER_FAMILY : 0;
        int shortPos = mPreferences.getBoolean(PREF_FILTER_SHORT_POS, false) ? ApiClient.FILTER_SHORT_POS : 0;
        int morpho = mPreferences.getBoolean(PREF_FILTER_MORPHO, false) ? ApiClient.FILTER_MORPHO : 0;
        int pos = mPreferences.getBoolean(PREF_FILTER_POS_FILTER, false) ? ApiClient.FILTER_POS_FILTER : 0;
        return family | shortPos | morpho | pos;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Internal
    ///////////////////////////////////////////////////////////////////////////

    private File getLangsFile() {
        File dir = mContext.getFilesDir();
        return new File(dir, LANGS_FILE_NAME);
    }

}
