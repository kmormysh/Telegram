package com.katy.telegram.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

public class ImageHelper {
    public static File saveBitmap(Bitmap bmp) {
        String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
        OutputStream outStream = null;
        Random random = new Random(Byte.MAX_VALUE);
        String imageName = String.format("%d.png", (int) (random.nextInt() + Byte.MAX_VALUE / 2.0));
        File file = new File(extStorageDirectory, imageName);
        if (file.exists()) {
            file.delete();
            file = new File(extStorageDirectory, imageName);
        }
        try {
            outStream = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.flush();
            outStream.close();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return file;
    }
//
//    public static void galleryAddPic(String imagePath, Context context) {
//        File externalStorageKelegram = new File(Environment.getExternalStoragePublicDirectory(
//                Environment.DIRECTORY_PICTURES) + "/Kelegram/");
//        if (!externalStorageKelegram.exists()) {
//            externalStorageKelegram.mkdirs();
//        }
//
//        try {
//            InputStream in = new FileInputStream(imagePath);
//            OutputStream out = new FileOutputStream(externalStorageKelegram.getPath() + imagePath);
//
//            // Copy the bits from instream to outstream
//            byte[] buf = new byte[1024];
//            int len;
//
//            while ((len = in.read(buf)) > 0) {
//                out.write(buf, 0, len);
//            }
//
//            in.close();
//            out.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}

