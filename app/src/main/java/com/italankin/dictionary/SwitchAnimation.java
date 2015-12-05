package com.italankin.dictionary;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class SwitchAnimation {

    private View mView;
    private float mTranslationX;
    private float mTranslationY;
    private long mDuration;

    private OnSwitchListener mListener;
    private AnimatorSet mAnimation;

    private boolean mState = false;

    public SwitchAnimation(View v, float translationX, float translationY, long duration, OnSwitchListener listener) {
        mView = v;
        mListener = listener;
        mTranslationX = translationX;
        mTranslationY = translationY;
        mDuration = duration / 2;
        mAnimation = getAnimatorSet1();
    }

    public void start() {
        if (mState) {
            throw new IllegalStateException();
        }
        mAnimation.start();
        mState = true;
    }

    public void cancel() {
        if (mAnimation != null && mAnimation.isRunning()) {
            mAnimation.cancel();
        }
    }

    private AnimatorSet getAnimatorSet1() {
        AnimatorSet set = new AnimatorSet();

        ObjectAnimator translateX = ObjectAnimator.ofFloat(mView, "translationX", 0, mTranslationX);
        ObjectAnimator translateY = ObjectAnimator.ofFloat(mView, "translationY", 0, mTranslationY);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(mView, "alpha", 1, 0);

        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (mListener != null) {
                    mListener.onSwitch();
                }
                mAnimation = getAnimatorSet2();
                mAnimation.start();
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

        ObjectAnimator translateX = ObjectAnimator.ofFloat(mView, "translationX", mTranslationX, 0);
        ObjectAnimator translateY = ObjectAnimator.ofFloat(mView, "translationY", mTranslationY, 0);

        ObjectAnimator alpha = ObjectAnimator.ofFloat(mView, "alpha", 0, 1);

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

    public interface OnSwitchListener {
        void onSwitch();
    }

}
