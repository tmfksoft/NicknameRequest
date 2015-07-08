package com.infermc.nicknamerequest;

import com.infermc.stale.PlayerExpiredEvent;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.logging.Logger;

/**
 * Created by Thomas on 08/07/2015.
 */
public class StaleAPIEvents implements Listener {
    private Main parent;

    public StaleAPIEvents(Main p) {
        this.parent = p;
    }

    public void onExpire(PlayerExpiredEvent event) {
        int expired = 0;
        List<OfflinePlayer> players = event.getPlayers();
        for (OfflinePlayer p : players) {
            User u = parent.userViaName(p.getName());
            if (u != null) {
                parent.getUsers().remove(u.getRequest().getUUID()); // I assume.
                expired++;
            }
        }
        Logger.getLogger("Minecraft").info(expired + " Nicknames and Requests expired out of " + players.size());
    }
}
