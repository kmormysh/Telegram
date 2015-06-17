package com.katy.telegram.Activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.katy.telegram.Managers.AccountManager;
import com.katy.telegram.Managers.TgClient;
import com.katy.telegram.R;

import org.drinkless.td.libcore.telegram.TdApi;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnTextChanged;

public class ActivationActivity extends BaseActivity implements TgClient.TLResponseCallback {

    private static final int ACTIVATION_CODE_LENGTH = 5;

    @InjectView(R.id.toolbar)
    Toolbar toolbar;
    @InjectView(R.id.activation_information)
    TextView activationInformation;
    @InjectView(R.id.code)
    EditText code;
    @InjectView(R.id.edittext_line)
    View edittextLine;
    @InjectView(R.id.wrong_code)
    TextView wrongCode;

    @OnTextChanged(value = R.id.code)
    void onActivationCodeChanged(CharSequence text) {
        edittextLine.setBackgroundColor(getResources().getColor(R.color.background_blue));
        wrongCode.setVisibility(View.GONE);
        if (text.length() == ACTIVATION_CODE_LENGTH) {
            doActivate();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activation);
        ButterKnife.inject(this);

        toolbar.setTitle(getResources().getString(R.string.label_activation_code));
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_back));
        setSupportActionBar(toolbar);

        activationInformation.setText(activationInformation.getText() + " " + AccountManager.getInstance().getCurrentPhoneNumber());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activation_code, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_check) {
            doActivate();
            return true;
        } else {
            startActivity(new Intent(getApplicationContext(), RegistrationActivity.class));
            return super.onOptionsItemSelected(item);
        }
    }

    private void doActivate() {
        if (isBusy())
            return;

        showProgress(true);
        TdApi.AuthSetCode authSetCode = new TdApi.AuthSetCode(code.getText().toString());
        TgClient.send(authSetCode, this);
    }

    @Override
    public void onResult(TdApi.TLObject object) {
        if (object.getConstructor() == TdApi.AuthStateOk.CONSTRUCTOR) {
            Intent intent = new Intent(getApplicationContext(), ConversationsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else {
            wrongCode.setVisibility(View.VISIBLE);
            edittextLine.setBackgroundColor(getResources().getColor(R.color.color_error_message));
        }
        showProgress(false);
    }
}
