package com.italankin.dictionary.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.italankin.dictionary.BuildConfig
import com.italankin.dictionary.R

/**
 * Root fragment for settings screen.
 */
class SettingsRootFragment : PreferenceFragmentCompat() {

    private var mCallbacks: Callbacks? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mCallbacks = context as Callbacks
    }

    override fun onCreatePreferences(savedInstanceState: Bundle, rootKey: String) {
        addPreferencesFromResource(R.xml.prefs_root)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val version = findPreference<Preference>("version")!!
        version.title = getString(R.string.version, BuildConfig.VERSION_NAME)
    }

    override fun onDetach() {
        super.onDetach()
        mCallbacks = null
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        val key = preference.key
        return when (key) {
            "open_source_libs" -> {
                Toast.makeText(activity, "TODO", Toast.LENGTH_SHORT).show()
                // TODO
                true
            }
            "search_filters" -> {
                mCallbacks!!.onSearchFiltersClick()
                true
            }
            else -> super.onPreferenceTreeClick(preference)
        }
    }

    interface Callbacks {
        fun onSearchFiltersClick()
    }
}
