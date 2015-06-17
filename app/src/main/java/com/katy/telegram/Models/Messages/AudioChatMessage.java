package com.katy.telegram.Models.Messages;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.katy.telegram.Activities.BaseActivity;
import com.katy.telegram.Adapters.ChatAdapter;
import com.katy.telegram.R;
import com.katy.telegram.Utils.TdFileHelper;

import org.drinkless.td.libcore.telegram.TdApi;

import java.io.FileInputStream;

public class AudioChatMessage extends ChatMessage {

    private int fileId;
    private boolean stopDownloading;
    private String filePath;
    private boolean initialized;
    private ProgressBar progressBar;
    private MediaPlayer mp;

    public AudioChatMessage(TdApi.Chat chat, TdApi.Message message, TdApi.User from) {
        super(chat, message, from);
    }

    public TdApi.MessageAudio getMessageAudio() {
        return (TdApi.MessageAudio) getMessage().message;
    }

    public void startDownloadAudio() {
        TdApi.Audio audio = getMessageAudio().audio;
        if (audio.audio instanceof TdApi.FileEmpty) {
            fileId = (int) TdFileHelper.getFileId(audio.audio);
            TdFileHelper.getInstance().getFile(audio.audio, true);
        }
    }

    public void onEventMainThread(final TdApi.UpdateFile file) {
        if (file.fileId == fileId) {
            filePath = file.path;
            if (getAssignedViewHolder() != null) {
                Context activity = getAssignedViewHolder().activity;
                ((ImageButton) getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.control_button))
                        .setImageResource(R.drawable.ic_play);
                (getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.control_button)).setTag(R.drawable.ic_play);
                (getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.roundedbutton))
                        .setBackgroundDrawable(activity.getResources().getDrawable(R.drawable.roundedbutton));

                Drawable seekbarThumb = activity.getResources().getDrawable(R.drawable.seek_bar_thumb);
                SeekBar seekbar = (SeekBar) getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.seek_bar);
                seekbar.setThumb(seekbarThumb);
                seekbar.invalidate();
            }
        }
    }

    public void onEventMainThread(TdApi.UpdateFileProgress fileProgress) {
        if (fileId == fileProgress.fileId && !stopDownloading) {
            if (getAssignedViewHolder() != null) {
                if (getAssignedViewHolder().getMessageFrameLayout() != null) {
                    TdApi.Audio audio = getMessageAudio().audio;
                    ((ImageButton) getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.control_button))
                            .setImageResource(R.drawable.ic_pause);
                    (getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.control_button)).setTag(R.drawable.ic_pause);

                    TextView duration = (TextView) getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.duration);

                    //update progress bar
                    progressBar = (ProgressBar) getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.progressBar);
                    fileProgressUpdate(fileProgress.ready, fileProgress.size);

                    int minutes = audio.duration / 60;
                    int seconds = audio.duration - minutes * 60;
                    duration.setText(String.format("%d:%02d", minutes, seconds));
//
                    ((SeekBar) getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.seek_bar))
                            .setThumb(getAssignedViewHolder().activity.getResources().getDrawable(R.drawable.seek_bar_thumb));
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
                    if (controlButton.getTag() == null) { //start downloading
                        startDownloadAudio();
                    } else if (filePath == null) { //stop downloading
                        stopDownloading = true;
                        controlButton.setTag(null);
                        controlButton.setVisibility(View.VISIBLE);
                        controlButton.setImageResource(R.drawable.ic_download);
                        (getAssignedViewHolder().getMessageFrameLayout().findViewById(R.id.progressBar)).setVisibility(View.VISIBLE);
                    } else if (controlButton.getTag().equals(R.drawable.ic_pause)) { // stop playing
                        controlButton.setTag(R.drawable.ic_play);
                        controlButton.setImageResource(R.drawable.ic_play);
                        mp.stop();
                    } else if (controlButton.getTag() != null && controlButton.getTag().equals(R.drawable.ic_play)) { // play
                        controlButton.setTag(R.drawable.ic_pause);
                        controlButton.setImageResource(R.drawable.ic_pause);
                        if (mp == null) {
                            setupAudioPlayer(filePath);
                        }
                        mp.start();
                    }
                }
            });
        }
        if (initialized)
            return;
        initialized = true;
    }

    public void setupAudioPlayer(String path) {
        mp = new MediaPlayer();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(path);
            mp.setDataSource(fis.getFD());
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mp.setVolume(100f, 100f);
            mp.setLooping(false);
            mp.prepare();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(BaseActivity.getCurrentActivity(), "OPUS is not supported yet :(", Toast.LENGTH_SHORT).show();
        }
    }
}
