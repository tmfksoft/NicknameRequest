package com.infermc.nicknamerequest;


import java.util.UUID;

public class NickRequest {
    private String nickname;
    private boolean status = false;
    private boolean waiting = true;
    private UUID uuid;

    NickRequest(String who, String nick) {
        this.uuid = UUID.fromString(who);
        this.nickname = nick;
    }

    // Setters.
    public void setNickname(String nick) {
        nickname = nick;
    }
    public void setUUID(UUID id) { uuid = id; }
    public void setStatus(boolean s) {
        status = s;
    }
    public void setWaiting(boolean w) { waiting = w; }

    // Getters
    public String getNickname() {
        return nickname;
    }
    public UUID getUUID() { return uuid; }
    public boolean getStatus() {
        return status;
    }
    public boolean isWaiting() {
        return waiting;
    }
}
