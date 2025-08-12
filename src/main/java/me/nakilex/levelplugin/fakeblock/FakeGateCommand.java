package me.nakilex.levelplugin.fakeblock;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.environment.stage.StageSelectionStore;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple in-game editor for fake gates using a wand similar to WorldEdit.
 */
public class FakeGateCommand implements CommandExecutor {
    private final QuestGateManager manager;
    private final ItemStack wand;

    public FakeGateCommand(Main plugin) {
        this.manager = plugin.getQuestGateManager();
        // Use the shared stage wand for selections
        this.wand = StageSelectionStore.WAND;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players.");
            return true;
        }
        if (args.length == 0) return false;
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "wand":
                player.getInventory().addItem(wand.clone());
                player.sendMessage(ChatColor.GREEN + "Wand given.");
                return true;
            case "list":
                var ids = manager.getGateIds();
                if (ids.isEmpty()) {
                    player.sendMessage(ChatColor.YELLOW + "No gates defined.");
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (String idKey : ids) {
                        QuestGate g = manager.getGate(idKey);
                        boolean closed = g != null && g.isClosed(player.getUniqueId());
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(idKey).append("(").append(closed ? "closed" : "open").append(")");
                    }
                    player.sendMessage(ChatColor.YELLOW + "Gates: " + sb);
                }
                return true;
            case "create":
                if (args.length < 3) return false;
                if (!StageSelectionStore.hasSelection(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Select two positions first.");
                    return true;
                }
                var pos1 = StageSelectionStore.getPos1(player.getUniqueId());
                var pos2 = StageSelectionStore.getPos2(player.getUniqueId());
                Bukkit.getLogger().info("[FakeGateDebug] create pos1=" + format(pos1) + " pos2=" + format(pos2));
                String id = args[1].toLowerCase();
                Map<Location, BlockData> map = null;
                Material mat = null;
                if ("#selection".equalsIgnoreCase(args[2])) {
                    map = new HashMap<>();
                    World world = pos1.getWorld();
                    int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
                    int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
                    int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
                    int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
                    int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
                    int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
                    for (int x = minX; x <= maxX; x++) {
                        for (int y = minY; y <= maxY; y++) {
                            for (int z = minZ; z <= maxZ; z++) {
                                Location l = new Location(world, x, y, z);
                                map.put(l, l.getBlock().getBlockData());
                            }
                        }
                    }
                } else {
                    mat = Material.matchMaterial(args[2]);
                    if (mat == null) mat = Material.BARRIER;
                }
                boolean closed = true;
                int idx = 3;
                if (args.length > idx) {
                    String state = args[idx].toLowerCase();
                    if (state.equals("open") || state.equals("closed")) {
                        closed = state.equals("closed");
                        idx++;
                    }
                }
                GateAnimation anim = args.length > idx ? GateAnimation.fromString(args[idx]) : GateAnimation.INSTANT;
                long ticks = 40L;
                if (args.length > idx + 1) {
                    try { ticks = Math.round(Double.parseDouble(args[idx + 1]) * 20.0); } catch (NumberFormatException ignored) {}
                }
                QuestGate gate = map != null ?
                        new QuestGate(id, pos1, pos2, map, closed, anim, ticks) :
                        new QuestGate(id, pos1, pos2, mat.createBlockData(), closed, anim, ticks);
                manager.createGate(gate);
                player.sendMessage(ChatColor.YELLOW + "Gate " + id + " created and " + (closed ? "closed" : "open") + ".");
                return true;
            case "toggle":
                if (args.length < 2) return false;
                if (manager.toggleGate(player, args[1])) {
                    QuestGate g = manager.getGate(args[1]);
                    boolean isClosed = g != null && g.isClosed(player.getUniqueId());
                    player.sendMessage(ChatColor.YELLOW + "Gate " + args[1] + " is now " + (isClosed ? "closed" : "open") + ".");
                } else {
                    player.sendMessage(ChatColor.RED + "Gate not found.");
                }
                return true;
            case "open":
                if (args.length < 2) return false;
                Player target = player;
                if (args.length > 2) {
                    target = Bukkit.getPlayer(args[2]);
                    if (target == null) {
                        player.sendMessage(ChatColor.RED + "Player not found.");
                        return true;
                    }
                }
                if (manager.openGate(target, args[1])) {
                    player.sendMessage(ChatColor.YELLOW + "Gate " + args[1] + " opened for " + target.getName() + ".");
                } else {
                    player.sendMessage(ChatColor.RED + "Gate not found.");
                }
                return true;
            case "close":
                if (args.length < 2) return false;
                target = player;
                if (args.length > 2) {
                    target = Bukkit.getPlayer(args[2]);
                    if (target == null) {
                        player.sendMessage(ChatColor.RED + "Player not found.");
                        return true;
                    }
                }
                if (manager.closeGate(target, args[1])) {
                    player.sendMessage(ChatColor.YELLOW + "Gate " + args[1] + " closed for " + target.getName() + ".");
                } else {
                    player.sendMessage(ChatColor.RED + "Gate not found.");
                }
                return true;
            case "remove":
                if (args.length < 2) return false;
                if (manager.removeGate(args[1])) {
                    player.sendMessage(ChatColor.GREEN + "Gate removed.");
                } else {
                    player.sendMessage(ChatColor.RED + "Gate not found.");
                }
                return true;
            case "debug":
                boolean enabled = manager.toggleDebug();
                player.sendMessage(ChatColor.YELLOW + "Gate debug " + (enabled ? "enabled" : "disabled"));
                return true;
            default:
                return false;
        }
    }

    private static String format(Location loc) {
        return loc.getBlockX()+","+loc.getBlockY()+","+loc.getBlockZ();
    }
}
