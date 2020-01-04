package com.italankin.dictionary.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.italankin.dictionary.api.ApiClient;
import com.italankin.dictionary.dto.Language;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

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

    private final File filesDir;
    private final SharedPreferences preferences;
    private final Gson gson = new Gson();

    private Disposable mSaveLanguagesSub;

    public SharedPrefs(Context context) {
        filesDir = context.getFilesDir();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Destination language
    ///////////////////////////////////////////////////////////////////////////

    public void setDestLang(String code) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREF_DEST, code);
        editor.apply();
    }

    public void setDestLang(Language lang) {
        setDestLang(lang.getCode());
    }

    public String getDestLang() {
        return preferences.getString(PREF_DEST, null);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Source language
    ///////////////////////////////////////////////////////////////////////////

    public void setSourceLang(String code) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREF_SOURCE, code);
        editor.apply();
    }

    public void setSourceLang(Language lang) {
        setSourceLang(lang.getCode());
    }

    public String getSourceLang() {
        return preferences.getString(PREF_SOURCE, null);
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
            mSaveLanguagesSub = Completable
                    .fromCallable(() -> {
                        Collections.sort(list);
                        File file = getLangsFile();
                        if (!file.delete()) {
                            Timber.d("saveLanguagesList: delete failed");
                        }
                        FileOutputStream fs = new FileOutputStream(file);
                        String json = gson.toJson(list);
                        fs.write(json.getBytes());
                        fs.close();
                        String locale = Locale.getDefault().getLanguage();
                        preferences.edit().putString(PREF_LANGS_LOCALE, locale).apply();
                        return true;
                    })
                    .subscribeOn(Schedulers.io())
                    .subscribe(() -> Timber.d("saveLanguagesList: success"),
                            throwable -> Timber.e(throwable, "saveLanguagesList:"));
        }
    }

    public Single<List<Language>> getLanguagesList() {
        return Single
                .fromCallable(() -> {
                    File file = getLangsFile();
                    FileReader fs = new FileReader(file);
                    Type collectionType = new TypeToken<List<Language>>() {}.getType();
                    return gson.<List<Language>>fromJson(fs, collectionType);
                })
                .doOnError(throwable -> {
                    purgeLanguages();
                    Timber.e(throwable, "getLanguagesList:");
                });
    }

    public void setLangsTimestamp(Date date) {
        String timestamp = LANGS_TIMESTAMP.format(date);
        preferences.edit().putString(PREF_LANGS_TIMESTAMP, timestamp).apply();
    }

    public boolean shouldUpdateLangs() {
        boolean updatedLastTwoWeeks = false;
        if (preferences.contains(PREF_LANGS_TIMESTAMP)) {
            try {
                String s = preferences.getString(PREF_LANGS_TIMESTAMP, null);
                Date timestamp = LANGS_TIMESTAMP.parse(s);
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_MONTH, -14);
                updatedLastTwoWeeks = timestamp.after(calendar.getTime());
            } catch (ParseException e) {
                // failed to parse date
            }
        }
        String savedLocale = preferences.getString(PREF_LANGS_LOCALE, null);
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
            Timber.d("purgeLanguages: %s", file.delete());
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Other
    ///////////////////////////////////////////////////////////////////////////

    public boolean showShareFab() {
        return preferences.getBoolean(PREF_SHOW_SHARE_FAB, true);
    }

    public boolean lookupReverse() {
        return preferences.getBoolean(PREF_LOOKUP_REVERSE, true);
    }

    public boolean backFocusSearch() {
        return preferences.getBoolean(PREF_BACK_FOCUS, false);
    }

    public boolean closeOnShare() {
        return preferences.getBoolean(PREF_CLOSE_ON_SHARE, false);
    }

    public boolean shareIncludeTranscription() {
        return preferences.getBoolean(PREF_INCLUDE_TRANSCRIPTION, false);
    }

    public int getSearchFilter() {
        int family = preferences.getBoolean(PREF_FILTER_FAMILY, true) ? ApiClient.FILTER_FAMILY : 0;
        int shortPos = preferences.getBoolean(PREF_FILTER_SHORT_POS, false) ? ApiClient.FILTER_SHORT_POS : 0;
        int morpho = preferences.getBoolean(PREF_FILTER_MORPHO, false) ? ApiClient.FILTER_MORPHO : 0;
        int pos = preferences.getBoolean(PREF_FILTER_POS_FILTER, false) ? ApiClient.FILTER_POS_FILTER : 0;
        return family | shortPos | morpho | pos;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Internal
    ///////////////////////////////////////////////////////////////////////////

    private File getLangsFile() {
        return new File(filesDir, LANGS_FILE_NAME);
    }

}
