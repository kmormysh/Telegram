package com.katy.telegram.Activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.katy.telegram.Adapters.CountryAdapter;
import com.katy.telegram.R;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class CountriesListActivity extends BaseActivity {

    @InjectView(R.id.toolbar)
    Toolbar toolbar;
    @OnClick(R.id.toolbar) void cancelResult() {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }
    @InjectView(R.id.countries_list)
    ListView countriesList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_countries);
        ButterKnife.inject(this);

        toolbar.setTitle(getResources().getString(R.string.label_country));
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_back));

        CountryAdapter countryAdapter = new CountryAdapter(this);
        countriesList.setAdapter(countryAdapter);

        countriesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView name = (TextView) ((RelativeLayout) view).getChildAt(1);
                TextView code = (TextView) ((RelativeLayout) view).getChildAt(2);
                Intent intent = new Intent();
                intent.putExtra("name", name.getText().toString());
                intent.putExtra("code", code.getText().toString());
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });
    }
}
