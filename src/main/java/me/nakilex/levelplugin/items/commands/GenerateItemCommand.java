package me.nakilex.levelplugin.items.commands;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GenerateItemCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /genitem [player] <mobType> <level> [amount]");
            return true;
        }

        int idx = 0;
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target != null && args.length >= 3) {
            idx = 1;
        } else {
            if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                sender.sendMessage("§cUsage: /genitem <player> <mobType> <level> [amount]");
                return true;
            }
        }

        String mobType = args[idx];
        if (idx + 1 >= args.length) {
            sender.sendMessage("§cUsage: /genitem [player] <mobType> <level> [amount]");
            return true;
        }

        int level;
        try {
            level = Integer.parseInt(args[idx + 1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid level: " + args[idx + 1]);
            return true;
        }

        int amount = 1;
        if (idx + 2 < args.length) {
            try {
                amount = Integer.parseInt(args[idx + 2]);
            } catch (NumberFormatException ignored) { }
        }

        for (int i = 0; i < amount; i++) {
            CustomItem item = ItemManager.getInstance().generateItem(mobType, level);
            target.getInventory().addItem(ItemUtil.createItemStackFromCustomItem(item, 1, target));
        }

        sender.sendMessage("§aGenerated " + amount + " item(s) for " + target.getName());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            String start = args[0].toLowerCase(Locale.ROOT);
            Bukkit.getOnlinePlayers().forEach(p -> {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(start)) {
                    suggestions.add(p.getName());
                }
            });
            ConfigurationSection mobs = Main.getInstance().getMobRewardsConfig().getConfig().getConfigurationSection("mobs");
            if (mobs != null) {
                for (String key : mobs.getKeys(false)) {
                    if (key.toLowerCase(Locale.ROOT).startsWith(start)) {
                        suggestions.add(key);
                    }
                }
            }
            return suggestions;
        }
        if (args.length == 2) {
            Player possible = Bukkit.getPlayerExact(args[0]);
            if (possible != null) {
                String start = args[1].toLowerCase(Locale.ROOT);
                ConfigurationSection mobs = Main.getInstance().getMobRewardsConfig().getConfig().getConfigurationSection("mobs");
                if (mobs != null) {
                    for (String key : mobs.getKeys(false)) {
                        if (key.toLowerCase(Locale.ROOT).startsWith(start)) {
                            suggestions.add(key);
                        }
                    }
                }
            }
        }
        return suggestions;
    }
}
