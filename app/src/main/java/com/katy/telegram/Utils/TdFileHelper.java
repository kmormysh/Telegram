package com.katy.telegram.Utils;

import com.katy.telegram.Managers.TgClient;

import org.drinkless.td.libcore.telegram.TdApi;

import java.io.File;
import java.util.HashMap;

import de.greenrobot.event.EventBus;

public class TdFileHelper {

    private HashMap<Integer, String> filePathMap = new HashMap<>();

    private static TdFileHelper instance;

    public static TdFileHelper getInstance() {
        if (instance == null) {
            synchronized (TdFileHelper.class) {
                if (instance == null) {
                    instance = new TdFileHelper();
                    EventBus.getDefault().register(instance);
                }
            }
        }
        return instance;
    }

    private TdFileHelper() {
    }

    /*
    * gets file from cache or downloads it if downloadIfNotExist is set to true
    * */
    public String getFile(TdApi.File file, boolean downloadIfNotExist) {
        return getFile(file, downloadIfNotExist, true);
    }

    /*
    * gets file from cache or downloads it if downloadIfNotExist is set to true
    * */
    public String getFile(TdApi.File file, boolean downloadIfNotExist, boolean postUpdateEvenIfInCache) {
        if (file instanceof TdApi.FileLocal){
            TdApi.FileLocal fileLocal = (TdApi.FileLocal) file;
            if (postUpdateEvenIfInCache)
                EventBus.getDefault().post(new TdApi.UpdateFile(fileLocal.id, fileLocal.size, fileLocal.path));
            return fileLocal.path;
        }

        TdApi.FileEmpty fileEmpty = (TdApi.FileEmpty) file;

        String filePath;
        synchronized (instance) {
            filePath = filePathMap.get(fileEmpty.id);
        }

        if (filePath != null) {
            if (postUpdateEvenIfInCache)
                EventBus.getDefault().post(new TdApi.UpdateFile(fileEmpty.id, fileEmpty.size, filePath));
            return filePath;
        }
        if (downloadIfNotExist) {
            TdApi.DownloadFile downloadFile = new TdApi.DownloadFile(fileEmpty.id);
            TgClient.send(downloadFile);
        }
        return null;
    }

    public void onEvent(TdApi.UpdateFile file) {
        synchronized (instance) {
            filePathMap.put(file.fileId, file.path);
        }
    }

    public static boolean sameId(TdApi.File file, long fileId) {
        return getFileId(file) == fileId;
    }

    public static long getFileId(TdApi.File file) {
        if (file instanceof TdApi.FileEmpty) {
            return ((TdApi.FileEmpty) file).id;
        }
        return ((TdApi.FileLocal) file).id;
    }
}
