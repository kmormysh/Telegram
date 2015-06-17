package com.katy.telegram.Activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.katy.telegram.Managers.AccountManager;
import com.katy.telegram.Managers.CountriesManager;
import com.katy.telegram.Managers.TgClient;
import com.katy.telegram.R;

import org.drinkless.td.libcore.telegram.TdApi;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnTextChanged;

public class RegistrationActivity extends BaseActivity implements TgClient.TLResponseCallback {

    @InjectView(R.id.country_code)
    TextView country_code;
    @InjectView(R.id.phone_number)
    EditText phone_number;
    @InjectView(R.id.toolbar)
    Toolbar toolbar;
    @InjectView(R.id.country_name)
    TextView country_name;

    private String phone;

    @OnClick(R.id.country_name)
    void chooseCountryName() {
        Intent intent = new Intent(RegistrationActivity.this, CountriesListActivity.class);
        startActivityForResult(intent, RESULT_FIRST_USER);
    }

    @OnClick(R.id.country_code)
    void chooseCountryCode() {
        chooseCountryName(); //do the same
    }

    @OnTextChanged(value = R.id.country_code)
    void onCountryCodeChanged(CharSequence text) {
        country_name.setText(CountriesManager.getInstance().getCountryByCode(text.toString()));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CountriesManager.getInstance().preloadCountries();
        setContentView(R.layout.activity_registration);
        ButterKnife.inject(this);

        toolbar.setTitle(getResources().getString(R.string.label_phone_number));
        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            country_name.setText(data.getStringExtra("name"));
            country_code.setText(data.getStringExtra("code"));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.login_phone, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_check) {
            doRegister();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void doRegister() {
        if (isBusy())
            return;

        phone = phone_number.getText().toString();
        if (!phone.equals("")) {
            showProgress(true);
            TdApi.AuthSetPhoneNumber smsSender = new TdApi.AuthSetPhoneNumber(country_code.getText().toString() + phone);
            TgClient.send(smsSender, this);
        } else {
            reportWrongNumberError();
        }
    }

    private void reportWrongNumberError() {
        Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResult(TdApi.TLObject object) {
        if (object.getConstructor() == TdApi.AuthStateWaitSetCode.CONSTRUCTOR) {
            AccountManager.getInstance().setCurrentPhoneNumber(country_code.getText().toString() + phone);
            Intent intent = new Intent(getApplicationContext(), ActivationActivity.class);
            intent.putExtra("phone", country_code.getText().toString() + phone);
            startActivity(intent);
        } else {
            reportWrongNumberError();
        }
        showProgress(false);
    }
}
