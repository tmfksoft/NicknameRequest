package com.infermc.nicknamerequest;

import org.bukkit.entity.Player;

public class User {
    private NickRequest request = null;
    private String nickname = null;
    private String username = null;
    private Player player = null;

    // Setters
    public void setRequest(NickRequest r) {
        request = r;
    }
    public void setNickname(String n) {
        nickname = n;
    }
    public void setUsername(String u) {
        username = u;
    }
    public void setPlayer(Player p) {
        player = p;
    }

    // Getters
    public NickRequest getRequest() {
        return request;
    }
    public String getNickname() {
        return nickname;
    }
    public String getUsername() {
        return username;
    }
    public Player getPlayer() {
        return player;
    }
}
