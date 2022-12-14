package com.infermc.nicknamerequest;

import ca.pn.commands.CommandManager;
import com.infermc.nicknamerequest.commands.NickCommand;
import com.infermc.nicknamerequest.database.database;
import com.infermc.nicknamerequest.database.fileDatabase;
import com.infermc.nicknamerequest.database.sqlDatabase;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
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

import java.util.*;

// WARNING! DO NOT LOOK DIRECTLY AT THE SAUCE, FAILURE TO AVOID LOOKING AT THE SAUCE MAY RESULT IN EYE IRRITATION!

public class NicknameRequest extends JavaPlugin implements Listener {
    // Database
    public database db = null;

    // Providers
    public Permission perms;
    public Chat chat;

    // Other vars
    private boolean debug = false;

    // API
    private NicknameAPI api = null;
    private CommandManager commandManager = new CommandManager(this);

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

        // Register our commands.
        this.commandManager.registerCommand(new NickCommand(this.commandManager));
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
        return this.commandManager.onCommand(sender, cmd, label, args);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        customJoin(event.getPlayer());
    }

    // Custom functions
    public NicknameAPI getNicknameAPI() {
        return this.api;
    }

    public void customJoin(Player player) {
        UUID uid = player.getUniqueId();
        User u = db.getUser(uid);
        // If we loaded their nick, apply it!
        if (u != null) {
            if (!u.getUsername().equals(player.getName())) u.setUsername(player.getName());
            if (u.getNickname() != null) applyNickname(u);
        } else {
            // No such user, create them
            getLogger().info("Creating user.");

            boolean restricted = false;
            Long restrictTime = 0L;

            if (getConfig().getBoolean("auto-restrict",false)) {
                restricted = true;
                if (getConfig().getLong("auto-restrict-time",0) != 0) {
                    restrictTime = (new java.util.Date().getTime()/1000) + getConfig().getLong("auto-restrict-time");
                }
            }

            u = new User(this,this.db,uid,player.getName(),null,restricted,restrictTime,null);
            this.db.updateUser(u);
        }

        // Update their request status
        requestStatus(player);
    }

    public void applyNickname(User u) {
        String nickname = u.getNickname();
        Player player = u.getPlayer();

        // If they're not online. Skip doing anything.
        if (player == null) {
            return;
        }

        String format = getConfig().getString("nick-format");
        format = format.replace("{NICK}",nickname);

        if (perms != null && chat != null) {
            format = format.replace("{PREFIX}",chat.getPlayerPrefix(player));
            format = format.replace("{SUFFIX}",chat.getPlayerSuffix(player));
            format = format.replace("{GROUP}",chat.getPrimaryGroup(player));
        }
        player.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(format + "&r"));
    }

    public void requestStatus(Player player) {
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
    public Long parseTime(String time) {
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
    public String getString(String name, HashMap<String, Object> fields) {
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
