package org.dxdrillbassx.mineChatJson;

import net.luckperms.api.LuckPermsProvider;
import net.md_5.bungee.api.chat.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatManager implements Listener, PluginMessageListener {
    private final Main plugin;
    private final DatabaseManager databaseManager;
    private final Map<UUID, String> playerData = new HashMap<>();
    private boolean chatEnabled = true;

    public ChatManager(Main plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!chatEnabled) {
            event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("chat-toggle-msg", "&cЧат отключён! 😔")));
            event.setCancelled(true);
            return;
        }

        Player player = event.getPlayer();
        String rawMessage = event.getMessage();
        boolean isGlobal = plugin.getConfig().getBoolean("use-global-chat") || rawMessage.startsWith("!");

        // Remove '!' prefix for global chat messages
        String message = isGlobal && rawMessage.startsWith("!") ? rawMessage.substring(1) : rawMessage;

        // Process emojis
        message = plugin.getEmojiManager().processMessage(message, player);

        // Apply chat effect after determining global/local
        String effect = databaseManager.getPlayerEffect(player.getUniqueId());
        if (effect != null) {
            message = effect + message;
        }

        String format = getChatFormat(player, message, isGlobal);
        TextComponent component = createChatComponent(player, message, format);

        event.setCancelled(true);
        for (Player recipient : event.getRecipients()) {
            if (databaseManager.isChatSoundEnabled(recipient.getUniqueId())) {
                String soundName = databaseManager.getPlayerSound(recipient.getUniqueId());
                try {
                    Sound sound = Sound.valueOf(soundName.replace("minecraft:", "").replace(".", "_").toUpperCase());
                    recipient.playSound(recipient.getLocation(), sound, 1.0f, 1.0f);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid sound name for player " + recipient.getName() + ": " + soundName + ". Defaulting to ENTITY_EXPERIENCE_ORB_PICKUP.");
                    recipient.playSound(recipient.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                }
            }
            recipient.spigot().sendMessage(component);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String joinMessage = databaseManager.getPlayerJoinMessage(player.getUniqueId());
        String playerName = databaseManager.getPlayerNickname(player.getUniqueId());
        if (playerName == null) {
            playerName = player.getName();
        } else {
            playerName = playerName.replace("*", "");
        }

        // Suppress default join message
        event.setJoinMessage(null);

        if (joinMessage != null) {
            // Replace %player% with player name or nickname and apply color codes
            String formattedMessage = ChatColor.translateAlternateColorCodes('&',
                    joinMessage.replace("%player%", playerName));
            Bukkit.broadcastMessage(formattedMessage);
        } else {
            // Use default Minecraft join message format if no custom message is set
            String defaultMessage = ChatColor.YELLOW + playerName + " joined the game";
            Bukkit.broadcastMessage(defaultMessage);
        }

        // Request BungeeCord data
        requestPlayerData(player, "marry");
        requestPlayerData(player, "kill");
        requestPlayerData(player, "tag");
    }

    private String getChatFormat(Player player, String message, boolean isGlobal) {
        String world = player.getWorld().getName();
        boolean isWorldChat = plugin.getConfig().getStringList("worldList").contains(world) &&
                plugin.getConfig().getBoolean("worldchat.enable");

        String format;
        if (isWorldChat) {
            format = plugin.getConfig().getString("worldchat.format", "%prefix%player%suffix &a✎ &f%message");
        } else if (isGlobal) {
            format = plugin.getConfig().getString("formats.global", "&9[&cⒼ&9] %js%!marry! %js%!tag! %js%%prefix%player%suffix%js% &a✎ &f%message");
        } else {
            format = plugin.getConfig().getString("formats.local", "&9[&cⓁ&9] %js%!marry! %js%!tag! %js%%prefix%player%suffix%js% &a✎ &7%message");
        }

        String nickname = databaseManager.getPlayerNickname(player.getUniqueId());
        return format.replace("%player", nickname != null ? nickname : player.getName());
    }

    private TextComponent createChatComponent(Player player, String message, String format) {
        String color = databaseManager.getPlayerColor(player.getUniqueId());
        String processedFormat = format.replace("%prefix", LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId()).getCachedData().getMetaData().getPrefix() != null ?
                        LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId()).getCachedData().getMetaData().getPrefix() : "")
                .replace("%suffix", LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId()).getCachedData().getMetaData().getSuffix() != null ?
                        LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId()).getCachedData().getMetaData().getSuffix() : "")
                .replace("%message", color + message)
                .replace("%js%", "")
                .replace("!marry!", playerData.getOrDefault(player.getUniqueId() + "_marry", "Single"))
                .replace("!tag!", playerData.getOrDefault(player.getUniqueId() + "_tag", "NoClan"))
                .replace("!kills!", playerData.getOrDefault(player.getUniqueId() + "_kill", "0"));

        TextComponent component = new TextComponent(ChatColor.translateAlternateColorCodes('&', processedFormat));
        addHoverAndClickEvents(component, player);
        return component;
    }

    private void addHoverAndClickEvents(TextComponent component, Player player) {
        plugin.getConfig().getConfigurationSection("json").getKeys(false).forEach(key -> {
            if (component.getText().contains(key)) {
                BaseComponent[] hoverText = new ComponentBuilder(
                        String.join("\n", plugin.getConfig().getStringList("json." + key + ".text"))
                                .replace("%player%", player.getName())
                                .replace("%marry_format%", playerData.getOrDefault(player.getUniqueId() + "_marry", "Single"))
                                .replace("%kills%", playerData.getOrDefault(player.getUniqueId() + "_kill", "0"))
                ).create();

                component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText));
                if (plugin.getConfig().getString("json." + key + ".runCmd") != null) {
                    component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                            plugin.getConfig().getString("json." + key + ".runCmd")));
                } else if (plugin.getConfig().getString("json." + key + ".suggestChat") != null) {
                    component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                            plugin.getConfig().getString("json." + key + ".suggestChat").replace("%player%", player.getName())));
                }
            }
        });
    }

    public void requestPlayerData(Player player, String dataType) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF("MineChatJsonData");
        ByteArrayDataOutput data = ByteStreams.newDataOutput();
        data.writeUTF(dataType);
        data.writeUTF(player.getUniqueId().toString());
        out.writeShort(data.toByteArray().length);
        out.write(data.toByteArray());
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();
        if (!subChannel.equals("MineChatJsonData")) return;

        try {
            short len = in.readShort();
            byte[] msgBytes = new byte[len];
            in.readFully(msgBytes);
            ByteArrayDataInput data = ByteStreams.newDataInput(msgBytes);
            String dataType = data.readUTF();
            String uuid = data.readUTF();
            String value = data.readUTF();

            playerData.put(UUID.fromString(UUID.fromString(uuid) + "_" + dataType), value);
            plugin.getLogger().info("Received BungeeCord data: type=" + dataType + ", uuid=" + uuid + ", value=" + value);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid UUID received: " + e.getMessage());
        }
    }

    public void setChatEnabled(boolean enabled) {
        this.chatEnabled = enabled;
    }

    public boolean isChatEnabled() {
        return chatEnabled;
    }
}