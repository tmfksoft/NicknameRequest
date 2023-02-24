package com.infermc.nicknamerequest.commands;

import ca.pn.commands.Command;
import ca.pn.commands.CommandManager;
import com.infermc.nicknamerequest.NicknameRequest;
import com.infermc.nicknamerequest.PendingRequest;
import com.infermc.nicknamerequest.User;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public class InfoCommand extends Command {

	public InfoCommand(CommandManager manager, Command parentCommand) {
		super(manager, parentCommand);

		this.commandName = "info";
		this.requiredPermission = "nicknamerequest.info";
	}

	@Override
	public boolean onCall(CommandSender sender, String[] args) {
		NicknameRequest plugin = (NicknameRequest) this.manager.parentPlugin;
 		if (sender instanceof Player && args.length == 0) {
			Player player = (Player) sender;
			UUID uid = player.getUniqueId();
			User u = plugin.db.getUser(uid);
			HashMap<String, Object> langFields = new HashMap<String, Object>();
			if (u.getNickname() == null) {
				sender.sendMessage(plugin.colourFormat(plugin.getString("info-self-no-nick",null)));
			} else {
				langFields.put("nick",u.getNickname());
				sender.sendMessage(plugin.colourFormat(plugin.getString("info-self-current-nick",langFields)));
			}
			if (u.getRequest() == null) {
				sender.sendMessage(plugin.colourFormat(plugin.getString("info-self-no-request",null)));
			} else {
				PendingRequest req = u.getRequest();
				langFields.put("nick",req.getNickname());
				sender.sendMessage(plugin.colourFormat(plugin.getString("info-self-current-request",langFields)));
			}
			if (!u.isRestricted()) {
				sender.sendMessage(plugin.colourFormat(plugin.getString("info-self-not-restricted",null)));
			} else {
				if (u.getRestrictTime() == null) {
					sender.sendMessage(plugin.colourFormat(plugin.getString("info-self-perm-restricted",null)));
				} else {
					Long wait = u.getRestrictTime() - (new Date().getTime() / 1000);
					langFields.put("time",wait);
					sender.sendMessage(plugin.colourFormat(plugin.getString("info-self-timed-restricted",langFields)));
				}
			}
		} else if (args.length >= 1) {
			if (sender.hasPermission("nicknamerequest.info.others")) {
				User u = plugin.db.userViaName(args[1]);
				if (u != null) {
					HashMap<String, Object> langFields = new HashMap<String, Object>();
					langFields.put("username", u.getUsername());
					if (u.getNickname() == null) {
						sender.sendMessage(plugin.colourFormat(plugin.getString("info-other-no-nick", langFields)));
					} else {
						langFields.put("nick", u.getNickname());
						sender.sendMessage(plugin.colourFormat(plugin.getString("info-other-current-nick", langFields)));
					}
					if (u.getRequest() == null) {
						sender.sendMessage(plugin.colourFormat(plugin.getString("info-other-no-request", langFields)));
					} else {
						PendingRequest req = u.getRequest();
						langFields.put("nick", req.getNickname());
						sender.sendMessage(plugin.colourFormat(plugin.getString("info-other-current-request", langFields)));
					}
					if (!u.isRestricted()) {
						sender.sendMessage(plugin.colourFormat(plugin.getString("info-other-not-restricted", langFields)));
					} else {
						if (u.getRestrictTime() == null) {
							sender.sendMessage(plugin.colourFormat(plugin.getString("info-other-perm-restricted", langFields)));
						} else {
							Long wait = u.getRestrictTime() - (new Date().getTime() / 1000);
							langFields.put("time", wait);
							sender.sendMessage(plugin.colourFormat(plugin.getString("info-other-timed-restricted", langFields)));
						}
					}
				} else {
					sender.sendMessage(plugin.colourFormat(plugin.getString("standard-invalid-user",null)));
				}
			} else {
				sender.sendMessage(plugin.colourFormat(plugin.getString("info-other-denied",null)));
			}
		} else {
			sender.sendMessage(plugin.colourFormat(plugin.getString("standard-user-only-cmd",null)));
		}
		return true;
	}
}
