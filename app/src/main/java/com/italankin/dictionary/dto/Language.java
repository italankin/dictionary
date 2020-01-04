package com.italankin.dictionary.dto;

import androidx.annotation.NonNull;

/**
 * Class for handling languages data.
 */
public class Language implements Comparable<Language> {

    private String code;
    private String name;

    public Language(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    @Override
    public int compareTo(@NonNull Language another) {
        return this.name.compareTo(another.name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return code.equals(((Language) o).code);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }
}
