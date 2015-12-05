package com.italankin.dictionary.utils;

import android.content.Context;
import android.content.SharedPreferences;
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
    public static final String PREF_SOURCE = "source";
    public static final String PREF_DEST = "dest";

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
        mPreferences = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        mGson = new Gson();
    }

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

}
