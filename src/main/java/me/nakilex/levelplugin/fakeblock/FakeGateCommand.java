package me.nakilex.levelplugin.fakeblock;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.environment.stage.StageSelectionStore;
import me.nakilex.levelplugin.utils.SchematicUtil;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

/**
 * Simple in-game editor for fake gates using a wand similar to WorldEdit.
 */
public class FakeGateCommand implements TabExecutor {
    private final Main plugin;
    private final QuestGateManager manager;
    private final ItemStack wand;

    public FakeGateCommand(Main plugin) {
        this.plugin = plugin;
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
                if (args.length < 4) return false;
                String id = args[1].toLowerCase();

                // Determine selection or use existing gate bounds
                Location pos1;
                Location pos2;
                QuestGate existing = manager.getGate(id);
                if (existing == null) {
                    if (!StageSelectionStore.hasSelection(player.getUniqueId())) {
                        player.sendMessage(ChatColor.RED + "Select two positions first.");
                        return true;
                    }
                    pos1 = StageSelectionStore.getPos1(player.getUniqueId());
                    pos2 = StageSelectionStore.getPos2(player.getUniqueId());
                } else {
                    pos1 = existing.getPos1();
                    pos2 = existing.getPos2();
                }

                String blockArg = args[2];
                String stateArg = args[3].toLowerCase();
                boolean updateClosed;
                if (stateArg.equals("closed")) {
                    updateClosed = true;
                } else if (stateArg.equals("open")) {
                    updateClosed = false;
                } else {
                    return false; // invalid state token
                }
                Bukkit.getLogger().info("[FakeGateDebug] create id=" + id + " state=" + (updateClosed ? "closed" : "open")
                        + " pos1=" + format(pos1) + " pos2=" + format(pos2));

                Material mat = null;
                Map<Location, BlockData> selMap = null;
                if ("#selection".equalsIgnoreCase(blockArg)) {
                    if (!StageSelectionStore.hasSelection(player.getUniqueId())) {
                        player.sendMessage(ChatColor.RED + "Select two positions first.");
                        return true;
                    }
                    selMap = captureSelection(StageSelectionStore.getPos1(player.getUniqueId()),
                            StageSelectionStore.getPos2(player.getUniqueId()));
                    File schem = new File(manager.getSchematicFolder(), id + "_" + (updateClosed ? "closed" : "open") + ".schem");
                    SchematicUtil.saveSchematic(pos1, pos2, schem, plugin.getLogger());
                } else {
                    mat = Material.matchMaterial(blockArg);
                    if (mat == null) {
                        mat = updateClosed ? Material.BARRIER : Material.AIR;
                    }
                }

                int idx = 4;
                GateAnimation anim = null;
                if (args.length > idx) {
                    anim = GateAnimation.fromString(args[idx]);
                    if (anim != null) idx++;
                }
                Long ticks = null;
                if (args.length > idx) {
                    try { ticks = Math.round(Double.parseDouble(args[idx]) * 20.0); } catch (NumberFormatException ignored) {}
                }

                Map<Location, BlockData> closedMap = null;
                Map<Location, BlockData> openMap = null;
                BlockData closedData = existing != null ? existing.getClosedData() : Material.BARRIER.createBlockData();
                boolean defaultClosed = existing != null ? existing.isDefaultClosed() : updateClosed;

                if (existing != null) {
                    if (existing.hasCustomBlocks()) {
                        closedMap = new HashMap<>(existing.getClosedDataMap());
                    }
                    if (existing.hasOpenCustomBlocks()) {
                        openMap = new HashMap<>(existing.getOpenDataMap());
                    }
                    if (anim == null) anim = existing.getAnimation();
                    if (ticks == null) ticks = existing.getAnimationTicks();
                }

                if (updateClosed) {
                    if (selMap != null) {
                        closedMap = selMap;
                    } else if (mat != null) {
                        closedData = mat.createBlockData();
                        closedMap = null; // uniform block
                    }
                } else {
                    if (selMap != null) {
                        openMap = selMap;
                    } else if (mat != null) {
                        openMap = buildUniformMap(pos1, pos2, mat.createBlockData());
                    }
                }

                if (anim == null) anim = GateAnimation.INSTANT;
                if (ticks == null) ticks = 40L;

                if (manager.isDebug()) {
                    Bukkit.getLogger().info("[FakeGateDebug] final id=" + id + " updateClosed=" + updateClosed
                            + " closedMap=" + (closedMap != null ? closedMap.size() : 0)
                            + " openMap=" + (openMap != null ? openMap.size() : 0));
                }

                QuestGate gate = QuestGate.create(id, pos1, pos2, closedData, closedMap, openMap, defaultClosed, anim, ticks);
                manager.createGate(gate);
                player.sendMessage(ChatColor.YELLOW + "Gate " + id + " updated (" + (updateClosed ? "closed" : "open") + " state).");
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

    private static Map<Location, BlockData> captureSelection(Location pos1, Location pos2) {
        Map<Location, BlockData> map = new HashMap<>();
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
                    BlockData data = l.getBlock().getBlockData();
                    if (!data.getMaterial().isAir()) {
                        map.put(l, data);
                    }
                }
            }
        }
        return map;
    }

    private static Map<Location, BlockData> buildUniformMap(Location pos1, Location pos2, BlockData data) {
        Map<Location, BlockData> map = new HashMap<>();
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
                    map.put(new Location(world, x, y, z), data.clone());
                }
            }
        }
        return map;
    }

    private static List<String> blockSuggestions(String prefix) {
        List<String> opts = new ArrayList<>();
        String lower = prefix.toLowerCase();
        if ("#selection".startsWith(lower)) opts.add("#selection");
        for (Material m : Material.values()) {
            if (!m.isBlock()) continue;
            String name = m.name().toLowerCase();
            if (name.startsWith(lower)) opts.add(name);
        }
        return opts;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) return Collections.emptyList();
        if (args.length == 1) {
            return Arrays.asList("wand", "list", "create", "toggle", "open", "close", "remove", "debug")
                    .stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create":
                if (args.length == 3) {
                    return blockSuggestions(args[2]);
                } else if (args.length == 4) {
                    return Arrays.asList("open", "closed").stream()
                            .filter(s -> s.startsWith(args[3].toLowerCase())).toList();
                } else if (args.length == 5) {
                    return Arrays.stream(GateAnimation.values()).map(a -> a.name().toLowerCase())
                            .filter(s -> s.startsWith(args[4].toLowerCase())).toList();
                }
                break;
            case "toggle":
            case "open":
            case "close":
            case "remove":
                if (args.length == 2) {
                    return manager.getGateIds().stream()
                            .filter(id -> id.startsWith(args[1].toLowerCase())).toList();
                }
                if ((sub.equals("open") || sub.equals("close")) && args.length == 3) {
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                            .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase())).toList();
                }
                break;
            default:
                break;
        }
        return Collections.emptyList();
    }
}
