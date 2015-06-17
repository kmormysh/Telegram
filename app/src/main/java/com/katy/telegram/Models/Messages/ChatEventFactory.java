package com.katy.telegram.Models.Messages;

import org.drinkless.td.libcore.telegram.TdApi;

import de.greenrobot.event.EventBus;

public class ChatEventFactory {

    /**
     * Creates ChatEvent from tdapi.message
     */
    public static ChatEvent create(TdApi.Chat chat, TdApi.Message message, TdApi.User from) {
        ChatEvent event = null;
        if (message.message instanceof TdApi.MessageText) {
            event = new TextChatMessage(chat, message, from);
        } else if (message.message instanceof TdApi.MessageSticker) {
            event = new StickerChatMessage(chat, message, from);
        } else if (message.message instanceof TdApi.MessagePhoto) {
            event = new PhotoChatMessage(chat, message, from);
        } else if (message.message instanceof TdApi.MessageAudio) {
            event = new AudioChatMessage(chat, message, from);
        } else if (message.message instanceof TdApi.MessageVideo) {
            event = new VideoChatMessage(chat, message, from);
        } else if (message.message instanceof TdApi.MessageChatChangePhoto) {
            event = new GroupPhotoChangedNotification(chat, from);
        } else if (message.message instanceof TdApi.MessageGeoPoint) {
            event = new GeoPointChatMessage(chat, message, from);
        } else if (message.message instanceof TdApi.MessageDocument) {
            String[] documentType = ((TdApi.MessageDocument) message.message).document.mimeType.split("/");
            if (documentType[0].equals("audio")) {
                event = new AudioChatMessage(chat, message, from);
            } else if (documentType[0].equals("video")) {
                event = new VideoChatMessage(chat, message, from);
            } else {
                event = new DocumentChatMessage(chat, message, from);
            }
        }

        if (event == null) {
            event = new NotImplementedYetChatMessage(chat, message, from);
        }

        EventBus.getDefault().register(event);
        return event;
    }
}
