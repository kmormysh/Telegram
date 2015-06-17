package com.katy.telegram.Models.Messages;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.katy.telegram.Activities.ImageViewActivity;
import com.katy.telegram.Adapters.ChatAdapter;
import com.katy.telegram.R;
import com.katy.telegram.Utils.FileSizeUnitsHelper;
import com.katy.telegram.Utils.TdFileHelper;

import org.drinkless.td.libcore.telegram.TdApi;

import java.io.File;

public class PhotoChatMessage extends ChatMessage {

    private boolean initialized;
    private boolean stopDownloading;
    private int fileId;
    private ProgressBar progressBar;
    private String largePhotoPath;
    private int largePhotoId;
    private int largePhotoSize;

    public PhotoChatMessage(TdApi.Chat chat, TdApi.Message message, TdApi.User from) {
        super(chat, message, from);
    }

    public TdApi.MessagePhoto getMessagePhoto() {
        return (TdApi.MessagePhoto) getMessage().message;
    }

    public void startDownloadThumbnail(boolean thumbnail) {
        TdApi.PhotoSize[] photos = getMessagePhoto().photo.photos;
        if (photos[photos.length - 1].photo instanceof TdApi.FileEmpty) {
            largePhotoId = ((TdApi.FileEmpty) photos[photos.length - 1].photo).id;
            largePhotoSize = ((TdApi.FileEmpty) photos[photos.length - 1].photo).size;
        } else {
            largePhotoId = ((TdApi.FileLocal) photos[photos.length - 1].photo).id;
            largePhotoSize = ((TdApi.FileLocal) photos[photos.length - 1].photo).size;
        }

        int index = thumbnail ? 0 : photos.length - 1;
        fileId = (int) TdFileHelper.getFileId(photos[index].photo);
        TdFileHelper.getInstance().getFile(photos[index].photo, true);
    }

    @Override
    public void onViewAttached(ChatAdapter.ChatMessageViewHolder holder) {
        super.onViewAttached(holder);
        final ImageButton controlButton = (ImageButton) getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.control_button);

        if (largePhotoPath != null) {
            controlButton.setVisibility(View.GONE);
        } else if (largePhotoPath == null && controlButton != null) {
            controlButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (controlButton.getTag() != null && controlButton.getTag().equals(R.drawable.ic_pause)) {
                        stopDownloading = true;
                        controlButton.setTag(null);
                        controlButton.setVisibility(View.VISIBLE);
                        controlButton.setImageResource(R.drawable.ic_download);
                        (getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.progressBar)).setVisibility(View.VISIBLE);
                    } else {
                        startDownloadThumbnail(false);
                    }
                }
            });
        }

        ImageView imageView = (ImageView) getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.image);
        if (imageView != null) {
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (largePhotoPath != null) {
                        Intent intent = new Intent(getAssignedViewHolder().activity, ImageViewActivity.class);
                        largePhotoPath = TdFileHelper.getInstance().getFile(new TdApi.FileEmpty(largePhotoId, 1), false);
                        intent.putExtra("imagePath", largePhotoPath);
                        getAssignedViewHolder().activity.startActivity(intent);
                    }
                }
            });
        }

        TdFileHelper.getInstance().getFile(getMessagePhoto().photo.photos[0].photo, false);
        if (initialized)
            return;
        initialized = true;
    }

    @Override
    public void onViewDetached(ChatAdapter.ChatMessageViewHolder holder) {
        super.onViewDetached(holder);
        startDownloadThumbnail(true);
    }

    public void onEventMainThread(TdApi.UpdateFileProgress fileProgress) {
        if (fileId == fileProgress.fileId && largePhotoId == fileProgress.fileId && !stopDownloading) {
            if (getAssignedViewHolder() != null) {
                if (getAssignedViewHolder().getMessageFrameLayout() != null) {
                    TdApi.PhotoSize[] photos = getMessagePhoto().photo.photos;
                    ((ImageButton) getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.control_button))
                            .setImageResource(R.drawable.ic_pause);
                    (getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.control_button)).setTag(R.drawable.ic_pause);

                    TextView image_size = (TextView) getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.image_size);

                    //update progress bar
                    progressBar = (ProgressBar) getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.progressBar);
                    fileProgressUpdate(fileProgress.ready, fileProgress.size);

                    //update download file size progress
                    String fileProgressSize = FileSizeUnitsHelper.readableFileSize(fileProgress.ready);
                    int size;
                    if (photos[photos.length - 1].photo instanceof TdApi.FileEmpty) {
                        size = ((TdApi.FileEmpty) photos[photos.length - 1].photo).size;
                    } else {
                        size = ((TdApi.FileLocal) photos[photos.length - 1].photo).size;
                    }
                    String fileSize = FileSizeUnitsHelper.readableFileSize(size);
                    image_size.setText(String.format("Downdloaded %s of %s", fileProgressSize, fileSize));
                }
            }
        }
    }

    private Handler mHandler = new Handler();
    private int progress = 0;

    public void fileProgressUpdate(final int currentSize, final int actualSize) {
        new Thread(new Runnable() {
            public void run() {
                progress = (int) (currentSize * 100 / (double) actualSize);
                while (progress < 100) {
                    progress++;
                    mHandler.post(new Runnable() {
                        public void run() {
                            if (progressBar != null)
                                progressBar.setProgress(progress);
                        }
                    });
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public void onEventMainThread(final TdApi.UpdateFile file) {

        if (fileId == file.fileId) {
            if (largePhotoPath == null)
                largePhotoPath = TdFileHelper.getInstance().getFile(new TdApi.FileEmpty(largePhotoId, 1), false);
            if (getAssignedViewHolder() != null) {
                ImageView imageView = (ImageView) getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.image);
                if (imageView != null) {
                    imageView.setImageBitmap(BitmapFactory.decodeFile(file.path));
                }

                TextView image_name = (TextView) getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.image_name);
                if (image_name != null) {
                    image_name.setText(new File(file.path).getName());
                }

                TextView image_size = (TextView) getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.image_size);
                if (image_size != null) {
                    image_size.setText(FileSizeUnitsHelper.readableFileSize(largePhotoSize));
                }

                if (largePhotoPath == null && imageView != null) {
                    (getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.progressBar)).setVisibility(View.VISIBLE);
                    (getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.control_button)).setVisibility(View.VISIBLE);
                } else {
                    (getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.progressBar)).setVisibility(View.GONE);
                    (getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.control_button)).setVisibility(View.GONE);
                }
            }
        }
    }
}
