package com.katy.telegram.Utils;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryService {

    private Activity activity;
    private PhotoChosenListener listener;
    static final int REQUEST_CAMERA = 200;
    static final int SELECT_FILE = 201;

    public PhotoGalleryService(Activity activity) {
        this.activity = activity;
    }

    public void choose(boolean fromCamera, @NonNull PhotoChosenListener listener) {
        this.listener = listener;

        if (fromCamera) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            activity.startActivityForResult(intent, REQUEST_CAMERA);
        } else {
            Intent intent = new Intent(
                    Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            if (Build.VERSION.SDK_INT >= 18) {
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            }
            activity.startActivityForResult(Intent.createChooser(intent, "Select File"), SELECT_FILE);
        }
    }

    public List<Uri> handleActivityResult(int requestCode, int resultCode, Intent data) {
        List<Uri> selectedImagesPaths = new ArrayList<>();

        BitmapProcessor bitmapProcessor = listener.onChosen();
        if (bitmapProcessor == null) {
            throw new IllegalArgumentException("BitmapProcessor is null");
        }

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CAMERA) {
                Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                Uri uri = Uri.fromFile(ImageHelper.saveBitmap(bitmap));
                selectedImagesPaths.add(uri);

            } else if (requestCode == SELECT_FILE) {
                if (Build.VERSION.SDK_INT >= 18 && null == data.getData()) {
                    ClipData clipdata = data.getClipData();
                    for (int i = 0; i < clipdata.getItemCount(); i++) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), clipdata.getItemAt(i).getUri());
                            Uri uri = Uri.fromFile(ImageHelper.saveBitmap(bitmap));
                            selectedImagesPaths.add(uri);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    InputStream inputStream = null;
                    try {
                        inputStream = activity.getContentResolver().openInputStream(data.getData());
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        Uri uri = Uri.fromFile(ImageHelper.saveBitmap(bitmap));
                        selectedImagesPaths.add(uri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            listener.onCancel();
        }
        return selectedImagesPaths;
    }

    public interface PhotoChosenListener {
        void onCancel();

        BitmapProcessor onChosen();
    }

    public interface BitmapProcessor {
        void Process(Bitmap bmp, boolean isLast);
    }
}
