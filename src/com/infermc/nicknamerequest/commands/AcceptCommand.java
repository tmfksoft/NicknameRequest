package com.infermc.nicknamerequest.commands;

import ca.pn.commands.Command;
import ca.pn.commands.CommandManager;
import com.infermc.nicknamerequest.NicknameRequest;
import com.infermc.nicknamerequest.User;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Date;

public class AcceptCommand extends Command {

    public AcceptCommand(CommandManager manager, Command parentCommand) {
        super(manager, parentCommand);

        this.commandName = "accept";
        this.commandUsage = "[username]";
        this.commandDescription = "Accepts a users nickname request";
    }

    @Override
    public boolean onCall(CommandSender sender, String[] args) {
        NicknameRequest plugin = (NicknameRequest) this.manager.parentPlugin;
        if (args.length >= 2) {
            if (plugin.db.userViaName(args[1]) != null) {
                User u = plugin.db.userViaName(args[1]);
                if (u.getRequest() != null) {
                    u.getRequest().setWaiting(false);
                    u.getRequest().setStatus(true);
                    if (!u.isRestricted() && plugin.getConfig().getLong("accept-cooldown",0) > 0) {
                        Long rTime = (new Date().getTime()/1000) + plugin.getConfig().getLong("accept-cooldown");
                        u.setRestrictTime(rTime);
                        u.setRestricted(true);
                    }
                    sender.sendMessage(plugin.colourFormat("&aNickname successfully accepted."));

                    for (Player p : plugin.getServer().getOnlinePlayers()) {
                        if (p.hasPermission("nicknamerequest.notify")) {
                            p.sendMessage(plugin.colourFormat("&bThe nickname '&r&f"+u.getRequest().getNickname()+ "&b' by "+u.getUsername()+" has been &a&laccepted&r&b by "+sender.getName()));
                        }
                    }
                    // THIS NEEDS SORTING
                    // TODO
                    if (plugin.getServer().getPlayer(u.getUUID()) != null) plugin.customJoin(plugin.getServer().getPlayer(u.getUUID()));
                } else {
                    sender.sendMessage(plugin.colourFormat("&cThat user hasn't requested a nickname!"));
                }
            } else {
                sender.sendMessage(plugin.colourFormat("&cNo such user!"));
            }
        } else {
            sender.sendMessage(plugin.colourFormat("&cSyntax: /nick accept username"));
        }
        return true;
    }
}
