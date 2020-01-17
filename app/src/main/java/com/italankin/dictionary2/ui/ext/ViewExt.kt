package com.italankin.dictionary2.ui.ext

import android.view.View
import android.widget.Spinner
import androidx.annotation.Px

fun Spinner.setSelection(position: Int, animate: Boolean = true, silent: Boolean = false) {
    if (silent) {
        val listener = onItemSelectedListener
        onItemSelectedListener = null
        setSelection(position, animate)
        onItemSelectedListener = listener
    } else {
        setSelection(position, animate)
    }
}

fun View.setPaddings(@Px left: Int = 0, @Px top: Int = 0, @Px right: Int = 0, @Px bottom: Int = 0) {
    setPadding(left, top, right, bottom)
}
