package com.italankin.dictionary.dto;

import android.os.Parcel;
import android.os.Parcelable;

public class Translation extends Attribute implements Parcelable {

    public Synonym[] syn;
    public Mean[] mean;
    public Example[] ex;

    public Translation() {
    }

    public static class Synonym extends Attribute implements Parcelable {
        public Synonym() {
        }

        public Synonym(Parcel source) {
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

    public static class Mean extends Attribute implements Parcelable {
        public Mean() {
        }

        public Mean(Parcel source) {
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

    public static class Example extends Attribute implements Parcelable {
        public Translation[] tr;

        public Example() {
        }

        public Example(Parcel source) {
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
            dest.writeParcelableArray(tr, 0);
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
        text = source.readString();
        pos = source.readString();
        num = source.readString();
        gen = source.readString();
        asp = source.readString();
        syn = source.createTypedArray(Synonym.CREATOR);
        mean = source.createTypedArray(Mean.CREATOR);
        ex = source.createTypedArray(Example.CREATOR);
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
