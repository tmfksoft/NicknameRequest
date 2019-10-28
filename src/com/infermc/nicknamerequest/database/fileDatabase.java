package com.infermc.nicknamerequest.database;

import com.infermc.nicknamerequest.NicknameRequest;
import com.infermc.nicknamerequest.PendingRequest;
import com.infermc.nicknamerequest.User;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class fileDatabase extends database {
    // Some needed vars
    private NicknameRequest parent;

    // Files
    private File usersFile;

    // User storage
    private HashMap<String,User> users = new HashMap<String,User>();

    public fileDatabase(NicknameRequest p) {
        parent = p;
        usersFile = new File(p.getDataFolder()+"/users.yml");
        p.getLogger().info("NicknameRequest is using FlatFile");
    }

    @Override
    public User getUser(UUID uuid) {
        String uid = uuid.toString();
        if (users.containsKey(uid)) return users.get(uid);
        return null;
    }
    public HashMap<String, User> getUsers(){
        return users;
    }

    // Load Users
    public void loadUsers() {
        // Clear any loaded requests and load new ones.
        users.clear();
        if (usersFile.exists()) {
            // Read it! C:
            //parent.getLogger().info("Loading all users.");
            YamlConfiguration requested = new YamlConfiguration();
            try {
                requested.load(usersFile);
                for (String s : requested.getRoot().getKeys(false)) {
                    ConfigurationSection section = requested.getRoot().getConfigurationSection(s);
                    if (section.contains("username")) {

                        // User(NicknameRequest pl, database db, UUID u, String username, String nickname, boolean restricted, Long restrictTime)

                        String nickname = section.getString("nickname", null);
                        if (nickname.equals("")) nickname = null;
                        String username = section.getString("username");
                        UUID uid = UUID.fromString(s);

                        boolean restricted = section.getBoolean("restricted",false);
                        long restrictTime = section.getLong("restrictTime",0L);

                        // Does their user file contain a request?
                        PendingRequest req = null;
                        if (section.getConfigurationSection("request") != null) {
                            ConfigurationSection requestSection = section.getConfigurationSection("request");
                            if (requestSection.contains("nickname")) {
                                req = new PendingRequest(parent, this, requestSection.getString("nickname"));
                                if (requestSection.contains("waiting")) req.setWaiting(requestSection.getBoolean("waiting"));
                                if (requestSection.contains("status")) req.setStatus(requestSection.getBoolean("status"));
                            }
                        }
                        User u = new User(parent,this,uid,username,nickname, restricted, restrictTime, req);
                        users.put(s,u);
                    }
                }
                //parent.getLogger().info("Loaded "+users.size()+" users.");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InvalidConfigurationException e) {
                e.printStackTrace();
            }
        }
    }
    // Save current user nicknames to file.
    public void saveUsers() {
        YamlConfiguration loadedUsers = new YamlConfiguration();
        int count = 0;
        for (Map.Entry<String,User> e : users.entrySet()) {
            count++;

            ConfigurationSection userSection = loadedUsers.createSection(e.getKey());

            // To be shortened down.
            if (e.getValue().getUsername() != null) userSection.set("username",e.getValue().getUsername());
            if (e.getValue().getNickname() != null) userSection.set("nickname",e.getValue().getNickname());

            if (e.getValue().isRestricted()) {
                userSection.set("restricted",true);
                userSection.set("restrictTime",e.getValue().getRestrictTime());
            }

            // Got a request? Goody
            if (e.getValue().getRequest() != null) {
                ConfigurationSection requestSection = userSection.createSection("request");
                requestSection.set("nickname",e.getValue().getRequest().getNickname());
                requestSection.set("status",e.getValue().getRequest().getStatus());
                requestSection.set("waiting",e.getValue().getRequest().isWaiting());
            }
        }

        try {
            //parent.getLogger().info("Saved all "+count+" users.");
            loadedUsers.save(usersFile);
        } catch (IOException e) {
            parent.getLogger().info("Unable to save nicknames to file :( Data may be lost!");
            e.printStackTrace();
        }
    }

    @Override
    public void updateUser(User u) {
        // Save everything to disk (Best we can do)
        this.parent.getLogger().info("Update user called on "+u.getUUID());
        users.put(u.getUUID().toString(),u);
        saveUsers();
    }

    public boolean nickTaken(String nick) {
        // Is it already in use?
        for (Map.Entry<String,User> u : users.entrySet()) {
            if (u.getValue().getNickname() != null) {
                // Check if this users nickname matches.
                if (u.getValue().getNickname().equalsIgnoreCase(nick)) return true;
            }
            if (u.getValue().getRequest() != null) {
                // Check if this users request matches.
                String reqNick = ChatColor.stripColor(parent.colourFormat(u.getValue().getRequest().getNickname()));
                String prepNick = ChatColor.stripColor(parent.colourFormat(nick));
                if (reqNick.equalsIgnoreCase(prepNick)) return true;
            }
        }

        return false;
    }

    public User userViaName(String name) {
        for (Map.Entry<String,User> u : users.entrySet()) {
            if (u.getValue().getUsername().equalsIgnoreCase(name)) return u.getValue();
        }
        return null;
    }
}
