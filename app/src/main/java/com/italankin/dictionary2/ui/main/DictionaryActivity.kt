package com.italankin.dictionary2.ui.main

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ShareCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arellomobile.mvp.MvpAppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.italankin.dictionary.App
import com.italankin.dictionary.R
import com.italankin.dictionary.api.dto.Language
import com.italankin.dictionary.api.dto.Result
import com.italankin.dictionary.api.dto.TranslationEx
import com.italankin.dictionary.ui.main.LanguageAdapter
import com.italankin.dictionary.ui.main.TranslationAdapter
import com.italankin.dictionary.ui.main.TranslationAdapter.OnAdapterItemClickListener
import com.italankin.dictionary.ui.settings.SettingsActivity
import com.italankin.dictionary.ui.translation.TranslationActivity
import com.italankin.dictionary.utils.SharedPrefs
import com.italankin.dictionary2.ui.ext.setPaddings
import com.italankin.dictionary2.ui.main.DictionaryView.ContextMenuAction
import com.italankin.dictionary2.ui.main.widget.CustomToolbar
import com.italankin.dictionary2.ui.main.widget.InputFieldView
import javax.inject.Inject
import kotlin.LazyThreadSafetyMode.NONE

@SuppressLint("RestrictedApi")
class DictionaryActivity : MvpAppCompatActivity(), DictionaryView, CustomToolbar.LanguageCallbacks {

    companion object {
        private const val SHARE_FAB_ANIM_DURATION = 300L
        private const val REQUEST_CODE_SHARE = 1
    }

    @Inject
    lateinit var clipboardManager: ClipboardManager

    @Inject
    lateinit var prefs: SharedPrefs

    private val presenter: DictionaryPresenter by lazy(NONE) {
        App.presenters().dictionaryPresenter
    }

    private lateinit var root: CoordinatorLayout
    private lateinit var inputField: InputFieldView
    private lateinit var toolbar: CustomToolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var shareFab: FloatingActionButton

    private lateinit var recyclerViewAdapter: TranslationAdapter
    private var hasResult: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        App.injector().inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inputField = findViewById(R.id.input_field)
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.recycler_view)
        shareFab = findViewById(R.id.btn_share)
        shareFab.setOnClickListener { presenter.shareLastResult() }

        initToolbar()
        initInputField()
        initRecyclerView()

        setControlsState(false)
    }


    override fun onStart() {
        super.onStart()
        setShowFab(hasResult && prefs.showShareFab)
    }

    override fun onStop() {
        super.onStop()
        presenter.saveLanguages()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SHARE && resultCode == RESULT_OK && prefs.closeOnShare) {
            finish()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            handleIntent(intent)
        }
    }

    override fun onBackPressed() {
        if (prefs.backFocusSearch && !inputField.hasFocus()) {
            inputField.requestFocus()
        } else {
            super.onBackPressed()
        }
    }

    override fun onLanguagesResult(languages: List<Language>, sourceIndex: Int, destIndex: Int) {
        toolbar.setLanguages(languages, sourceIndex, destIndex)
        toolbar.animateAppearance()
        setControlsState(true)
        if (!handleIntent(intent)) {
            // TODO
        }
    }

    override fun onLanguagesError() {
        setControlsState(false)
        Snackbar.make(root, R.string.error_langs, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.retry) { presenter.loadLanguages() }
                .show()
    }

    override fun switchLanguages(sourceIndex: Int, destIndex: Int) {
        toolbar.setSelectedLanguages(sourceIndex, destIndex)
    }

    override fun onLookupResult(result: Result) {
        hasResult = true
        inputField.setText(result.text)
        inputField.clearFocus()
        inputField.setTranscription(result.transcription)
        recyclerViewAdapter.setData(result.translations)
        recyclerView.smoothScrollToPosition(0)
        recyclerView.visibility = View.VISIBLE
        inputField.setProgressBarVisibility(false)
        setShowFab(prefs.showShareFab)
    }

    override fun onEmptyResult() {
        showError(R.string.error_no_results)
    }

    override fun showProgress(show: Boolean) {
        inputField.setProgressBarVisibility(show)
    }

    override fun startTranslationActivity(translation: TranslationEx) {
        val intent = TranslationActivity.getStartIntent(this, translation)
        startActivity(intent)
    }

    override fun showError(@StringRes message: Int) {
        inputField.setProgressBarVisibility(false)
        Snackbar.make(root, if (message != 0) message else R.string.error, Snackbar.LENGTH_LONG)
                .setAction(android.R.string.ok) { /* empty callback just to show OK button */ }
                .show()
    }

    override fun copyClip(clip: String) {
        clipboardManager.primaryClip = ClipData.newPlainText(getString(R.string.clip), clip)
        Toast.makeText(this, R.string.msg_copied, Toast.LENGTH_SHORT).show()
    }

    override fun shareContent(subject: String?, text: String?) {
        if (subject == null || text == null) {
            Toast.makeText(this, R.string.error_share, Toast.LENGTH_SHORT).show()
        } else {
            val intent = ShareCompat.IntentBuilder
                    .from(this)
                    .setType("text/plain")
                    .setSubject(subject)
                    .setText(text)
                    .createChooserIntent()
            startActivityForResult(intent, REQUEST_CODE_SHARE)
        }
    }

    override fun onSourceLanguageChanged(newPosition: Int) {
        toolbar.setSelectedLanguages(sourceIndex = newPosition)
        presenter.setSourceLanguage(newPosition)
    }

    override fun onDestLanguageChanged(newPosition: Int) {
        toolbar.setSelectedLanguages(destIndex = newPosition)
        presenter.setDestLanguage(newPosition)
    }

    override fun onNothingSelected() {
        presenter.saveLanguages()
    }

    private fun setControlsState(enabled: Boolean) {
        inputField.isEnabled = enabled
        recyclerView.isEnabled = enabled
    }

    private fun initToolbar() {
        toolbar.setAdapterListener(LanguageAdapter.CheckedChangeListener { language, isChecked ->
            // TODO mark language as favorite
            presenter.saveLanguages()
        })
        toolbar.setLanguageCallbacks(this)
        toolbar.setMenu(R.menu.main) { item ->
            return@setMenu when (item.itemId) {
                R.id.action_share -> {
                    presenter.shareLastResult()
                    true
                }
                R.id.action_history -> {
                    // TODO show history
                    true
                }
                R.id.action_settings -> {
                    startActivity(SettingsActivity.getStartIntent(this))
                    true
                }
                else -> false
            }
        }
        toolbar.setOnButtonSwapClickListener {
            toolbar.animateSwapLanguages {
                presenter.swapLanguages()
            }
        }
    }

    private fun initInputField() {
        inputField.setOnLookupClickListener {
            presenter.lookup(inputField.getText())
        }
        inputField.setOnTranscriptionClickListener {
            val intent = ShareCompat.IntentBuilder
                    .from(this)
                    .setType("text/plain")
                    .setText(inputField.getTranscriptionText())
                    .createChooserIntent()
            startActivity(intent)
        }
    }

    private fun initRecyclerView() {
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerViewAdapter = TranslationAdapter(this)
        recyclerViewAdapter.setHasStableIds(true)
        recyclerViewAdapter.setListener(object : OnAdapterItemClickListener {
            override fun onItemClick(position: Int) {
                presenter.showTranslation(position)
            }

            override fun onItemMenuClick(position: Int, menuItemId: Int) {
                if (menuItemId == R.id.action_lookup_word) {
                    presenter.lookupByTranslation(position)
                    return
                }
                val contextMenuAction = when (menuItemId) {
                    R.id.action_copy_mean -> ContextMenuAction.COPY_MEAN
                    R.id.action_copy_synonyms -> ContextMenuAction.COPY_SYNONYM
                    R.id.action_copy_translation -> ContextMenuAction.COPY_TRANSLATION
                    else -> throw IllegalArgumentException("unknown menuItemId=$menuItemId")
                }
                presenter.copyClip(position, contextMenuAction)
            }
        })
        recyclerView.adapter = recyclerViewAdapter
    }

    private fun handleIntent(intent: Intent?): Boolean {
        if (intent == null) {
            return false
        }
        if (intent.type == "text/plain" && intent.hasExtra(Intent.EXTRA_TEXT)) {
            val max = resources.getInteger(R.integer.max_input_length)
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.take(max) ?: return false
            if (text.isNotEmpty()) {
                intent.type = null
                inputField.setText(text)
                presenter.lookup(text)
                return true
            }
        }
        return false
    }

    private fun setShowFab(show: Boolean, animated: Boolean = true) {
        if (show == shareFab.isVisible) {
            return
        }
        val bottomPadding = if (show) R.dimen.list_bottom_padding_fab else R.dimen.list_bottom_padding_no_fab
        recyclerView.setPaddings(bottom = resources.getDimensionPixelSize(bottomPadding))
        if (show) {
            shareFab.visibility = View.VISIBLE
            if (animated) {
                shareFab.scaleX = 0f
                shareFab.scaleY = 0f
                shareFab.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(SHARE_FAB_ANIM_DURATION)
                        .setInterpolator(DecelerateInterpolator(2f))
                        .start()
            }
        } else {
            shareFab.visibility = View.GONE
        }
    }
}
