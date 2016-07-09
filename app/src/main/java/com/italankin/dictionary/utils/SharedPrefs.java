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
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.italankin.dictionary.R;
import com.italankin.dictionary.api.ApiClient;
import com.italankin.dictionary.dto.Language;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;

/**
 * Wrapper around {@link SharedPreferences} for this application purposes.
 */
public class SharedPrefs {

    private static final String LANGS = "langs.json";

    private static final String PREF_SOURCE = "source";
    private static final String PREF_DEST = "dest";
    private static final String PREF_LANGS_LOCALE = "langs_locale";
    private static String PREF_LOOKUP_REVERSE;
    private static String PREF_BACK_FOCUS;
    private static String PREF_CLOSE_ON_SHARE;
    private static String PREF_FILTER_FAMILY;
    private static String PREF_FILTER_SHORT_POS;
    private static String PREF_FILTER_MORPHO;
    private static String PREF_FILTER_POS_FILTER;

    private SharedPreferences mPreferences;
    private Context mContext;
    private Gson mGson;

    public SharedPrefs(Context context) {
        mContext = context;
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mGson = new Gson();

        PREF_LOOKUP_REVERSE = context.getString(R.string.pref_key_lookup_reverse);
        PREF_BACK_FOCUS = context.getString(R.string.pref_key_back_focus);
        PREF_CLOSE_ON_SHARE = context.getString(R.string.pref_key_close_on_share);
        PREF_FILTER_FAMILY = context.getString(R.string.pref_key_filter_family);
        PREF_FILTER_SHORT_POS = context.getString(R.string.pref_key_filter_short_pos);
        PREF_FILTER_MORPHO = context.getString(R.string.pref_key_filter_morpho);
        PREF_FILTER_POS_FILTER = context.getString(R.string.pref_key_filter_pos_filter);
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

    public void putLangs(List<Language> list, String locale) throws IOException {
        File file = getLangsFile();
        FileOutputStream fs = new FileOutputStream(file);
        String json = mGson.toJson(list);
        fs.write(json.getBytes());
        fs.close();
        mPreferences.edit().putString(PREF_LANGS_LOCALE, locale).apply();
    }

    public List<Language> getLangs() throws IOException {
        File file = getLangsFile();
        FileReader fs = new FileReader(file);
        Type collectionType = new TypeToken<List<Language>>() {
        }.getType();
        return mGson.fromJson(fs, collectionType);
    }

    public boolean shouldUpdateLangs() {
        String savedLocale = mPreferences.getString(PREF_LANGS_LOCALE, null);
        String currentLocale = Locale.getDefault().getLanguage();
        return currentLocale != null && !currentLocale.equals(savedLocale) || !hasLangsFile();
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

    public boolean backFocusSearch() {
        return mPreferences.getBoolean(PREF_BACK_FOCUS, false);
    }

    public boolean closeOnShare() {
        return mPreferences.getBoolean(PREF_CLOSE_ON_SHARE, false);
    }

    public int getSearchFilter() {
        int family = mPreferences.getBoolean(PREF_FILTER_FAMILY, true) ? ApiClient.FILTER_FAMILY : 0;
        int shortPos = mPreferences.getBoolean(PREF_FILTER_SHORT_POS, false) ? ApiClient.FILTER_SHORT_POS : 0;
        int morpho = mPreferences.getBoolean(PREF_FILTER_MORPHO, false) ? ApiClient.FILTER_MORPHO : 0;
        int pos = mPreferences.getBoolean(PREF_FILTER_POS_FILTER, false) ? ApiClient.FILTER_POS_FILTER : 0;
        return family | shortPos | morpho | pos;
    }

}
