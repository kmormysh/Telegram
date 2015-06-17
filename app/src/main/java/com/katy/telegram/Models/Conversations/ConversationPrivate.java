package com.katy.telegram.Models.Conversations;

import com.katy.telegram.Adapters.ConversationsAdapter;
import com.katy.telegram.Managers.TgClient;
import com.katy.telegram.Utils.TdFileHelper;

import org.drinkless.td.libcore.telegram.TdApi;

public class ConversationPrivate extends Conversation {

    public ConversationPrivate(TdApi.Chat chat) {
        super(chat);
    }

    @Override
    public void onViewAttached(ConversationsAdapter.ViewHolder holder) {
        super.onViewAttached(holder);
        startDownloadingImages();
    }

    private void startDownloadingImages() {
        TdApi.File photoSmall = getPrivateChatInfo().user.photoSmall;
        TdFileHelper.getInstance().getFile(photoSmall, true);
    }

    public TdApi.PrivateChatInfo getPrivateChatInfo() {
        return (TdApi.PrivateChatInfo) getChat().type;
    }

    public void onEventMainThread(TdApi.UpdateFile file) {
        if (!TdFileHelper.sameId(getPrivateChatInfo().user.photoSmall, file.fileId))
            return;

        getPrivateChatInfo().user.photoSmall = new TdApi.FileLocal(file.fileId, file.size, file.path);
        if (getAssignedViewHolder() != null) {
            TdApi.User user = getPrivateChatInfo().user;
            setChatAvatar(file.path, user.firstName + " " + user.lastName, getAssignedViewHolder());
        }
    }
}
