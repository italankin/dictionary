package com.italankin.dictionary2.ui.main.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.italankin.dictionary.R

class InputFieldView : CardView {

    companion object {
        private const val PROGRESS_ANIM_DURATION: Long = 300
    }

    private val input: EditText
    private val lookup: View
    private val transcription: TextView
    private val progressBar: ProgressBar

    private var onLookupCLicklistener: ((View) -> Unit)? = null

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        inflate(context, R.layout.input_field, this)
        input = findViewById(R.id.edit_input)
        transcription = findViewById(R.id.text_transcription)
        lookup = findViewById(R.id.lookup)
        progressBar = findViewById(R.id.progress_bar)

        setOnClickListener { input.requestFocus() }

        input.setImeActionLabel(context.getString(R.string.lookup), EditorInfo.IME_ACTION_SEARCH)
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                onLookupCLicklistener?.invoke(lookup)
                true
            } else {
                false
            }
        }
    }

    override fun setEnabled(enabled: Boolean) {
        input.isEnabled = enabled
        lookup.isEnabled = enabled
        super.setEnabled(enabled)
    }

    override fun requestFocus(direction: Int, previouslyFocusedRect: Rect?): Boolean {
        input.requestFocus()
        return super.requestFocus(direction, previouslyFocusedRect)
    }

    override fun clearFocus() {
        input.clearFocus()
        super.clearFocus()
    }

    fun setOnTranscriptionClickListener(listener: (View) -> Unit) {
        transcription.setOnClickListener(listener)
    }

    fun setOnLookupClickListener(listener: (View) -> Unit) {
        onLookupCLicklistener = listener
        lookup.setOnClickListener(listener)
    }

    fun setTranscription(text: String?) {
        if (text != null) {
            transcription.text = context.getString(R.string.transcription_format, text)
        } else {
            transcription.text = null
        }
    }

    fun setProgressBarVisibility(visible: Boolean) {
        if (visible && progressBar.visibility != View.VISIBLE) {
            progressBar.animate()
                    .alpha(0f)
                    .setDuration(PROGRESS_ANIM_DURATION)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            progressBar.visibility = View.INVISIBLE
                        }
                    })
                    .start()

        } else if (!visible && progressBar.visibility != View.INVISIBLE) {
            progressBar.alpha = 0f
            progressBar.visibility = View.VISIBLE
            progressBar.animate()
                    .alpha(1f)
                    .setDuration(PROGRESS_ANIM_DURATION)
                    .setListener(null)
                    .start()
        }
    }

    fun setText(text: String) {
        input.setText(text)
    }

    fun getText(): String {
        return input.text.toString().trim()
    }

    fun getTranscriptionText(): String {
        return transcription.text.toString()
    }
}
