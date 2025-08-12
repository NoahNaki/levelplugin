package me.nakilex.levelplugin.environment.stage;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.nakilex.levelplugin.environment.stage.StageSelectionStore;

/**
 * Provides an editor for defining town stage areas using a wand.
 */
public class TownStageCommand implements CommandExecutor {
    private final TownStageManager manager;
    private final ItemStack wand;

    public TownStageCommand(TownStageManager manager) {
        this.manager = manager;
        this.wand = StageSelectionStore.WAND;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length == 0) return false;
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "wand":
                p.getInventory().addItem(wand.clone());
                p.sendMessage(ChatColor.GREEN + "Wand given.");
                return true;
            case "list":
                var names = manager.getStageNames();
                if (names.isEmpty()) {
                    p.sendMessage(ChatColor.YELLOW + "No stages defined.");
                } else {
                    p.sendMessage(ChatColor.YELLOW + "Stages: " + String.join(", ", names));
                }
                return true;
            case "create":
                if (args.length < 3) return false;
                if (!StageSelectionStore.hasSelection(p.getUniqueId())) {
                    p.sendMessage(ChatColor.RED + "Select two positions first.");
                    return true;
                }
                Location pos1 = StageSelectionStore.getPos1(p.getUniqueId());
                Location pos2 = StageSelectionStore.getPos2(p.getUniqueId());
                String name = args[1].toLowerCase();
                int level = parseInt(args[2], 1);
                int priority = 0;
                if (args.length > 3) {
                    priority = parseInt(args[3], 0);
                    if (priority < 0) priority = 0;
                }
                Location origin = p.getLocation().getBlock().getLocation();
                manager.createStage(name, level, 1, pos1, pos2, origin, priority);
                p.sendMessage(ChatColor.GREEN + "Stage " + name + " created.");
                return true;
            case "remove":
                if (args.length < 3) return false;
                String rName = args[1].toLowerCase();
                int rLevel = parseInt(args[2], 1);
                if (manager.removeStage(rName, rLevel, 1)) {
                    p.sendMessage(ChatColor.GREEN + "Stage removed.");
                } else {
                    p.sendMessage(ChatColor.RED + "Stage not found.");
                }
                return true;
            default:
                return false;
        }
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static String format(Location loc) {
        return loc.getBlockX()+","+loc.getBlockY()+","+loc.getBlockZ();
    }
}
