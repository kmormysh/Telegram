package com.katy.telegram.Fragments;

import android.app.Fragment;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.katy.telegram.Adapters.ChoosePhotoAdapter;
import com.katy.telegram.Managers.TgClient;
import com.katy.telegram.R;
import com.katy.telegram.Utils.PhotoGalleryService;

import org.drinkless.td.libcore.telegram.TdApi;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AttachPhotoFragment extends Fragment implements ChoosePhotoAdapter.ImageAttachCallback, PhotoGalleryService.PhotoChosenListener, PhotoGalleryService.BitmapProcessor, TgClient.TLResponseCallback {

    private RecyclerView listView;
    private List<Uri> images;
    private ChoosePhotoAdapter choosePhotoAdapter;
    private Cursor imagecursor;

    private final int NUMBER_OF_IMAGES_TO_LOAD = 5;
    private final int REQUIRED_IMAGE_SIZE = 120;

    private final String[] img = {MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID};
    private final Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    private LinearLayoutManager linearLayoutManager;
    private TextView choose_from_gallery;
    private PhotoGalleryService photoGalleryService;
    private List<Uri> imagesToSend = new ArrayList<>();

    private View attachImage;
    private boolean anyImageSelectedInQuickGallery;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        attachImage = (View) inflater.inflate(R.layout.fragment_attach_files, container, false);

        DisplayMetrics dm = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        final int RECYCLER_VIEW_VISIBLE_IMAGES = (int) ((double) dm.widthPixels / REQUIRED_IMAGE_SIZE);

        photoGalleryService = new PhotoGalleryService(getActivity());
        images = new ArrayList<>();

        listView = (RecyclerView) attachImage.findViewById(R.id.choose_photo);
        listView.setHasFixedSize(true);
        linearLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        listView.setLayoutManager(linearLayoutManager);

        choosePhotoAdapter = new ChoosePhotoAdapter(getActivity().getApplicationContext(), images, this);
        listView.setAdapter(choosePhotoAdapter);
        listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if ((linearLayoutManager.getItemCount() - RECYCLER_VIEW_VISIBLE_IMAGES) <= linearLayoutManager.findFirstVisibleItemPosition() + RECYCLER_VIEW_VISIBLE_IMAGES) {
                    loadImagesFromPhone(NUMBER_OF_IMAGES_TO_LOAD);
                }
            }
        });

        imagecursor = getActivity().managedQuery(uri, img, null, null, MediaStore.Images.Media.DATE_ADDED + " DESC");
        loadImagesFromPhone(RECYCLER_VIEW_VISIBLE_IMAGES);

        final AttachPhotoFragment activity = this;

        TextView take_photo = (TextView) attachImage.findViewById(R.id.take_photo);
        take_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                photoGalleryService.choose(true, activity);
            }
        });

        choose_from_gallery = (TextView) attachImage.findViewById(R.id.choose_from_gallery);
        choose_from_gallery.setTag(R.string.choose_from_gallery);
        choose_from_gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (anyImageSelectedInQuickGallery) {
                    //send selected photos
                    sendResultImages(imagesToSend);
                    choosePhotoAdapter.clearSelectedImages();
                    choosePhotoAdapter.notifyDataSetChanged();

                    choose_from_gallery.setText(R.string.choose_from_gallery);
                } else {
                    //open gallery
                    photoGalleryService.choose(false, activity);
                }
            }
        });

        return attachImage;
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        List<Uri> uriList = photoGalleryService.handleActivityResult(requestCode, resultCode, data);

            sendResultImages(uriList);
            /*for (Uri imageUri : uriList) {
                String path = imageUri.getPath();
                TdApi.InputMessagePhoto inputMessagePhoto = new TdApi.InputMessagePhoto(path);
                TdApi.SendMessage sendMessage = new TdApi.SendMessage(chatId, inputMessagePhoto);
                TgClient.send(sendMessage, this);
            */

//            choose_from_gallery.setTag(null);
//            imagesToSend.addAll(uriList);
//            choosePhotoAdapter.notifyDataSetChanged();
//        } else {
//            choose_from_gallery.setTag(R.string.choose_from_gallery);
//        }
    }

    private void sendResultImages(List<Uri> uriList) {
        if (uriList.size() > 0) {
            if (getActivity() instanceof PhotoGalleryImagesHandler) {
                ((PhotoGalleryImagesHandler) getActivity()).onImagesSelected(uriList);
            }
        }
    }

    private void loadImagesFromPhone(int number_of_images_to_load) {
        if (imagecursor == null)
            return;

        while (imagecursor.moveToNext() && number_of_images_to_load > 0) {
            File file = new File(imagecursor.getString(0));
            images.add(Uri.fromFile(file));
            number_of_images_to_load--;
        }
        choosePhotoAdapter.notifyDataSetChanged();
    }

    @Override
    public void onAttachImage(int numberOfImagesSelected, Uri imageUri) {
        if (numberOfImagesSelected == 0) {
            choose_from_gallery.setText(getResources().getString(R.string.choose_from_gallery));
            choose_from_gallery.setTag(R.string.choose_from_gallery);
            anyImageSelectedInQuickGallery = false;
        } else {
            if (numberOfImagesSelected == 1) {
                choose_from_gallery.setText(String.format("Send %d photo", numberOfImagesSelected));
            } else {
                choose_from_gallery.setText(String.format("Send %d photos", numberOfImagesSelected));
            }
            anyImageSelectedInQuickGallery = true;
            if (!imagesToSend.contains(imageUri)) {
                imagesToSend.add(imageUri);
            } else {
                imagesToSend.remove(imageUri);
            }
        }
    }

    @Override
    public void onCancel() {

    }

    @Override
    public PhotoGalleryService.BitmapProcessor onChosen() {
        return this;
    }

    @Override
    public void Process(Bitmap bmp, boolean isLast) {

    }

    @Override
    public void onResult(TdApi.TLObject object) {

    }

    public interface PhotoGalleryImagesHandler {
        void onImagesSelected(List<Uri> uris);
    }
}
