package com.katy.telegram.Models.Messages;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.katy.telegram.Activities.BaseActivity;
import com.katy.telegram.Adapters.ChatAdapter;
import com.katy.telegram.R;
import com.katy.telegram.Utils.FileSizeUnitsHelper;
import com.katy.telegram.Utils.TdFileHelper;

import org.drinkless.td.libcore.telegram.TdApi;

import java.io.File;

public class DocumentChatMessage extends ChatMessage {

    private int fileId;
    private String filePath;
    private boolean initialized;
    private ProgressBar progressBar;
    private int fileThumbId;
    private String fileThumbPath;

    public DocumentChatMessage(TdApi.Chat chat, TdApi.Message message, TdApi.User from) {
        super(chat, message, from);
    }

    public TdApi.MessageDocument getMessageDocument() {
        return (TdApi.MessageDocument) getMessage().message;
    }

    public void startDownloadDocument() {
        TdApi.Document document = getMessageDocument().document;
        fileId = (int) TdFileHelper.getFileId(document.document);
        if (document.document instanceof TdApi.FileEmpty) {
            TdFileHelper.getInstance().getFile(document.document, true);
        } else {
            filePath = ((TdApi.FileLocal) document.document).path;
        }
    }

    @Override
    public void onViewAttached(ChatAdapter.ChatMessageViewHolder holder) {
        super.onViewAttached(holder);
        if (initialized)
            return;
        initialized = true;

        FrameLayout relativeLayout = getAssignedViewHolder().getMessageFrameLayout();
        final ImageButton controlButton = (ImageButton) relativeLayout.findViewById(R.id.control_button);
        if (controlButton != null) {
            controlButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (filePath == null) {
                        startDownloadDocument();
                    } else {
                        Intent newIntent = new Intent(Intent.ACTION_VIEW);
                        newIntent.setDataAndType(Uri.fromFile(new File(filePath)), getMessageDocument().document.mimeType);
                        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        BaseActivity currentActivity = BaseActivity.getCurrentActivity();
                        try {
                            currentActivity.startActivity(newIntent);
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(currentActivity, "No handler for this type of file.", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            });
        }

        fileThumbPath = TdFileHelper.getInstance().getFile(getMessageDocument().document.document, false);

        if (fileThumbPath == null) {
            startDownloadThumbnail();
        }
    }

    public void startDownloadThumbnail() {
        TdApi.PhotoSize thumbFile = getMessageDocument().document.thumb;
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

                ImageView thumb = (ImageView) relativeLayout.findViewById(R.id.file_thumb);
                if (thumb != null) {
                    thumb.setImageBitmap(BitmapFactory.decodeFile(filePath));
                }

                final ImageButton controlButton = (ImageButton) relativeLayout.findViewById(R.id.control_button);
                progressBar = (ProgressBar) relativeLayout.findViewById(R.id.progressBar);
                BaseActivity currentActivity = BaseActivity.getCurrentActivity();

                progressBar.setVisibility(filePath != null ? View.GONE : View.VISIBLE);
                controlButton.setImageDrawable(filePath != null ? currentActivity.getResources().getDrawable(R.drawable.ic_file)
                        : currentActivity.getResources().getDrawable(R.drawable.ic_download_blue));
                controlButton.setTag(filePath != null ? R.drawable.ic_file : null);

            }
        } else if (file.fileId == fileThumbId) {
            fileThumbPath = file.path;
            if (getAssignedViewHolder() != null) {
                FrameLayout relativeLayout = getAssignedViewHolder().getMessageFrameLayout();
                ImageView videoThubm = (ImageView) relativeLayout.findViewById(R.id.file_thumb);
                if (videoThubm != null) {
                    videoThubm.setImageBitmap(BitmapFactory.decodeFile(fileThumbPath));

                    ImageButton controlButton = (ImageButton) relativeLayout.findViewById(R.id.control_button);
                    controlButton.setImageResource(filePath != null ? R.drawable.ic_file : R.drawable.ic_download_blue);
                    controlButton.setTag(filePath != null ? R.drawable.ic_file : null);
                }
            }
        }

    }

    public void onEventMainThread(TdApi.UpdateFileProgress fileProgress) {
        if (fileId == fileProgress.fileId) {
            if (getAssignedViewHolder() != null) {
                if (getAssignedViewHolder().getMessageFrameLayout() != null) {
                    TdApi.File document = getMessageDocument().document.document;

                    ((ImageButton) getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.control_button))
                            .setImageResource(R.drawable.ic_pause_blue);
                    (getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.control_button)).setTag(R.drawable.ic_pause_blue);

                    TextView image_size = (TextView) getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.image_size);

                    //update progress bar
                    progressBar = (ProgressBar) getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.progressBar);
                    fileProgressUpdate(fileProgress.ready, fileProgress.size);

                    //update download file size progress
                    String fileProgressSize = FileSizeUnitsHelper.readableFileSize(fileProgress.ready);
                    int size;
                    if (document instanceof TdApi.FileEmpty) {
                        size = ((TdApi.FileEmpty) document).size;
                    } else {
                        size = ((TdApi.FileLocal) document).size;
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
}
