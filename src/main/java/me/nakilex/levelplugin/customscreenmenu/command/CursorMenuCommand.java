package me.nakilex.levelplugin.customscreenmenu.command;

import me.nakilex.levelplugin.customscreenmenu.CustomScreenMenuPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CursorMenuCommand implements CommandExecutor, TabCompleter {
    private final CustomScreenMenuPlugin plugin;

    public CursorMenuCommand(CustomScreenMenuPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(Component.text("/cursormenu <run|stop|items|itemsstop|reload>"));
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "run" -> {
                if (!player.hasPermission("cursormenu.run")) {
                    player.sendMessage(Component.text("No permission."));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /cursormenu run <menu>"));
                    return true;
                }
                plugin.getMenuManager().openMenu(player, args[1]);
            }
            case "stop" -> {
                if (!player.hasPermission("cursormenu.stop")) {
                    player.sendMessage(Component.text("No permission."));
                    return true;
                }
                plugin.getMenuManager().closeMenu(player);
            }
            case "items" -> {
                if (!player.hasPermission("cursormenu.items")) {
                    player.sendMessage(Component.text("No permission."));
                    return true;
                }
                ItemStack stack;
                if (args.length >= 2) {
                    Material mat = Material.matchMaterial(args[1]);
                    stack = mat != null ? new ItemStack(mat) : player.getInventory().getItemInMainHand();
                } else {
                    stack = player.getInventory().getItemInMainHand();
                }
                plugin.getMenuManager().startShowcase(player, stack);
            }
            case "itemsstop" -> {
                if (!player.hasPermission("cursormenu.items")) {
                    player.sendMessage(Component.text("No permission."));
                    return true;
                }
                plugin.getMenuManager().stopShowcase(player);
            }
            case "reload" -> {
                if (!player.hasPermission("cursormenu.reload")) {
                    player.sendMessage(Component.text("No permission."));
                    return true;
                }
                plugin.getMenuManager().reload();
                player.sendMessage(Component.text("Reloaded menus."));
            }
            default -> player.sendMessage(Component.text("/cursormenu <run|stop|items|itemsstop|reload>"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.add("run");
            list.add("stop");
            list.add("items");
            list.add("itemsstop");
            list.add("reload");
            return list;
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("run")) {
                list.addAll(plugin.getMenuManager().getMenuIds());
            } else if (args[0].equalsIgnoreCase("items")) {
                for (Material mat : Material.values()) {
                    list.add(mat.name().toLowerCase());
                }
            }
        }
        return list;
    }
}
