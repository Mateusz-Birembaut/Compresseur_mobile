package com.example.compresseur_mobile;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class LanguageAdapter extends ArrayAdapter<Language> {

    private final List<Language> originalList;
    private final LayoutInflater inflater;

    public LanguageAdapter(Context ctx, List<Language> langs) {
        super(ctx, 0, new ArrayList<>(langs));
        inflater = LayoutInflater.from(ctx);
        originalList = new ArrayList<>(langs);
    }


    @NonNull
    @Override
    public View getView(int pos, View convertView, @NonNull ViewGroup parent) {
        return createView(pos, convertView, parent);
    }
    @Override
    public View getDropDownView(int pos, View convertView, @NonNull ViewGroup parent) {
        return createView(pos, convertView, parent);
    }
    private View createView(int pos, View cv, ViewGroup parent) {
        if (cv == null) {
            cv = inflater.inflate(R.layout.item_language, parent, false);
        }
        Language lang = getItem(pos);
        ImageView iv = cv.findViewById(R.id.flag);
        TextView tv = cv.findViewById(R.id.name);
        assert lang != null;
        iv.setImageResource(lang.flag);
        tv.setText(lang.displayName);
        return cv;
    }

    @NonNull @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                results.values = originalList;
                results.count = originalList.size();
                return results;
            }
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                clear();
                addAll((List<Language>) results.values);
                notifyDataSetChanged();
            }
            @Override
            public CharSequence convertResultToString(Object resultValue) {
                return ((Language) resultValue).displayName;
            }
        };
    }

}



