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
package com.italankin.dictionary.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.italankin.dictionary.R;
import com.italankin.dictionary.dto.Language;

import java.util.List;

/**
 * Adapter for language list.
 */
public class LanguageAdapter extends ArrayAdapter<Language> implements View.OnClickListener {

    private LayoutInflater inflater;
    private List<Language> list;

    public LanguageAdapter(Context context, List<Language> list) {
        super(context, 0, list);
        this.inflater = LayoutInflater.from(context);
        this.list = list;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        Language item = getItem(position);

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_language, parent, false);
            holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.text_lang);
            holder.fav = (CheckBox) convertView.findViewById(R.id.cb_fav);
            holder.fav.setOnClickListener(this);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.name.setText(item.getName());
        holder.fav.setChecked(item.isFavorite());
        holder.fav.setTag(position);

        return convertView;
    }

    @Override
    public void onClick(View v) {
        Language item = list.get((int) v.getTag());
        item.setFavorite(!item.isFavorite());
    }

    private static class ViewHolder {
        public TextView name;
        public CheckBox fav;
    }

}
