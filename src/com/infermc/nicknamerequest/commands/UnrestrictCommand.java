package com.infermc.nicknamerequest.commands;

import ca.pn.commands.Command;
import ca.pn.commands.CommandManager;
import com.infermc.nicknamerequest.NicknameRequest;
import com.infermc.nicknamerequest.User;
import org.bukkit.command.CommandSender;

public class UnrestrictCommand extends Command {


	public UnrestrictCommand(CommandManager manager, Command parentCommand) {
		super(manager, parentCommand);

		this.commandName = "unrestrict";
		this.commandDescription = "Unrestrict a users ability to request a nickname";
		this.commandUsage = "[username]";

		this.requiredPermission = "nicknamerequest.restrict";
	}

	@Override
	public boolean onCall(CommandSender sender, String[] args) {
		NicknameRequest plugin = (NicknameRequest) this.manager.parentPlugin;
		// TODO
		if (args.length >= 2) {
			if (plugin.db.userViaName(args[1]) != null) {
				User u = plugin.db.userViaName(args[1]);
				u.setRestricted(false);
				sender.sendMessage(plugin.colourFormat("&aNickname request access for " + u.getUsername() + " is no longer restricted."));
			} else {
				sender.sendMessage(plugin.colourFormat("&cNo such user!"));
			}
		} else {
			sender.sendMessage("Syntax: /nick unrestrict username");
		}
		return true;
	}
}
