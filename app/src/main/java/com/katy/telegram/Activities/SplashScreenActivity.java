package com.katy.telegram.Activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.katy.telegram.Activities.ActivationActivity;
import com.katy.telegram.Activities.RegistrationActivity;
import com.katy.telegram.Activities.ConversationsActivity;
import com.katy.telegram.Managers.TgClient;

import org.drinkless.td.libcore.telegram.TdApi;

public class SplashScreenActivity extends Activity implements TgClient.TLResponseCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TdApi.AuthGetState authStateRequest = new TdApi.AuthGetState();
        TgClient.send(authStateRequest, this);
    }

    @Override
    public void onResult(TdApi.TLObject object) {
        if (object instanceof TdApi.AuthStateOk) {
            startActivity(new Intent(getApplicationContext(), ConversationsActivity.class));
        }
        else if (object instanceof TdApi.AuthStateWaitSetCode) {
            startActivity(new Intent(getApplicationContext(), ActivationActivity.class));
        }
        else if (object instanceof TdApi.AuthStateWaitSetPhoneNumber) {
            startActivity(new Intent(getApplicationContext(), RegistrationActivity.class));
        }
        else if (object instanceof TdApi.Error) {
            TgClient.send(new TdApi.AuthReset(), this);
        }
        else {
            startActivity(new Intent(getApplicationContext(), RegistrationActivity.class));
        }
    }
}
