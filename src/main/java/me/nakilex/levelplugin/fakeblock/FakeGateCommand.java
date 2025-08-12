package me.nakilex.levelplugin.fakeblock;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.environment.stage.StageSelectionStore;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Simple in-game editor for fake gates using a wand similar to WorldEdit.
 */
public class FakeGateCommand implements TabExecutor {
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
                Map<Location, BlockData> closedMap = null;
                Material mat = null;
                if ("#selection".equalsIgnoreCase(args[2])) {
                    closedMap = captureSelection(pos1, pos2);
                } else {
                    mat = Material.matchMaterial(args[2]);
                    if (mat == null) mat = Material.BARRIER;
                }

                Map<Location, BlockData> openMap = null;
                int idx = 3;
                if (args.length > idx && isBlockArg(args[idx])) {
                    String token = args[idx];
                    if ("#selection".equalsIgnoreCase(token)) {
                        openMap = captureSelection(pos1, pos2);
                    } else {
                        Material openMat = Material.matchMaterial(token);
                        if (openMat != null) {
                            openMap = buildUniformMap(pos1, pos2, openMat.createBlockData());
                        }
                    }
                    idx++;
                }

                if (manager.isDebug()) {
                    Bukkit.getLogger().info("[FakeGateDebug] captured closed=" + (closedMap != null ? closedMap.size() : 0)
                            + " open=" + (openMap != null ? openMap.size() : 0));
                }

                Boolean closed = null;
                if (args.length > idx) {
                    String state = args[idx].toLowerCase();
                    if (state.equals("open") || state.equals("closed")) {
                        closed = state.equals("closed");
                        idx++;
                    }
                }

                GateAnimation anim = null;
                if (args.length > idx) {
                    anim = GateAnimation.fromString(args[idx]);
                    idx++;
                }
                Long ticks = null;
                if (args.length > idx) {
                    try { ticks = Math.round(Double.parseDouble(args[idx]) * 20.0); } catch (NumberFormatException ignored) {}
                }

                QuestGate existing = manager.getGate(id);
                if (existing != null) {
                    if (closedMap == null) {
                        closedMap = existing.hasCustomBlocks() ? new HashMap<>(existing.getClosedDataMap()) : null;
                    }
                    if (openMap == null) {
                        openMap = existing.hasOpenCustomBlocks() ? new HashMap<>(existing.getOpenDataMap()) : null;
                    }
                    if (mat == null && !existing.hasCustomBlocks()) {
                        mat = existing.getClosedData().getMaterial();
                    }
                    if (closed == null) {
                        closed = existing.isDefaultClosed();
                    }
                    if (anim == null) {
                        anim = existing.getAnimation();
                    }
                    if (ticks == null) {
                        ticks = existing.getAnimationTicks();
                    }
                }

                if (mat == null) mat = Material.BARRIER;
                if (closed == null) closed = true;
                if (anim == null) anim = GateAnimation.INSTANT;
                if (ticks == null) ticks = 40L;

                if (manager.isDebug()) {
                    Bukkit.getLogger().info("[FakeGateDebug] final id=" + id + " closed=" + closed
                            + " closedMap=" + (closedMap != null ? closedMap.size() : 0)
                            + " openMap=" + (openMap != null ? openMap.size() : 0));
                }

                BlockData closedData = mat.createBlockData();
                QuestGate gate = QuestGate.create(id, pos1, pos2, closedData, closedMap, openMap, closed, anim, ticks);
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
                    map.put(l, l.getBlock().getBlockData());
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

    private static boolean isBlockArg(String token) {
        return "#selection".equalsIgnoreCase(token) || Material.matchMaterial(token) != null;
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
                    List<String> opts = new ArrayList<>();
                    opts.addAll(blockSuggestions(args[3]));
                    opts.addAll(Arrays.asList("open", "closed"));
                    return opts.stream().filter(s -> s.startsWith(args[3].toLowerCase())).toList();
                } else {
                    boolean openProvided = args.length > 3 && isBlockArg(args[3]);
                    if (openProvided) {
                        if (args.length == 5) {
                            return Arrays.asList("open", "closed").stream()
                                    .filter(s -> s.startsWith(args[4].toLowerCase())).toList();
                        } else if (args.length == 6) {
                            return Arrays.stream(GateAnimation.values()).map(a -> a.name().toLowerCase())
                                    .filter(s -> s.startsWith(args[5].toLowerCase())).toList();
                        }
                    } else {
                        if (args.length == 5) {
                            return Arrays.stream(GateAnimation.values()).map(a -> a.name().toLowerCase())
                                    .filter(s -> s.startsWith(args[4].toLowerCase())).toList();
                        }
                    }
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
