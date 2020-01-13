package com.italankin.dictionary.ui.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.italankin.dictionary.R

/**
 * Additional fragment for settings screen. Displays search options.
 */
class SettingsSearchOptionsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle, rootKey: String) {
        addPreferencesFromResource(R.xml.prefs_search)
    }
}
