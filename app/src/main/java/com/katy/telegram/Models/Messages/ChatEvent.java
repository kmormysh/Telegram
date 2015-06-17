package com.katy.telegram.Models.Messages;

import com.katy.telegram.TelegramApplication;

public abstract class ChatEvent {

    /*
    * fake subscription, needed for ChatEventFactory to be able to register all it's children to EventBus
    * */
    public void onEvent(TelegramApplication application){
    }
}

