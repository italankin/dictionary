package com.italankin.dictionary.ui.main.util

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnEnd

class SwitchAnimation(
        private val view: View,
        private val translationX: Float,
        private val translationY: Float,
        private val duration: Long,
        private val listener: (() -> Unit)? = null
) {

    companion object {
        private const val INTERPOLATION_FACTOR = 3f
    }

    fun start() {
        AnimatorSet().apply {
            playSequentially(switchIn(), switchOut())
            start()
        }
    }

    private fun switchIn() = AnimatorSet().apply {
        doOnEnd {
            listener?.invoke()
        }
        interpolator = DecelerateInterpolator(INTERPOLATION_FACTOR)
        playTogether(
                ObjectAnimator.ofFloat(view, "translationX", 0f, translationX),
                ObjectAnimator.ofFloat(view, "translationY", 0f, translationY),
                ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
        )
        duration = this@SwitchAnimation.duration / 2
    }

    private fun switchOut() = AnimatorSet().apply {
        interpolator = DecelerateInterpolator(INTERPOLATION_FACTOR)
        playTogether(
                ObjectAnimator.ofFloat(view, "translationX", translationX, 0f),
                ObjectAnimator.ofFloat(view, "translationY", translationY, 0f),
                ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        )
        duration = this@SwitchAnimation.duration / 2
    }
}
