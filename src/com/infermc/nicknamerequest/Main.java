package com.infermc.nicknamerequest;

import com.infermc.stale.PlayerDataExpired;
import com.infermc.stale.StaleAPI;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.security.Permission;
import java.util.*;

public class Main extends JavaPlugin implements Listener {
    // Files
    private File usersFile = new File(getDataFolder()+"/users.yml");

    // Temporary Storage
    private HashMap<String,User> users = new HashMap<String,User>();

    // Providers
    Permission perms;
    Chat chat;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("NicknameRequest Enabled ~ Yeey");

        // Load Saved Data
        loadUsers();
        if(!setupVault()){
            getLogger().info("Unable to load Vault, Group names won't be shown in the request list!");
        }
        if (getServer().getPluginManager().getPlugin("StaleAPI") == null) {
            getLogger().info("Unable to load StaleAPI, Nicknames and requests will never expire. (NicknameRequest could start to lag)");
        } else {
            getLogger().info("StaleAPI Detected! Nicknames/Requests will expire over time.");
        }

        // Process currently logged in users (aka /reload)
        for (Player p : getServer().getOnlinePlayers()) {
            customJoin(p);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabled - Running Save Commands.");
        saveUsers();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Commands!
        if (args.length >= 1) {
            Player player;
            if (args[0].equalsIgnoreCase("status") && sender.hasPermission("nicknamerequest.status")) {
                if (sender instanceof Player) {
                    player = (Player) sender;
                    if (users.get(player.getUniqueId().toString()).getRequest() != null) {
                        requestStatus(player);
                    } else {
                        sender.sendMessage(colourFormat("&bYou don't have a nickname requested currently."));
                    }
                } else {
                    sender.sendMessage(colourFormat("&bOnly users may use this command presently."));
                }
                return true;
            } else if (args[0].equalsIgnoreCase("request") && sender.hasPermission("nicknamerequest.request")) {
                if (sender instanceof Player) {
                    player = (Player) sender;
                    String uid = player.getUniqueId().toString();
                    if (args.length == 2) {
                        if (!nickTaken(args[1])) {
                            if (!isValid(args[1],player)) {
                                sender.sendMessage(colourFormat("&cDisallowed nickname! Your nickname contains disallowed formatting!"));
                                return true;
                            }
                            NickRequest req = new NickRequest(uid,args[1]);
                            // Do we notify staff?
                            if (users.get(uid).getRequest() == null) {
                                // Yes, It's a new request.
                                sender.sendMessage("Nickname requested!");
                                for (Player p : getServer().getOnlinePlayers()) {
                                    if (p.hasPermission("nicknamerequest.notify")) {
                                        p.sendMessage(colourFormat("&b"+player.getName() + " has requested the nickname " + args[1]));
                                    }
                                }

                                // Add it.
                                users.get(uid).setRequest(req);
                            } else {
                                // No, update a previous request. Remove the old one.
                                sender.sendMessage(colourFormat("&aNickname request updated."));

                                // Update it.
                                users.get(uid).getRequest().setNickname(args[1]);
                            }
                            return true;
                        } else {
                            // Nick is in use?
                            sender.sendMessage(colourFormat("&cThat Nickname is already in use or has already been requested!"));
                        }
                    } else {
                        sender.sendMessage(colourFormat("&bUsage: /nick request nickname"));
                    }

                } else {
                    sender.sendMessage("This is a User Only Command!");
                }
                return true;
            } else if (args[0].equalsIgnoreCase("cancel")) {
                if (sender instanceof Player) {
                    player = (Player) sender;
                    String uid = player.getUniqueId().toString();
                    if (users.get(uid).getRequest() == null) {
                        sender.sendMessage(colourFormat("&bYou've not made a nickname request!"));
                    } else {
                        users.get(uid).setRequest(null);
                        sender.sendMessage(colourFormat("&aNickname Request Cancelled."));
                    }
                } else {
                    sender.sendMessage(colourFormat("&cThis is a User Only Command!"));
                }
                return true;
            } else if (args[0].equalsIgnoreCase("accept") && sender.hasPermission("nicknamerequest.accept")) {
                if (args.length >= 2) {
                    if (userViaName(args[1]) != null) {
                        User u = userViaName(args[1]);
                        if (u.getRequest() != null) {
                            u.getRequest().setWaiting(false);
                            u.getRequest().setStatus(true);
                            sender.sendMessage(colourFormat("&aNickname successfully accepted."));

                            for (Player p : getServer().getOnlinePlayers()) {
                                if (p.hasPermission("nicknamerequest.notify")) {
                                    p.sendMessage(colourFormat("&bThe nickname '&r&f"+u.getRequest().getNickname()+ "&b' by "+u.getUsername()+" has been &a&laccepted&r&b by "+sender.getName()));
                                }
                            }

                            if (u.getPlayer() != null) customJoin(u.getPlayer());
                        } else {
                            sender.sendMessage(colourFormat("&cThat user hasn't requested a nickname!"));
                        }
                    } else {
                        sender.sendMessage(colourFormat("&cNo such user!"));
                    }
                } else {
                    sender.sendMessage(colourFormat("&cSyntax: /nick accept username"));
                }
                return true;
            } else if (args[0].equalsIgnoreCase("deny") && sender.hasPermission("nicknamerequest.deny")) {
                if (args.length >= 2) {
                    if (userViaName(args[1]) != null) {
                        User u = userViaName(args[1]);
                        if (u.getRequest() != null) {
                            u.getRequest().setWaiting(false);
                            u.getRequest().setStatus(false);
                            sender.sendMessage(colourFormat("&aNickname successfully denied."));
                            if (u.getPlayer() != null) customJoin(u.getPlayer());

                            for (Player p : getServer().getOnlinePlayers()) {
                                if (p.hasPermission("nicknamerequest.notify")) {
                                    p.sendMessage(colourFormat("&bThe nickname '&r&f"+u.getRequest().getNickname()+ "&b' by "+u.getUsername()+" has been &c&ldenied&r&b by "+sender.getName()));
                                }
                            }

                        } else {
                            sender.sendMessage(colourFormat("&cThat user hasn't requested a nickname!"));
                        }
                    } else {
                        sender.sendMessage(colourFormat("&cNo such user!"));
                    }
                } else {
                    sender.sendMessage(colourFormat("&cSyntax: /nick deny username"));
                }
                return true;
            } else if (args[0].equalsIgnoreCase("set") && sender.hasPermission("nicknamerequest.set")) {
                if (args.length == 2) {
                    // Self
                    if (sender instanceof Player) {
                        player = (Player) sender;
                        User u = users.get(player.getUniqueId().toString());

                        // Update users nicknamerequest.
                        u.setNickname(args[1]);
                        player.setDisplayName(colourFormat(args[1]+"&r"));
                        sender.sendMessage("Your nickname was changed to "+colourFormat(args[1]));
                    } else {
                        sender.sendMessage("Syntax: /nick set username nickname");
                    }
                } else if (args.length >= 3) {
                    // Other
                    if (userViaName(args[1]) != null) {
                        User u = userViaName(args[1]);

                        u.setNickname(args[2]);
                        if (u.getPlayer() != null) {
                            u.getPlayer().setDisplayName(colourFormat(args[2]+"&r"));
                        }
                        sender.sendMessage(colourFormat("&bThe nickname of "+u.getUsername()+" was changed to "+args[2]));
                    } else {
                        sender.sendMessage(colourFormat("&cNo such user!"));
                    }
                } else {
                    sender.sendMessage("Syntax: /nick set [username] nickname");
                }
                return true;
            } else if (args[0].equalsIgnoreCase("list") && sender.hasPermission("nicknamerequest.list")) {
                // Lists pending requests
                sender.sendMessage(colourFormat("&2Outstanding Nickname Requests:"));
                int count = 0;
                for (Map.Entry<String,User> u : users.entrySet()) {
                    if (u.getValue().getRequest() != null) {
                        if (u.getValue().getRequest().isWaiting()) {
                            User user = u.getValue();
                            String nick = colourFormat(user.getRequest().getNickname() + "&r");
                            if (perms != null && chat != null) {
                                String group;
                                if (user.getPlayer() != null) {
                                    group = chat.getPrimaryGroup(user.getPlayer());
                                } else {
                                    // A feeble attempt.
                                    group = chat.getPrimaryGroup(getServer().getWorlds().get(0).getName(),getServer().getOfflinePlayer(user.getRequest().getUUID()));
                                }
                                sender.sendMessage(colourFormat("   &9- &r&f" + nick + "&9 by " + user.getUsername()+" ("+group+")"));
                            } else {
                                sender.sendMessage(colourFormat("   &9- &r&f" + nick + "&9 by " + user.getUsername()));
                            }
                            count++;
                        }
                    }
                }
                if (count == 0) sender.sendMessage(colourFormat("   &9There are no requests."));
                sender.sendMessage(colourFormat("&2Accept/Deny any request via &a/nick accept|deny username"));
                return true;
            } else if (args[0].equalsIgnoreCase("remove")) {
                // Disable your nickname or another users.
                if (args.length == 1) {
                    // Self.
                    if (sender.hasPermission("nicknamerequest.remove.self")) {
                        if (sender instanceof Player) {
                            player = (Player) sender;
                            sender.sendMessage(colourFormat("&bNickname removed!"));
                            player.setDisplayName(null);
                            users.get(player.getUniqueId().toString()).setNickname(null);
                        } else {
                            sender.sendMessage("Syntax: /nick remove username");
                        }
                    }
                } else {
                    // Other user.
                    if (sender.hasPermission("nicknamerequest.remove.others")) {
                        if (userViaName(args[1]) != null) {
                            User u = userViaName(args[1]);
                            u.setNickname(null);
                            if (u.getPlayer() != null) u.getPlayer().setDisplayName(null);
                            sender.sendMessage(colourFormat("&bNickname for " + u.getUsername() + " removed!"));
                        } else {
                            sender.sendMessage(colourFormat("&cNo such user!"));
                        }
                    }
                }
                return true;
            } else if (args[0].equalsIgnoreCase("version")) {
                sender.sendMessage(colourFormat("NicknameRequest version 0.3"));
                sender.sendMessage(colourFormat("Nickname Requester - Easy nicknaming for users and staff."));
                sender.sendMessage(colourFormat("Author: Thomas Edwards (TMFKSOFT/MajesticFudgie)"));

            }
        } else {
            return false;
        }
        return false;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        getLogger().info("Player joined");
        customJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        customQuit(event.getPlayer());
    }

    // Custom functions
    public void customJoin(Player player) {
        String uid = player.getUniqueId().toString();
        // If we loaded their nick, apply it!
        if (users.containsKey(uid)) {
            users.get(uid).setUsername(player.getName());
            if (users.get(uid).getNickname() != null) {
                String nickname = users.get(uid).getNickname();
                player.setDisplayName(colourFormat(nickname + "&r"));
            }
        } else {
            User u = new User();
            u.setUsername(player.getName());
            users.put(player.getUniqueId().toString(),u);
        }

        // Set their player object.
        users.get(uid).setPlayer(player);

        requestStatus(player);
    }

    public void customQuit(Player player) {
        // Remove their player.
        users.get(player.getUniqueId().toString()).setPlayer(null);
    }

    public void requestStatus(Player player) {
        // Check if they've got a pending request.
        NickRequest nr = users.get(player.getUniqueId().toString()).getRequest();
        if (nr == null) return;

        // Ensure its not already waiting
        String nick = colourFormat(nr.getNickname() + "&r");
        if (!nr.isWaiting()) {
            // Was it approved?
            if (nr.getStatus()) {
                player.sendMessage(colourFormat("&aYour Nickname '"+nick+"&a' was approved and applied!"));
                player.setDisplayName(nick);
                // Update the nicknames.
                users.get(player.getUniqueId().toString()).setNickname(nr.getNickname());
            } else {
                player.sendMessage(colourFormat("&cYour Nickname '"+nick+"&c' was denied and wasn't applied."));
            }
            // Remove the request.
            users.get(player.getUniqueId().toString()).setRequest(null);
        } else {
            player.sendMessage(colourFormat("&bYour nickname '"+nick+"&b' is still awaiting approval."));
            player.sendMessage(colourFormat("&bCancel your Nickname Request via &a/nick cancel"));
        }
    }

    // Load Users
    public void loadUsers() {
        // Clear any loaded requests and load new ones.
        users.clear();
        if (usersFile.exists()) {
            // Read it! C:
            getLogger().info("Loading all users.");
            YamlConfiguration requested = new YamlConfiguration();
            try {
                requested.load(usersFile);
                for (String s : requested.getRoot().getKeys(false)) {
                    ConfigurationSection section = requested.getRoot().getConfigurationSection(s);
                    User u = new User();
                    if (section.contains("username")) {
                        u.setNickname(section.getString("nickname"));
                        u.setUsername(section.getString("username"));

                        // Does their user file contain a request?
                        if (section.getConfigurationSection("request") != null) {
                            ConfigurationSection requestSection = section.getConfigurationSection("request");
                            if (requestSection.contains("nickname")) {
                                NickRequest req = new NickRequest(s, requestSection.getString("nickname"));
                                if (requestSection.contains("waiting")) req.setWaiting(requestSection.getBoolean("waiting"));
                                if (requestSection.contains("status")) req.setStatus(requestSection.getBoolean("status"));
                                u.setRequest(req);
                            }
                        }
                        users.put(s,u);
                    }
                }
                getLogger().info("Loaded "+users.size()+" users.");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InvalidConfigurationException e) {
                e.printStackTrace();
            }
        }
    }
    // Save current user nicknames to file.
    public void saveUsers() {
        YamlConfiguration loadedUsers = new YamlConfiguration();
        int count = 0;
        for (Map.Entry<String,User> e : users.entrySet()) {
           count++;

            ConfigurationSection userSection = loadedUsers.createSection(e.getKey());

            // To be shortened down.
            if (e.getValue().getUsername() != null) userSection.set("username",e.getValue().getUsername());
            if (e.getValue().getNickname() != null) userSection.set("nickname",e.getValue().getNickname());

            // Got a request? Goody
            if (e.getValue().getRequest() != null) {
                ConfigurationSection requestSection = userSection.createSection("request");
                requestSection.set("nickname",e.getValue().getRequest().getNickname());
                requestSection.set("status",e.getValue().getRequest().getStatus());
                requestSection.set("waiting",e.getValue().getRequest().isWaiting());
            }
        }

        try {
            getLogger().info("Saved all "+count+" users.");
            loadedUsers.save(usersFile);
        } catch (IOException e) {
            getLogger().info("Unable to save nicknames to file :( Data may be lost!");
            e.printStackTrace();
        }
    }

    public boolean nickTaken(String nick) {
        // Is it already in use?
        for (Map.Entry<String,User> u : users.entrySet()) {
            if (u.getValue().getNickname() != null) {
                // Check if this users nickname matches.
                if (u.getValue().getNickname().equalsIgnoreCase(nick)) return true;
            }
            if (u.getValue().getRequest() != null) {
                // Check if this users request matches.
                String reqNick = ChatColor.stripColor(colourFormat(u.getValue().getRequest().getNickname()));
                String prepNick = ChatColor.stripColor(colourFormat(nick));
                if (reqNick.equalsIgnoreCase(prepNick)) return true;
            }
        }

        return false;
    }

    public User userViaName(String name) {
        for (Map.Entry<String,User> u : users.entrySet()) {
            if (u.getValue().getUsername().equalsIgnoreCase(name)) return u.getValue();
        }
        return null;
    }

    public String colourFormat(String msg) {
        return ChatColor.translateAlternateColorCodes('&',msg);
    }

    public boolean isValid(String nick, Player player) {
        // Convert to lowercase for the sake of sanity.
        nick = nick.toLowerCase();

        // General Formatting
        if (nick.contains("&l") && !player.hasPermission("nicknamerequest.allow.bold")) return false;
        if (nick.contains("&o") && !player.hasPermission("nicknamerequest.allow.italic")) return false;
        if (nick.contains("&n") && !player.hasPermission("nicknamerequest.allow.underline")) return false;
        if (nick.contains("&m") && !player.hasPermission("nicknamerequest.allow.strikethrough")) return false;
        if (nick.contains("&k") && !player.hasPermission("nicknamerequest.allow.obfuscated")) return false;

        // Colours - Dark
        if (nick.contains("&1") && !player.hasPermission("nicknamerequest.allow.dark_blue")) return false;
        if (nick.contains("&2") && !player.hasPermission("nicknamerequest.allow.dark_green")) return false;
        if (nick.contains("&3") && !player.hasPermission("nicknamerequest.allow.dark_aqua")) return false;
        if (nick.contains("&4") && !player.hasPermission("nicknamerequest.allow.dark_red")) return false;
        if (nick.contains("&5") && !player.hasPermission("nicknamerequest.allow.dark_purple")) return false;
        if (nick.contains("&8") && !player.hasPermission("nicknamerequest.allow.dark_gray")) return false;

        // Colours - Normal
        if (nick.contains("&0") && !player.hasPermission("nicknamerequest.allow.black")) return false;
        if (nick.contains("&6") && !player.hasPermission("nicknamerequest.allow.gold")) return false;
        if (nick.contains("&7") && !player.hasPermission("nicknamerequest.allow.gray")) return false;
        if (nick.contains("&9") && !player.hasPermission("nicknamerequest.allow.blue")) return false;
        if (nick.contains("&a") && !player.hasPermission("nicknamerequest.allow.green")) return false;
        if (nick.contains("&b") && !player.hasPermission("nicknamerequest.allow.aqua")) return false;
        if (nick.contains("&c") && !player.hasPermission("nicknamerequest.allow.red")) return false;
        if (nick.contains("&e") && !player.hasPermission("nicknamerequest.allow.yellow")) return false;
        if (nick.contains("&f") && !player.hasPermission("nicknamerequest.allow.white")) return false;

        // Colours - Light
        if (nick.contains("&d") && !player.hasPermission("nicknamerequest.allow.light_purple")) return false;
        return true;
    }

    /* When a user hasn't been on for a while */
    @SuppressWarnings("unused")
    public void onExpire(PlayerDataExpired event) {
        //
    }

    // Vault Support
    private boolean setupVault(){
        // Check if its loaded.
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;

        // Attempt to register perms.
        if (!setupPerms()) return false;
        if (!setupChat()) return false;

        return true;
    }
    private boolean setupPerms() {
        // Attempt to register perms.
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }
    private boolean setupChat() {
        // Attempt to register perms.
        RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
        chat = rsp.getProvider();
        return chat != null;
    }
}
