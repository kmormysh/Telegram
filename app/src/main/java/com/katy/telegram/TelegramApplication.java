package com.katy.telegram;

import android.app.Application;

import org.drinkless.td.libcore.telegram.Client;
import org.drinkless.td.libcore.telegram.TG;
import org.drinkless.td.libcore.telegram.TdApi;

import de.greenrobot.event.EventBus;

public class TelegramApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        TG.setDir(getFilesDir().toString());
        TG.setUpdatesHandler(new TestHandler());
    }

    private class TestHandler implements Client.ResultHandler {
        @Override
        public void onResult(TdApi.TLObject object) {
            final TdApi.TLObject tlObject = object;

            EventBus.getDefault().post(tlObject);
        }
    }
}
