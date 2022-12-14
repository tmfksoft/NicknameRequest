package com.infermc.nicknamerequest.commands;

import ca.pn.commands.Command;
import ca.pn.commands.CommandManager;
import com.infermc.nicknamerequest.NicknameRequest;
import com.infermc.nicknamerequest.User;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Date;
import java.util.UUID;

public class RequestCommand extends Command {

    RequestCommand(CommandManager manager, Command parentCommand) {
        super(manager, parentCommand);

        this.requiredPermission = "nicknamerequest.request";
        this.commandName = "request";
        this.commandDescription = "Updates/Creates your nickname request";
        this.commandUsage = "[nick]";
    }

    public boolean onCall(CommandSender sender, String[] args) {
        Player player;
        NicknameRequest plugin = (NicknameRequest) this.manager.parentPlugin;
        if (sender.hasPermission("nicknamerequest.request")) {
            if (sender instanceof Player) {
                player = (Player) sender;
                UUID uid = player.getUniqueId();
                if (!plugin.db.getUser(uid).isRestricted()) {
                    if (args.length >= 2) {
                        String nick = StringUtils.join(ArrayUtils.subarray(args, 1, args.length), " ");
                        if (!plugin.db.nickTaken(nick)) {
                            if (!plugin.isValid(nick, player)) {
                                sender.sendMessage(plugin.colourFormat("&cDisallowed nickname! Your nickname contains disallowed formatting!"));
                                return true;
                            }
                            User u = plugin.db.getUser(uid);
                            // Do we notify staff?
                            if (u.getRequest() == null) {
                                // Yes, It's a new request.
                                u.newRequest(nick);
                                sender.sendMessage(plugin.colourFormat(plugin.getString("info-requested",null)));

                                TextComponent player_str = new TextComponent(plugin.colourFormat("&b" + player.getName()));
                                TextComponent req_str = new TextComponent(plugin.colourFormat("&b has requested the nickname '" + nick + "&r&b'"));
                                TextComponent accept = new TextComponent(plugin.colourFormat(" &b[ &aAccept &b|"));
                                TextComponent deny = new TextComponent(plugin.colourFormat(" &cDeny &b]"));

                                accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nick accept " + player.getName()));
                                accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Accept nickname").create()));

                                deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nick deny " + player.getName()));
                                deny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Deny nickname").create()));

                                // If theres perms groups. Add them to the message!
                                if (plugin.perms != null && plugin.chat != null) {
                                    String group;
                                    if (player != null) {
                                        group = plugin.chat.getPrimaryGroup(player);
                                    } else {
                                        // A feeble attempt.
                                        group = plugin.chat.getPrimaryGroup(plugin.getServer().getWorlds().get(0).getName(), plugin.getServer().getOfflinePlayer(player.getUniqueId()));
                                    }
                                    TextComponent text_group = new TextComponent(" (" + group + ")");

                                    player_str.addExtra(text_group);
                                }
                                player_str.addExtra(req_str);
                                player_str.addExtra(accept);
                                player_str.addExtra(deny);

                                for (Player p : plugin.getServer().getOnlinePlayers()) {
                                    if (p.hasPermission("nicknamerequest.notify")) {
                                        // TODO Fix this!
                                        p.spigot().sendMessage(player_str);
                                    }
                                }
                            } else {
                                // No, update a previous request. Remove the old one.
                                sender.sendMessage(plugin.colourFormat("&aNickname request updated."));

                                // Update it.
                                u.getRequest().setNickname(args[1]);
                            }
                            // TODO Remove and have requests update themselves in the DB
                            plugin.db.updateUser(u);
                            return true;
                        } else {
                            // Nick is in use?
                            sender.sendMessage(plugin.colourFormat("&cThat Nickname is already in use or has already been requested!"));
                        }
                    } else {
                        sender.sendMessage(plugin.colourFormat("&bUsage: /nick request nickname"));
                    }
                } else {
                    if (plugin.db.getUser(uid).getRestrictTime() == null) {
                        sender.sendMessage(plugin.colourFormat("&cSorry, you're not allowed to request a nickname indefinitely."));
                    } else {
                        Long wait = plugin.db.getUser(uid).getRestrictTime() - (new Date().getTime()/1000);
                        sender.sendMessage(plugin.colourFormat("&cSorry, you're not allowed to request a nickname, please wait "+wait+" seconds."));
                    }
                }
            } else {
                sender.sendMessage("This is a User Only Command!");
            }
            return true;
        }
        return false;
    }
}
