package org.dxdrillbassx.mineChatJson;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private ChatManager chatManager;
    private DatabaseManager databaseManager;
    private CommandManager commandManager;
    private EmojiManager emojiManager;
    private InventoryListener inventoryListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        initializeManagers();
        registerCommandsAndEvents();

        // Prepare database connection status
        String dbStatus = getConfig().getBoolean("mysql.enable") && databaseManager.isMySQLConnected()
                ? "\u001B[32mMySQL: Connected ✅"
                : "\u001B[31mMySQL: Not Connected 🚫";

        // Check if LuckPerms is detected
        String luckPermsStatus = getServer().getPluginManager().getPlugin("LuckPerms") != null
                ? "\u001B[32mLuckPerms: Detected ✅"
                : "\u001B[31mLuckPerms: Not Detected 🚫";

        getLogger().info("\u001B[36m=======================================");
        getLogger().info("\u001B[33m  MineChatJson v1.1.0 by dxdrillbassx");
        getLogger().info("\u001B[36m=======================================");
        getLogger().info(dbStatus);
        getLogger().info(luckPermsStatus);
        getLogger().info("\u001B[32m  Плагин загружен! Готов к чатику! 🚀✨");
        getLogger().info("\u001B[36m=======================================");
        getLogger().info("\u001B[0m");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        getLogger().info("\u001B[36m=======================================");
        getLogger().info("\u001B[31m  MineChatJson v1.1.0 отключён! 😴");
        getLogger().info("\u001B[36m=======================================");
        getLogger().info("\u001B[0m");
    }

    private void initializeManagers() {
        databaseManager = new DatabaseManager(this);
        emojiManager = new EmojiManager(this);
        chatManager = new ChatManager(this, databaseManager);
        inventoryListener = new InventoryListener(this, chatManager, emojiManager);
        commandManager = new CommandManager(this, chatManager, databaseManager, emojiManager, inventoryListener);
    }

    private void registerCommandsAndEvents() {
        getServer().getPluginManager().registerEvents(chatManager, this);
        getServer().getPluginManager().registerEvents(inventoryListener, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", chatManager);

        commandManager.registerCommands();
    }

    public void reloadPluginConfig() {
        // Перезагружаем конфигурацию
        reloadConfig();

        // Закрываем существующее соединение с базой данных, если оно открыто
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }

        // Очищаем существующие слушатели событий и каналы сообщений
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getServer().getMessenger().unregisterIncomingPluginChannel(this);

        // Переинициализируем менеджеры
        initializeManagers();

        // Перерегистрируем команды и события
        registerCommandsAndEvents();

        getLogger().info("Конфигурация плагина MineChatJson перезагружена!");
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public EmojiManager getEmojiManager() {
        return emojiManager;
    }
}