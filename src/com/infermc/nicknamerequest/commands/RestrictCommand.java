package com.infermc.nicknamerequest.commands;

import ca.pn.commands.Command;
import ca.pn.commands.CommandManager;
import com.infermc.nicknamerequest.NicknameRequest;
import com.infermc.nicknamerequest.User;
import org.bukkit.command.CommandSender;

import java.util.Date;

public class RestrictCommand extends Command {
	public RestrictCommand(CommandManager manager, Command parentCommand) {
		super(manager, parentCommand);

		this.commandName = "restrict";
		this.commandDescription = "Restricts a users ability to request nicknames for an optional amount of time.";
		this.commandUsage = "[username] [time]";

		this.requiredPermission= "nicknamerequest.restrict";
	}

	@Override
	public boolean onCall(CommandSender sender, String[] args) {
		NicknameRequest plugin = (NicknameRequest) this.manager.parentPlugin;

		// THIS NEEDS SORTING
		// TODO
		if (args.length >= 2) {
			if (plugin.db.userViaName(args[1]) != null) {
				User u = plugin.db.userViaName(args[1]);
				u.setRestricted(true);
				Long rTime = null;
				if (args.length == 3) {
					rTime = plugin.parseTime(args[2]);
					if (rTime == null) {
						sender.sendMessage(plugin.colourFormat("&cInvalid time format, units of time must be singular and end in s(econds), m(inutes), h(hours) or d(days)! E.g. 24h"));
						return true;
					}
					long curTime = (new Date().getTime() / 1000)+rTime;
					u.setRestrictTime(curTime);
				}
				if (rTime == null) {
					sender.sendMessage(plugin.colourFormat("&aNickname request access for " + u.getUsername() + " is now restricted."));
				} else {
					sender.sendMessage(plugin.colourFormat("&aNickname request access for " + u.getUsername() + " is now restricted for "+rTime+" seconds."));
				}
			} else {
				sender.sendMessage(plugin.colourFormat("&cNo such user!"));
			}
		}
		return true;
	}
}
