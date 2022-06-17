package com.infermc.nicknamerequest.commands;

import ca.pn.commands.Command;
import ca.pn.commands.CommandManager;
import com.infermc.nicknamerequest.NicknameRequest;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class StatusCommand extends Command {

    public StatusCommand(CommandManager manager, Command parentCommand) {
        super(manager, parentCommand);

        this.requiredPermission = "nicknamerequest.status";
        this.commandName = "status";
        this.commandDescription = "Checks the status of your nickname request, if any.";
        this.commandUsage = "";

    }

    public boolean onCall(CommandSender sender, String[] args) {
        Player player;
        NicknameRequest plugin = (NicknameRequest) this.manager.parentPlugin;
        if (sender instanceof Player) {
            player = (Player) sender;
            UUID playerUuid = player.getUniqueId();
            if (plugin.db.getUser(playerUuid).getRequest() != null) {
                plugin.requestStatus(player);
            } else {
                sender.sendMessage(plugin.colourFormat("&bYou don't have a nickname requested currently."));
            }
        } else {
            sender.sendMessage(plugin.colourFormat("&bOnly users may use this command presently."));
        }
        return true;
    }
}
