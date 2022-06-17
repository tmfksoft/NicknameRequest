package com.infermc.nicknamerequest.commands;

import ca.pn.commands.Command;
import ca.pn.commands.CommandManager;
import com.infermc.nicknamerequest.NicknameRequest;
import com.infermc.nicknamerequest.User;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RemoveCommand extends Command {

    public RemoveCommand(CommandManager manager, Command parentCommand) {
        super(manager, parentCommand);

        this.commandName = "remove";
        this.commandDescription = "Removes your or another users nickname, if any";
        this.commandUsage = "[username]";

        this.requiredPermission = "nicknamerequest.remove.*";
    }

    @Override
    public boolean onCall(CommandSender sender, String[] args) {
        NicknameRequest plugin = (NicknameRequest) this.manager.parentPlugin;
        // Disable your nickname or another users.
        if (args.length <= 0) {
            // Self.
            if (sender.hasPermission("nicknamerequest.remove.self")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    sender.sendMessage(plugin.colourFormat("&bNickname removed!"));
                    player.displayName(null);
                    User u = plugin.db.getUser(player.getUniqueId());
                    u.setNickname(null);
                } else {
                    sender.sendMessage("Syntax: /nick remove [username]");
                }
            }
        } else {
            // Other user.
            if (sender.hasPermission("nicknamerequest.remove.others")) {
                if (plugin.db.userViaName(args[0]) != null) {
                    User u = plugin.db.userViaName(args[0]);
                    u.setNickname(null);
                    if (u.getPlayer() != null) u.getPlayer().displayName(null);
                    sender.sendMessage(plugin.colourFormat("&bNickname for " + u.getUsername() + " removed!"));
                } else {
                    sender.sendMessage(plugin.colourFormat("&cNo such user!"));
                }
            }
        }
        return true;
    }
}
