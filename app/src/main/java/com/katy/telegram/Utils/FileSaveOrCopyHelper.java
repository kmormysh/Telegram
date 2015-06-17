package com.katy.telegram.Utils;

import android.os.Environment;
import android.widget.Toast;

import com.katy.telegram.Activities.BaseActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileSaveOrCopyHelper {

    public static String copyFile(File src) throws IOException {
//        if (new File(filePath.replace(src.getName(), "")).exists()) {
//            filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Kelegram/Video/" + src.getName();
//            return;
//        }

        File dst = new File(Environment.getExternalStorageDirectory() + "/Kelegram/Video/");
        if (!dst.exists()) {
            boolean mkdir = dst.mkdirs();
            if (!mkdir) {
                Toast.makeText(BaseActivity.getCurrentActivity(), "Can't create folder :(", Toast.LENGTH_SHORT).show();
            }
        }

        InputStream is = new FileInputStream(src);
        String path = dst.getPath() + "/" + src.getName();
        OutputStream os = new FileOutputStream(new File(path));
        byte[] buff = new byte[1024];
        int len;
        while ((len = is.read(buff)) > 0) {
            os.write(buff, 0, len);
        }
        is.close();
        os.close();

        return path;
    }

}
