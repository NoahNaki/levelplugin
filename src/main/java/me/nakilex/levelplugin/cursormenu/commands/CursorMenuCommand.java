package me.nakilex.levelplugin.cursormenu.commands;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.cursormenu.CursorMenuService;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Handles /cursormenu commands for opening menus and item showcases.
 */
public class CursorMenuCommand implements TabExecutor {
    private final CursorMenuService service;
    private final Main plugin;

    public CursorMenuCommand(Main plugin, CursorMenuService service) {
        this.plugin = plugin;
        this.service = service;
    }

    private boolean hasPermission(Player player, String node) {
        if (player.hasPermission(node)) {
            return true;
        }
        player.sendMessage("No permission.");
        return false;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage("Usage: /cursormenu <run|stop|items|itemsstop|reload>");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ENGLISH);
        switch (sub) {
            case "run" -> {
                if (!hasPermission(player, "cursormenu.run")) return true;
                if (args.length < 2) {
                    player.sendMessage("Specify a menu name.");
                    return true;
                }
                service.openMenu(player, args[1]);
            }
            case "stop" -> {
                if (!hasPermission(player, "cursormenu.stop")) return true;
                service.closeMenu(player);
            }
            case "items" -> {
                if (!hasPermission(player, "cursormenu.items")) return true;
                ItemStack item;
                if (args.length > 1) {
                    try {
                        int id = Integer.parseInt(args[1]);
                        me.nakilex.levelplugin.items.data.CustomItem tpl = plugin.getItemManager().getTemplateById(id);
                        if (tpl == null) {
                            player.sendMessage("Unknown item id.");
                            return true;
                        }
                        item = ItemUtil.createItemStackFromCustomItem(tpl, 1, player);
                    } catch (NumberFormatException ex) {
                        player.sendMessage("Invalid item id.");
                        return true;
                    }
                } else {
                    item = player.getInventory().getItemInMainHand();
                }
                if (item == null || item.getType().isAir()) {
                    player.sendMessage("Hold an item first.");
                    return true;
                }
                service.getShowcaseManager().startShowcase(player, item);
            }
            case "itemsstop" -> {
                if (!hasPermission(player, "cursormenu.items")) return true;
                service.getShowcaseManager().stopShowcase(player);
            }
            case "reload" -> {
                if (!hasPermission(player, "cursormenu.reload")) return true;
                service.reloadMenus();
                player.sendMessage("Cursor menus reloaded.");
            }
            default -> player.sendMessage("Unknown subcommand.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            result.add("run");
            result.add("stop");
            result.add("items");
            result.add("itemsstop");
            result.add("reload");
            return result.stream().filter(s -> s.startsWith(args[0].toLowerCase(Locale.ENGLISH))).collect(Collectors.toList());
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("run")) {
                return service.getMenuIds().stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase(Locale.ENGLISH)))
                        .sorted().collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("items")) {
                ItemManager mgr = plugin.getItemManager();
                return mgr.getAllTemplates().keySet().stream()
                        .map(String::valueOf)
                        .filter(s -> s.startsWith(args[1]))
                        .sorted().collect(Collectors.toList());
            }
        }
        return result;
    }
}
