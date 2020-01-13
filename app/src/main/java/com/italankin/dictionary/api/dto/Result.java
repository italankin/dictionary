package com.italankin.dictionary.api.dto;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Container object used to represent query result.
 */
public class Result implements Parcelable {

    public static final Result EMPTY = new Result(Collections.emptyList());

    public List<Definition> rawResult;
    public String text;
    public List<TranslationEx> translations;
    public String transcription;

    public Result(List<Definition> definitions) {
        this.rawResult = definitions;
        this.transcription = "";
        this.text = "";
        List<TranslationEx> list = new ArrayList<>();
        for (Definition d : definitions) {
            for (Translation t : d.tr) {
                list.add(new TranslationEx(t));
            }
            if (d.text != null && text.length() == 0) {
                this.text = d.text;
            }
            if (d.ts != null && transcription.length() == 0) {
                this.transcription = d.ts;
            }
        }
        this.translations = list;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (translations != null && !translations.isEmpty()) {
            TranslationEx t;
            for (int i = 0; i < translations.size(); i++) {
                t = translations.get(i);
                if (i != 0) {
                    sb.append("\n");
                }
                sb.append(t.text);
                if (t.means != null && t.means.length() > 0) {
                    sb.append(" (");
                    sb.append(t.means);
                    sb.append(")");
                }
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Result result = (Result) o;

        return text.equals(result.text);

    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Parcelable
    ///////////////////////////////////////////////////////////////////////////

    protected static Result from(Parcel in) {
        List<Definition> list = in.createTypedArrayList(Definition.CREATOR);
        return new Result(list);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(rawResult);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Result> CREATOR = new Creator<Result>() {
        @Override
        public Result createFromParcel(Parcel in) {
            return Result.from(in);
        }

        @Override
        public Result[] newArray(int size) {
            return new Result[size];
        }
    };

}
