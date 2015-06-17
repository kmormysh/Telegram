package com.katy.telegram.Managers;

import com.katy.telegram.Activities.BaseActivity;
import com.katy.telegram.Models.Country;
import com.katy.telegram.R;

import java.util.ArrayList;
import java.util.Locale;

public class CountriesManager {
    private boolean preloaded;
    private ArrayList<Country> countries;
    private static CountriesManager instance;

    private CountriesManager() {
    }

    public static CountriesManager getInstance() {
        if (instance == null) {
            synchronized (CountriesManager.class) {
                if (instance == null) {
                    instance = new CountriesManager();
                }
                return instance;
            }
        }
        return instance;
    }

    public void preloadCountries(){
        if (preloaded)
            return;
        preloaded = true;

        countries = new ArrayList<>();
        String[] resource = BaseActivity.getCurrentActivity().getResources().getStringArray(R.array.country_codes);
        for (String row : resource) {
            String[] details = row.split(",");
            countries.add(new Country(new Locale("", details[1]).getDisplayCountry(), details[0]));
        }
    }

    public ArrayList<Country> getList(){
        preloadCountries();
        return countries;
    }

    public String getCountryByCode(String code){
        preloadCountries();
        for (Country country : countries) {
            if (country.getCode().equals(code)){
                return country.getName();
            }
        }
        return " ";
    }

    public void guessCurrentCountry(){//add callback
        throw new UnsupportedOperationException();
    }
}
