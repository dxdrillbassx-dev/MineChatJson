package org.dxdrillbassx.mineChatJson;

import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class EmojiManager {
    private final Main plugin;
    private final Map<String, String> emojis = new HashMap<>();

    public EmojiManager(Main plugin) {
        this.plugin = plugin;
        initializeEmojis();
    }

    private void initializeEmojis() {
        emojis.put("smile", "😊");
        emojis.put("heart", "❤️");
        emojis.put("star", "⭐");
        emojis.put("fire", "🔥");
        emojis.put("rocket", "🚀");
        emojis.put("crown", "👑");
    }

    public String processMessage(String message, Player player) {
        String processed = message;
        for (Map.Entry<String, String> entry : emojis.entrySet()) {
            if (player.hasPermission("minechatjson.emoji." + entry.getKey())) {
                processed = processed.replace(":" + entry.getKey() + ":", entry.getValue());
            }
        }
        return processed;
    }

    public String getEmojiSymbol(String emojiKey) {
        return emojis.get(emojiKey.toLowerCase());
    }

    public Inventory createEmojiInventory(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, "Выбор эмодзи");

        int slot = 0;
        for (Map.Entry<String, String> entry : emojis.entrySet()) {
            if (player.hasPermission("minechatjson.emoji." + entry.getKey())) {
                ItemStack item = new ItemStack(Material.PAPER);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(entry.getValue() + " :" + entry.getKey() + ":");
                    meta.setLore(Arrays.asList("Нажмите, чтобы использовать", "в следующем сообщении", "&7Привет, " + player.getName() + "! " + entry.getValue()));
                    item.setItemMeta(meta);
                }
                inv.setItem(slot++, item);
            }
        }
        return inv;
    }
}