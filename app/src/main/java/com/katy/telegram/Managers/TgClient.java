package com.katy.telegram.Managers;

import com.katy.telegram.Activities.BaseActivity;

import org.drinkless.td.libcore.telegram.Client;
import org.drinkless.td.libcore.telegram.TG;
import org.drinkless.td.libcore.telegram.TdApi;

public class TgClient {
    public static void send(TdApi.TLFunction function, final TLResponseCallback callback, final boolean toUiThread){
        TG.getClientInstance().send(function, new Client.ResultHandler() {
            @Override
            public void onResult(final TdApi.TLObject object)  {
                if (callback == null){
                    return;
                }

                if (toUiThread && BaseActivity.getCurrentActivity() != null){
                    BaseActivity.getCurrentActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callback.onResult(object);
                        }
                    });
                } else {
                    callback.onResult(object);
                }
            }
        });
    }

    public static void send(TdApi.TLFunction function, final TLResponseCallback callback){
        send(function, callback, true);
    }

    public static void send(TdApi.TLFunction function){
        send(function, null, true);
    }

    public interface TLResponseCallback {
        void onResult(TdApi.TLObject object);
    }
}
