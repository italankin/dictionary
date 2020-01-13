package com.italankin.dictionary.api.dto

import android.os.Parcel
import android.os.Parcelable

/**
 * Extended [Translation] class containing additional fields useful for UI.
 */
class TranslationEx(t: Translation) : Translation() {

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<TranslationEx> = object : Parcelable.Creator<TranslationEx> {

            override fun createFromParcel(source: Parcel): TranslationEx {
                return TranslationEx(Translation(source))
            }

            override fun newArray(size: Int): Array<TranslationEx?> {
                return arrayOfNulls(size)
            }
        }
    }

    /**
     * Contains all means as a single string delimited by [.DELIMITER]
     */
    @JvmField
    var means: String
    /**
     * Contains all examples as a single string delimited by [.DELIMITER]
     */
    @JvmField
    var examples: String
    /**
     * Contains all synonyms as a single string delimited by [.DELIMITER]
     */
    @JvmField
    var synonyms: String

    private fun concatText(attrs: Array<out Attribute>?): String {
        return if (attrs == null || attrs.isEmpty()) {
            ""
        } else {
            attrs.joinToString()
        }
    }

    init {
        mean = t.mean
        ex = t.ex
        syn = t.syn
        text = t.text
        pos = t.pos
        num = t.num
        gen = t.gen
        asp = t.asp
        examples = concatText(ex)
        synonyms = concatText(syn)
        means = concatText(mean)
    }
}
