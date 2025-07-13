package me.nakilex.levelplugin.environment.stage;

import me.nakilex.levelplugin.Main;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.nakilex.levelplugin.environment.stage.StageSelectionStore;

import java.util.UUID;

/**
 * Provides an editor for defining building stage areas using a wand.
 */
public class BuildingStageCommand implements CommandExecutor, Listener {
    private final BuildingStageManager manager;
    private final Main plugin;
    private final ItemStack wand;

    public BuildingStageCommand(Main plugin, BuildingStageManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        wand = StageSelectionStore.WAND;
        plugin.getCommand("buildingstage").setExecutor(this);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
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
                Location pos1 = StageSelectionStore.getPos1(p.getUniqueId());
                Location pos2 = StageSelectionStore.getPos2(p.getUniqueId());
                if (pos1 == null || pos2 == null) {
                    p.sendMessage(ChatColor.RED + "Select two positions first.");
                    return true;
                }
                // Arguments: <building> <level> <stage>
                String bName = args[1].toLowerCase();
                int level = parseInt(args[2], 1);
                int stage = parseInt(args[3], 1);
                // Save where the player ran the command and raise it one block
                Location stand = p.getLocation().clone().add(0.5, 1.0, 0.5);
                Location origin = p.getLocation().getBlock().getLocation();
                manager.createStage(bName, level, stage, pos1, pos2, stand, origin);
                p.sendMessage(ChatColor.GREEN + "Stage " + bName + " created.");
                return true;
            case "schem":
                if (args.length < 4) return false;
                Location s1 = StageSelectionStore.getPos1(p.getUniqueId());
                Location s2 = StageSelectionStore.getPos2(p.getUniqueId());
                if (s1 == null || s2 == null) {
                    p.sendMessage(ChatColor.RED + "Select two positions first.");
                    return true;
                }
                String sbName = args[1].toLowerCase();
                int sLevel = parseInt(args[2], 1);
                int sStage = parseInt(args[3], 1);
                Location sStand = p.getLocation().clone().add(0.5, 1.0, 0.5);
                Location sOrigin = p.getLocation().getBlock().getLocation();
                manager.createStageSchem(sbName, sLevel, sStage, s1, s2, sStand, sOrigin);
                p.sendMessage(ChatColor.GREEN + "Stage " + sbName + " schematic saved.");
                return true;
            case "remove":
                if (args.length < 4) return false;
                String rbName = args[1].toLowerCase();
                int rLevel = parseInt(args[2], 1);
                int rStage = parseInt(args[3], 1);
                if (manager.removeStage(rbName, rLevel, rStage)) {
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
                int dy = buildOrigin.getBlockY() - townOrigin.getBlockY();
                int dz = buildOrigin.getBlockZ() - townOrigin.getBlockZ();
                manager.linkBuilding(town, lbName, dx, dy, dz);
                p.sendMessage(ChatColor.GREEN + "Linked " + lbName + " to " + town + ".");
                return true;
            default:
                return false;
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        ItemStack inHand = event.getItem();
        if (inHand == null || !inHand.isSimilar(wand)) return;
        // Ignore off-hand interactions to prevent duplicate messages
        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) return;
        if (event.getClickedBlock() == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        Location loc = event.getClickedBlock().getLocation();
        if (event.getAction().name().contains("LEFT")) {
            StageSelectionStore.setPos1(player.getUniqueId(), loc);
            player.sendMessage(ChatColor.AQUA + "Pos1 set " + format(loc));
        } else if (event.getAction().name().contains("RIGHT")) {
            StageSelectionStore.setPos2(player.getUniqueId(), loc);
            player.sendMessage(ChatColor.AQUA + "Pos2 set " + format(loc));
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
