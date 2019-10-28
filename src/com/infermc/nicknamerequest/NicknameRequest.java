package com.infermc.nicknamerequest;

import com.infermc.nicknamerequest.database.database;
import com.infermc.nicknamerequest.database.fileDatabase;
import com.infermc.nicknamerequest.database.sqlDatabase;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.omg.CORBA.OBJECT_NOT_EXIST;

import java.util.*;

// WARNING! DO NOT LOOK DIRECTLY AT THE SAUCE, FAILIURE TO AVOID LOOKING AT THE SAUCE MAY RESULT IN EYE IRRITATION!

public class NicknameRequest extends JavaPlugin implements Listener {
    // Database
    private database db = null;

    // Providers
    private Permission perms;
    private Chat chat;

    // Other vars
    private boolean debug = false;

    // API
    private NicknameAPI api = null;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("NicknameRequest v"+this.getDescription().getVersion()+" Enabled ~ Yeey");

        if(!setupVault()) getLogger().info("Unable to load Vault, Group names won't be shown in the request list!");

        if (getServer().getPluginManager().getPlugin("StaleAPI") == null) {
            getLogger().info("Unable to load StaleAPI, Nicknames and requests will never expire. (PendingRequest could start to lag)");
        } else {
            getServer().getPluginManager().registerEvents(new StaleAPIEvents(this,db),this);
            getLogger().info("StaleAPI Detected! Nicknames/Requests will now expire over time.");
        }

        // Handle config.
        saveDefaultConfig();

        // Load the database
        String dbType = getConfig().getConfigurationSection("database").getString("type","file");
        if (dbType.equalsIgnoreCase("file")) {
            db = new fileDatabase(this);
        } else if (dbType.equalsIgnoreCase("mysql")) {
            db = new sqlDatabase(this);

        } else {
            getLogger().warning("Unknown database format '"+dbType+"'!");
            getLogger().info("NicknameRequest is unable to continue.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }


        // Load Saved Data
        db.loadUsers();

        // Attempted to import old data
        if (!getConfig().getConfigurationSection("database").getString("import", "none").equalsIgnoreCase("none")) {

            String imported = getConfig().getConfigurationSection("database").getString("import", "none");
            if (!imported.equalsIgnoreCase(dbType)) {
                // Attempt to import.
                getLogger().info("Attempting to import old database data.");
                database old = null;
                if (imported.equalsIgnoreCase("file")) {
                    old = new fileDatabase(this);
                } else if (imported.equalsIgnoreCase("mysql")) {
                    old = new sqlDatabase(this);
                } else {
                    getLogger().info("Unknown source database format '"+imported+"', skipping import.");
                }
                if (old != null) {
                    old.loadUsers();
                    ArrayList<User> uList = new ArrayList<User>();
                    getLogger().info("Starting to import users from old database.");
                    for (Map.Entry<String, User> u : old.getUsers().entrySet()) {
                        User user = u.getValue();
                        // Skip people lacking any modifications to their data.
                        if (user.getNickname() != null || user.getRequest() != null || user.isRestricted()) {
                            if (debug) getLogger().info("Importing: " + user.getUUID().toString() + " " + user.getUsername() + " " + user.getNickname());
                            uList.add(user);
                        }
                    }
                    getLogger().info("Finished importing users from old database!");

                    if (uList.size() > 200) getLogger().warning("The users database is larger than 200 users! This may take a few minutes and could crash your server!");
                    double est_time = Math.ceil( (uList.size()*0.155) / 60);
                    getLogger().info("Database import will take approx "+est_time+" minutes.");

                    getLogger().info("Starting to store users in new database.");
                    // Attempt to switch to MySQL
                    for (User u : uList) {
                        if (debug) getLogger().info("Storing: " + u.getUUID().toString() + " " + u.getUsername() + " " + u.getNickname());
                        db.updateUser(u);
                    }
                    getLogger().info("Finished storing users in new database!");
                    getLogger().info("Imported and stored "+uList.size()+" users and their associated requests.");
                    getLogger().info("To avoid against importing old data over the new data please disable importing in the config.");
                }
            }
        }

        // Process currently logged in users (aka /reload)
        for (Player p : getServer().getOnlinePlayers()) {
            customJoin(p);
        }

        api = new NicknameAPI(this,db);
    }
    @Override
    public void onDisable() {
        if (db != null) {
            getLogger().info("Disabled - Running Save Commands.");
            db.saveUsers();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Commands!
        if (args.length >= 1) {
            Player player;
            if (args[0].equalsIgnoreCase("status") && sender.hasPermission("nicknamerequest.status")) {
                if (sender instanceof Player) {
                    player = (Player) sender;
                    if (db.getUser(player.getUniqueId()).getRequest() != null) {
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
                    UUID uid = player.getUniqueId();
                    if (!db.getUser(uid).isRestricted()) {
                        if (args.length >= 2) {
                            //String nick = args[1];
                            String nick = StringUtils.join(ArrayUtils.subarray(args, 1, args.length), " ");
                            if (!db.nickTaken(nick)) {
                                if (!isValid(nick, player)) {
                                    sender.sendMessage(colourFormat("&cDisallowed nickname! Your nickname contains disallowed formatting!"));
                                    return true;
                                }
                                User u = db.getUser(uid);
                                // Do we notify staff?
                                if (u.getRequest() == null) {
                                    // Yes, It's a new request.
                                    u.newRequest(nick);
                                    sender.sendMessage(colourFormat(getString("info-requested",null)));

                                    TextComponent player_str = new TextComponent(colourFormat("&b" + player.getName()));
                                    TextComponent req_str = new TextComponent(colourFormat("&b has requested the nickname '" + nick + "&r&b'"));
                                    TextComponent accept = new TextComponent(colourFormat(" &b[ &aAccept &b|"));
                                    TextComponent deny = new TextComponent(colourFormat(" &cDeny &b]"));

                                    accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nick accept " + player.getName()));
                                    accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Accept nickname").create()));

                                    deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nick deny " + player.getName()));
                                    deny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Deny nickname").create()));

                                    // If theres perms groups. Add them to the message!
                                    if (perms != null && chat != null) {
                                        String group;
                                        if (player != null) {
                                            group = chat.getPrimaryGroup(player);
                                        } else {
                                            // A feeble attempt.
                                            group = chat.getPrimaryGroup(getServer().getWorlds().get(0).getName(), getServer().getOfflinePlayer(player.getUniqueId()));
                                        }
                                        TextComponent text_group = new TextComponent(" (" + group + ")");

                                        player_str.addExtra(text_group);
                                    }
                                    player_str.addExtra(req_str);
                                    player_str.addExtra(accept);
                                    player_str.addExtra(deny);

                                    for (Player p : getServer().getOnlinePlayers()) {
                                        if (p.hasPermission("nicknamerequest.notify")) {
                                            p.spigot().sendMessage(player_str);
                                        }
                                    }
                                } else {
                                    // No, update a previous request. Remove the old one.
                                    sender.sendMessage(colourFormat("&aNickname request updated."));

                                    // Update it.
                                    u.getRequest().setNickname(args[1]);
                                }
                                // TODO Remove and have requests update themselves in the DB
                                db.updateUser(u);
                                return true;
                            } else {
                                // Nick is in use?
                                sender.sendMessage(colourFormat("&cThat Nickname is already in use or has already been requested!"));
                            }
                        } else {
                            sender.sendMessage(colourFormat("&bUsage: /nick request nickname"));
                        }
                    } else {
                        if (db.getUser(uid).getRestrictTime() == null) {
                            sender.sendMessage(colourFormat("&cSorry, you're not allowed to request a nickname indefinitely."));
                        } else {
                            Long wait = db.getUser(uid).getRestrictTime() - (new Date().getTime()/1000);
                            sender.sendMessage(colourFormat("&cSorry, you're not allowed to request a nickname, please wait "+wait+" seconds."));
                        }
                    }
                } else {
                    sender.sendMessage("This is a User Only Command!");
                }
                return true;
            } else if (args[0].equalsIgnoreCase("cancel")) {
                if (sender instanceof Player) {
                    player = (Player) sender;
                    UUID uid = player.getUniqueId();
                    User u = db.getUser(uid);
                    if (u.getRequest() == null) {
                        sender.sendMessage(colourFormat("&bYou've not made a nickname request!"));
                    } else {
                        u.setRequest(null);
                        sender.sendMessage(colourFormat("&aNickname Request Cancelled."));
                    }
                } else {
                    sender.sendMessage(colourFormat("&cThis is a User Only Command!"));
                }
                return true;
            } else if (args[0].equalsIgnoreCase("accept") && sender.hasPermission("nicknamerequest.accept")) {
                if (args.length >= 2) {
                    if (db.userViaName(args[1]) != null) {
                        User u = db.userViaName(args[1]);
                        if (u.getRequest() != null) {
                            u.getRequest().setWaiting(false);
                            u.getRequest().setStatus(true);
                            if (!u.isRestricted() && getConfig().getLong("accept-cooldown",0) > 0) {
                                Long rTime = (new Date().getTime()/1000) + getConfig().getLong("accept-cooldown");
                                u.setRestrictTime(rTime);
                                u.setRestricted(true);
                            }
                            sender.sendMessage(colourFormat("&aNickname successfully accepted."));

                            for (Player p : getServer().getOnlinePlayers()) {
                                if (p.hasPermission("nicknamerequest.notify")) {
                                    p.sendMessage(colourFormat("&bThe nickname '&r&f"+u.getRequest().getNickname()+ "&b' by "+u.getUsername()+" has been &a&laccepted&r&b by "+sender.getName()));
                                }
                            }
                            // THIS NEEDS SORTING
                            // TODO
                            if (getServer().getPlayer(u.getUUID()) != null) customJoin(getServer().getPlayer(u.getUUID()));
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
                    if (db.userViaName(args[1]) != null) {
                        User u = db.userViaName(args[1]);
                        if (u.getRequest() != null) {
                            u.getRequest().setWaiting(false);
                            u.getRequest().setStatus(false);
                            if (!u.isRestricted() && getConfig().getLong("deny-cooldown",0) > 0) {
                                Long rTime = (new Date().getTime()/1000) + getConfig().getLong("deny-cooldown");
                                u.setRestrictTime(rTime);
                                u.setRestricted(true);
                            }
                            sender.sendMessage(colourFormat("&aNickname successfully denied."));

                            for (Player p : getServer().getOnlinePlayers()) {
                                if (p.hasPermission("nicknamerequest.notify")) {
                                    p.sendMessage(colourFormat("&bThe nickname '&r&f"+u.getRequest().getNickname()+ "&b' by "+u.getUsername()+" has been &c&ldenied&r&b by "+sender.getName()));
                                }
                            }

                            // THIS NEEDS SORTING
                            // TODO
                            if (getServer().getPlayer(u.getUUID()) != null) customJoin(getServer().getPlayer(u.getUUID()));

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
                        User u = db.getUser(player.getUniqueId());
                        // Update users nicknamerequest.
                        u.setNickname(args[1]);
                        sender.sendMessage(colourFormat("&bYour nickname was changed to &r"+args[1]));
                    } else {
                        sender.sendMessage("Syntax: /nick set username nickname");
                    }
                } else if (args.length >= 3) {
                    // Other
                    if (db.userViaName(args[1]) != null) {
                        User u = db.userViaName(args[1]);
                        u.setNickname(args[2]);
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
                for (Map.Entry<String,User> u : db.getUsers().entrySet()) {
                    if (u.getValue().getRequest() != null) {
                        if (u.getValue().getRequest().isWaiting()) {
                            User user = u.getValue();
                            String nick = colourFormat(user.getRequest().getNickname() + "&r");

                            TextComponent req_str = new TextComponent(colourFormat("   &9- &r&f" + nick + "&9 by " + user.getUsername()));
                            TextComponent accept = new TextComponent(colourFormat(" &b[ &aAccept &b|"));
                            TextComponent deny = new TextComponent(colourFormat(" &cDeny &b]"));

                            accept.setClickEvent( new ClickEvent( ClickEvent.Action.RUN_COMMAND, "/nick accept "+user.getUsername() ) );
                            accept.setHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Accept nickname").create() ) );

                            deny.setClickEvent( new ClickEvent( ClickEvent.Action.RUN_COMMAND, "/nick deny "+user.getUsername() ) );
                            deny.setHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Deny nickname").create() ) );

                            // If theres perms groups. Add them to the message!
                            if (perms != null && chat != null) {
                                String group;
                                if (user.getPlayer() != null) {
                                    group = chat.getPrimaryGroup(user.getPlayer());
                                } else {
                                    // A feeble attempt.
                                    group = chat.getPrimaryGroup(getServer().getWorlds().get(0).getName(),getServer().getOfflinePlayer(user.getRequest().getUUID()));
                                }
                                TextComponent text_group = new TextComponent(" (" + group + ")");

                                req_str.addExtra(text_group);
                            }

                            if (sender instanceof Player) {
                                // If they're a player send the fancy string.
                                player = (Player) sender;

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
                            User u = db.getUser(player.getUniqueId());
                            u.setNickname(null);
                        } else {
                            sender.sendMessage("Syntax: /nick remove username");
                        }
                    }
                } else {
                    // Other user.
                    if (sender.hasPermission("nicknamerequest.remove.others")) {
                        if (db.userViaName(args[1]) != null) {
                            User u = db.userViaName(args[1]);
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
                sender.sendMessage(colourFormat("PendingRequest version "+this.getDescription().getVersion()));
                sender.sendMessage(colourFormat("Nickname Requester - Easy nickname management for users and staff."));
                sender.sendMessage(colourFormat("Author: Thomas Edwards (TMFKSOFT/MajesticFudgie)"));
                return true;
            } else if (args[0].equalsIgnoreCase("restrict") && sender.hasPermission("nicknamerequest.restrict")) {
                // THIS NEEDS SORTING
                // TODO
                if (args.length >= 2) {
                    if (db.userViaName(args[1]) != null) {
                        User u = db.userViaName(args[1]);
                        u.setRestricted(true);
                        Long rTime = null;
                        if (args.length == 3) {
                            rTime = parseTime(args[2]);
                            if (rTime == null) {
                                sender.sendMessage(colourFormat("&cInvalid time format, units of time must be singular and end in s(econds), m(inutes), h(hours) or d(days)! E.g. 24h"));
                                return true;
                            }
                            long curTime = (new Date().getTime() / 1000)+rTime;
                            u.setRestrictTime(curTime);
                        }
                        if (rTime == null) {
                            sender.sendMessage(colourFormat("&aNickname request access for " + u.getUsername() + " is now restricted."));
                        } else {
                            sender.sendMessage(colourFormat("&aNickname request access for " + u.getUsername() + " is now restricted for "+rTime+" seconds."));
                        }
                    } else {
                        sender.sendMessage(colourFormat("&cNo such user!"));
                    }
                }
                return true;
            } else if (args[0].equalsIgnoreCase("unrestrict") && sender.hasPermission("nicknamerequest.restrict")) {
                // TODO
                if (args.length >= 2) {
                    if (db.userViaName(args[1]) != null) {
                        User u = db.userViaName(args[1]);
                        u.setRestricted(false);
                        sender.sendMessage(colourFormat("&aNickname request access for " + u.getUsername() + " is no longer restricted."));
                    } else {
                        sender.sendMessage(colourFormat("&cNo such user!"));
                    }
                } else {
                    sender.sendMessage("Syntax: /nick unrestrict username");
                }
                return true;
            } else if (args[0].equalsIgnoreCase("info") && (sender.hasPermission("nicknamerequest.info") || sender.hasPermission("nicknamerequest.info.others"))) {
                if (sender instanceof Player && args.length == 1) {
                    player = (Player) sender;
                    UUID uid = player.getUniqueId();
                    User u = db.getUser(uid);
                    HashMap<String, Object> langFields = new HashMap<String, Object>();
                    if (u.getNickname() == null) {
                        sender.sendMessage(colourFormat(getString("info-self-no-nick",null)));
                    } else {
                        langFields.put("nick",u.getNickname());
                        sender.sendMessage(colourFormat(getString("info-self-current-nick",langFields)));
                    }
                    if (u.getRequest() == null) {
                        sender.sendMessage(colourFormat(getString("info-self-no-request",null)));
                    } else {
                        PendingRequest req = u.getRequest();
                        langFields.put("nick",req.getNickname());
                        sender.sendMessage(colourFormat(getString("info-self-current-request",langFields)));
                    }
                    if (!u.isRestricted()) {
                        sender.sendMessage(colourFormat(getString("info-self-not-restricted",null)));
                    } else {
                        if (u.getRestrictTime() == null) {
                            sender.sendMessage(colourFormat(getString("info-self-perm-restricted",null)));
                        } else {
                            Long wait = u.getRestrictTime() - (new Date().getTime() / 1000);
                            langFields.put("time",wait);
                            sender.sendMessage(colourFormat(getString("info-self-timed-restricted",langFields)));
                        }
                    }
                } else if (args.length > 1) {
                    if (sender.hasPermission("nicknamerequest.info.others")) {
                        User u = db.userViaName(args[1]);
                        if (u != null) {
                            HashMap<String, Object> langFields = new HashMap<String, Object>();
                            langFields.put("username", u.getUsername());
                            if (u.getNickname() == null) {
                                sender.sendMessage(colourFormat(getString("info-other-no-nick", langFields)));
                            } else {
                                langFields.put("nick", u.getNickname());
                                sender.sendMessage(colourFormat(getString("info-other-current-nick", langFields)));
                            }
                            if (u.getRequest() == null) {
                                sender.sendMessage(colourFormat(getString("info-other-no-request", langFields)));
                            } else {
                                PendingRequest req = u.getRequest();
                                langFields.put("nick", req.getNickname());
                                sender.sendMessage(colourFormat(getString("info-other-current-request", langFields)));
                            }
                            if (!u.isRestricted()) {
                                sender.sendMessage(colourFormat(getString("info-other-not-restricted", langFields)));
                            } else {
                                if (u.getRestrictTime() == null) {
                                    sender.sendMessage(colourFormat(getString("info-other-perm-restricted", langFields)));
                                } else {
                                    Long wait = u.getRestrictTime() - (new Date().getTime() / 1000);
                                    langFields.put("time", wait);
                                    sender.sendMessage(colourFormat(getString("info-other-timed-restricted", langFields)));
                                }
                            }
                        } else {
                            sender.sendMessage(colourFormat(getString("standard-invalid-user",null)));
                        }
                    } else {
                        sender.sendMessage(colourFormat(getString("info-other-denied",null)));
                    }
                } else {
                    sender.sendMessage(colourFormat(getString("standard-user-only-cmd",null)));
                }
                return true;
            }
        } else {
            return true;
        }
        return true;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        customJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        customQuit(event.getPlayer());
    }

    // Custom functions
    public NicknameAPI getNicknameAPI() {
        return this.api;
    }

    protected void customJoin(Player player) {
        UUID uid = player.getUniqueId();
        User u = db.getUser(uid);
        // If we loaded their nick, apply it!
        if (u != null) {
            u.setPlayer(player);
            if (!u.getUsername().equals(player.getName())) u.setUsername(player.getName());
            if (u.getNickname() != null) applyNickname(u);
        } else {
            // No such user, create them
            getLogger().info("Created user.");

            boolean restricted = false;
            Long restrictTime = 0L;

            if (getConfig().getBoolean("auto-restrict",false)) {
                restricted = true;
                if (getConfig().getLong("auto-restrict-time",0) != 0) {
                    restrictTime = (new java.util.Date().getTime()/1000) + getConfig().getLong("auto-restrict-time");
                }
            }

            u = new User(this,this.db,uid,player.getName(),null,restricted,restrictTime,null);
            u.setPlayer(player);
        }

        // Update their request status
        requestStatus(player);
    }

    protected void applyNickname(User u) {
        String nickname = u.getNickname();
        Player player = u.getPlayer();

        String format = getConfig().getString("nick-format");
        format = format.replace("{NICK}",nickname);

        if (perms != null && chat != null) {
            format = format.replace("{PREFIX}",chat.getPlayerPrefix(player));
            format = format.replace("{SUFFIX}",chat.getPlayerSuffix(player));
            format = format.replace("{GROUP}",chat.getPrimaryGroup(player));
        }
        player.setDisplayName(colourFormat(format + "&r"));
    }

    private void customQuit(Player player) {
        // Remove their player.
        db.getUser(player.getUniqueId()).setPlayer(null);
    }

    private void requestStatus(Player player) {
        // Check if they've got a pending request.
        User u = db.getUser(player.getUniqueId());
        if (u == null) return;

        PendingRequest nr = u.getRequest();
        if (nr == null) return;

        // Ensure its not already waiting
        String nick = colourFormat(nr.getNickname() + "&r");
        if (!nr.isWaiting()) {
            // Was it approved?
            if (nr.getStatus()) {
                player.sendMessage(colourFormat("&aYour Nickname '"+nick+"&a' was approved and applied!"));
                player.setDisplayName(nick);
                // Update the nicknames.
                u.setNickname(nr.getNickname());
            } else {
                player.sendMessage(colourFormat("&cYour Nickname '"+nick+"&c' was denied and wasn't applied."));
            }
            // Remove the request.
            u.setRequest(null);
        } else {
            player.sendMessage(colourFormat("&bYour nickname '"+nick+"&b' is still awaiting approval."));
            player.sendMessage(colourFormat("&bCancel your Nickname Request via &a/nick cancel"));
        }
    }

    public String colourFormat(String msg) {
        return ChatColor.translateAlternateColorCodes('&',msg);
    }
    private Long parseTime(String time) {
        // Parse a simple time string into seconds.
        String unit = time.substring(time.length()-1).trim();
        String dur = time.substring(0,time.length()-1);

        Long unitDur = Long.parseLong(dur);
        Long duration;

        if (unit.equalsIgnoreCase("m")) {
            // Minutes
            duration = unitDur*60;
        } else if (unit.equalsIgnoreCase("d")) {
            // Days
            duration = unitDur*86400;
        } else if (unit.equalsIgnoreCase("h")) {
            // Hours
            duration = unitDur*3600;
        } else if (unit.equalsIgnoreCase("s")) {
            // Seconds
            duration = unitDur;
        } else {
            // Unknown unit of time :<
            return null;
        }
        return duration;
    }

    private boolean isValid(String nick, Player player) {
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

        // Spaces
        if (nick.contains(" ") && !player.hasPermission("nicknamerequest.allow.spaces")) return false;

        return true;
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
        if (rsp == null) return false;
        perms = rsp.getProvider();
        return perms != null;
    }
    private boolean setupChat() {
        // Attempt to register perms.
        RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
        if (rsp == null) return false;
        chat = rsp.getProvider();
        return chat != null;
    }
    private String getString(String name,HashMap<String,Object> fields) {
        ConfigurationSection section = getConfig().getConfigurationSection("messages");
        if (section == null) {
            getLogger().info("Unable to get language string '"+name.toLowerCase()+"'! Messages config section doesn't exist!");
            return "lang:"+name.toLowerCase();
        }
        if (section.getString(name,null) == null) {
            getLogger().info("Unable to get language string '"+name.toLowerCase()+"'!");
            return "lang:"+name.toLowerCase();
        }
        String str = section.getString(name);
        if (fields != null) {
            for (String field : fields.keySet()) {
                str = str.replace("{" + field.toUpperCase() + "}", fields.get(field).toString());
            }
        }
        return str;
    }
}
