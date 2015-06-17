package com.katy.telegram.Adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.katy.telegram.R;

import java.util.ArrayList;
import java.util.List;

public class ChoosePhotoAdapter extends RecyclerView.Adapter<ChoosePhotoAdapter.ViewHolder> {

    private final int NUMBER_OF_IMAGES_TO_LOAD = 5;
    private final int REQUIRED_IMAGE_SIZE = 120;

    private final Context context;
    private List<Uri> images;
    private List<Uri> selectedImages = new ArrayList<>();
    private LayoutInflater layoutInflater;
    private ImageAttachCallback imageAttachCallback;

    public class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView image;
        private ImageView shadow;
        private ImageView attach_check;

        public ViewHolder(View v, ViewGroup viewGroup) {
            super(v);
            image = (ImageView) v.findViewById(R.id.image);
            shadow = (ImageView) v.findViewById(R.id.image_shadow);
            attach_check = (ImageView) v.findViewById(R.id.attach_check);
        }

        public void setListener(ImageAttachCallback imageAttachCallback, int position) {
            Uri uri = images.get(position);
            if (selectedImages.contains(uri)) {
                shadow.setVisibility(View.GONE);
                attach_check.setVisibility(View.GONE);
                selectedImages.remove(images.get(position));
                imageAttachCallback.onAttachImage(selectedImages.size(), uri);
            } else {
                shadow.setVisibility(View.VISIBLE);
                attach_check.setVisibility(View.VISIBLE);
                selectedImages.add(images.get(position));
                imageAttachCallback.onAttachImage(selectedImages.size(), uri);
            }
        }

        public void checkImage(int position) {
            Uri uri = images.get(position);
            if (selectedImages.contains(uri)) {
                shadow.setVisibility(View.VISIBLE);
                attach_check.setVisibility(View.VISIBLE);
            } else {
                shadow.setVisibility(View.GONE);
                attach_check.setVisibility(View.GONE);
            }
        }
    }

    public ChoosePhotoAdapter(Context context, List<Uri> images, ImageAttachCallback imageAttachCallback) {
        this.context = context;
        this.images = images;
        this.imageAttachCallback = imageAttachCallback;
        this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public ChoosePhotoAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_choose_photo, viewGroup, false);
        return new ViewHolder(v, viewGroup);
    }

    @Override
    public void onBindViewHolder(final ChoosePhotoAdapter.ViewHolder viewHolder, final int i) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(images.get(i).getPath(), options);
        int width_tmp = options.outWidth, height_tmp = options.outHeight;

        options = new BitmapFactory.Options();
        int scale = 1;
        while (true) {
            if (width_tmp / 2 < REQUIRED_IMAGE_SIZE
                    || height_tmp / 2 < REQUIRED_IMAGE_SIZE)
                break;
            width_tmp /= 2;
            height_tmp /= 2;
            scale++;
        }
        options.inSampleSize = scale;
        Bitmap bitmap = BitmapFactory.decodeFile(images.get(i).getPath(), options);

        viewHolder.image.setImageBitmap(bitmap);
        viewHolder.checkImage(i);

        viewHolder.image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewHolder.setListener(imageAttachCallback, i);
            }
        });
    }

    public void clearSelectedImages(){
        selectedImages.clear();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    public interface ImageAttachCallback {
        void onAttachImage(int numberOfImagesSelected, Uri imageUri);
    }
}
