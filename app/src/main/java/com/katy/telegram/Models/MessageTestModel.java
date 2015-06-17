package com.katy.telegram.Models;


import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MessageTestModel {
    private int id;
    private String image;
    private Boolean isGroupChat;
    private String user_name;
    private Boolean isRead;
    private String time;
    private Boolean isRecentlyOnline;
    private String last_message;
    private int number_of_unread_messages;
    private String music_path;

    public MessageTestModel(int id, Boolean isGroupChat, String user_name, Boolean isRead, Boolean isRecentlyOnline,
                            String last_message, int number_of_unread_messages, String music_path){
        this.id = id;
        this.isGroupChat = isGroupChat;
        this.user_name = user_name;
        this.isRead = isRead;
        this.isRecentlyOnline = isRecentlyOnline;
        this.last_message = last_message;
        this.number_of_unread_messages = number_of_unread_messages;
        this.time = new SimpleDateFormat("HH:mm a").format(Calendar.getInstance().getTime());
        this.music_path = music_path;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Boolean getIsGroupChat() {
        return isGroupChat;
    }

    public void setIsGroupChat(Boolean isGroupChat) {
        this.isGroupChat = isGroupChat;
    }

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Boolean getIsRecentlyOnline() {
        return isRecentlyOnline;
    }

    public void setIsRecentlyOnline(Boolean isRecentlyOnline) {
        this.isRecentlyOnline = isRecentlyOnline;
    }

    public String getLast_message() {
        return last_message;
    }

    public void setLast_message(String last_message) {
        this.last_message = last_message;
    }

    public int getNumber_of_unread_messages() {
        return number_of_unread_messages;
    }

    public void setNumber_of_unread_messages(Integer number_of_unread_messages) {
        this.number_of_unread_messages = number_of_unread_messages;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        return (((MessageTestModel)o).getId() == this.getId());
    }

    public String getMusic_path() {
        return music_path;
    }

    public void setMusic_path(String music_path) {
        this.music_path = music_path;
    }
}
