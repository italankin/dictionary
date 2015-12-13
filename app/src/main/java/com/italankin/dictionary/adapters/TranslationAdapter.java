package com.italankin.dictionary.adapters;

import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.italankin.dictionary.R;
import com.italankin.dictionary.dto.TranslationEx;

import java.util.List;

public class TranslationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private List<TranslationEx> mDataset;
    private int mHeaderSize;
    private boolean[] mStates;
    private OnAdapterItemClickListener mListener;

    public TranslationAdapter(int headerSize, OnAdapterItemClickListener listener) {
        mHeaderSize = headerSize;
        mListener = listener;
    }

    public void setItems(List<TranslationEx> list) {
        mDataset = list;
        mStates = new boolean[list == null ? 0 : list.size()];
        notifyDataSetChanged();
    }

    public void remove(int position) {
        mDataset.remove(position);
        notifyItemRemoved(position + 1);
        notifyItemRangeChanged(position + 1, mDataset.size());
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.dummy_header, parent, false);
            return new HeaderViewHolder(v, mHeaderSize);
        } else if (viewType == TYPE_ITEM) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attribute, parent, false);
            return new ViewHolder(v);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if (viewHolder instanceof ViewHolder) {
            final ViewHolder holder = (ViewHolder) viewHolder;
            position = position - 1; // 1st item is header

            TranslationEx item = mDataset.get(position);

            holder.position = position;
            holder.text.setText(item.text);
            if (TextUtils.isEmpty(item.pos)) {
                holder.pos.setVisibility(View.GONE);
            } else {
                holder.pos.setText(String.format("(%s)", item.pos));
                holder.pos.setVisibility(View.VISIBLE);
            }
            holder.means.setText(item.means);
            holder.syns.setText(item.synonyms);

            if (!mStates[position]) {
                holder.itemView.setAlpha(0);
                holder.itemView.setTranslationY(100);
                holder.itemView.animate()
                        .setStartDelay(100)
                        .alpha(1)
                        .translationY(0)
                        .setInterpolator(new DecelerateInterpolator(3f))
                        .setDuration(600)
                        .start();
                mStates[position] = true;
            }
        }
    }

    @Override
    public int getItemCount() {
        return ((mDataset == null) ? 0 : mDataset.size()) + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return TYPE_HEADER;
        }

        return TYPE_ITEM;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView text;
        public TextView means;
        public TextView syns;
        public TextView pos;
        public ImageView menu;
        public int position;

        public ViewHolder(View v) {
            super(v);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onItemClick(position);
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
                                mListener.onItemMenuClick(position, item.getItemId());
                            }
                            return true;
                        }
                    });
                    pm.show();
                }
            });
        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {

        public HeaderViewHolder(View v, int height) {
            super(v);
            v.setMinimumHeight(height);
        }

    }

    public interface OnAdapterItemClickListener {
        void onItemClick(int position);

        void onItemMenuClick(int position, int menuItemId);
    }

}