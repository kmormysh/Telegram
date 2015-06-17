package com.katy.telegram.Models.Messages;

import org.drinkless.td.libcore.telegram.TdApi;

public class AddParticipantNotification extends ChatEvent {

    private final TdApi.User who;
    private final TdApi.User whom;

    public AddParticipantNotification(TdApi.User who, TdApi.User whom) {

        this.who = who;
        this.whom = whom;
    }

    public TdApi.User getWhom() {
        return whom;
    }

    public TdApi.User getWho() {
        return who;
    }
}