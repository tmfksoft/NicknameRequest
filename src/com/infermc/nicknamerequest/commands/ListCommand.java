package com.infermc.nicknamerequest.commands;

import ca.pn.commands.Command;
import ca.pn.commands.CommandManager;
import com.infermc.nicknamerequest.NicknameRequest;
import com.infermc.nicknamerequest.PendingRequest;
import com.infermc.nicknamerequest.User;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class ListCommand extends Command {
    public ListCommand(CommandManager manager, Command parentCommand) {
        super(manager, parentCommand);

        this.commandName = "list";
        this.requiredPermission = "nicknamerequest.list";
    }

    @Override
    public boolean onCall(CommandSender sender, String[] args) {
        NicknameRequest plugin = (NicknameRequest) this.manager.parentPlugin;
        // Lists pending requests
        sender.sendMessage(plugin.colourFormat("&2Outstanding Nickname Requests:"));
        int count = 0;
        for (Map.Entry<String, User> u : plugin.db.getUsers().entrySet()) {
            if (u.getValue().getRequest() != null) {
                if (u.getValue().getRequest().isWaiting()) {
                    User user = u.getValue();
                    String nick = plugin.colourFormat(user.getRequest().getNickname() + "&r");

                    TextComponent req_str = new TextComponent(plugin.colourFormat("   &9- &r&f" + nick + "&9 by " + user.getUsername()));
                    TextComponent accept = new TextComponent(plugin.colourFormat(" &b[ &aAccept &b|"));
                    TextComponent deny = new TextComponent(plugin.colourFormat(" &cDeny &b]"));

                    accept.setClickEvent( new ClickEvent( ClickEvent.Action.RUN_COMMAND, "/nick accept "+user.getUsername() ) );
                    accept.setHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Accept nickname").create() ) );

                    deny.setClickEvent( new ClickEvent( ClickEvent.Action.RUN_COMMAND, "/nick deny "+user.getUsername() ) );
                    deny.setHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Deny nickname").create() ) );

                    // If theres perms groups. Add them to the message!
                    if (plugin.perms != null && plugin.chat != null) {
                        String group;
                        if (user.getPlayer() != null) {
                            group = plugin.chat.getPrimaryGroup(user.getPlayer());
                        } else {
                            // A feeble attempt.
                            PendingRequest pendingRequest = user.getRequest();
                            if (pendingRequest != null) {
                                OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(pendingRequest.getUUID());
                                String worldName = plugin.getServer().getWorlds().get(0).getName();
                                group = plugin.chat.getPrimaryGroup(worldName, offlinePlayer);
                            } else {
                                // Failed to get this request, skip.
                                continue;
                            }
                        }
                        TextComponent text_group = new TextComponent(" (" + group + ")");

                        req_str.addExtra(text_group);
                    }

                    if (sender instanceof Player) {
                        // If they're a player send the fancy string.
                        Player player = (Player) sender;

                        // Only for players, useless as server can't click :P
                        req_str.addExtra(accept);
                        req_str.addExtra(deny);

                        player.spigot().sendMessage(req_str);
                    } else {
                        sender.sendMessage(req_str.toLegacyText());
                    }
                    count++;
                }
            }
        }
        if (count == 0) sender.sendMessage(plugin.colourFormat("   &9There are no requests."));
        sender.sendMessage(plugin.colourFormat("&2Accept/Deny any request via &a/nick accept|deny username"));
        return true;
    }
}
