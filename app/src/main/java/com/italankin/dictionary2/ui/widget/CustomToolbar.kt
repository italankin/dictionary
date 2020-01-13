package com.italankin.dictionary2.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MenuItem
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.MenuRes
import androidx.appcompat.widget.Toolbar
import com.italankin.dictionary.R
import com.italankin.dictionary.api.dto.Language
import com.italankin.dictionary.ui.main.LanguageAdapter
import com.italankin.dictionary.ui.main.util.SwitchAnimation
import com.italankin.dictionary2.ui.ext.doSilently
import kotlin.math.sign

class CustomToolbar : Toolbar {

    companion object {
        private const val ANIM_SWITCH_DURATION = 450L
        private const val ANIM_SWAP_DURATION = 300L
        private const val ANIM_TOOLBAR_DURATION = 600L
    }

    private lateinit var spinnerSource: Spinner
    private lateinit var spinnerDest: Spinner
    private val btnSwap: View

    private var languageCallbacks: LanguageCallbacks? = null
    private var adapter: LanguageAdapter

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        val inset = context.resources.getDimensionPixelSize(R.dimen.toolbar_inset)
        setContentInsetsRelative(inset, inset)
        navigationIcon = null
        adapter = LanguageAdapter(context)
        inflate(context, R.layout.toolbar_inner, this)
        btnSwap = findViewById<View>(R.id.swap_langs)
        btnSwap.setOnLongClickListener {
            Toast.makeText(context, R.string.toast_swap_langs, Toast.LENGTH_SHORT).show()
            true
        }
        initSpinners()
    }

    fun setMenu(@MenuRes res: Int, listener: (MenuItem) -> Boolean) {
        inflateMenu(res)
        setOnMenuItemClickListener(listener)
    }

    fun setLanguageCallbacks(callbacks: LanguageCallbacks) {
        this.languageCallbacks = callbacks
    }

    fun setAdapterListener(listener: LanguageAdapter.CheckedChangeListener) {
        adapter.setListener(listener)
    }

    fun setLanguages(languages: List<Language>, sourceIndex: Int, destIndex: Int) {
        adapter.setDataset(languages)
        setSelectedLanguages(sourceIndex, destIndex)
    }

    fun setSelectedLanguages(sourceIndex: Int, destIndex: Int) {
        spinnerDest.doSilently {
            setSelection(destIndex)
        }
        spinnerSource.doSilently {
            setSelection(sourceIndex)
        }
    }

    fun notifyLanguagesChanged() {
        adapter.notifyDataSetChanged()
    }

    fun animateAppearance() {
        val toolbarInnerLayout = findViewById<View>(R.id.toolbar_inner_layout)
        toolbarInnerLayout.visibility = View.VISIBLE
        toolbarInnerLayout.translationY = -toolbarInnerLayout.height.toFloat()
        toolbarInnerLayout.alpha = 0f
        toolbarInnerLayout.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(ANIM_TOOLBAR_DURATION)
                .setInterpolator(DecelerateInterpolator(2.5f))
                .start()
    }

    fun animateSwapLanguages(onSwap: () -> Unit) {
        val rotationDelta = sign(btnSwap.rotation) * -180f
        btnSwap.rotation = 0f
        btnSwap.animate()
                .rotationBy(rotationDelta)
                .setDuration(ANIM_SWAP_DURATION)
                .start()

        spinnerDest.isEnabled = false
        spinnerSource.isEnabled = false

        SwitchAnimation(
                spinnerDest,
                (-spinnerDest.height).toFloat(),
                0f,
                ANIM_SWITCH_DURATION
        ) {
            spinnerDest.isEnabled = true
            spinnerSource.isEnabled = true
            onSwap()
        }.start()

        SwitchAnimation(
                spinnerSource,
                spinnerSource.height.toFloat(),
                0f,
                ANIM_SWITCH_DURATION,
                null
        ).start()
    }

    private fun initSpinners() {
        spinnerSource = findViewById(R.id.spinner_lang_source)
        spinnerSource.adapter = adapter
        spinnerSource.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                languageCallbacks?.onSourceLanguageChanged(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                languageCallbacks?.onNothingSelected()
            }
        }

        spinnerDest = findViewById(R.id.spinner_lang_dest)
        spinnerDest.adapter = adapter
        spinnerDest.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                languageCallbacks?.onDestLanguageChanged(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                languageCallbacks?.onNothingSelected()
            }
        }
    }

    interface LanguageCallbacks {
        fun onSourceLanguageChanged(newPosition: Int)

        fun onDestLanguageChanged(newPosition: Int)

        fun onNothingSelected()
    }
}
