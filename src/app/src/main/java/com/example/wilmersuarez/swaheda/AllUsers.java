package com.example.wilmersuarez.swaheda;

public class AllUsers {
    public String user_image;
    public String user_name;

    public AllUsers() {} // Needed for Firebase

    public AllUsers(String user_image, String user_name) {
        this.user_image = user_image;
        this.user_name = user_name;
    }

    public String getUser_image() {
        return user_image;
    }

    public void setUser_image(String user_image) {
        this.user_image = user_image;
    }

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }
}
