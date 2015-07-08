package com.infermc.nicknamerequest;


public class NickRequest {
    private String nickname;
    private boolean status = false;
    private boolean waiting = true;

    NickRequest(String who, String nick) {
        this.nickname = nick;
    }

    // Setters.
    public void setNickname(String nick) {
        nickname = nick;
    }
    public void setStatus(boolean s) {
        status = s;
    }
    public void setWaiting(boolean w) {
        waiting = w;
    }

    // Getters
    public String getNickname() {
        return nickname;
    }
    public boolean getStatus() {
        return status;
    }
    public boolean isWaiting() {
        return waiting;
    }
}
