package com.infermc.nicknamerequest.commands;

import ca.pn.commands.Command;
import ca.pn.commands.CommandManager;
import com.infermc.nicknamerequest.NicknameRequest;
import com.infermc.nicknamerequest.User;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class CancelCommand extends Command {

    private NicknameRequest plugin;

    CancelCommand(CommandManager manager, Command parentCommand) {
        super(manager, parentCommand);

        this.requiredPermission = "nicknamerequest.cancel";

        this.commandName = "cancel";
        this.commandDescription = "Cancels your nickname request, if any";
        this.commandUsage = "";
    }

    public boolean onCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            UUID uid = player.getUniqueId();
            User u = this.plugin.db.getUser(uid);
            if (u.getRequest() == null) {
                sender.sendMessage(this.plugin.colourFormat("&bYou've not made a nickname request!"));
            } else {
                u.setRequest(null);
                sender.sendMessage(this.plugin.colourFormat("&aNickname Request Cancelled."));
            }
        } else {
            sender.sendMessage(this.plugin.colourFormat("&cThis is a User Only Command!"));
        }
        return true;
    }
}
