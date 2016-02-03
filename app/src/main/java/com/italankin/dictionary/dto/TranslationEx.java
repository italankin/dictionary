package com.italankin.dictionary.dto;

import android.os.Parcel;

public class TranslationEx extends Translation {

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

        if (mean != null) {
            means = "";
            for (Mean m : mean) {
                if (means.length() > 0) {
                    means += ", " + m.text;
                } else {
                    means = m.text;
                }
            }
        }

        if (ex != null) {
            examples = "";
            for (Example e : ex) {
                if (examples.length() > 0) {
                    examples += ", " + e.text;
                } else {
                    examples = e.text;
                }
            }
        }

        if (syn != null) {
            synonyms = "";
            for (Synonym s : syn) {
                if (synonyms.length() > 0) {
                    synonyms += ", " + s.text;
                } else {
                    synonyms = s.text;
                }
            }
        }
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
