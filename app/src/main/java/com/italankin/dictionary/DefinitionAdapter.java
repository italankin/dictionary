package com.italankin.dictionary;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import com.italankin.dictionary.dto.Translation;

import java.util.List;

public class DefinitionAdapter extends RecyclerView.Adapter<DefinitionAdapter.ViewHolder> {

    private List<Translation> mDataset;
    private boolean[] mStates;

    public DefinitionAdapter(List<Translation> list) {
        mDataset = list;
        mStates = new boolean[list.size()];
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attribute, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Translation item = mDataset.get(position);
        holder.text.setText(item.text);
        if (item.mean != null) {
            String means = "";
            for (Translation.Mean m : item.mean) {
                if (means.length() > 0) {
                    means += ", " + m.text;
                } else {
                    means = m.text;
                }
            }
            holder.sub.setText(means);
        }
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView text;
        public TextView sub;

        public ViewHolder(View v) {
            super(v);
            text = (TextView) v.findViewById(R.id.text1);
            sub = (TextView) v.findViewById(R.id.text2);
        }
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

}