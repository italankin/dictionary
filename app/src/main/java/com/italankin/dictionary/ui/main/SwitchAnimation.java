/*
 * Copyright 2016 Igor Talankin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.italankin.dictionary.ui.main;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * Animation for creating a switch-like effect.
 */
class SwitchAnimation {

    private View mView;
    private float mTranslationX;
    private float mTranslationY;
    private long mDuration;

    private OnSwitchListener mListener;
    private AnimatorSet mAnimation;

    private boolean mStateFired = false;

    /**
     * Specific switch animation.
     *
     * @param v            view object to animate
     * @param translationX x amplitude
     * @param translationY y amplitude
     * @param duration     duration of the animation
     * @param listener     switch listener
     */
    public SwitchAnimation(View v, float translationX, float translationY, long duration,
                           OnSwitchListener listener) {
        mView = v;
        mListener = listener;
        mTranslationX = translationX;
        mTranslationY = translationY;
        mDuration = duration / 2;
        mAnimation = new AnimatorSet();
        mAnimation.playSequentially(getAnimatorSet1(), getAnimatorSet2());
    }

    /**
     * Starts the animation.
     */
    public void start() {
        if (mStateFired) {
            throw new IllegalStateException("Cannot start animation twice");
        }
        mAnimation.start();
        mStateFired = true;
    }

    /**
     * Cancels the animation.
     */
    public void cancel() {
        if (mAnimation != null && mAnimation.isRunning()) {
            mAnimation.cancel();
        }
    }

    private AnimatorSet getAnimatorSet1() {
        AnimatorSet set = new AnimatorSet();

        ObjectAnimator translateX = ObjectAnimator.ofFloat(mView, "translationX", 0f, mTranslationX);
        ObjectAnimator translateY = ObjectAnimator.ofFloat(mView, "translationY", 0f, mTranslationY);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(mView, "alpha", 1f, 0f);

        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (mListener != null) {
                    mListener.onSwitch();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });

        set.setInterpolator(new DecelerateInterpolator(3f));
        set.playTogether(translateX, translateY, alpha);
        set.setDuration(mDuration);

        return set;
    }

    private AnimatorSet getAnimatorSet2() {
        AnimatorSet set = new AnimatorSet();

        ObjectAnimator translateX = ObjectAnimator.ofFloat(mView, "translationX", mTranslationX, 0f);
        ObjectAnimator translateY = ObjectAnimator.ofFloat(mView, "translationY", mTranslationY, 0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(mView, "alpha", 0f, 1f);

        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mListener = null;
                mView = null;
                mAnimation = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });

        set.setInterpolator(new DecelerateInterpolator(3f));
        set.playTogether(translateX, translateY, alpha);
        set.setDuration(mDuration);

        return set;
    }

    /**
     * Animation events listener.
     */
    public interface OnSwitchListener {
        /**
         * Triggered when animation is mid-air (when view is actually invisible and ready to
         * "switch" state.
         */
        void onSwitch();
    }

}
