package com.italankin.dictionary.ui.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.italankin.dictionary.R;
import com.italankin.dictionary.dto.Language;

import java.util.Collections;
import java.util.List;

/**
 * Adapter for language list.
 */
class LanguageAdapter extends BaseAdapter implements CompoundButton.OnCheckedChangeListener {

    private final LayoutInflater inflater;
    private final List<Language> dataset;
    private CheckedChangeListener listener;

    public LanguageAdapter(Context context, List<Language> dataset) {
        this.inflater = LayoutInflater.from(context);
        if (dataset == null) {
            this.dataset = Collections.emptyList();
        } else {
            this.dataset = dataset;
        }
    }

    public void setListener(CheckedChangeListener listener) {
        this.listener = listener;
    }

    @Override
    @SuppressLint("ViewHolder") // As there's only one selected item, VH is not necessary
    public View getView(int position, View convertView, ViewGroup parent) {
        Language item = getItem(position);
        convertView = inflater.inflate(R.layout.item_spinner_language, parent, false);
        TextView text = convertView.findViewById(R.id.text);
        text.setText(item.getName());
        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_spinner_language_dropdown, parent, false);
            holder = new ViewHolder();
            holder.text = convertView.findViewById(R.id.text);
            holder.checkBox = convertView.findViewById(R.id.checkbox);
            holder.checkBox.setOnCheckedChangeListener(this);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Language item = getItem(position);
        holder.text.setText(item.getName());
        holder.checkBox.setTag(item);
//        holder.checkBox.setChecked(item.isFavorite()); // TODO

        return convertView;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Language lang = (Language) buttonView.getTag();
        if (listener != null) {
            listener.onCheckedChange(lang, isChecked);
        }
    }

    @Override
    public int getCount() {
        return dataset.size();
    }

    @Override
    public Language getItem(int position) {
        return dataset.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).hashCode();
    }

    public interface CheckedChangeListener {
        void onCheckedChange(Language language, boolean isChecked);
    }

    private static class ViewHolder {
        public TextView text;
        public CheckBox checkBox;
    }

}
