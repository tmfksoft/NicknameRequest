package com.infermc.nicknamerequest;


import com.infermc.nicknamerequest.database.database;

import java.util.UUID;

public class PendingRequest {
    private String nickname;
    private boolean status = false;
    private boolean waiting = true;
    private UUID uuid = null;
    private int requestTime;

    private NicknameRequest plugin;
    private database db;

    public PendingRequest(NicknameRequest pl, database db, String nick) {
        this.nickname = nick;
        this.plugin = pl;
        this.db = db;
    }

    // Setters.
    public void setNickname(String nick) {
        nickname = nick;
    }

    @Deprecated
    public void setUUID(UUID id) { uuid = id; }
    public void setStatus(boolean s) {
        status = s;
    }
    public void setWaiting(boolean w) { waiting = w; }
    public void setRequestTime(int time) { requestTime = time; }

    // Getters
    public String getNickname() {
        return nickname;
    }
    @Deprecated
    public UUID getUUID() { return uuid; }
    public boolean getStatus() {
        return status;
    }
    public boolean isWaiting() {
        return waiting;
    }
}
