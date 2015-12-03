package com.italankin.dictionary.dto;

import android.support.annotation.NonNull;

public class Language implements Comparable<Language> {

    private String code;
    private String name;
    private boolean favorite = false;

    public Language(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public Language(String code, String name, boolean fav) {
        this.code = code;
        this.name = name;
        this.favorite = fav;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean mFavorite) {
        this.favorite = mFavorite;
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
