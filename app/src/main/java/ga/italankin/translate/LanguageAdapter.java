package ga.italankin.translate;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;

public class LanguageAdapter extends ArrayAdapter<Language> implements View.OnClickListener {

    private LayoutInflater inflater;
    private ArrayList<Language> list;

    public LanguageAdapter(Context context, ArrayList<Language> list) {
        super(context, 0, list);
        this.inflater = LayoutInflater.from(context);
        this.list = list;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        Language item = getItem(position);

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.dialog_list_item, parent, false);
            holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.tvLangName);
            holder.fav = (CheckBox) convertView.findViewById(R.id.cbFavorite);
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

    private class ViewHolder {
        public TextView name;
        public CheckBox fav;
    }

}
