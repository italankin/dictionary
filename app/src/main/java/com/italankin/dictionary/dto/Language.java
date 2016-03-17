package com.italankin.dictionary.dto;

import android.support.annotation.NonNull;

/**
 * Class for handling languages data.
 */
public class Language implements Comparable<Language> {

    private String code;
    private String name;
    private boolean favorite = false;

    public Language(String code, String name) {
        this.code = code;
        this.name = name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    @Override
    public int compareTo(@NonNull Language another) {
        if (this.favorite && !another.favorite) {
            return -1;
        } else if (!this.favorite && another.favorite) {
            return 1;
        }
        return this.name.compareTo(another.name);
    }

}
