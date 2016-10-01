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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.italankin.dictionary.R;

/**
 * This class is used to add bottom padding to the {@link RecyclerView} to workaround problem with
 * {@link RecyclerView#computeVerticalScrollOffset()} returning 0 when RecyclerView has bottom padding.
 */
class LastItemExtraPadding extends RecyclerView.ItemDecoration {

    private final int offset;

    public LastItemExtraPadding(Context context) {
        Resources res = context.getResources();
        int margin = res.getDimensionPixelSize(R.dimen.fab_share_margin);
        int size = res.getDimensionPixelSize(R.dimen.fab_share_size);
        offset = margin * 2 + size;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int pos = parent.getChildAdapterPosition(view);
        int count = parent.getAdapter().getItemCount();
        if (pos == count - 1) {
            outRect.bottom = offset;
        }
    }

}
