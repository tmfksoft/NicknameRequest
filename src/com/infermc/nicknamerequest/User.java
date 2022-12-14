package com.infermc.nicknamerequest;

import com.infermc.nicknamerequest.database.database;
import org.bukkit.entity.Player;

import java.util.Date;
import java.util.UUID;

public class User {
    private PendingRequest request = null;
    private String nickname = null;
    private UUID uuid = null;
    private String username = null;
    private Player player = null;
    private boolean restricted = false;
    private Long restrictTime = 0L;

    private NicknameRequest plugin;
    private database db;
    public User(NicknameRequest pl, database db, UUID u, String username, String nickname, boolean restricted, Long restrictTime, PendingRequest r) {
        this.plugin = pl;
        this.db = db;
        this.uuid = u;
        this.username = username;
        this.nickname = nickname;
        this.restricted  = restricted;
        this.restrictTime = restrictTime;
        this.request = r;
    }

    // Setters
    public void setRequest(PendingRequest r) {
        request = r;
        db.updateUser(this);
    }
    protected void putRequest(PendingRequest r) {
        this.request = r;
    }

    public void setNickname(String n) {
        nickname = n;
        db.updateUser(this);
        if (this.player != null) {
            if (n != null) {
                this.plugin.applyNickname(this);
            } else {
                this.player.displayName(null);
            }
        }
    }
    public void setUsername(String u) {
        username = u;
        db.updateUser(this);
    }
    public void setRestricted(boolean r) {
        if (restricted != r) {
            restricted = r;
            db.updateUser(this);
        }
    }
    public void setRestrictTime(Long rt) {
        if (restrictTime != rt) {
            restrictTime = rt;
            db.updateUser(this);
        }
    }
    public void setUUID(UUID id) {
        if (this.uuid != id) {
            uuid = id;
            db.updateUser(this);
        }
    }

    // Getters
    public PendingRequest getRequest() {
        return request;
    }
    public String getNickname() {
        return nickname;
    }
    public String getUsername() {
        return username;
    }
    public Player getPlayer() {
        for (Player player : this.plugin.getServer().getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(this.username)) {
                return player;
            }
        }
        return null;
    }
    public boolean getRestricted() { return restricted; }
    public Long getRestrictTime() { return restrictTime; }
    public UUID getUUID() { return uuid; }

    // Magic
    public PendingRequest newRequest(String nickname) {
        PendingRequest r = new PendingRequest(this.plugin,this.db,nickname, this.uuid);
        this.setRequest(r);
        return r;
    }

    public boolean isRestricted() {
        if (restrictTime == null) {
            return restricted;
        } else {
            if (restricted) {
                long curTime = new Date().getTime() / 1000;
                if (curTime >= restrictTime) {
                    restrictTime = null;
                    restricted = false;
                    return false;
                } else {
                    return true;
                }
            }
        }
        return false;
    }
}
