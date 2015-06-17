package com.katy.telegram.Managers;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.katy.telegram.Activities.BaseActivity;
import com.katy.telegram.Activities.SplashScreenActivity;

import org.drinkless.td.libcore.telegram.TdApi;

import java.util.concurrent.CountDownLatch;

import de.greenrobot.event.EventBus;

public class AccountManager {
    private static AccountManager instance;
    private TdApi.User currentUser;
    private boolean loadInitiated;
    private String phoneNumber;

    private AccountManager() {
    }

    public static AccountManager getInstance() {
        if (instance == null) {
            synchronized (AccountManager.class) {
                if (instance == null) {
                    instance = new AccountManager();
                    EventBus.getDefault().register(instance);
                }
                return instance;
            }
        }
        return instance;
    }

    public TdApi.User getCurrentUser() {
        if (currentUser == null) {
            synchronized (AccountManager.class) {
                loadCurrentUser();
            }
        }
        return currentUser;
    }

    public String getCurrentPhoneNumber(){
        if (phoneNumber == null){
            phoneNumber = PreferenceManager.getDefaultSharedPreferences(BaseActivity.getCurrentActivity()).getString("phone", null);
        }
        return phoneNumber;
    }

    public void setCurrentPhoneNumber(String phoneNumber){
        this.phoneNumber = phoneNumber;
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(BaseActivity.getCurrentActivity()).edit();
        editor.putString("phone", phoneNumber);
        editor.apply();
    }

    public void loadCurrentUser() {
        if (loadInitiated) {
            return;
        }
        loadInitiated = true;

        final CountDownLatch latch = new CountDownLatch(1);
        TgClient.send(new TdApi.GetMe(), new TgClient.TLResponseCallback() {
            @Override
            public void onResult(final TdApi.TLObject object) {
                if (object instanceof TdApi.User) {
                    currentUser = (TdApi.User) object;
                    if (currentUser.photoSmall instanceof TdApi.FileEmpty) {
                        TdApi.DownloadFile downloadFile = new TdApi.DownloadFile(((TdApi.FileEmpty) currentUser.photoSmall).id);
                        TgClient.send(downloadFile, this);
                    }
                    if (currentUser.photoBig instanceof TdApi.FileEmpty) {
                        TdApi.DownloadFile downloadFile = new TdApi.DownloadFile(((TdApi.FileEmpty) currentUser.photoBig).id);
                        TgClient.send(downloadFile, this);
                    }
                    latch.countDown();
                }
            }
        }, false);
        try {
            latch.await();
        } catch (InterruptedException e) {
            // pass
        }
    }

    public void onEventMainThread(TdApi.UpdateFile file) {
        if (currentUser.photoSmall instanceof TdApi.FileEmpty) {
            if (file.fileId == ((TdApi.FileEmpty) currentUser.photoSmall).id) {
                currentUser.photoSmall = new TdApi.FileLocal(file.fileId, file.size, file.path);
                EventBus.getDefault().post(this);
            }
        } else if (currentUser.photoBig instanceof TdApi.FileEmpty) {
            if (file.fileId == ((TdApi.FileEmpty) currentUser.photoBig).id) {
                currentUser.photoBig = new TdApi.FileLocal(file.fileId, file.size, file.path);
                EventBus.getDefault().post(this);
            }
        }
    }

    public void logout() {
        TdApi.AuthReset authReset = new TdApi.AuthReset(false);
        TgClient.send(authReset, new TgClient.TLResponseCallback() {
            @Override
            public void onResult(TdApi.TLObject object) {
                setCurrentPhoneNumber(null);
                BaseActivity currentActivity = BaseActivity.getCurrentActivity();
                Intent intent = new Intent(currentActivity.getApplicationContext(), SplashScreenActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                currentActivity.startActivity(intent);
            }
        });
    }
}
