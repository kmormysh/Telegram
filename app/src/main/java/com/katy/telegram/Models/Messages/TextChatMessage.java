package com.katy.telegram.Models.Messages;

import org.drinkless.td.libcore.telegram.TdApi;

public class TextChatMessage extends ChatMessage{

    public TextChatMessage(TdApi.Chat chat, TdApi.Message message, TdApi.User from) {
        super(chat, message, from);
    }

    public TdApi.MessageText getMessageText() {
        return (TdApi.MessageText)getMessage().message;
    }
}


