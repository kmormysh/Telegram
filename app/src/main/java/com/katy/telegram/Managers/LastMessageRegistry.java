package com.katy.telegram.Managers;

import org.drinkless.td.libcore.telegram.TdApi;

import java.util.HashMap;

import de.greenrobot.event.EventBus;

/**
 * Created by Egorbo on 19.05.2015.
 */
public class LastMessageRegistry {

    private static HashMap<Long, Integer> chatAndMsgMap = new HashMap<>();
    private static LastMessageRegistry instance;

    private LastMessageRegistry() {
    }

    public static LastMessageRegistry getInstance() {
        if (instance == null) {
            synchronized (LastMessageRegistry.class) {
                if (instance == null) {
                    instance = new LastMessageRegistry();
                    EventBus.getDefault().register(instance);
                }
                return instance;
            }
        }
        return instance;
    }

    public void onEvent(TdApi.UpdateNewMessage newMessage){
        setLastMessageId(newMessage.message.chatId, newMessage.message.id);
    }

    public void setLastMessageId(long chatId, Integer messageId){
        synchronized (chatAndMsgMap){
            Integer msgId = chatAndMsgMap.get(chatId);
            msgId = Math.max(msgId == null ? 0 : msgId, messageId);
            msgId = msgId <= 0 ? 10000000 : msgId;
            chatAndMsgMap.put(chatId, msgId);
        }
    }

    public int getLastMessageId(long chatId){
        synchronized (chatAndMsgMap){
            return 10000000;
            //seems like a bug in TD lib :(
            //Integer value = chatAndMsgMap.get(chatId);
            //return value == null || value <= 0 ? 10000000 : value;
        }
    }
}
