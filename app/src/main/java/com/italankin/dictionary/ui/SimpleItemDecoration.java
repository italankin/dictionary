package com.italankin.dictionary.ui;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Simple item decorator for adding child padding.
 */
public class SimpleItemDecoration extends RecyclerView.ItemDecoration {

    /**
     * left, top, right, bottom
     */
    private int[] mOffsets = {0, 0, 0, 0};

    public SimpleItemDecoration() {
    }

    public SimpleItemDecoration(int left, int top, int right, int bottom) {
        mOffsets = new int[]{left, top, right, bottom};
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.set(mOffsets[0], mOffsets[1], mOffsets[2], mOffsets[3]);
    }

}
