/*
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
