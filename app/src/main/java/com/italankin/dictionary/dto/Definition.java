package com.italankin.dictionary.dto;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class Definition extends Attribute {

    /**
     * Transcription
     */
    public String ts;

    /**
     * Array of translations
     */
    public Translation[] tr;

    ///////////////////////////////////////////////////////////////////////////
    // Parcelable
    ///////////////////////////////////////////////////////////////////////////

    protected Definition(Parcel in) {
        super(in);
        ts = in.readString();
        tr = in.createTypedArray(Translation.CREATOR);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(ts);
        dest.writeTypedArray(tr, 0);
    }

    public static final Creator<Definition> CREATOR = new Parcelable.Creator<Definition>() {
        @Override
        public Definition createFromParcel(Parcel in) {
            return new Definition(in);
        }

        @Override
        public Definition[] newArray(int size) {
            return new Definition[size];
        }
    };

}
