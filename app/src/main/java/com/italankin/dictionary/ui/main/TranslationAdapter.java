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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.italankin.dictionary.R;
import com.italankin.dictionary.dto.TranslationEx;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter class for displaying translations in {@link MainActivity}
 */
class TranslationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_SPACE = 1;

    private final LayoutInflater mInflater;
    private final List<TranslationEx> mDataset = new ArrayList<>(0);

    private boolean mShowExtraSpace = false;
    private OnAdapterItemClickListener mListener;

    public TranslationAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
    }

    /**
     * Update adapter dataset with new items.
     *
     * @param data new items of list
     */
    public void setData(@NonNull List<TranslationEx> data) {
        int size = mDataset.size();
        mDataset.clear();
        notifyItemRangeRemoved(0, size);
        mDataset.addAll(data);
        notifyItemRangeInserted(0, mDataset.size());
    }

    public void showExtraSpace(boolean show) {
        if (mShowExtraSpace != show) {
            mShowExtraSpace = show;
            if (mShowExtraSpace) {
                notifyItemInserted(mDataset.size() + 1);
            } else {
                notifyItemRemoved(mDataset.size() + 1);
            }
        }
    }

    /**
     * Set on click listener for items.
     *
     * @param listener listener
     */
    public void setListener(@Nullable OnAdapterItemClickListener listener) {
        mListener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_ITEM) {
            View v = mInflater.inflate(R.layout.item_attribute, parent, false);
            return new ItemViewHolder(v);
        }
        return new SpaceViewHolder(mInflater.inflate(R.layout.item_space, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if (getItemViewType(position) == TYPE_SPACE) {
            return;
        }
        ItemViewHolder holder = (ItemViewHolder) viewHolder;

        TranslationEx item = mDataset.get(position);

        holder.text.setText(item.text);
        if (TextUtils.isEmpty(item.pos)) {
            holder.pos.setVisibility(View.GONE);
        } else {
            holder.pos.setText(String.format("(%s)", item.pos));
            holder.pos.setVisibility(View.VISIBLE);
        }
        holder.means.setText(item.means);
        holder.syns.setText(item.synonyms);
    }

    @Override
    public int getItemCount() {
        return mDataset.size() + (mShowExtraSpace ? 1 : 0);
    }

    @Override
    public int getItemViewType(int position) {
        if (mShowExtraSpace && position == mDataset.size()) {
            return TYPE_SPACE;
        }
        return TYPE_ITEM;
    }

    @Override
    public long getItemId(int position) {
        if (getItemViewType(position) == TYPE_SPACE) {
            return Long.MAX_VALUE;
        }
        return mDataset.get(position).hashCode();
    }

    /**
     * Listener interface for handling click events.
     */
    public interface OnAdapterItemClickListener {
        /**
         * Triggered when user clicks on list item.
         *
         * @param position item position
         */
        void onItemClick(int position);

        /**
         * Triggered when user clicks popup list item.
         *
         * @param position   item position
         * @param menuItemId menu item id
         */
        void onItemMenuClick(int position, int menuItemId);
    }

    public static class SpaceViewHolder extends RecyclerView.ViewHolder {
        public SpaceViewHolder(View itemView) {
            super(itemView);
        }
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView text;
        TextView means;
        TextView syns;
        TextView pos;
        ImageView menu;

        private PopupMenu popupMenu;

        public ItemViewHolder(View view) {
            super(view);
            view.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onItemClick(getAdapterPosition());
                }
            });
            text = view.findViewById(R.id.text);
            means = view.findViewById(R.id.means);
            syns = view.findViewById(R.id.synonyms);
            pos = view.findViewById(R.id.pos);
            menu = view.findViewById(R.id.overflow);
            popupMenu = new PopupMenu(view.getContext(), menu);
            popupMenu.inflate(R.menu.attribute);
            popupMenu.setOnMenuItemClickListener(item -> {
                if (mListener != null) {
                    mListener.onItemMenuClick(getAdapterPosition(), item.getItemId());
                }
                return true;
            });
            menu.setOnClickListener(v -> popupMenu.show());
        }
    }

}