package com.katy.telegram.Models.Conversations;

import android.content.res.Resources;
import android.graphics.Color;
import android.view.View;

import com.katy.telegram.Activities.BaseActivity;
import com.katy.telegram.Adapters.ConversationsAdapter;
import com.katy.telegram.Managers.ChatsManager;
import com.katy.telegram.R;
import com.katy.telegram.Utils.RoundImage;
import com.katy.telegram.Utils.AvatarHelper;

import org.drinkless.td.libcore.telegram.TdApi;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.TimeZone;

public abstract class Conversation {
    private TdApi.Chat chat;
    private long chatId;

    private ConversationsAdapter.ViewHolder assignedViewHolder;

    public Conversation(TdApi.Chat chat) {
        this.chat = chat;
        this.chatId = chat.id;
    }

    public TdApi.Chat getChat() {
        return chat;
    }

    public void onViewDetached(ConversationsAdapter.ViewHolder holder) {
        if (holder == assignedViewHolder) {
            assignedViewHolder = null;
        }
    }

    public void onViewAttached(ConversationsAdapter.ViewHolder holder) {
        assignedViewHolder = holder;
        updateLastMessageLayout(getChat().topMessage);
    }

    protected ConversationsAdapter.ViewHolder getAssignedViewHolder() {
        return assignedViewHolder;
    }

    public long getChatId() {
        return chatId;
    }

    public void onEventMainThread(TdApi.UpdateNewMessage newMessage) {
        if (getChatId() == newMessage.message.chatId) {
            chat.topMessage = newMessage.message;
            if (getAssignedViewHolder() != null) {
                getAssignedViewHolder().textNumberOfUnreadMessages.setText(Integer.toString(++chat.unreadCount));
                getAssignedViewHolder().textNumberOfUnreadMessages.setVisibility(View.VISIBLE);
                getAssignedViewHolder().imageUnreadMessage.setVisibility(View.VISIBLE);
                getAssignedViewHolder().imageUnreadMessage.setImageResource(R.drawable.ic_badge);
                updateLastMessageLayout(newMessage.message);
            }
        }
    }

    private void updateLastMessageLayout(TdApi.Message newMessage) {
        SimpleDateFormat df = new SimpleDateFormat("HH:mm");
        df.setTimeZone(TimeZone.getDefault());
        getAssignedViewHolder().textTime.setText(df.format(((long) newMessage.date) * 1000L));

        Resources resources = BaseActivity.getCurrentActivity().getResources();
        if (newMessage.message instanceof TdApi.MessageText) {
            String messageText = ((TdApi.MessageText) newMessage.message).text;
            getAssignedViewHolder().textMessage.setText(messageText);
            getAssignedViewHolder().textMessage.setTextColor(resources.getColor(R.color.color_time));
        } else {
            getAssignedViewHolder().textMessage.setTextColor(resources.getColor(R.color.background_blue));
            if (newMessage.message instanceof TdApi.MessageAudio) {
                getAssignedViewHolder().textMessage.setText("Audio");
            } else if (newMessage.message instanceof TdApi.MessageVideo) {
                getAssignedViewHolder().textMessage.setText("Video");
            } else if (newMessage.message instanceof TdApi.MessagePhoto) {
                getAssignedViewHolder().textMessage.setText("Image");
            } else if (newMessage.message instanceof TdApi.MessageDocument){
                getAssignedViewHolder().textMessage.setText("Document");
            } else if (newMessage.message instanceof TdApi.MessageGeoPoint){
                getAssignedViewHolder().textMessage.setText("Map");
            }
        }
    }

    public void onEventMainThread(TdApi.UpdateChatReadInbox chatReadInbox) {
        if (getChatId() == chatReadInbox.chatId) {
            chat.unreadCount = chatReadInbox.unreadCount;
            chat.lastReadInboxMessageId = chatReadInbox.lastRead;
            if (getAssignedViewHolder() != null) {
                getAssignedViewHolder().textNumberOfUnreadMessages.setVisibility(View.VISIBLE);
                getAssignedViewHolder().imageUnreadMessage.setVisibility(View.VISIBLE);
                getAssignedViewHolder().imageUnreadMessage.setImageResource(R.drawable.ic_badge);
                getAssignedViewHolder().textNumberOfUnreadMessages.setText(chatReadInbox.unreadCount);
            }
        }
    }

    public void onEventMainThread(TdApi.UpdateChatReadOutbox chatReadOutbox) {
        if (getChatId() == chatReadOutbox.chatId) {
            getChat().lastReadOutboxMessageId = chatReadOutbox.lastRead;
            HashMap<Long, Integer> lastReadOutboxMap = ChatsManager.getInstance().getLastReadOutboxMap();
            lastReadOutboxMap.put(chatId, chatReadOutbox.lastRead);
            ChatsManager.getInstance().setLastReadOutboxMap(lastReadOutboxMap);
            getAssignedViewHolder().imageOnlineStatus.setVisibility(View.GONE);
        }
    }

    public void onEventMainThread(TdApi.UpdateMessageDate updateMessageDate) {
        if (getChatId() == updateMessageDate.chatId) {
            if (updateMessageDate.newDate == 0) {
                getAssignedViewHolder().imageUnreadMessage.setImageResource(R.drawable.ic_error);
            } else {
                getAssignedViewHolder().imageUnreadMessage.setImageResource(R.drawable.ic_badge);
                getAssignedViewHolder().imageUnreadMessage.setVisibility(View.GONE);
            }
        }
    }

    public void onEventMainThread(TdApi.UpdateMessageId updateMessageId) {
        if (getChatId() == updateMessageId.chatId) {
            if (getChat().topMessage.id == updateMessageId.oldId) {
                getAssignedViewHolder().imageOnlineStatus.setVisibility(View.GONE);
                getChat().topMessage.id = updateMessageId.newId;
            }
        }
    }

    protected void setChatAvatar(String imagePath, String chatName, ConversationsAdapter.ViewHolder viewHolder) {
        RoundImage roundedImage = AvatarHelper.createRoundImage(getAssignedViewHolder().context, imagePath, chatName);
        AvatarHelper.createImageShortName(imagePath == null, chatName, viewHolder.textShortName);
        viewHolder.imageAvatar.setImageDrawable(roundedImage);
        viewHolder.imageAvatar.setBackgroundColor(Color.TRANSPARENT);
    }
}
