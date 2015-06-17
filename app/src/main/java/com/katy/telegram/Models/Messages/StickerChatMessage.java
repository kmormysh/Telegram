package com.katy.telegram.Models.Messages;


import android.graphics.BitmapFactory;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.katy.telegram.Adapters.ChatAdapter;
import com.katy.telegram.R;
import com.katy.telegram.Utils.TdFileHelper;

import org.drinkless.td.libcore.telegram.TdApi;

public class StickerChatMessage extends ChatMessage {

    private int fileId;
    private String filePath;
    private boolean initialized;

    public StickerChatMessage(TdApi.Chat chat, TdApi.Message message, TdApi.User from) {
        super(chat, message, from);
    }

    public TdApi.MessageSticker getMessageSticker() {
        return (TdApi.MessageSticker) getMessage().message;
    }

    public void startDownloadSticker() {
        TdApi.Sticker sticker = getMessageSticker().sticker;
        fileId = (int) TdFileHelper.getFileId(sticker.sticker);
        if (sticker.sticker instanceof TdApi.FileEmpty) {
            TdFileHelper.getInstance().getFile(sticker.sticker, true);
        } else {
            filePath = ((TdApi.FileLocal) sticker.sticker).path;
        }
    }

    @Override
    public void onViewAttached(ChatAdapter.ChatMessageViewHolder holder) {
        super.onViewAttached(holder);
        if (initialized)
            return;
        initialized = true;

        filePath = TdFileHelper.getInstance().getFile(getMessageSticker().sticker.sticker, false);
        if (filePath == null) {
            startDownloadSticker();
        }
    }

    public void onEventMainThread(final TdApi.UpdateFile file) {
        if (file.fileId == fileId) {
            filePath = file.path;
            if (getAssignedViewHolder() != null) {
                FrameLayout relativeLayout = getAssignedViewHolder().getMessageFrameLayout();

                ImageView sticker = (ImageView) relativeLayout.findViewById(R.id.sticker);
                if (sticker != null) {
                    sticker.setImageBitmap(BitmapFactory.decodeFile(filePath));
                }
            }
        }
    }
}
