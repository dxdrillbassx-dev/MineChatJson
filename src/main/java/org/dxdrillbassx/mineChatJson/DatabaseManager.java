package org.dxdrillbassx.mineChatJson;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DatabaseManager {
    private final JavaPlugin plugin;
    private Connection mysqlConnection;
    private final Map<UUID, String> playerColors = new HashMap<>();
    private final Map<UUID, String> playerNicknames = new HashMap<>();
    private final Map<UUID, Boolean> playerChatSounds = new HashMap<>();
    private final Map<UUID, String> playerEffects = new HashMap<>();
    private final Map<UUID, String> playerSounds = new HashMap<>();
    private final Map<UUID, String> playerJoinMessages = new HashMap<>();

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        if (plugin.getConfig().getBoolean("mysql.enable")) {
            setupMySQL();
        }
    }

    private void setupMySQL() {
        String host = plugin.getConfig().getString("mysql.host", "127.0.0.1");
        int port = plugin.getConfig().getInt("mysql.port", 3306);
        String db = plugin.getConfig().getString("mysql.db", "games");
        String login = plugin.getConfig().getString("mysql.login", "root");
        String pass = plugin.getConfig().getString("mysql.pass", "");
        String url = "jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false";

        plugin.getLogger().info("Попытка подключения к MySQL: " + url);

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            mysqlConnection = DriverManager.getConnection(url, login, pass);
            mysqlConnection.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS chat_colors (uuid VARCHAR(36) PRIMARY KEY, color VARCHAR(16))"
            );
            mysqlConnection.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS player_nicknames (uuid VARCHAR(36) PRIMARY KEY, nickname VARCHAR(16))"
            );
            mysqlConnection.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS chat_sounds (uuid VARCHAR(36) PRIMARY KEY, sound_enabled BOOLEAN, sound_type VARCHAR(64))"
            );
            mysqlConnection.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS chat_effects (uuid VARCHAR(36) PRIMARY KEY, effect VARCHAR(16))"
            );
            mysqlConnection.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS join_messages (uuid VARCHAR(36) PRIMARY KEY, message VARCHAR(100))"
            );
            plugin.getLogger().info("MySQL подключён успешно! ✅");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("Класс драйвера MySQL не найден! Проверьте наличие MySQL Connector.");
            e.printStackTrace();
        } catch (SQLException e) {
            plugin.getLogger().severe("Не удалось подключиться к MySQL: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean isMySQLConnected() {
        if (mysqlConnection == null) {
            return false;
        }
        try {
            return !mysqlConnection.isClosed() && mysqlConnection.isValid(2);
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при проверке соединения с MySQL: " + e.getMessage());
            return false;
        }
    }

    public void closeConnection() {
        if (mysqlConnection != null) {
            try {
                mysqlConnection.close();
                plugin.getLogger().info("MySQL соединение закрыто! 🔌");
            } catch (SQLException e) {
                plugin.getLogger().severe("Ошибка при закрытии MySQL соединения: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public String getPlayerColor(UUID uuid) {
        if (!plugin.getConfig().getBoolean("mysql.enable")) {
            return playerColors.getOrDefault(uuid, plugin.getConfig().getString("colors.default-color", "&f"));
        }

        try (PreparedStatement stmt = mysqlConnection.prepareStatement("SELECT color FROM chat_colors WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("color");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при получении цвета игрока: " + e.getMessage());
            e.printStackTrace();
        }
        return plugin.getConfig().getString("colors.default-color", "&f");
    }

    public void setPlayerColor(UUID uuid, String color) {
        playerColors.put(uuid, color);
        if (!plugin.getConfig().getBoolean("mysql.enable")) return;

        try (PreparedStatement stmt = mysqlConnection.prepareStatement(
                "INSERT INTO chat_colors (uuid, color) VALUES (?, ?) ON DUPLICATE KEY UPDATE color = ?"
        )) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, color);
            stmt.setString(3, color);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при установке цвета игрока: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void removePlayerColor(UUID uuid) {
        playerColors.remove(uuid);
        if (!plugin.getConfig().getBoolean("mysql.enable")) return;

        try (PreparedStatement stmt = mysqlConnection.prepareStatement("DELETE FROM chat_colors WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при удалении цвета игрока: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String getPlayerNickname(UUID uuid) {
        String nickname = playerNicknames.getOrDefault(uuid, null);
        if (nickname != null) {
            return nickname + "*";
        }
        return null;
    }

    public void setPlayerNickname(UUID uuid, String nickname) {
        playerNicknames.put(uuid, nickname);
        if (!plugin.getConfig().getBoolean("mysql.enable")) return;

        try (PreparedStatement stmt = mysqlConnection.prepareStatement(
                "INSERT INTO player_nicknames (uuid, nickname) VALUES (?, ?) ON DUPLICATE KEY UPDATE nickname = ?"
        )) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, nickname);
            stmt.setString(3, nickname);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при установке ника игрока: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void removePlayerNickname(UUID uuid) {
        playerNicknames.remove(uuid);
        if (!plugin.getConfig().getBoolean("mysql.enable")) return;

        try (PreparedStatement stmt = mysqlConnection.prepareStatement("DELETE FROM player_nicknames WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при удалении ника игрока: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean isChatSoundEnabled(UUID uuid) {
        if (!plugin.getConfig().getBoolean("mysql.enable")) {
            return playerChatSounds.getOrDefault(uuid, plugin.getConfig().getBoolean("chat.sound.enabled", true));
        }

        try (PreparedStatement stmt = mysqlConnection.prepareStatement("SELECT sound_enabled FROM chat_sounds WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("sound_enabled");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при получении статуса звука чата: " + e.getMessage());
            e.printStackTrace();
        }
        return plugin.getConfig().getBoolean("chat.sound.enabled", true);
    }

    public String getPlayerSound(UUID uuid) {
        if (!plugin.getConfig().getBoolean("mysql.enable")) {
            return playerSounds.getOrDefault(uuid, plugin.getConfig().getString("chat.sound.type", "minecraft:entity.experience_orb.pickup"));
        }

        try (PreparedStatement stmt = mysqlConnection.prepareStatement("SELECT sound_type FROM chat_sounds WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String sound = rs.getString("sound_type");
                return sound != null ? sound : plugin.getConfig().getString("chat.sound.type", "minecraft:entity.experience_orb.pickup");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при получении звука игрока: " + e.getMessage());
            e.printStackTrace();
        }
        return plugin.getConfig().getString("chat.sound.type", "minecraft:entity.experience_orb.pickup");
    }

    public void setChatSound(UUID uuid, boolean enabled, String soundType) {
        playerChatSounds.put(uuid, enabled);
        playerSounds.put(uuid, soundType);
        if (!plugin.getConfig().getBoolean("mysql.enable")) return;

        try (PreparedStatement stmt = mysqlConnection.prepareStatement(
                "INSERT INTO chat_sounds (uuid, sound_enabled, sound_type) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE sound_enabled = ?, sound_type = ?"
        )) {
            stmt.setString(1, uuid.toString());
            stmt.setBoolean(2, enabled);
            stmt.setString(3, soundType);
            stmt.setBoolean(4, enabled);
            stmt.setString(5, soundType);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при установке звука чата: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setChatSound(UUID uuid, boolean enabled) {
        setChatSound(uuid, enabled, plugin.getConfig().getString("chat.sound.type", "minecraft:entity.experience_orb.pickup"));
    }

    public String getPlayerEffect(UUID uuid) {
        if (!plugin.getConfig().getBoolean("mysql.enable")) {
            return playerEffects.getOrDefault(uuid, null);
        }

        try (PreparedStatement stmt = mysqlConnection.prepareStatement("SELECT effect FROM chat_effects WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("effect");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при получении эффекта текста: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public void setPlayerEffect(UUID uuid, String effect) {
        playerEffects.put(uuid, effect);
        if (!plugin.getConfig().getBoolean("mysql.enable")) return;

        try (PreparedStatement stmt = mysqlConnection.prepareStatement(
                "INSERT INTO chat_effects (uuid, effect) VALUES (?, ?) ON DUPLICATE KEY UPDATE effect = ?"
        )) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, effect);
            stmt.setString(3, effect);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при установке эффекта текста: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void removePlayerEffect(UUID uuid) {
        playerEffects.remove(uuid);
        if (!plugin.getConfig().getBoolean("mysql.enable")) return;

        try (PreparedStatement stmt = mysqlConnection.prepareStatement("DELETE FROM chat_effects WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при удалении эффекта текста: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String getPlayerJoinMessage(UUID uuid) {
        if (!plugin.getConfig().getBoolean("mysql.enable")) {
            return playerJoinMessages.getOrDefault(uuid, null);
        }

        try (PreparedStatement stmt = mysqlConnection.prepareStatement("SELECT message FROM join_messages WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("message");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при получении приветственного сообщения: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public void setPlayerJoinMessage(UUID uuid, String message) {
        playerJoinMessages.put(uuid, message);
        if (!plugin.getConfig().getBoolean("mysql.enable")) return;

        try (PreparedStatement stmt = mysqlConnection.prepareStatement(
                "INSERT INTO join_messages (uuid, message) VALUES (?, ?) ON DUPLICATE KEY UPDATE message = ?"
        )) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, message);
            stmt.setString(3, message);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при установке приветственного сообщения: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void removePlayerJoinMessage(UUID uuid) {
        playerJoinMessages.remove(uuid);
        if (!plugin.getConfig().getBoolean("mysql.enable")) return;

        try (PreparedStatement stmt = mysqlConnection.prepareStatement("DELETE FROM join_messages WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при удалении приветственного сообщения: " + e.getMessage());
            e.printStackTrace();
        }
    }
}