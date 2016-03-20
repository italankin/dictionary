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
package com.italankin.dictionary.adapters;

import android.content.Context;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.italankin.dictionary.R;
import com.italankin.dictionary.dto.TranslationEx;

import java.util.List;

/**
 * Adapter class for displaying translations in {@link com.italankin.dictionary.ui.MainActivity}
 */
public class TranslationAdapter extends RecyclerView.Adapter<TranslationAdapter.ViewHolder> {

    private final LayoutInflater mInflater;

    private List<TranslationEx> mDataset;

    private OnAdapterItemClickListener mListener;

    public TranslationAdapter(Context context, List<TranslationEx> data) {
        mInflater = LayoutInflater.from(context);
        mDataset = data;
    }

    /**
     * Set on click listener for items.
     *
     * @param listener listener
     */
    public void setListener(OnAdapterItemClickListener listener) {
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = mInflater.inflate(R.layout.item_attribute, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
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
        return (mDataset == null) ? 0 : mDataset.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView text;
        public TextView means;
        public TextView syns;
        public TextView pos;
        public ImageView menu;

        public ViewHolder(View v) {
            super(v);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onItemClick(getAdapterPosition());
                    }
                }
            });
            text = (TextView) v.findViewById(R.id.text);
            means = (TextView) v.findViewById(R.id.means);
            syns = (TextView) v.findViewById(R.id.synonyms);
            pos = (TextView) v.findViewById(R.id.pos);
            menu = (ImageView) v.findViewById(R.id.overflow);
            menu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu pm = new PopupMenu(v.getContext(), v);
                    pm.inflate(R.menu.menu_translation);
                    pm.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            if (mListener != null) {
                                mListener.onItemMenuClick(getAdapterPosition(), item.getItemId());
                            }
                            return true;
                        }
                    });
                    pm.show();
                }
            });
        }
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

}