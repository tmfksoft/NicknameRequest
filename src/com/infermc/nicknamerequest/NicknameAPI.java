package com.infermc.nicknamerequest;

import com.infermc.nicknamerequest.database.database;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class NicknameAPI {

    private NicknameRequest plugin;
    private database db;

    public NicknameAPI(NicknameRequest pl, database d) {
        this.plugin = pl;
        this.db = d;
    }

    public User getUser(UUID uuid) {
        return this.db.getUser(uuid);
    }
    public User getUser(Player player) {
        return this.db.getUser(player.getUniqueId());
    }
    public User getUser(OfflinePlayer player) {
        return this.db.getUser(player.getUniqueId());
    }
}
