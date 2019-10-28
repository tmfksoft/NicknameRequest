package com.infermc.nicknamerequest;

import com.infermc.nicknamerequest.database.database;
import com.infermc.stale.PlayerExpiredEvent;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.logging.Logger;

public class StaleAPIEvents implements Listener {
    private NicknameRequest parent;
    private database db;

    public StaleAPIEvents(NicknameRequest p, database db) {
        this.db = db;
        this.parent = p;
    }

    @EventHandler
    public void onExpire(PlayerExpiredEvent event) {
        int expired = 0;
        List<OfflinePlayer> players = event.getPlayers();
        for (OfflinePlayer p : players) {
            User u = this.db.userViaName(p.getName());
            if (u != null) {
                this.db.getUsers().remove(u.getUUID().toString()); // I assume.
                expired++;
            }
        }
        Logger.getLogger("Minecraft").info(expired + " Nicknames and Requests expired out of " + players.size());
    }
}
