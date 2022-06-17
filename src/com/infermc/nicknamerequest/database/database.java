package com.infermc.nicknamerequest.database;

import com.infermc.nicknamerequest.PendingRequest;
import com.infermc.nicknamerequest.User;

import java.util.HashMap;
import java.util.UUID;

public interface database {
    User getUser(UUID uuid);
    HashMap<String, User> getUsers();
    void loadUsers();
    void saveUsers();
    void updateUser(User u);
    boolean nickTaken(String nick);
    User userViaName(String name);
}