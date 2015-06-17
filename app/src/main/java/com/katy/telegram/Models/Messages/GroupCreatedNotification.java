package com.katy.telegram.Models.Messages;

public class GroupCreatedNotification extends ChatEvent {

    private int fromId;
    private String groupChatName;

    public GroupCreatedNotification(int fromId, String groupChatName) {
        this.fromId = fromId;
        this.groupChatName = groupChatName;
    }

    public int getFromId() {
        return fromId;
    }

    public String getGroupChatName() {
        return groupChatName;
    }
}
