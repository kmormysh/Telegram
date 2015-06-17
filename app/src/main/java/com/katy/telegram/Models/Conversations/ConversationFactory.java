package com.katy.telegram.Models.Conversations;

import org.drinkless.td.libcore.telegram.TdApi;

import de.greenrobot.event.EventBus;

public class ConversationFactory {

    public static Conversation create(TdApi.Chat chat) {
        Conversation conversation = null;
        if (chat.type instanceof TdApi.PrivateChatInfo) {
            conversation = new ConversationPrivate(chat);
        } else //if chat.type instanceof TdApi.GroupChatInfo
        {
            conversation = new ConversationGroup(chat);
        }
        EventBus.getDefault().register(conversation);
        return conversation;
    }

}
