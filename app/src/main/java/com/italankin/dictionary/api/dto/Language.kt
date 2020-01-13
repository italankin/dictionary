package com.italankin.dictionary.api.dto

/**
 * Class for handling languages data.
 */
class Language(
        val code: String,
        val name: String
) : Comparable<Language> {

    override fun compareTo(other: Language) = name.compareTo(other.name)

    override fun hashCode() = code.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Language) return false
        return code == other.code
    }
}
