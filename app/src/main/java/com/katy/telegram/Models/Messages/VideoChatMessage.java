package com.katy.telegram.Models.Messages;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.katy.telegram.Activities.BaseActivity;
import com.katy.telegram.Adapters.ChatAdapter;
import com.katy.telegram.R;
import com.katy.telegram.Utils.FileSaveOrCopyHelper;
import com.katy.telegram.Utils.TdFileHelper;

import org.drinkless.td.libcore.telegram.TdApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class VideoChatMessage extends ChatMessage {

    private int fileId;
    private String filePath;
    private boolean stopDownloading;
    private ProgressBar progressBar;
    private boolean initialized;
    private int fileThumbId;
    private String fileThumbPath;

    public VideoChatMessage(TdApi.Chat chat, TdApi.Message message, TdApi.User from) {
        super(chat, message, from);
    }

    public TdApi.MessageVideo getMessageVideo() {
        return (TdApi.MessageVideo) getMessage().message;
    }

    public void startDownloadVideo() {
        TdApi.Video video = getMessageVideo().video;
        fileId = (int) TdFileHelper.getFileId(video.video);
        if (video.video instanceof TdApi.FileEmpty) {
            TdFileHelper.getInstance().getFile(video.video, true);
        } else {
            filePath = ((TdApi.FileLocal) video.video).path;
        }
    }

    public void startDownloadThumbnail() {
        TdApi.PhotoSize thumbFile = getMessageVideo().video.thumb;
        TdApi.File thumbPhoto = thumbFile.photo;
        fileThumbId = (int) TdFileHelper.getFileId(thumbPhoto);
        if (thumbPhoto instanceof TdApi.FileEmpty) {
            TdFileHelper.getInstance().getFile(thumbPhoto, true);
        }
    }

    public void onEventMainThread(final TdApi.UpdateFile file) {
        if (file.fileId == fileId) {
            filePath = file.path;
            if (getAssignedViewHolder() != null) {
                FrameLayout relativeLayout = getAssignedViewHolder().getMessageFrameLayout();
                (relativeLayout.findViewById(R.id.progressBar)).setVisibility(View.GONE);

                ImageButton controlButton = (ImageButton) relativeLayout.findViewById(R.id.control_button);
                controlButton.setImageResource(R.drawable.ic_play);
                controlButton.setTag(R.drawable.ic_play);

                ImageView videoThubm = (ImageView) relativeLayout.findViewById(R.id.video_thubm);
                videoThubm.setImageBitmap(BitmapFactory.decodeFile(fileThumbPath));
            }
        } else if (file.fileId == fileThumbId) {
            fileThumbPath = file.path;
            if (getAssignedViewHolder() != null) {
                FrameLayout relativeLayout = getAssignedViewHolder().getMessageFrameLayout();
                ImageView videoThubm = (ImageView) relativeLayout.findViewById(R.id.video_thubm);
                videoThubm.setImageBitmap(BitmapFactory.decodeFile(fileThumbPath));

                ImageButton controlButton = (ImageButton) relativeLayout.findViewById(R.id.control_button);
                if (filePath != null) {
                    controlButton.setImageResource(R.drawable.ic_play);
                } else {
                    controlButton.setImageResource(R.drawable.ic_download);
                }
            }
        }
    }

    public void onEventMainThread(TdApi.UpdateFileProgress fileProgress) {
        if (fileId == fileProgress.fileId && !stopDownloading) {
            if (getAssignedViewHolder() != null) {
                if (getAssignedViewHolder().getMessageFrameLayout() != null) {
                    getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.control_button).setTag(R.drawable.ic_download);
                    ((ImageButton) getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.control_button))
                            .setImageResource(R.drawable.ic_pause);
                    (getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.control_button)).setTag(R.drawable.ic_pause);

                    //update progress bar
                    progressBar = (ProgressBar) getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.progressBar);
                    fileProgressUpdate(fileProgress.ready, fileProgress.size);
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

    @Override
    public void onViewAttached(ChatAdapter.ChatMessageViewHolder holder) {
        super.onViewAttached(holder);
        final ImageButton controlButton = (ImageButton) getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.control_button);
        if (controlButton != null) {
            controlButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (filePath == null && controlButton.getTag() == null) { //start downloading
                        startDownloadVideo();
                    } else if (filePath == null) { //stop downloading
                        stopDownloading = true;
                        controlButton.setTag(null);
                        controlButton.setVisibility(View.VISIBLE);
                        controlButton.setImageResource(R.drawable.ic_download);
                        (getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.progressBar)).setVisibility(View.VISIBLE);
                        ((ProgressBar) (getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.progressBar))).setProgress(0);
                    } else if (filePath != null) { //play
                        try {
                            filePath = FileSaveOrCopyHelper.copyFile(new File(filePath));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(filePath));
                        intent.setDataAndType(Uri.parse(filePath), "video/mp4");
                        BaseActivity.getCurrentActivity().startActivity(intent);
                    }
                }
            });
        }

        TdFileHelper.getInstance().getFile(getMessageVideo().video.thumb.photo, false);
        TdFileHelper.getInstance().getFile(getMessageVideo().video.video, false);

        if (initialized)
            return;
        initialized = true;

        startDownloadThumbnail();
    }
}
