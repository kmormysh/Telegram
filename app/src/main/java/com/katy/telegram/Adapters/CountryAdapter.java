package com.katy.telegram.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.katy.telegram.Managers.CountriesManager;
import com.katy.telegram.Models.Country;
import com.katy.telegram.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CountryAdapter extends BaseAdapter {

    private List<Country> counties;
    private LayoutInflater layoutInflater;
    private Context context;

    public class ViewHolder {
        TextView textAlphabetLetter;
        TextView textCountryName;
        TextView textCountryCode;
    }

    public CountryAdapter(Context context) {
        this.context = context;
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        counties = CountriesManager.getInstance().getList();
    }

    @Override
    public int getCount() {
        return counties.size();
    }

    @Override
    public Object getItem(int position) {
        return counties.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.item_country, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.textAlphabetLetter = (TextView) convertView.findViewById(R.id.letter);
            viewHolder.textCountryName = (TextView) convertView.findViewById(R.id.country_name);
            viewHolder.textCountryCode = (TextView) convertView.findViewById(R.id.country_code);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        Country country = counties.get(position);
        boolean toShowFirstLetter;
        if (position == 0) {
            toShowFirstLetter = true;//always show it for the first element
        } else {
            char letterOfPrev = counties.get(position - 1).getName().charAt(0);
            char letterOfThis = country.getName().charAt(0);
            toShowFirstLetter = letterOfPrev != letterOfThis;
        }

        viewHolder.textAlphabetLetter.setText(toShowFirstLetter ? country.getName().substring(0, 1) : " ");
        viewHolder.textCountryName.setText(country.getName());
        viewHolder.textCountryCode.setText(country.getCode());

        return convertView;
    }
}
