package me.nakilex.levelplugin.screen;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Command to showcase the cursor menu system. Allows running a simple
 * menu comprised of an item display and text caption, stopping it, and
 * listing available config entries.
 */
public class CursorMenuCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final CursorMenuManager menuManager = new CursorMenuManager();
    private final Map<String, ItemStack> items = new HashMap<>();
    private final Map<String, String> texts = new HashMap<>();

    public CursorMenuCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    private void loadConfigs() {
        File dir = new File(plugin.getDataFolder(), "cursormenu");
        if (!dir.exists()) {
            dir.mkdirs();
            plugin.saveResource("cursormenu/config.yml", false);
            plugin.saveResource("cursormenu/items.yml", false);
            plugin.saveResource("cursormenu/text.yml", false);
        }
        FileConfiguration itemCfg = YamlConfiguration.loadConfiguration(new File(dir, "items.yml"));
        for (String key : itemCfg.getKeys(false)) {
            Material mat = Material.matchMaterial(itemCfg.getString(key + ".material", "STONE"));
            ItemStack stack = new ItemStack(mat != null ? mat : Material.STONE);
            if (itemCfg.contains(key + ".custom-model-data")) {
                ItemMeta meta = stack.getItemMeta();
                meta.setCustomModelData(itemCfg.getInt(key + ".custom-model-data"));
                stack.setItemMeta(meta);
            }
            items.put(key, stack);
        }
        FileConfiguration textCfg = YamlConfiguration.loadConfiguration(new File(dir, "text.yml"));
        for (String key : textCfg.getKeys(false)) {
            List<String> lines = textCfg.getStringList(key + ".lines");
            texts.put(key, String.join("\n", lines));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only");
            return true;
        }
        if (args.length == 0) return false;
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "run":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /cursormenu run <itemId> <textId>");
                    return true;
                }
                ItemStack item = items.get(args[1]);
                String text = texts.get(args[2]);
                if (item == null || text == null) {
                    player.sendMessage(ChatColor.RED + "Unknown item or text id");
                    return true;
                }
                Location base = player.getLocation().add(player.getLocation().getDirection());
                menuManager.open(player, item.clone(), text, base);
                player.sendMessage(ChatColor.YELLOW + "Menu opened.");
                return true;
            case "stop":
                menuManager.close(player);
                player.sendMessage(ChatColor.YELLOW + "Menu closed.");
                return true;
            case "list":
                player.sendMessage(ChatColor.YELLOW + "Items: " + String.join(", ", items.keySet()));
                player.sendMessage(ChatColor.YELLOW + "Texts: " + String.join(", ", texts.keySet()));
                return true;
            default:
                return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("run", "stop", "list").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("run")) {
            return items.keySet().stream()
                    .filter(k -> k.startsWith(args[1].toLowerCase()))
                    .sorted()
                    .collect(Collectors.toList());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("run")) {
            return texts.keySet().stream()
                    .filter(k -> k.startsWith(args[2].toLowerCase()))
                    .sorted()
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}

