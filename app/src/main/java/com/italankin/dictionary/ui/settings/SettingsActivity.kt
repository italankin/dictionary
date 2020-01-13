package com.italankin.dictionary.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.italankin.dictionary.R

/**
 * Activity for displaying and manipulating user preferences.
 */
class SettingsActivity : AppCompatActivity(), SettingsRootFragment.Callbacks {

    companion object {
        private const val TAG_ROOT = "root"
        private const val TAG_SEARCH_OPTIONS = "search_options"

        @JvmStatic
        fun getStartIntent(context: Context?): Intent {
            return Intent(context, SettingsActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .add(R.id.container, SettingsRootFragment(), TAG_ROOT)
                    .commit()
        }
    }

    override fun onSearchFiltersClick() {
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.animator.fragment_in, R.animator.fragment_out,
                        R.animator.fragment_in, R.animator.fragment_out)
                .replace(R.id.container, SettingsSearchOptionsFragment(), TAG_SEARCH_OPTIONS)
                .addToBackStack(null)
                .commit()
    }
}
