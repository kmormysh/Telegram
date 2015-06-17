package com.katy.telegram.Models.Messages;

public class UnreadMessagesBadge extends ChatEvent {
    private int unreadCount;

    public UnreadMessagesBadge(int unreadCount) {

        this.unreadCount = unreadCount;
    }

    public int getUnreadCount() {
        return unreadCount;
    }
}