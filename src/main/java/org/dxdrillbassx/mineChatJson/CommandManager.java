package org.dxdrillbassx.mineChatJson;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class CommandManager {
    private final JavaPlugin plugin;
    private final ChatManager chatManager;
    private final DatabaseManager databaseManager;
    private final EmojiManager emojiManager;
    private final InventoryListener inventoryListener;

    public CommandManager(JavaPlugin plugin, ChatManager chatManager, DatabaseManager databaseManager, EmojiManager emojiManager, InventoryListener inventoryListener) {
        this.plugin = plugin;
        this.chatManager = chatManager;
        this.databaseManager = databaseManager;
        this.emojiManager = emojiManager;
        this.inventoryListener = inventoryListener;
    }

    public void registerCommands() {
        plugin.getCommand("chatcolor").setExecutor(new ChatColorCommand());
        plugin.getCommand("togglechat").setExecutor(new ToggleChatCommand());
        plugin.getCommand("help").setExecutor(new HelpCommand());
        plugin.getCommand("rename").setExecutor(new RenameCommand());
        plugin.getCommand("chatsound").setExecutor(new ChatSoundCommand());
        plugin.getCommand("chatsound").setTabCompleter(new ChatSoundTabCompleter());
        plugin.getCommand("emoji").setExecutor(new EmojiCommand());
        plugin.getCommand("reload").setExecutor(new ReloadCommand());
        plugin.getCommand("chateffect").setExecutor(new ChatEffectCommand());
        plugin.getCommand("joinmessage").setExecutor(new JoinMessageCommand());
        plugin.getCommand("joinmessage").setTabCompleter(new JoinMessageTabCompleter());
    }

    private class ChatColorCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Эта команда только для игроков! 🚫");
                return true;
            }

            Player player = (Player) sender;
            if (!player.hasPermission("minechatjson.chatcolor")) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("colors.msgs.no-perm", "&cУ вас нет прав 🚫")));
                return true;
            }

            player.openInventory(inventoryListener.createColorInventory(player));
            return true;
        }
    }

    private class ToggleChatCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!sender.hasPermission("minechatjson.togglechat")) {
                sender.sendMessage(ChatColor.RED + "У вас нет прав! 🚫");
                return true;
            }

            chatManager.setChatEnabled(!chatManager.isChatEnabled());
            String status = chatManager.isChatEnabled() ? "&aЧат включён! ✅" : "&cЧат выключен! 🔇";
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', status));
            if (plugin.getConfig().getBoolean("chat.broadcast-actions", true)) {
                Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                        chatManager.isChatEnabled() ? "&aЧат включён администратором! ✅" : "&cЧат выключен администратором! 🔇"));
            }
            return true;
        }
    }

    private class HelpCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&l=== MineChatJson Помощь ==="));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/chatcolor &7- Открыть меню выбора цвета чата 🎨"));
            if (sender.hasPermission("minechatjson.togglechat")) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/togglechat &7- Включить/выключать чат 🔊🔇"));
            }
            if (sender.hasPermission("minechatjson.rename")) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/rename <ник> &7- Изменить отображаемый ник ✏️"));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/rename remove &7- Сбросить ник на оригинальный 🗑️"));
            }
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/chatsound &7- Включить/выключать звук чата или выбрать звук 🔊"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/emoji &7- Открыть меню выбора эмодзи или предпросмотр 😊"));
            if (sender.hasPermission("minechatjson.chateffect")) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/chateffect &7- Открыть меню выбора эффекта текста 🎨"));
            }
            if (sender.hasPermission("minechatjson.reload")) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/reload &7- Перезагрузить конфигурацию плагина 🔄"));
            }
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/joinmessage &7- Выбрать или установить приветственное сообщение 👋"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/help &7- Показать эту помощь ℹ️"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&l=== Плагин by dxdrillbassx ==="));
            return true;
        }
    }

    private class RenameCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Эта команда только для игроков! 🚫");
                return true;
            }

            Player player = (Player) sender;
            if (!player.hasPermission("minechatjson.rename")) {
                player.sendMessage(ChatColor.RED + "У вас нет прав! 🚫");
                return true;
            }

            if (args.length != 1) {
                player.sendMessage(ChatColor.RED + "Использование: /rename <новый_ник> или /rename remove");
                return true;
            }

            try {
                if (args[0].equalsIgnoreCase("remove")) {
                    databaseManager.removePlayerNickname(player.getUniqueId());
                    player.setDisplayName(player.getName());
                    player.setPlayerListName(player.getName());
                    player.sendMessage(ChatColor.GREEN + "Ник сброшен на оригинальный! 🗑️");
                    return true;
                }

                String newNick = args[0];
                if (newNick.length() > 16) {
                    player.sendMessage(ChatColor.RED + "Ник не может быть длиннее 16 символов! 🚫");
                    return true;
                }

                if (!newNick.matches("[a-zA-Z0-9_]+")) {
                    player.sendMessage(ChatColor.RED + "Ник может содержать только буквы, цифры и подчёркивания! 🚫");
                    return true;
                }

                player.setDisplayName(ChatColor.translateAlternateColorCodes('&', newNick));
                player.setPlayerListName(ChatColor.translateAlternateColorCodes('&', newNick));
                databaseManager.setPlayerNickname(player.getUniqueId(), newNick);
                player.sendMessage(ChatColor.GREEN + "Ник изменён на " + newNick + "! ✏️");
                return true;
            } catch (Exception e) {
                plugin.getLogger().severe("Ошибка при выполнении /rename: " + e.getMessage());
                e.printStackTrace();
                player.sendMessage(ChatColor.RED + "Произошла ошибка при смене ника! 😔");
                return true;
            }
        }
    }

    private class ChatSoundCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Эта команда только для игроков! 🚫");
                return true;
            }

            Player player = (Player) sender;
            if (args.length == 0) {
                boolean currentSoundState = databaseManager.isChatSoundEnabled(player.getUniqueId());
                boolean newSoundState = !currentSoundState;
                databaseManager.setChatSound(player.getUniqueId(), newSoundState);
                String status = newSoundState ? "&aЗвук чата включён! 🔊" : "&cЗвук чата выключен! 🔇";
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', status));
                return true;
            } else if (args[0].equalsIgnoreCase("select")) {
                if (!player.hasPermission("minechatjson.chatsound.select")) {
                    player.sendMessage(ChatColor.RED + "У вас нет прав для выбора звука! 🚫");
                    return true;
                }
                player.openInventory(inventoryListener.createSoundInventory(player));
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "Использование: /chatsound [select]");
                return true;
            }
        }
    }

    private class ChatSoundTabCompleter implements TabCompleter {
        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            List<String> completions = new ArrayList<>();
            if (args.length == 1 && sender.hasPermission("minechatjson.chatsound.select")) {
                completions.add("select");
            }
            return completions;
        }
    }

    private class EmojiCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Эта команда только для игроков! 🚫");
                return true;
            }

            Player player = (Player) sender;
            if (!player.hasPermission("minechatjson.emoji")) {
                player.sendMessage(ChatColor.RED + "У вас нет прав! 🚫");
                return true;
            }

            if (args.length == 0) {
                player.openInventory(emojiManager.createEmojiInventory(player));
                return true;
            } else if (args[0].equalsIgnoreCase("preview") && args.length == 2) {
                if (!player.hasPermission("minechatjson.emoji.preview")) {
                    player.sendMessage(ChatColor.RED + "У вас нет прав для предпросмотра эмодзи! 🚫");
                    return true;
                }
                String emojiKey = args[1].toLowerCase();
                String emoji = emojiManager.getEmojiSymbol(emojiKey);
                if (emoji == null || !player.hasPermission("minechatjson.emoji." + emojiKey)) {
                    player.sendMessage(ChatColor.RED + "Эмодзи не найдено или у вас нет прав! 🚫");
                    return true;
                }
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&eПредпросмотр: &fПривет, " + player.getName() + "! " + emoji));
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "Использование: /emoji [preview <название_эмодзи>]");
                return true;
            }
        }
    }

    private class ReloadCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!sender.hasPermission("minechatjson.reload")) {
                sender.sendMessage(ChatColor.RED + "У вас нет прав! 🚫");
                return true;
            }

            try {
                ((Main) plugin).reloadPluginConfig();
                sender.sendMessage(ChatColor.GREEN + "Конфигурация плагина успешно перезагружена! ✅");
                if (plugin.getConfig().getBoolean("chat.broadcast-actions", true)) {
                    Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                            "&aКонфигурация плагина перезагружена администратором! 🔄"));
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Ошибка при перезагрузке конфигурации: " + e.getMessage());
                e.printStackTrace();
                sender.sendMessage(ChatColor.RED + "Произошла ошибка при перезагрузке конфигурации! 😔");
            }
            return true;
        }
    }

    private class ChatEffectCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Эта команда только для игроков! 🚫");
                return true;
            }

            Player player = (Player) sender;
            if (!player.hasPermission("minechatjson.chateffect")) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("effects.msgs.no-perm", "&cУ вас нет прав 🚫")));
                return true;
            }

            player.openInventory(inventoryListener.createEffectInventory(player));
            return true;
        }
    }

    private class JoinMessageCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Эта команда только для игроков! 🚫");
                return true;
            }

            Player player = (Player) sender;
            if (!player.hasPermission("minechatjson.joinmessage")) {
                player.sendMessage(ChatColor.RED + "У вас нет прав! 🚫");
                return true;
            }

            if (args.length == 0) {
                player.openInventory(inventoryListener.createJoinMessageInventory(player));
                return true;
            } else if (args[0].equalsIgnoreCase("set") && args.length > 1) {
                if (!player.hasPermission("minechatjson.joinmessage.set")) {
                    player.sendMessage(ChatColor.RED + "У вас нет прав для установки собственного сообщения! 🚫");
                    return true;
                }
                String message = String.join(" ", args).substring(4); // Удаляем "set "
                if (message.length() > 100) {
                    player.sendMessage(ChatColor.RED + "Сообщение не может быть длиннее 100 символов! 🚫");
                    return true;
                }
                databaseManager.setPlayerJoinMessage(player.getUniqueId(), message);
                player.sendMessage(ChatColor.GREEN + "Приветственное сообщение установлено: " + ChatColor.translateAlternateColorCodes('&', message) + " ✅");
                return true;
            } else if (args[0].equalsIgnoreCase("remove")) {
                databaseManager.removePlayerJoinMessage(player.getUniqueId());
                player.sendMessage(ChatColor.GREEN + "Приветственное сообщение сброшено на стандартное! 🗑️");
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "Использование: /joinmessage [set <сообщение> | remove]");
                return true;
            }
        }
    }

    private class JoinMessageTabCompleter implements TabCompleter {
        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            List<String> completions = new ArrayList<>();
            if (args.length == 1 && sender.hasPermission("minechatjson.joinmessage.set")) {
                completions.add("set");
                completions.add("remove");
            }
            return completions;
        }
    }
}