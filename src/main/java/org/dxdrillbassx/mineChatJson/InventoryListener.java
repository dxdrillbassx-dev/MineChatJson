package org.dxdrillbassx.mineChatJson;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class InventoryListener implements Listener {
    private final Main plugin;
    private final ChatManager chatManager;
    private final EmojiManager emojiManager;

    public InventoryListener(Main plugin, ChatManager chatManager, EmojiManager emojiManager) {
        this.plugin = plugin;
        this.chatManager = chatManager;
        this.emojiManager = emojiManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String inventoryTitle = event.getView().getTitle();
        if (!inventoryTitle.equals(plugin.getConfig().getString("colors.inv-name", "Выбор цвета 💬")) &&
                !inventoryTitle.equals("Выбор эмодзи") &&
                !inventoryTitle.equals(plugin.getConfig().getString("effects.inv-name", "Выбор эффекта 💬")) &&
                !inventoryTitle.equals(plugin.getConfig().getString("chat.sounds.inv-name", "Выбор звука 🔊")) &&
                !inventoryTitle.equals(plugin.getConfig().getString("joinmessages.inv-name", "Выбор приветственного сообщения 👋"))) {
            return;
        }
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String clickedName = clicked.getItemMeta().getDisplayName();

        try {
            if (inventoryTitle.equals(plugin.getConfig().getString("colors.inv-name", "Выбор цвета 💬"))) {
                handleColorInventoryClick(player, clickedName);
            } else if (inventoryTitle.equals("Выбор эмодзи")) {
                handleEmojiInventoryClick(player, clickedName);
            } else if (inventoryTitle.equals(plugin.getConfig().getString("effects.inv-name", "Выбор эффекта 💬"))) {
                handleEffectInventoryClick(player, clickedName);
            } else if (inventoryTitle.equals(plugin.getConfig().getString("chat.sounds.inv-name", "Выбор звука 🔊"))) {
                handleSoundInventoryClick(player, clickedName);
            } else if (inventoryTitle.equals(plugin.getConfig().getString("joinmessages.inv-name", "Выбор приветственного сообщения 👋"))) {
                handleJoinMessageInventoryClick(player, clickedName);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при обработке клика в инвентаре: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage(ChatColor.RED + "Произошла ошибка при выборе! 😔");
        }
    }

    private void handleColorInventoryClick(Player player, String clickedName) {
        String currentColor = plugin.getDatabaseManager().getPlayerColor(player.getUniqueId());

        if (clickedName.equals(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("colors.remove-item.name", "&4Сбросить цвет 🗑️")))) {
            plugin.getDatabaseManager().removePlayerColor(player.getUniqueId());
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("colors.msgs.deleted", "&eЦвет сброшен до стандартного ⚪")));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            player.closeInventory();
            return;
        }

        if (plugin.getConfig().getConfigurationSection("colors.sitems") == null) {
            plugin.getLogger().severe("Секция 'colors.sitems' в конфиге отсутствует или некорректна!");
            player.sendMessage(ChatColor.RED + "Ошибка: Конфигурация цветов недоступна! 😔");
            return;
        }

        for (String colorKey : plugin.getConfig().getConfigurationSection("colors.sitems").getKeys(false)) {
            String path = "colors.sitems." + colorKey;
            String name = ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString(path + ".name", "&cНеизвестный цвет"));
            String format = plugin.getConfig().getString(path + ".format");

            if (clickedName.equals(name)) {
                if (!player.hasPermission("minechatjson.color." + colorKey.toLowerCase())) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            plugin.getConfig().getString("colors.msgs.no-perm", "&cУ вас нет прав 🚫")));
                    return;
                }
                if (format == null) {
                    plugin.getLogger().warning("Отсутствует формат для цвета '" + colorKey + "'");
                    player.sendMessage(ChatColor.RED + "Ошибка: Формат цвета не задан! 😔");
                    return;
                }
                if (format.equals(currentColor)) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            plugin.getConfig().getString("colors.msgs.already-choosen", "&eЭтот цвет уже выбран 🎨")));
                    return;
                }
                plugin.getDatabaseManager().setPlayerColor(player.getUniqueId(), format);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("colors.msgs.color-set", "&aЦвет успешно установлен! ✅")));
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                player.closeInventory();
                return;
            }
        }
    }

    private void handleEmojiInventoryClick(Player player, String clickedName) {
        for (String emojiKey : plugin.getConfig().getConfigurationSection("emojis").getKeys(false)) {
            String symbol = plugin.getConfig().getString("emojis." + emojiKey + ".symbol");
            if (clickedName.equals(symbol + " :" + emojiKey + ":")) {
                if (!player.hasPermission("minechatjson.emoji." + emojiKey)) {
                    player.sendMessage(ChatColor.RED + "У вас нет прав на этот эмодзи! 🚫");
                    return;
                }
                player.chat(":" + emojiKey + ":");
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                return;
            }
        }
    }

    private void handleEffectInventoryClick(Player player, String clickedName) {
        String currentEffect = plugin.getDatabaseManager().getPlayerEffect(player.getUniqueId());

        if (clickedName.equals(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("effects.remove-item.name", "&4Сбросить эффект 🗑️")))) {
            plugin.getDatabaseManager().removePlayerEffect(player.getUniqueId());
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("effects.msgs.deleted", "&eЭффект сброшен до стандартного ⚪")));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            player.closeInventory();
            return;
        }

        if (plugin.getConfig().getConfigurationSection("effects.sitems") == null) {
            plugin.getLogger().severe("Секция 'effects.sitems' в конфиге отсутствует или некорректна!");
            player.sendMessage(ChatColor.RED + "Ошибка: Конфигурация эффектов недоступна! 😔");
            return;
        }

        for (String effectKey : plugin.getConfig().getConfigurationSection("effects.sitems").getKeys(false)) {
            String path = "effects.sitems." + effectKey;
            String name = ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString(path + ".name", "&cНеизвестный эффект"));
            String format = plugin.getConfig().getString(path + ".format");

            if (clickedName.equals(name)) {
                if (!player.hasPermission("minechatjson.effect." + effectKey.toLowerCase())) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            plugin.getConfig().getString("effects.msgs.no-perm", "&cУ вас нет прав 🚫")));
                    return;
                }
                if (format == null) {
                    plugin.getLogger().warning("Отсутствует формат для эффекта '" + effectKey + "'");
                    player.sendMessage(ChatColor.RED + "Ошибка: Формат эффекта не задан! 😔");
                    return;
                }
                if (format.equals(currentEffect)) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            plugin.getConfig().getString("effects.msgs.already-choosen", "&eЭтот эффект уже выбран 🎨")));
                    return;
                }
                plugin.getDatabaseManager().setPlayerEffect(player.getUniqueId(), format);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("effects.msgs.effect-set", "&aЭффект успешно установлен! ✅")));
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                player.closeInventory();
                return;
            }
        }
    }

    private void handleSoundInventoryClick(Player player, String clickedName) {
        String currentSound = plugin.getDatabaseManager().getPlayerSound(player.getUniqueId());

        if (clickedName.equals(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("chat.sounds.remove-item.name", "&4Сбросить звук 🗑️")))) {
            plugin.getDatabaseManager().setChatSound(player.getUniqueId(), true,
                    plugin.getConfig().getString("chat.sound.type", "minecraft:entity.experience_orb.pickup"));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("chat.sounds.msgs.deleted", "&eЗвук сброшен до стандартного ⚪")));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            player.closeInventory();
            return;
        }

        if (plugin.getConfig().getConfigurationSection("chat.sounds.sitems") == null) {
            plugin.getLogger().severe("Секция 'chat.sounds.sitems' в конфиге отсутствует или некорректна!");
            player.sendMessage(ChatColor.RED + "Ошибка: Конфигурация звуков недоступна! 😔");
            return;
        }

        for (String soundKey : plugin.getConfig().getConfigurationSection("chat.sounds.sitems").getKeys(false)) {
            String path = "chat.sounds.sitems." + soundKey;
            String name = ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString(path + ".name", "&cНеизвестный звук"));
            String sound = plugin.getConfig().getString(path + ".sound");

            if (clickedName.equals(name)) {
                if (!player.hasPermission("minechatjson.sound." + soundKey.toLowerCase())) {
                    player.sendMessage(ChatColor.RED + "У вас нет прав для выбора этого звука! 🚫");
                    return;
                }
                if (sound == null) {
                    plugin.getLogger().warning("Отсутствует звук для '" + soundKey + "'");
                    player.sendMessage(ChatColor.RED + "Ошибка: Звук не задан! 😔");
                    return;
                }
                if (sound.equals(currentSound)) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&eЭтот звук уже выбран 🔊"));
                    return;
                }
                plugin.getDatabaseManager().setChatSound(player.getUniqueId(), true, sound);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&aЗвук успешно установлен! ✅"));
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                player.closeInventory();
                return;
            }
        }
    }

    private void handleJoinMessageInventoryClick(Player player, String clickedName) {
        String currentMessage = plugin.getDatabaseManager().getPlayerJoinMessage(player.getUniqueId());

        if (clickedName.equals(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("joinmessages.remove-item.name", "&4Сбросить сообщение 🗑️")))) {
            plugin.getDatabaseManager().removePlayerJoinMessage(player.getUniqueId());
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&eПриветственное сообщение сброшено до стандартного ⚪"));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            player.closeInventory();
            return;
        }

        if (plugin.getConfig().getConfigurationSection("joinmessages.sitems") == null) {
            plugin.getLogger().severe("Секция 'joinmessages.sitems' в конфиге отсутствует или некорректна!");
            player.sendMessage(ChatColor.RED + "Ошибка: Конфигурация сообщений недоступна! 😔");
            return;
        }

        for (String messageKey : plugin.getConfig().getConfigurationSection("joinmessages.sitems").getKeys(false)) {
            String path = "joinmessages.sitems." + messageKey;
            String name = ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString(path + ".name", "&cНеизвестное сообщение"));
            String message = plugin.getConfig().getString(path + ".message");

            if (clickedName.equals(name)) {
                if (!player.hasPermission("minechatjson.joinmessage")) {
                    player.sendMessage(ChatColor.RED + "У вас нет прав для выбора этого сообщения! 🚫");
                    return;
                }
                if (message == null) {
                    plugin.getLogger().warning("Отсутствует сообщение для '" + messageKey + "'");
                    player.sendMessage(ChatColor.RED + "Ошибка: Сообщение не задано! 😔");
                    return;
                }
                if (message.equals(currentMessage)) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&eЭто сообщение уже выбрано 👋"));
                    return;
                }
                plugin.getDatabaseManager().setPlayerJoinMessage(player.getUniqueId(), message);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&aПриветственное сообщение успешно установлено! ✅"));
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                player.closeInventory();
                return;
            }
        }
    }

    public Inventory createColorInventory(Player player) {
        Inventory inv = org.bukkit.Bukkit.createInventory(null, 9,
                plugin.getConfig().getString("colors.inv-name", "Выбор цвета 💬"));
        String currentColor = plugin.getDatabaseManager().getPlayerColor(player.getUniqueId());

        if (plugin.getConfig().getConfigurationSection("colors.sitems") == null) {
            plugin.getLogger().severe("Секция 'colors.sitems' в конфиге отсутствует или некорректна!");
            return inv;
        }

        for (String colorKey : plugin.getConfig().getConfigurationSection("colors.sitems").getKeys(false)) {
            String path = "colors.sitems." + colorKey;
            String name = plugin.getConfig().getString(path + ".name", "&cНеизвестный цвет");
            String materialName = plugin.getConfig().getString(path + ".material", "WHITE_WOOL");
            int position = plugin.getConfig().getInt(path + ".position", 1);
            List<String> lore = plugin.getConfig().getStringList(path + ".lore");

            Material material = Material.getMaterial(materialName.toUpperCase());
            if (material == null) {
                plugin.getLogger().warning("Некорректный материал '" + materialName + "' для цвета '" + colorKey + "', используется WHITE_WOOL");
                material = Material.WHITE_WOOL;
            }

            ItemStack item = new ItemStack(material, 1);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
                meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
                item.setItemMeta(meta);
            }
            if (position >= 1 && position <= 9) {
                inv.setItem(position - 1, item);
            } else {
                plugin.getLogger().warning("Некорректная позиция '" + position + "' для цвета '" + colorKey + "', пропускается");
            }
        }

        String removeMaterialName = plugin.getConfig().getString("colors.remove-item.material", "BARRIER");
        Material removeMaterial = Material.getMaterial(removeMaterialName.toUpperCase());
        if (removeMaterial == null) {
            plugin.getLogger().warning("Некорректный материал '" + removeMaterialName + "' для remove-item, используется BARRIER");
            removeMaterial = Material.BARRIER;
        }
        ItemStack removeItem = new ItemStack(removeMaterial, 1);
        ItemMeta removeMeta = removeItem.getItemMeta();
        if (removeMeta != null) {
            removeMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("colors.remove-item.name", "&4Сбросить цвет 🗑️")));
            removeMeta.setLore(plugin.getConfig().getStringList("colors.remove-item.lore").stream()
                    .map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            removeItem.setItemMeta(removeMeta);
        }
        int removePosition = plugin.getConfig().getInt("colors.remove-item.position", 9);
        if (removePosition >= 1 && removePosition <= 9) {
            inv.setItem(removePosition - 1, removeItem);
        } else {
            plugin.getLogger().warning("Некорректная позиция '" + removePosition + "' для remove-item, используется слот 8");
            inv.setItem(8, removeItem);
        }

        return inv;
    }

    public Inventory createEffectInventory(Player player) {
        Inventory inv = org.bukkit.Bukkit.createInventory(null, 9,
                plugin.getConfig().getString("effects.inv-name", "Выбор эффекта 💬"));
        String currentEffect = plugin.getDatabaseManager().getPlayerEffect(player.getUniqueId());

        if (plugin.getConfig().getConfigurationSection("effects.sitems") == null) {
            plugin.getLogger().severe("Секция 'effects.sitems' в конфиге отсутствует или некорректна!");
            return inv;
        }

        for (String effectKey : plugin.getConfig().getConfigurationSection("effects.sitems").getKeys(false)) {
            String path = "effects.sitems." + effectKey;
            String name = plugin.getConfig().getString(path + ".name", "&cНеизвестный эффект");
            String materialName = plugin.getConfig().getString(path + ".material", "FEATHER");
            int position = plugin.getConfig().getInt(path + ".position", 1);
            List<String> lore = plugin.getConfig().getStringList(path + ".lore");

            Material material = Material.getMaterial(materialName.toUpperCase());
            if (material == null) {
                plugin.getLogger().warning("Некорректный материал '" + materialName + "' для эффекта '" + effectKey + "', используется FEATHER");
                material = Material.FEATHER;
            }

            ItemStack item = new ItemStack(material, 1);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
                meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
                item.setItemMeta(meta);
            }
            if (position >= 1 && position <= 9) {
                inv.setItem(position - 1, item);
            } else {
                plugin.getLogger().warning("Некорректная позиция '" + position + "' для эффекта '" + effectKey + "', пропускается");
            }
        }

        String removeMaterialName = plugin.getConfig().getString("effects.remove-item.material", "BARRIER");
        Material removeMaterial = Material.getMaterial(removeMaterialName.toUpperCase());
        if (removeMaterial == null) {
            plugin.getLogger().warning("Некорректный материал '" + removeMaterialName + "' для remove-item, используется BARRIER");
            removeMaterial = Material.BARRIER;
        }
        ItemStack removeItem = new ItemStack(removeMaterial, 1);
        ItemMeta removeMeta = removeItem.getItemMeta();
        if (removeMeta != null) {
            removeMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("effects.remove-item.name", "&4Сбросить эффект 🗑️")));
            removeMeta.setLore(plugin.getConfig().getStringList("effects.remove-item.lore").stream()
                    .map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            removeItem.setItemMeta(removeMeta);
        }
        int removePosition = plugin.getConfig().getInt("effects.remove-item.position", 9);
        if (removePosition >= 1 && removePosition <= 9) {
            inv.setItem(removePosition - 1, removeItem);
        } else {
            plugin.getLogger().warning("Некорректная позиция '" + removePosition + "' для remove-item, используется слот 8");
            inv.setItem(8, removeItem);
        }

        return inv;
    }

    public Inventory createSoundInventory(Player player) {
        Inventory inv = org.bukkit.Bukkit.createInventory(null, 9,
                plugin.getConfig().getString("chat.sounds.inv-name", "Выбор звука 🔊"));
        String currentSound = plugin.getDatabaseManager().getPlayerSound(player.getUniqueId());

        if (plugin.getConfig().getConfigurationSection("chat.sounds.sitems") == null) {
            plugin.getLogger().severe("Секция 'chat.sounds.sitems' в конфиге отсутствует или некорректна!");
            return inv;
        }

        for (String soundKey : plugin.getConfig().getConfigurationSection("chat.sounds.sitems").getKeys(false)) {
            String path = "chat.sounds.sitems." + soundKey;
            String name = plugin.getConfig().getString(path + ".name", "&cНеизвестный звук");
            String materialName = plugin.getConfig().getString(path + ".material", "NOTE_BLOCK");
            int position = plugin.getConfig().getInt(path + ".position", 1);
            List<String> lore = plugin.getConfig().getStringList(path + ".lore");

            Material material = Material.getMaterial(materialName.toUpperCase());
            if (material == null) {
                plugin.getLogger().warning("Некорректный материал '" + materialName + "' для звука '" + soundKey + "', используется NOTE_BLOCK");
                material = Material.NOTE_BLOCK;
            }

            ItemStack item = new ItemStack(material, 1);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
                meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
                item.setItemMeta(meta);
            }
            if (position >= 1 && position <= 9) {
                inv.setItem(position - 1, item);
            } else {
                plugin.getLogger().warning("Некорректная позиция '" + position + "' для звука '" + soundKey + "', пропускается");
            }
        }

        String removeMaterialName = plugin.getConfig().getString("chat.sounds.remove-item.material", "BARRIER");
        Material removeMaterial = Material.getMaterial(removeMaterialName.toUpperCase());
        if (removeMaterial == null) {
            plugin.getLogger().warning("Некорректный материал '" + removeMaterialName + "' для remove-item, используется BARRIER");
            removeMaterial = Material.BARRIER;
        }
        ItemStack removeItem = new ItemStack(removeMaterial, 1);
        ItemMeta removeMeta = removeItem.getItemMeta();
        if (removeMeta != null) {
            removeMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("chat.sounds.remove-item.name", "&4Сбросить звук 🗑️")));
            removeMeta.setLore(plugin.getConfig().getStringList("chat.sounds.remove-item.lore").stream()
                    .map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            removeItem.setItemMeta(removeMeta);
        }
        int removePosition = plugin.getConfig().getInt("chat.sounds.remove-item.position", 9);
        if (removePosition >= 1 && removePosition <= 9) {
            inv.setItem(removePosition - 1, removeItem);
        } else {
            plugin.getLogger().warning("Некорректная позиция '" + removePosition + "' для remove-item, используется слот 8");
            inv.setItem(8, removeItem);
        }

        return inv;
    }

    public Inventory createJoinMessageInventory(Player player) {
        Inventory inv = org.bukkit.Bukkit.createInventory(null, 9,
                plugin.getConfig().getString("joinmessages.inv-name", "Выбор приветственного сообщения 👋"));
        String currentMessage = plugin.getDatabaseManager().getPlayerJoinMessage(player.getUniqueId());

        if (plugin.getConfig().getConfigurationSection("joinmessages.sitems") == null) {
            plugin.getLogger().severe("Секция 'joinmessages.sitems' в конфиге отсутствует или некорректна!");
            return inv;
        }

        for (String messageKey : plugin.getConfig().getConfigurationSection("joinmessages.sitems").getKeys(false)) {
            String path = "joinmessages.sitems." + messageKey;
            String name = plugin.getConfig().getString(path + ".name", "&cНеизвестное сообщение");
            String materialName = plugin.getConfig().getString(path + ".material", "PAPER");
            int position = plugin.getConfig().getInt(path + ".position", 1);
            List<String> lore = plugin.getConfig().getStringList(path + ".lore");

            Material material = Material.getMaterial(materialName.toUpperCase());
            if (material == null) {
                plugin.getLogger().warning("Некорректный материал '" + materialName + "' для сообщения '" + messageKey + "', используется PAPER");
                material = Material.PAPER;
            }

            ItemStack item = new ItemStack(material, 1);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
                meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
                item.setItemMeta(meta);
            }
            if (position >= 1 && position <= 9) {
                inv.setItem(position - 1, item);
            } else {
                plugin.getLogger().warning("Некорректная позиция '" + position + "' для сообщения '" + messageKey + "', пропускается");
            }
        }

        String removeMaterialName = plugin.getConfig().getString("joinmessages.remove-item.material", "BARRIER");
        Material removeMaterial = Material.getMaterial(removeMaterialName.toUpperCase());
        if (removeMaterial == null) {
            plugin.getLogger().warning("Некорректный материал '" + removeMaterialName + "' для remove-item, используется BARRIER");
            removeMaterial = Material.BARRIER;
        }
        ItemStack removeItem = new ItemStack(removeMaterial, 1);
        ItemMeta removeMeta = removeItem.getItemMeta();
        if (removeMeta != null) {
            removeMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("joinmessages.remove-item.name", "&4Сбросить сообщение 🗑️")));
            removeMeta.setLore(plugin.getConfig().getStringList("joinmessages.remove-item.lore").stream()
                    .map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            removeItem.setItemMeta(removeMeta);
        }
        int removePosition = plugin.getConfig().getInt("joinmessages.remove-item.position", 9);
        if (removePosition >= 1 && removePosition <= 9) {
            inv.setItem(removePosition - 1, removeItem);
        } else {
            plugin.getLogger().warning("Некорректная позиция '" + removePosition + "' для remove-item, используется слот 8");
            inv.setItem(8, removeItem);
        }

        return inv;
    }
}