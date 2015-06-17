package com.katy.telegram.Models.Messages;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.katy.telegram.Adapters.ChatAdapter;
import com.katy.telegram.Managers.AccountManager;
import com.katy.telegram.Managers.TgClient;
import com.katy.telegram.R;
import com.katy.telegram.Utils.RoundImage;
import com.katy.telegram.Utils.AvatarHelper;
import com.katy.telegram.Utils.TdFileHelper;

import org.drinkless.td.libcore.telegram.TdApi;

public abstract class ChatMessage extends ChatEvent implements TgClient.TLResponseCallback {

    private TdApi.Message message;
    private final TdApi.Chat chat;
    private TdApi.User from;
    private ChatAdapter.ChatMessageViewHolder assignedViewHolder;
    private TdApi.User authorOfForwardedMsg;

    public ChatMessage(TdApi.Chat chat, TdApi.Message message, TdApi.User from) {
        this.chat = chat;
        this.message = message;
        this.from = from;
        if (message.forwardFromId > 0) {
            TgClient.send(new TdApi.GetUser(message.forwardFromId), this);
        }
    }

    public TdApi.Message getMessage() {
        return message;
    }

    public TdApi.User getFrom() {
        return from;
    }

    public void onViewDetached(ChatAdapter.ChatMessageViewHolder holder) {
        if (holder == assignedViewHolder) {
            assignedViewHolder = null;
        }
    }

    public void onViewAttached(ChatAdapter.ChatMessageViewHolder holder) {
        assignedViewHolder = holder;
        updateAuthorOfForwardedMsgLayout();
    }

    protected ChatAdapter.ChatMessageViewHolder getAssignedViewHolder() {
        return assignedViewHolder;
    }

    public void onEventMainThread(TdApi.UpdateMessageId updateMessageId) {
        if (message.chatId == updateMessageId.chatId) {
            if (message.id == updateMessageId.oldId || message.id == updateMessageId.newId) {
                //message is delivered to the server but is unread till UpdateChatReadOutbox
                getAssignedViewHolder().message_status.setVisibility(View.VISIBLE);
                getAssignedViewHolder().message_status.setImageResource(R.drawable.ic_unread);
                message.id = updateMessageId.newId;
            }
        }
    }

    public void onEventMainThread(TdApi.UpdateChatReadOutbox updateChatReadOutbox) {
        if (message.fromId == AccountManager.getInstance().getCurrentUser().id) {
            chat.lastReadOutboxMessageId = updateChatReadOutbox.lastRead;
            if (message.id > updateChatReadOutbox.lastRead) {
                getAssignedViewHolder().message_status.setImageResource(R.drawable.ic_unread);
            } else {
                getAssignedViewHolder().message_status.setImageResource(R.drawable.ic_clock);
                getAssignedViewHolder().message_status.setVisibility(View.GONE);
            }
        }
    }

    public int getLastReadMessage() {
        return chat.lastReadOutboxMessageId;
    }

    @Override
    public void onResult(TdApi.TLObject object) {
        if (object instanceof TdApi.User) {
            authorOfForwardedMsg = (TdApi.User) object;
            updateAuthorOfForwardedMsgLayout();
        }
    }

    public void onEventMainThread(TdApi.UpdateFile updateFile) {
        if (authorOfForwardedMsg != null && TdFileHelper.getFileId(authorOfForwardedMsg.photoSmall) == updateFile.fileId) {
            updateAuthorOfForwardedMsgLayout();
        }
    }

    private void updateAuthorOfForwardedMsgLayout() {
        if (authorOfForwardedMsg == null)
            return;

        if (assignedViewHolder != null) {
            FrameLayout message_content = assignedViewHolder.message_content;
            TextView short_name = (TextView) message_content.findViewById(R.id.short_name);
            ImageView avatar = (ImageView) message_content.findViewById(R.id.avatar);

            String filePath = TdFileHelper.getInstance().getFile(authorOfForwardedMsg.photoSmall, true, false);

            RoundImage roundedImage = AvatarHelper.createRoundImage(assignedViewHolder.activity, filePath, Integer.toString(message.forwardFromId));
            AvatarHelper.createImageShortName(filePath == null || filePath.equals(""), Integer.toString(message.forwardFromId), short_name);
            avatar.setImageDrawable(roundedImage);

            TextView user_name = (TextView) message_content.findViewById(R.id.user_name);
            user_name.setText(authorOfForwardedMsg.firstName + " " + authorOfForwardedMsg.lastName);
        }
    }
}

