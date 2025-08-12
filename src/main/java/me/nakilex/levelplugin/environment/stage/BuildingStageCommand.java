package me.nakilex.levelplugin.environment.stage;

import me.nakilex.levelplugin.Main;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.nakilex.levelplugin.environment.stage.StageSelectionStore;

/**
 * Provides an editor for defining building stage areas using a wand.
 */
public class BuildingStageCommand implements CommandExecutor {
    private final BuildingStageManager manager;
    private final Main plugin;
    private final ItemStack wand;

    public BuildingStageCommand(Main plugin, BuildingStageManager manager) {
        this.plugin = plugin;
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
                if (args.length < 4) return false;
                if (!StageSelectionStore.hasSelection(p.getUniqueId())) {
                    p.sendMessage(ChatColor.RED + "Select two positions first.");
                    return true;
                }
                Location pos1 = StageSelectionStore.getPos1(p.getUniqueId());
                Location pos2 = StageSelectionStore.getPos2(p.getUniqueId());
                // Arguments: <building> <stage> [priority]
                String bName = args[1].toLowerCase();
                int stage = parseInt(args[2], 1);
                int priority = 0;
                if (args.length > 3) {
                    priority = parseInt(args[3], 0);
                    if (priority < 0) priority = 0;
                }
                // Save where the player ran the command and raise it one block
                Location stand = p.getLocation().clone().add(0.5, 1.0, 0.5);
                Location origin = p.getLocation().getBlock().getLocation();
                manager.createStage(bName, stage, pos1, pos2, stand, origin, priority);
                p.sendMessage(ChatColor.GREEN + "Stage " + bName + " created.");
                return true;
            case "remove":
                if (args.length < 3) return false;
                String rbName = args[1].toLowerCase();
                int rStage = parseInt(args[2], 1);
                if (manager.removeStage(rbName, rStage)) {
                    p.sendMessage(ChatColor.GREEN + "Stage removed.");
                } else {
                    p.sendMessage(ChatColor.RED + "Stage not found.");
                }
                return true;
            case "link":
                if (args.length < 3) return false;
                String lbName = args[1].toLowerCase();
                String town = args[2].toLowerCase();
                var townStage = plugin.getTownStageManager().getStage(town, 1, 1);
                if (townStage == null) {
                    p.sendMessage(ChatColor.RED + "Unknown town.");
                    return true;
                }
                var buildOrigin = manager.getStageOrigin(lbName);
                if (buildOrigin == null) {
                    p.sendMessage(ChatColor.RED + "Unknown building stage.");
                    return true;
                }
                int tMinX = Math.min(townStage.pos1.getBlockX(), townStage.pos2.getBlockX());
                int tMinY = Math.min(townStage.pos1.getBlockY(), townStage.pos2.getBlockY());
                int tMinZ = Math.min(townStage.pos1.getBlockZ(), townStage.pos2.getBlockZ());
                var townOrigin = new Location(townStage.pos1.getWorld(),
                        tMinX + townStage.ox,
                        tMinY + townStage.oy,
                        tMinZ + townStage.oz);
                int dx = buildOrigin.getBlockX() - townOrigin.getBlockX();
                // Ignore the Y difference to allow flexible placement height
                int dz = buildOrigin.getBlockZ() - townOrigin.getBlockZ();
                manager.linkBuilding(town, lbName, dx, 0, dz);
                p.sendMessage(ChatColor.GREEN + "Linked " + lbName + " to " + town + ".");
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
