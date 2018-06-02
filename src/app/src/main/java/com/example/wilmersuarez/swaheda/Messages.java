package com.example.wilmersuarez.swaheda;

public class Messages {
    private String message, type;
    private boolean seen;
    private String from;
    private String receiverName;
    private long time;

    public Messages() {}

    public Messages(String message, String type, boolean seen, String from, long time) {
        this.message = message;
        this.type = type;
        this.seen = seen;
        this.from = from;
        this.time = time;
    }


    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
