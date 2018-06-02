package com.example.wilmersuarez.swaheda;

public class AllFriends {
    public String date;

    public AllFriends() {} // Needed for Firebase

    public AllFriends(String date) {
        this.date = date;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
