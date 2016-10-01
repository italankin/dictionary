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
package com.italankin.dictionary.dto;

import android.os.Parcel;

/**
 * Extended {@link Translation} class containing additional fields useful for UI.
 */
public class TranslationEx extends Translation {

    public static final String DELIMITER = ", ";

    /**
     * Contains all means as a single string delimited by {@link #DELIMITER}
     */
    public String means = "";

    /**
     * Contains all examples as a single string delimited by {@link #DELIMITER}
     */
    public String examples = "";

    /**
     * Contains all synonyms as a single string delimited by {@link #DELIMITER}
     */
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

    @Override
    public int hashCode() {
        return text.hashCode();
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
