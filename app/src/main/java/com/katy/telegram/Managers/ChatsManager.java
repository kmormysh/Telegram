package com.katy.telegram.Managers;

import org.drinkless.td.libcore.telegram.TdApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChatsManager {
    private HashMap<Long, TdApi.Chat> chatsMap = new HashMap<>();
    private HashMap<Long, Integer> lastReadOutboxMap = new HashMap<>();
    private static ChatsManager chatsManager;

    public static ChatsManager getInstance() {
        if (chatsManager == null) {
            synchronized (ChatsManager.class) {
                if (chatsManager == null) {
                    chatsManager = new ChatsManager();
                }
            }
        }
        return chatsManager;
    }

    private ChatsManager() {
    }

    public void getChat(Long chatId, final Callback<TdApi.Chat> callback) {
        TdApi.Chat existingChat = chatsMap.get(chatId);
        if (existingChat != null) {
            callback.onResult(existingChat);
            return;
        }
        TdApi.GetChat getChat = new TdApi.GetChat(chatId);
        TgClient.send(getChat, new TgClient.TLResponseCallback() {
            @Override
            public void onResult(TdApi.TLObject object) {
                TdApi.Chat chat = (TdApi.Chat) object;
                chatsMap.put(chat.id, chat);
                callback.onResult(chat);
            }
        });
    }

    public void getChats(final Callback<List<TdApi.Chat>> callback) {
        TdApi.GetChats getChats = new TdApi.GetChats(0, 20); //temporarily, TODO: implement incremental loading
        TgClient.send(getChats, new TgClient.TLResponseCallback() {
            @Override
            public void onResult(TdApi.TLObject object) {
                List<TdApi.Chat> result = new ArrayList<>();
                for (TdApi.Chat chat : ((TdApi.Chats) object).chats) {
                    LastMessageRegistry.getInstance().setLastMessageId(chat.topMessage.chatId, chat.topMessage.id);
                    chatsMap.put(chat.id, chat);
                    result.add(chat);
                }
                callback.onResult(result);
            }
        });
    }

    public void setLastReadOutboxMap(HashMap<Long, Integer> lastReadOutboxMap){
        this.lastReadOutboxMap = lastReadOutboxMap;
    }

    public HashMap<Long, Integer> getLastReadOutboxMap() {
        return lastReadOutboxMap;
    }

    public interface Callback<T> {
        void onResult(T result);
    }
}
