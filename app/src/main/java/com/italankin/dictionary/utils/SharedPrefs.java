package com.italankin.dictionary.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.italankin.dictionary.dto.Language;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

public class SharedPrefs {

    private static final String LANGS = "langs.json";
    private static final int DEFAULT_CACHE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final int DEFAULT_CACHE_AGE = 60 * 60 * 24 * 30; // 30 days

    public static final String PREF_SOURCE = "source";
    public static final String PREF_DEST = "dest";
    public static final String PREF_LOOKUP_REVERSE = "lookup_reverse";
    public static final String PREF_YANDEX_DICT = "yandex_dict";
    public static final String PREF_CACHE_RESULTS = "cache_results";
    public static final String PREF_CACHE_CLEAR = "cache_clear";
    public static final String PREF_CACHE_SIZE = "cache_size";
    public static final String PREF_CACHE_AGE = "cache_age";

    private static SharedPrefs INSTANCE;

    private SharedPreferences mPreferences;
    private Context mContext;
    private Gson mGson;

    public static SharedPrefs getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new SharedPrefs(context);
        }
        return INSTANCE;
    }

    private SharedPrefs(Context context) {
        mContext = context;
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mGson = new Gson();
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

    public void putLangs(List<Language> list) throws IOException {
        File file = getLangsFile();
        FileOutputStream fs = new FileOutputStream(file);
        String json = mGson.toJson(list);
        fs.write(json.getBytes());
        fs.close();
    }

    public List<Language> getLangs() throws IOException {
        File file = getLangsFile();
        FileReader fs = new FileReader(file);
        Type collectionType = new TypeToken<List<Language>>() {
        }.getType();
        return mGson.fromJson(fs, collectionType);
    }

    public boolean hasLangsFile() {
        return getLangsFile().exists();
    }

    @NonNull
    private File getLangsFile() {
        File dir = mContext.getFilesDir();
        return new File(dir, LANGS);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Other
    ///////////////////////////////////////////////////////////////////////////

    public boolean lookupReverse() {
        return mPreferences.getBoolean(PREF_LOOKUP_REVERSE, true);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Cache
    ///////////////////////////////////////////////////////////////////////////

    public boolean cacheResults() {
        return mPreferences.getBoolean(PREF_CACHE_RESULTS, true);
    }

    public int getCacheSize() {
        return mPreferences.getInt(PREF_CACHE_SIZE, DEFAULT_CACHE_SIZE);
    }

    public int getCacheAge() {
        return mPreferences.getInt(PREF_CACHE_AGE, DEFAULT_CACHE_AGE);
    }

}
