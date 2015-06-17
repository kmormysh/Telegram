package com.katy.telegram.Models.Messages;

import org.drinkless.td.libcore.telegram.TdApi;

public class GroupPhotoChangedNotification extends ChatEvent {

    private TdApi.Chat chat;
    private final TdApi.User who;

    public GroupPhotoChangedNotification(TdApi.Chat chat, TdApi.User who) {
        this.chat = chat;
        this.who = who;
    }

    public TdApi.User getWho() {
        return who;
    }

    public TdApi.Chat getChat() {
        return chat;
    }
}