package com.katy.telegram.Models.Conversations;

import com.katy.telegram.Adapters.ConversationsAdapter;
import com.katy.telegram.Managers.TgClient;
import com.katy.telegram.Utils.TdFileHelper;

import org.drinkless.td.libcore.telegram.TdApi;

public class ConversationGroup extends Conversation {
    public ConversationGroup(TdApi.Chat chat) {
        super(chat);
    }

    @Override
    public void onViewAttached(ConversationsAdapter.ViewHolder holder) {
        super.onViewAttached(holder);
        startDownloadingImages();
    }

    private void startDownloadingImages() {
        TdApi.File photoSmall = getChatGroup().groupChat.photoSmall;
        TdFileHelper.getInstance().getFile(photoSmall, true);
    }

    public TdApi.GroupChatInfo getChatGroup() {
        return (TdApi.GroupChatInfo) getChat().type;
    }

    public void onEventMainThread(TdApi.UpdateFile file) {
        if (!TdFileHelper.sameId(getChatGroup().groupChat.photoSmall, file.fileId))
            return;

        getChatGroup().groupChat.photoSmall = new TdApi.FileLocal(file.fileId, file.size, file.path);
        if (getAssignedViewHolder() != null) {
            setChatAvatar(file.path, getChatGroup().groupChat.title, getAssignedViewHolder());
        }
    }
}
