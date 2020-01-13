package com.italankin.dictionary.api.dto;

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
