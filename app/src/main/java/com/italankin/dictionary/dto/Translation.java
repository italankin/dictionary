package com.italankin.dictionary.dto;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Class represents a single word definition (or translation).
 */
public class Translation extends Attribute implements Parcelable {

    public Synonym[] syn;
    public Mean[] mean;
    public Example[] ex;

    public Translation() {
    }

    /**
     * Class for objects containing synonym data
     */
    public static class Synonym extends Attribute implements Parcelable {
        public Synonym(Parcel source) {
            super(source);
        }

        public static final Parcelable.Creator<Synonym> CREATOR = new Creator<Synonym>() {
            @Override
            public Synonym createFromParcel(Parcel source) {
                return new Synonym(source);
            }

            @Override
            public Synonym[] newArray(int size) {
                return new Synonym[size];
            }
        };
    }

    /**
     * Class for objects presenting meaning of the word
     */
    public static class Mean extends Attribute {
        public Mean(Parcel source) {
            super(source);
        }

        public static final Parcelable.Creator<Mean> CREATOR = new Creator<Mean>() {
            @Override
            public Mean createFromParcel(Parcel source) {
                return new Mean(source);
            }

            @Override
            public Mean[] newArray(int size) {
                return new Mean[size];
            }
        };
    }

    /**
     * Class for presenting examples for translation
     */
    public static class Example extends Attribute implements Parcelable {
        public Translation[] tr;

        public Example(Parcel source) {
            super(source);
            tr = source.createTypedArray(Translation.CREATOR);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeTypedArray(tr, 0);
        }

        public static final Parcelable.Creator<Example> CREATOR = new Creator<Example>() {
            @Override
            public Example createFromParcel(Parcel source) {
                return new Example(source);
            }

            @Override
            public Example[] newArray(int size) {
                return new Example[size];
            }
        };
    }

    ///////////////////////////////////////////////////////////////////////////
    // Parcelable
    ///////////////////////////////////////////////////////////////////////////

    public Translation(Parcel source) {
        super(source);
        syn = source.createTypedArray(Synonym.CREATOR);
        mean = source.createTypedArray(Mean.CREATOR);
        ex = source.createTypedArray(Example.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeTypedArray(syn, 0);
        dest.writeTypedArray(mean, 0);
        dest.writeTypedArray(ex, 0);
    }

    public static final Parcelable.Creator<Translation> CREATOR = new Creator<Translation>() {
        @Override
        public Translation createFromParcel(Parcel source) {
            return new Translation(source);
        }

        @Override
        public Translation[] newArray(int size) {
            return new Translation[size];
        }
    };

}
