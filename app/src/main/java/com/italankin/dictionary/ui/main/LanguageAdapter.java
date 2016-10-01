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

    public LanguageAdapter(Context context, List<Language> dataset) {
        this.inflater = LayoutInflater.from(context);
        if (dataset == null) {
            this.dataset = Collections.emptyList();
        } else {
            this.dataset = dataset;
        }
    }

    @Override
    @SuppressLint("ViewHolder") // As there's only one selected item, VH is not necessary
    public View getView(int position, View convertView, ViewGroup parent) {
        Language item = getItem(position);
        convertView = inflater.inflate(R.layout.item_spinner_language, parent, false);
        TextView text = (TextView) convertView.findViewById(R.id.text);
        text.setText(item.getName());
        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_spinner_language_dropdown, parent, false);
            holder = new ViewHolder();
            holder.text = (TextView) convertView.findViewById(R.id.text);
            holder.checkBox = (CheckBox) convertView.findViewById(R.id.checkbox);
            holder.checkBox.setOnCheckedChangeListener(this);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Language item = getItem(position);
        holder.text.setText(item.getName());
        holder.checkBox.setTag(item);
        holder.checkBox.setChecked(item.isFavorite());

        return convertView;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Language lang = (Language) buttonView.getTag();
        lang.setFavorite(isChecked);
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

    private static class ViewHolder {
        public TextView text;
        public CheckBox checkBox;
    }

}
