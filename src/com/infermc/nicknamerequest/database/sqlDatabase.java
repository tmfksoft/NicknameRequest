package com.infermc.nicknamerequest.database;

import com.infermc.nicknamerequest.NicknameRequest;
import com.infermc.nicknamerequest.PendingRequest;
import com.infermc.nicknamerequest.User;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.*;
import java.util.*;

public class sqlDatabase extends database {
    private Connection conn = null;
    private NicknameRequest parent;
    private String prefix;

    public sqlDatabase(NicknameRequest p) {
        parent = p;
        p.getLogger().info("NicknameRequest is using MySQL");
        setup();
    }

    private void setup() {
        ConfigurationSection databaseConfig = parent.getConfig().getConfigurationSection("database");
        String host = databaseConfig.getString("host");
        String port = databaseConfig.getString("port");
        String username = databaseConfig.getString("username");
        String password = databaseConfig.getString("password");
        prefix = databaseConfig.getString("prefix");
        String database = databaseConfig.getString("database");

        try {
            conn = DriverManager.getConnection("jdbc:mysql://"+host+":"+port+"/"+database+"?user="+username+"&password="+password);
            // Check if our tables exist, otherwise create them
            PreparedStatement stmt = conn.prepareStatement("SHOW TABLES LIKE '"+prefix+"users';");
            stmt.execute();
            ResultSet rs = stmt.getResultSet();
            if (!rs.next()) {
                parent.getLogger().info("Users table doesnt exist! Creating it.");
                Statement st = conn.createStatement();
                String query = "CREATE TABLE IF NOT EXISTS `" + prefix + "users` ( `id` int(11) NOT NULL AUTO_INCREMENT, `uuid` text NOT NULL, `username` text NOT NULL, `nickname` text NOT NULL, `restricted` int(11) NOT NULL, `restrictTime` int(11) NOT NULL, `stamp` int(11) NOT NULL, PRIMARY KEY (`id`));";
                st.executeUpdate(query);
            }
            stmt.close();
            rs.close();

            stmt = conn.prepareStatement("SHOW TABLES LIKE '"+prefix+"requests';");
            stmt.execute();
            rs = stmt.getResultSet();
            if (!rs.next()) {
                parent.getLogger().info("Requests table doesnt exist! Creating it.");
                Statement st = conn.createStatement();
                String query = "CREATE TABLE IF NOT EXISTS `" + prefix + "requests` ( `id` int(11) NOT NULL AUTO_INCREMENT, `uuid` text NOT NULL, `nickname` text NOT NULL, `status` int(11) NOT NULL, `waiting` int(11) NOT NULL, `stamp` int(11) NOT NULL, PRIMARY KEY (`id`));";
                st.executeUpdate(query);
            }
        } catch (SQLException ex) {
            // handle any errors
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            parent.getLogger().warning("Error connecting to MySQL, unable to continue. Disabling self.");
            parent.getServer().getPluginManager().disablePlugin(parent);
        }
    }

    @Override
    public User getUser(UUID uuid) {
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM `"+prefix+"users` WHERE `uuid`=?;");
            stmt.setString(1,uuid.toString());
            stmt.execute();
            ResultSet rs = stmt.getResultSet();

            if (rs.next()) {
                rs.first();

                String username = rs.getString("username");
                String nickname = rs.getString("nickname");
                if (nickname.equals("")) nickname = null;

                boolean restricted = rs.getBoolean("restricted");
                long restrictTime = 0L;
                if (restricted) restrictTime = rs.getLong("restrictTime");

                User u = new User(this.parent,this,uuid,username,nickname,restricted,restrictTime,getRequest(uuid));

                rs.close();
                return u;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private PendingRequest getRequest(UUID uuid) {
        PendingRequest req;
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM `"+prefix+"requests` WHERE `uuid`=?;");
            stmt.setString(1,uuid.toString());
            stmt.execute();
            ResultSet rs = stmt.getResultSet();

            if (rs.next()) {
                rs.first();

                req = new PendingRequest(parent, this,rs.getString("nickname"));

                boolean status = rs.getBoolean("status");
                boolean waiting = rs.getBoolean("waiting");
                int requestTime = rs.getInt("stamp");

                req.setStatus(status);
                req.setWaiting(waiting);
                req.setRequestTime(requestTime);
                rs.close();
                return req;
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public HashMap<String, User> getUsers() {
        HashMap<String,User> users = new HashMap<String, User>();
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM `"+prefix+"users`;");
            stmt.execute();
            ResultSet rs = stmt.getResultSet();
            while (rs.next()) {
                // User u = new User(this.parent,this,uuid,username,nickname,restricted,restrictTime);

                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String username = rs.getString("username");
                String nickname = rs.getString("nickname");
                if (nickname.equals("")) nickname = null;

                boolean restricted = rs.getBoolean("restricted");
                long restrictTime = 0L;
                if (restricted) restrictTime = rs.getLong("restrictTime");

                User u = new User(this.parent,this,uuid,username,nickname,restricted,restrictTime,getRequest(uuid));

                users.put(uuid.toString(),u);
            }
            rs.close();
            return users;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    @Override
    public void loadUsers() {
        // Not required.
    }

    @Override
    public void saveUsers() {
        // Not required.
    }

    @Override
    public void updateUser(User u) {
        if (u == null) return;
        if (u.getUUID() == null) return;
        this.parent.getLogger().info("Update user called on "+u.getUUID());
        try {
            // Handle the user themselves.
            PreparedStatement sel_stmt = conn.prepareStatement("SELECT * FROM `"+prefix+"users` WHERE `uuid`=?;");
            sel_stmt.setString(1,u.getUUID().toString());
            sel_stmt.execute();
            ResultSet rs = sel_stmt.getResultSet();
            if (rs.next()) {
                rs.first();
                // Update Query
                PreparedStatement stmt = conn.prepareStatement("UPDATE `"+prefix+"users` SET `nickname`= ?, `username`= ? , `restricted`= ? , `restrictTime`= ? WHERE `uuid`= ? ;");
                if (u.getNickname() != null) {
                    stmt.setString(1, u.getNickname());
                } else {
                    stmt.setString(1,"");
                }
                stmt.setString(2, u.getUsername());
                stmt.setBoolean(3, u.getRestricted());

                if (u.getRestrictTime() != null) {
                    stmt.setLong(4, u.getRestrictTime());
                } else {
                    stmt.setLong(4, 0);
                }
                stmt.setString(5, u.getUUID().toString());
                stmt.executeUpdate();
            } else {
                // Insert
                PreparedStatement stmt = conn.prepareStatement("INSERT INTO `"+prefix+"users` (uuid,username,nickname,restricted,restrictTime,stamp) VALUES (?,?,?,?,?,?)");
                stmt.setString(1, u.getUUID().toString());
                stmt.setString(2, u.getUsername());
                if (u.getNickname() != null) {
                    stmt.setString(3, u.getNickname());
                } else {
                    stmt.setString(3,"");
                }
                stmt.setBoolean(4, u.getRestricted());
                stmt.setLong(5, u.getRestrictTime());
                stmt.setLong(6, (new java.util.Date().getTime()/1000));
                stmt.executeUpdate();
            }
            rs.close();
            sel_stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        PendingRequest req = u.getRequest();
        try {
            PreparedStatement sel_stmt = conn.prepareStatement("SELECT * FROM `"+prefix+"requests` WHERE `uuid`=?;");
            sel_stmt.setString(1,u.getUUID().toString());
            sel_stmt.execute();
            ResultSet rs = sel_stmt.getResultSet();

            if (req != null) {
                if (rs.next()) {
                    rs.first();
                    // Update Query
                    PreparedStatement stmt = conn.prepareStatement("UPDATE `"+prefix+"requests` SET `nickname`= ?, `status`= ? , `waiting`= ? WHERE `uuid`= ? ;");
                    stmt.setString(1, req.getNickname());
                    stmt.setBoolean(2, req.getStatus());
                    stmt.setBoolean(3, req.isWaiting());
                    stmt.setString(4, u.getUUID().toString());
                    stmt.executeUpdate();
                    stmt.close();
                } else {
                    // Insert
                    PreparedStatement stmt = conn.prepareStatement("INSERT INTO `"+prefix+"requests` (uuid,nickname,status,waiting,stamp) VALUES (?,?,?,?,?)");
                    stmt.setString(1, u.getUUID().toString());
                    stmt.setString(2, req.getNickname());
                    stmt.setBoolean(3, req.getStatus());
                    stmt.setBoolean(4, req.isWaiting());
                    stmt.setLong(5, (new java.util.Date().getTime() / 1000));
                    stmt.executeUpdate();
                    stmt.close();
                }
            } else {
                // Remove request!
                PreparedStatement stmt = conn.prepareStatement("DELETE FROM `"+prefix+"requests` WHERE uuid = ?");
                stmt.setString(1,u.getUUID().toString());
                stmt.executeUpdate();
            }
            rs.close();
            sel_stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean nickTaken(String nick) {
        for (Map.Entry<String,User> u : getUsers().entrySet()) {
            if (u.getValue().getNickname() != null) {
                // Check if this users nickname matches.
                if (u.getValue().getNickname().equalsIgnoreCase(nick)) return true;
            }
            if (u.getValue().getRequest() != null) {
                // Check if this users request matches.
                String reqNick = ChatColor.stripColor(parent.colourFormat(u.getValue().getRequest().getNickname()));
                String prepNick = ChatColor.stripColor(parent.colourFormat(nick));
                if (reqNick.equalsIgnoreCase(prepNick)) return true;
            }
        }
        return false;
    }

    @Override
    public User userViaName(String name) {
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM `"+prefix+"users` WHERE `username` = ?;");
            stmt.setString(1,name);
            stmt.execute();
            ResultSet rs = stmt.getResultSet();
            if (rs.next()) {
                rs.first();

                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String username = rs.getString("username");

                String nickname = rs.getString("nickname");
                if (nickname.equals("")) nickname = null;

                boolean restricted = rs.getBoolean("restricted");
                long restrictTime = 0L;
                if (restricted) restrictTime = rs.getLong("restrictTime");

                return new User(this.parent,this,uuid,username,nickname,restricted,restrictTime,getRequest(uuid));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}