package com.italankin.dictionary.dto;

import android.os.Parcel;
import android.util.Log;

/**
 * Extended {@link Translation} class containing additional fields useful for UI.
 */
public class TranslationEx extends Translation {

    public static final String DELIMITER = ", ";

    /**
     * Contains all means as a single string delimited by {@link #DELIMITER}
     */
    public String means = "";
    public String examples = "";
    public String synonyms = "";

    public TranslationEx(Translation t) {
        mean = t.mean;
        ex = t.ex;
        syn = t.syn;
        text = t.text;
        pos = t.pos;
        num = t.num;
        gen = t.gen;
        asp = t.asp;

        examples = concatText(ex);
        synonyms = concatText(syn);
        means = concatText(mean);
    }

    private String concatText(Attribute[] attrs) {
        if (attrs == null || attrs.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0, s = attrs.length; i < s; i++) {
            if (i > 0) {
                sb.append(DELIMITER);
            }
            sb.append(attrs[i].text);
        }
        return sb.toString();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Parcelable
    ///////////////////////////////////////////////////////////////////////////

    public static final Creator<TranslationEx> CREATOR = new Creator<TranslationEx>() {
        @Override
        public TranslationEx createFromParcel(Parcel source) {
            return new TranslationEx(new Translation(source));
        }

        @Override
        public TranslationEx[] newArray(int size) {
            return new TranslationEx[size];
        }
    };

}
