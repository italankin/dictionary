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
import android.os.Parcelable;

/**
 * Basic class for translation results
 */
public abstract class Attribute implements Parcelable {

    /**
     * Text of article, translation or synonym
     */
    public String text;

    /**
     * Part of speech
     */
    public String pos;

    /**
     * Quantity
     */
    public String num;

    /**
     * Gender, if used
     */
    public String gen;

    /**
     * Aspect
     */
    public String asp;

    public Attribute() {
    }

    @Override
    public String toString() {
        return "{text=" + text +
                ", pos=" + pos +
                ", num=" + num +
                ", gen=" + gen +
                ", asp=" + asp +
                "}";
    }

    ///////////////////////////////////////////////////////////////////////////
    // Parcelable
    ///////////////////////////////////////////////////////////////////////////

    public Attribute(Parcel source) {
        text = source.readString();
        pos = source.readString();
        num = source.readString();
        gen = source.readString();
        asp = source.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(text);
        dest.writeString(pos);
        dest.writeString(num);
        dest.writeString(gen);
        dest.writeString(asp);
    }

}
