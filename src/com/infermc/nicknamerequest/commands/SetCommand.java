package com.infermc.nicknamerequest.commands;

import ca.pn.commands.Command;
import ca.pn.commands.CommandManager;
import com.infermc.nicknamerequest.NicknameRequest;
import com.infermc.nicknamerequest.User;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetCommand extends Command {


    public SetCommand(CommandManager manager, Command parentCommand) {
        super(manager, parentCommand);

        this.commandName = "set";
        this.commandDescription = "Sets your nick or another persons nickname";
        this.commandUsage = "[nick|username] (nick)";
        this.requiredPermission = "nicknamerequest.set";
    }

    @Override
    public boolean onCall(CommandSender sender, String[] args) {
        NicknameRequest plugin = (NicknameRequest) this.manager.parentPlugin;

        if (args.length == 1) {
            // Self
            if (sender instanceof Player) {
                Player player = (Player) sender;
                User u = plugin.db.getUser(player.getUniqueId());
                // Update users nicknamerequest.
                u.setNickname(args[0]);
                plugin.applyNickname(u);
                sender.sendMessage(plugin.colourFormat("&bYour nickname was changed to &r"+args[0]));
            } else {
                sender.sendMessage("Syntax: /nick set username nickname");
            }
        } else if (args.length >= 2) {
            // Other
            if (plugin.db.userViaName(args[0]) != null) {
                User u = plugin.db.userViaName(args[0]);
                u.setNickname(args[1]);

                plugin.applyNickname(u);

                sender.sendMessage(plugin.colourFormat("&bThe nickname of "+u.getUsername()+" was changed to "+args[1]));
            } else {
                sender.sendMessage(plugin.colourFormat("&cNo such user!"));
            }
        } else {
            sender.sendMessage("Syntax: /nick set [username] nickname");
        }
        return true;
    }
}
