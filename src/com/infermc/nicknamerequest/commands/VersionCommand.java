package com.infermc.nicknamerequest.commands;

import ca.pn.commands.Command;
import ca.pn.commands.CommandManager;
import com.infermc.nicknamerequest.NicknameRequest;
import org.bukkit.command.CommandSender;

public class VersionCommand extends Command {

	public VersionCommand(CommandManager manager, Command parentCommand) {
		super(manager, parentCommand);

		this.commandName = "version";
		this.commandDescription = "Gets the current version of Nickname Request";
	}

	@Override
	public boolean onCall(CommandSender sender, String[] args) {
		NicknameRequest plugin = (NicknameRequest) this.manager.parentPlugin;
		sender.sendMessage(this.format(plugin.getName()+" v"+plugin.getDescription().getVersion()));
		sender.sendMessage(this.format("Easy nickname management for users and staff."));
		sender.sendMessage(this.format("Author: Thomas Burnett-Taylor (TMFKSOFT/MajesticFudgie)"));
		return true;
	}
}
