package com.katy.telegram.Models.Messages;

import java.util.Date;

public class NewDaySeparatorNotification extends ChatEvent {
    private Date date;

    public NewDaySeparatorNotification(Date date) {

        this.date = date;
    }

    public Date getDate() {
        return date;
    }
}
