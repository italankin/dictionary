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

import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

public class HidingViewBehavior extends CoordinatorLayout.Behavior<View> {

    private static final Interpolator ANIM_INTERPOLATOR = new DecelerateInterpolator(2);
    private static final int ANIM_DURATION = 250;

    private final View hidingView;
    private final View scrollingView;
    private int maxOffset = 0;

    public HidingViewBehavior(View hidingView, View scrollingView) {
        this.hidingView = hidingView;
        this.scrollingView = scrollingView;
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        return dependency instanceof RecyclerView;
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, View child, int layoutDirection) {
        // set maximum offset to be equal view's height
        maxOffset = hidingView.getHeight();
        return false;
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, View child,
                                       View directTargetChild, View target, int nestedScrollAxes) {
        // we only interested in vertical scroll events
        return (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) > 0;
    }

    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, View child, View target,
                                  int dx, int dy, int[] consumed) {
        if (!isViewHidden()) {
            consumed[1] = offsetViews(dy);
        }
    }

    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout, View child, View target,
                               int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        if (dyUnconsumed < 0) {
            offsetViews(dyUnconsumed);
        }
    }

    @Override
    public boolean onNestedPreFling(CoordinatorLayout coordinatorLayout, View child, View target,
                                    float velocityX, float velocityY) {
        if (Math.abs(velocityY) > Math.abs(velocityX)) {
            if (velocityY > 0) {
                hideView();
            } else if (velocityY < 0) {
                showView();
            }
        }
        return false;
    }

    /**
     * Offset views, maximum of {@code abs(dy)} pixels.
     *
     * @param dy offset distance
     * @return actual offset
     */
    private int offsetViews(int dy) {
        float ty = hidingView.getTranslationY();
        float actual; // actual offset distance
        if (dy > 0) {
            // scroll towards end of the list
            actual = Math.min(dy, getMaximumOffset() + ty);
        } else {
            // scrool towards start of the list
            actual = Math.max(dy, ty);
        }
        if (actual != 0) {
            hidingView.setTranslationY(ty - actual);
            scrollingView.setTranslationY(scrollingView.getTranslationY() - actual);
        }
        return (int) actual;
    }

    private void showView() {
        if (!isViewHidden()) {
            return;
        }
        hidingView.animate()
                .translationY(0)
                .start();
        scrollingView.animate()
                .translationY(getMaximumOffset())
                .start();
    }

    private void hideView() {
        if (isViewHidden()) {
            return;
        }
        hidingView.animate()
                .translationY(-getMaximumOffset())
                .setInterpolator(ANIM_INTERPOLATOR)
                .setDuration(ANIM_DURATION)
                .start();
        scrollingView.animate()
                .translationY(0)
                .setInterpolator(ANIM_INTERPOLATOR)
                .setDuration(ANIM_DURATION)
                .start();
    }

    private boolean isViewHidden() {
        return Math.abs(hidingView.getTranslationY()) >= getMaximumOffset();
    }

    private int getMaximumOffset() {
        return maxOffset;
    }

}
