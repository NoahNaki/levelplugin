package me.nakilex.levelplugin.debug;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dungeon.Direction;
import me.nakilex.levelplugin.dungeon.DungeonManager;
import me.nakilex.levelplugin.dungeon.RoomTemplate;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Debug-only stronghold generator for rapid iterative visual checks.
 */
public class StrongholdDebugManager {
    private static final String TEMPLATE_WORLD_NAME = "flatland";
    private static final int DEFAULT_STEP_DELAY_TICKS = 8;
    private static final Set<Material> STRONGHOLD_IGNORED = Set.of(
            Material.WHITE_CONCRETE,
            Material.LIGHT_BLUE_CONCRETE,
            Material.REDSTONE_BLOCK,
            Material.PINK_WOOL,
            Material.LIME_WOOL
    );

    private final Main plugin;
    private final DungeonManager dungeonManager;
    private final List<TemplateOption> corners = new ArrayList<>();
    private final List<TemplateOption> straights = new ArrayList<>();
    private final List<TemplateOption> deadEnds = new ArrayList<>();
    private final List<TemplateOption> connectors = new ArrayList<>();
    private final List<TemplateOption> towers = new ArrayList<>();
    private final List<TemplateOption> gates = new ArrayList<>();

    private final Map<UUID, ActiveStronghold> activeByPlayer = new HashMap<>();
    private final Map<UUID, BukkitRunnable> activeTasks = new HashMap<>();

    public StrongholdDebugManager(Main plugin, DungeonManager dungeonManager) {
        this.plugin = plugin;
        this.dungeonManager = dungeonManager;
        loadTemplates();
    }

    public boolean handleStrongholdCommand(Player player, String[] args) {
        if (args.length < 2) {
            sendUsage(player);
            return true;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "spawn" -> {
                int size = parsePositiveInt(args, 2, 12);
                if (size <= 0) {
                    ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                            "Usage: /debug stronghold spawn <size>");
                    return true;
                }
                spawnStronghold(player, size, false, DEFAULT_STEP_DELAY_TICKS);
                return true;
            }
            case "spawnstep" -> {
                int size = parsePositiveInt(args, 2, 12);
                int delay = parsePositiveInt(args, 3, DEFAULT_STEP_DELAY_TICKS);
                if (size <= 0 || delay <= 0) {
                    ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                            "Usage: /debug stronghold spawnstep <size> [delayTicks]");
                    return true;
                }
                spawnStronghold(player, size, true, delay);
                return true;
            }
            case "despawn" -> {
                despawn(player, true);
                return true;
            }
            default -> {
                sendUsage(player);
                return true;
            }
        }
    }

    public List<String> tabComplete(String[] args) {
        if (args.length == 2) {
            return filter(List.of("spawn", "spawnstep", "despawn"), args[1]);
        }
        if (args.length == 3 && ("spawn".equalsIgnoreCase(args[1]) || "spawnstep".equalsIgnoreCase(args[1]))) {
            return filter(List.of("8", "12", "16", "20"), args[2]);
        }
        if (args.length == 4 && "spawnstep".equalsIgnoreCase(args[1])) {
            return filter(List.of("2", "4", "8", "12"), args[3]);
        }
        return List.of();
    }

    private void spawnStronghold(Player player, int size, boolean stepped, int delayTicks) {
        World templateWorld = Bukkit.getWorld(TEMPLATE_WORLD_NAME);
        if (templateWorld == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Template world 'flatland' is missing.");
            return;
        }
        if (!allTemplateFamiliesAvailable()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Stronghold templates are incomplete; check flatland capture regions.");
            return;
        }

        despawn(player, false);

        List<Node> graph = generateSnakeGraph(size);
        Map<Integer, Placement> placements = solvePlacements(graph, player.getLocation().getBlock().getLocation());
        if (placements.size() != graph.size()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Could not resolve all stronghold placements.");
            return;
        }

        ActiveStronghold active = new ActiveStronghold(player.getWorld());
        activeByPlayer.put(player.getUniqueId(), active);

        List<Integer> order = new ArrayList<>(placements.keySet());
        Collections.sort(order);

        if (!stepped) {
            for (Integer nodeId : order) {
                if (!pasteAndCapture(active, placements.get(nodeId))) {
                    rollbackAndClear(player.getUniqueId());
                    ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                            "Stronghold spawn failed; changes were rolled back.");
                    return;
                }
            }
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                    "Spawned stronghold debug layout (size=" + size + ").");
            return;
        }

        BukkitRunnable task = new BukkitRunnable() {
            int idx = 0;

            @Override
            public void run() {
                if (idx >= order.size()) {
                    activeTasks.remove(player.getUniqueId());
                    cancel();
                    ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                            "Spawned stronghold debug layout step-by-step (size=" + size + ").");
                    return;
                }
                Placement placement = placements.get(order.get(idx++));
                if (!pasteAndCapture(active, placement)) {
                    activeTasks.remove(player.getUniqueId());
                    cancel();
                    rollbackAndClear(player.getUniqueId());
                    ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                            "Stronghold spawn failed mid-step; changes were rolled back.");
                }
            }
        };
        activeTasks.put(player.getUniqueId(), task);
        task.runTaskTimer(plugin, 1L, delayTicks);
    }

    private void despawn(Player player, boolean announce) {
        UUID id = player.getUniqueId();
        BukkitRunnable task = activeTasks.remove(id);
        if (task != null) {
            task.cancel();
        }
        ActiveStronghold active = activeByPlayer.remove(id);
        if (active == null) {
            if (announce) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                        "No active stronghold debug instance to despawn.");
            }
            return;
        }
        restoreSnapshot(active);
        if (announce) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                    "Stronghold debug instance despawned and restored.");
        }
    }

    private void rollbackAndClear(UUID playerId) {
        ActiveStronghold active = activeByPlayer.remove(playerId);
        if (active != null) {
            restoreSnapshot(active);
        }
    }

    private boolean pasteAndCapture(ActiveStronghold active, Placement placement) {
        RoomTemplate template = placement.option.template;
        int connectorY = template.getConnectorMinY();
        int baseY = placement.center.getBlockY();
        World world = placement.center.getWorld();
        if (world == null) {
            return false;
        }

        for (RoomTemplate.BlockDef b : template.getBlocks()) {
            if (STRONGHOLD_IGNORED.contains(b.data.getMaterial())) {
                continue;
            }
            int[] vec = RoomTemplate.rotate(b.x - (int) Math.round(template.getCenterX()),
                    b.z - (int) Math.round(template.getCenterZ()), placement.rotation);
            int wx = placement.center.getBlockX() + vec[0];
            int wy = baseY + (b.y - connectorY);
            int wz = placement.center.getBlockZ() + vec[1];
            Block block = world.getBlockAt(wx, wy, wz);
            active.snapshot.putIfAbsent(block.getLocation(), block.getBlockData().clone());
        }

        DungeonManager.PasteResult result = dungeonManager.pasteRoom(
                null,
                template,
                placement.rotation,
                placement.center,
                null,
                false,
                STRONGHOLD_IGNORED
        );
        return result.success();
    }

    private void restoreSnapshot(ActiveStronghold active) {
        for (Map.Entry<Location, BlockData> entry : active.snapshot.entrySet()) {
            Location loc = entry.getKey();
            if (loc.getWorld() == null || !loc.getWorld().equals(active.world)) {
                continue;
            }
            loc.getBlock().setBlockData(entry.getValue(), false);
        }
    }

    private List<Node> generateSnakeGraph(int size) {
        int count = Math.max(2, size);
        List<Node> nodes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            nodes.add(new Node(i));
            if (i > 0) {
                nodes.get(i - 1).neighbors.add(i);
                nodes.get(i).neighbors.add(i - 1);
            }
        }
        return nodes;
    }

    private Map<Integer, Placement> solvePlacements(List<Node> graph, Location root) {
        Map<Integer, Placement> placements = new HashMap<>();
        Node first = graph.get(0);
        Set<Direction> firstDirs = requiredDirections(first, graph, placements);
        TemplateOption rootOption = selectTemplate(firstDirs);
        if (rootOption == null) {
            return placements;
        }
        int rootRot = findRotation(rootOption.template, firstDirs);
        placements.put(0, new Placement(rootOption, rootRot, root));

        for (int i = 1; i < graph.size(); i++) {
            Node node = graph.get(i);
            Set<Direction> dirs = requiredDirections(node, graph, placements);
            TemplateOption option = selectTemplate(dirs);
            if (option == null) {
                continue;
            }
            int rotation = findRotation(option.template, dirs);
            Placement prev = placements.get(i - 1);
            if (prev == null) {
                continue;
            }
            Direction fromPrev = dirs.iterator().next();
            Location center = offsetCenter(prev.center, fromPrev);
            placements.put(i, new Placement(option, rotation, center));
        }
        return placements;
    }

    private Location offsetCenter(Location current, Direction toward) {
        int spacing = 72;
        return switch (toward) {
            case NORTH -> current.clone().add(0, 0, -spacing);
            case SOUTH -> current.clone().add(0, 0, spacing);
            case EAST -> current.clone().add(spacing, 0, 0);
            case WEST -> current.clone().add(-spacing, 0, 0);
        };
    }

    private Set<Direction> requiredDirections(Node node, List<Node> graph, Map<Integer, Placement> placements) {
        Set<Direction> dirs = new LinkedHashSet<>();
        for (Integer nb : node.neighbors) {
            if (nb < node.id) {
                dirs.add(Direction.WEST);
            } else {
                dirs.add(Direction.EAST);
            }
        }
        if (dirs.isEmpty()) {
            dirs.add(Direction.NORTH);
        }
        return dirs;
    }

    private TemplateOption selectTemplate(Set<Direction> dirs) {
        int degree = dirs.size();
        if (degree <= 1) {
            TemplateOption pick = pickWeighted(deadEnds, towers);
            if (pick != null) {
                return pick;
            }
            return selectTemplateFallback(dirs);
        }

        boolean opposite = dirs.contains(Direction.NORTH) && dirs.contains(Direction.SOUTH)
                || dirs.contains(Direction.EAST) && dirs.contains(Direction.WEST);

        if (degree == 2 && opposite) {
            TemplateOption pick = pickWeighted(towers, gates, straights);
            if (pick != null) {
                return pick;
            }
            return selectTemplateFallback(dirs);
        }

        if (degree == 2) {
            TemplateOption pick = pickWeighted(towers, corners);
            if (pick != null) {
                return pick;
            }
            return selectTemplateFallback(dirs);
        }

        TemplateOption pick = randomFrom(connectors);
        return pick != null ? pick : selectTemplateFallback(dirs);
    }

    private TemplateOption selectTemplateFallback(Set<Direction> dirs) {
        for (TemplateOption option : concat(corners, straights, deadEnds, connectors, towers, gates)) {
            if (option.template.getConnectors().size() >= Math.max(1, dirs.size())) {
                return option;
            }
        }
        return null;
    }

    private int findRotation(RoomTemplate template, Set<Direction> target) {
        for (int r = 0; r < 4; r++) {
            if (template.getRotatedDirections(r).containsAll(target)) {
                return r;
            }
        }
        return 0;
    }

    @SafeVarargs
    private final TemplateOption pickWeighted(List<TemplateOption>... pools) {
        List<TemplateOption> all = new ArrayList<>();
        for (int i = 0; i < pools.length; i++) {
            List<TemplateOption> pool = pools[i];
            int weight = pools.length - i;
            for (TemplateOption option : pool) {
                for (int w = 0; w < weight; w++) {
                    all.add(option);
                }
            }
        }
        return all.isEmpty() ? null : all.get(ThreadLocalRandom.current().nextInt(all.size()));
    }

    private TemplateOption randomFrom(List<TemplateOption> options) {
        return options.isEmpty() ? null : options.get(ThreadLocalRandom.current().nextInt(options.size()));
    }

    private List<TemplateOption> concat(List<TemplateOption>... lists) {
        List<TemplateOption> all = new ArrayList<>();
        for (List<TemplateOption> list : lists) {
            all.addAll(list);
        }
        return all;
    }

    private void loadTemplates() {
        World world = Bukkit.getWorld(TEMPLATE_WORLD_NAME);
        if (world == null) {
            return;
        }

        add(corners, "corner_a", world, 473, -38, -5346, 543, -61, -5276);
        add(corners, "corner_b", world, 544, -38, -5631, 614, -61, -5701);
        add(corners, "corner_c", world, 614, -61, -5630, 544, -38, -5560);

        add(straights, "straight_a", world, 402, -38, -5276, 472, -61, -5346);
        add(straights, "straight_b", world, 472, -61, -5347, 402, -38, -5417);
        add(straights, "straight_c", world, 402, -38, -5418, 472, -61, -5488);
        add(straights, "straight_d", world, 472, -61, -5489, 402, -38, -5559);
        add(straights, "straight_e", world, 402, -38, -5560, 472, -61, -5630);
        add(straights, "straight_f", world, 472, -61, -5631, 402, -38, -5701);
        add(straights, "straight_g", world, 473, -38, -5701, 543, -61, -5631);
        add(straights, "straight_h", world, 543, -61, -5630, 473, -38, -5560);
        add(straights, "straight_i", world, 473, -38, -5417, 543, -61, -5347);

        add(deadEnds, "dead_end_a", world, 543, -38, -5418, 473, -61, -5488);
        add(deadEnds, "dead_end_b", world, 473, -61, -5489, 543, -38, -5559);

        add(connectors, "connector_a", world, 412, -61, -5711, 402, -38, -5701);
        add(connectors, "connector_b", world, 402, -38, -5721, 412, -61, -5711);

        add(towers, "tower_a", world, 615, -61, -5488, 685, -7, -5418);

        add(gates, "gate_a", world, 686, -61, -5346, 614, -10, -5418);
        add(gates, "gate_b", world, 686, -61, -5276, 614, -10, -5346);
    }

    private void add(List<TemplateOption> list, String id, World world,
                     int x1, int y1, int z1, int x2, int y2, int z2) {
        list.add(new TemplateOption(id, RoomTemplate.capture(world, x1, y1, z1, x2, y2, z2, false)));
    }

    private boolean allTemplateFamiliesAvailable() {
        return !corners.isEmpty() && !straights.isEmpty() && !deadEnds.isEmpty()
                && !connectors.isEmpty() && !towers.isEmpty() && !gates.isEmpty();
    }

    private int parsePositiveInt(String[] args, int index, int fallback) {
        if (args.length <= index) {
            return fallback;
        }
        try {
            int value = Integer.parseInt(args[index]);
            return value > 0 ? value : -1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private List<String> filter(List<String> values, String token) {
        String lower = token == null ? "" : token.toLowerCase(Locale.ROOT);
        return values.stream().filter(v -> v.startsWith(lower)).toList();
    }

    private void sendUsage(Player player) {
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Usage: /debug stronghold <spawn|spawnstep|despawn> ...");
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                " - /debug stronghold spawn <size>");
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                " - /debug stronghold spawnstep <size> [delayTicks]");
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                " - /debug stronghold despawn");
    }

    private static final class ActiveStronghold {
        private final World world;
        private final Map<Location, BlockData> snapshot = new HashMap<>();

        private ActiveStronghold(World world) {
            this.world = world;
        }
    }

    private static final class TemplateOption {
        private final String id;
        private final RoomTemplate template;

        private TemplateOption(String id, RoomTemplate template) {
            this.id = id;
            this.template = template;
        }
    }

    private static final class Node {
        private final int id;
        private final List<Integer> neighbors = new ArrayList<>();

        private Node(int id) {
            this.id = id;
        }
    }

    private static final class Placement {
        private final TemplateOption option;
        private final int rotation;
        private final Location center;

        private Placement(TemplateOption option, int rotation, Location center) {
            this.option = option;
            this.rotation = rotation;
            this.center = center;
        }
    }
}
