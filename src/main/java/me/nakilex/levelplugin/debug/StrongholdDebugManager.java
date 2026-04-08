package me.nakilex.levelplugin.debug;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dungeon.Direction;
import me.nakilex.levelplugin.dungeon.Dungeon;
import me.nakilex.levelplugin.dungeon.DungeonManager;
import me.nakilex.levelplugin.dungeon.RoomTemplate;
import me.nakilex.levelplugin.dungeon.generation.BranchingRandomGraphGenerator;
import me.nakilex.levelplugin.dungeon.generation.DungeonGraphGenerator;
import me.nakilex.levelplugin.dungeon.generation.GridNode;
import me.nakilex.levelplugin.dungeon.generation.SnakeGraphGenerator;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StrongholdDebugManager implements Listener {
    private static final Set<Material> TEMPLATE_IGNORE = EnumSet.of(Material.WHITE_CONCRETE, Material.LIGHT_BLUE_CONCRETE);
    private static final Set<Material> STRONGHOLD_SKIP = EnumSet.of(Material.REDSTONE_BLOCK, Material.PINK_WOOL, Material.LIME_WOOL);
    private static final String TEMPLATE_GUI_TITLE = "Stronghold Templates";

    private final Main plugin;
    private final DungeonManager dungeonManager;
    private final Random random = new Random();

    private final List<RoomTemplate> cornerTemplates = new ArrayList<>();
    private final List<RoomTemplate> straightTemplates = new ArrayList<>();
    private final List<RoomTemplate> deadEndTemplates = new ArrayList<>();
    private final List<RoomTemplate> connectorTemplates = new ArrayList<>();
    private final List<RoomTemplate> towerTemplates = new ArrayList<>();
    private final List<RoomTemplate> gateTemplates = new ArrayList<>();
    private final Map<String, TemplateEntry> templateEntries = new LinkedHashMap<>();
    private final Map<RoomTemplate, String> templateIds = new HashMap<>();

    private final Map<UUID, ActiveStronghold> activeByPlayer = new ConcurrentHashMap<>();
    private final Map<RoomTemplate, Integer> templateUsage = new HashMap<>();
    private final Deque<RoomTemplate> recentTemplates = new ArrayDeque<>();
    private volatile boolean templatesLoaded = false;
    private volatile double overlapAllowance = 0.10D;

    public StrongholdDebugManager(Main plugin, DungeonManager dungeonManager) {
        this.plugin = plugin;
        this.dungeonManager = dungeonManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void spawn(Player player, int size, GraphMode mode) {
        spawnInternal(player, size, -1, mode);
    }

    public void spawnStep(Player player, int size, long delayTicks, GraphMode mode) {
        spawnInternal(player, size, Math.max(1L, delayTicks), mode);
    }

    public void despawn(Player player) {
        ActiveStronghold active = activeByPlayer.remove(player.getUniqueId());
        if (active == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "No active stronghold to despawn.");
            return;
        }
        if (active.task != null) {
            active.task.cancel();
        }
        restoreSnapshot(active.restoreSnapshot);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Stronghold debug instance despawned and world restored.");
    }

    public void setOverlapAllowancePercent(double percent) {
        this.overlapAllowance = Math.max(0.0D, Math.min(1.0D, percent / 100.0D));
    }

    public double getOverlapAllowancePercent() {
        return overlapAllowance * 100.0D;
    }

    public void openTemplateGui(Player player) {
        if (!ensureTemplatesLoaded(player)) {
            return;
        }
        int size = Math.max(27, ((templateEntries.size() + 8) / 9) * 9);
        size = Math.min(54, size);
        Inventory inv = Bukkit.createInventory(null, size, TEMPLATE_GUI_TITLE);
        int slot = 0;
        for (TemplateEntry entry : templateEntries.values()) {
            if (slot >= inv.getSize()) break;
            inv.setItem(slot++, createTemplateToggleItem(entry));
        }
        player.openInventory(inv);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Click a template to toggle it on/off for stronghold generation.");
    }

    @EventHandler
    public void onTemplateGuiClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!TEMPLATE_GUI_TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String key = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        if (key == null || key.isBlank()) return;
        TemplateEntry entry = templateEntries.get(key.toLowerCase(Locale.ROOT));
        if (entry == null) return;
        entry.enabled = !entry.enabled;
        event.getInventory().setItem(slot, createTemplateToggleItem(entry));
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                entry.id + " is now " + (entry.enabled ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled") + ChatColor.GRAY + ".");
    }

    private void spawnInternal(Player player, int size, long stepDelayTicks, GraphMode graphMode) {
        if (size < 2) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Size must be at least 2.");
            return;
        }
        if (!ensureTemplatesLoaded(player)) {
            return;
        }

        ActiveStronghold previous = activeByPlayer.remove(player.getUniqueId());
        if (previous != null) {
            if (previous.task != null) previous.task.cancel();
            restoreSnapshot(previous.restoreSnapshot);
        }

        List<GridNode> graph = generateGraphForTemplates(graphMode, size);
        if (graph.isEmpty() || graph.size() < size) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Failed to generate stronghold graph for mode '" + graphMode.id() + "'.");
            return;
        }

        List<NodePlan> plans = new ArrayList<>();
        Map<Integer, NodePlan> planById = new HashMap<>();
        Map<Location, BlockData> snapshot = new HashMap<>();
        Dungeon debugDungeon = new Dungeon(player.getWorld(), "stronghold-debug-" + player.getUniqueId());
        templateUsage.clear();
        recentTemplates.clear();

        Location rootCenter = player.getLocation().getBlock().getLocation();

        if (graph.isEmpty()) {
            rollbackAndFail(player, snapshot, "Stronghold graph is empty.");
            return;
        }

        GridNode root = graph.get(0);
        List<GridNode> pendingNodes = new ArrayList<>(graph);
        pendingNodes.remove(0);

        int straightWallsSinceGate = 0;
        int towerCount = 0;
        int gateCount = 0;

        NodePlan rootPlan = placeNode(player, root, rootCenter, rootCenter, planById, snapshot, debugDungeon,
                straightWallsSinceGate, towerCount, gateCount, graph, graphMode);
        if (rootPlan == null) {
            return;
        }
        plans.add(rootPlan);
        planById.put(rootPlan.id, rootPlan);
        noteTemplateUsage(rootPlan.template);
        if (gateTemplates.contains(rootPlan.template)) {
            gateCount++;
            straightWallsSinceGate = 0;
        } else if (towerTemplates.contains(rootPlan.template)) {
            towerCount++;
            straightWallsSinceGate++;
        } else {
            EnumSet<Direction> dirs = rootPlan.node.directions();
            if (dirs.equals(EnumSet.of(Direction.NORTH, Direction.SOUTH)) || dirs.equals(EnumSet.of(Direction.EAST, Direction.WEST))) {
                straightWallsSinceGate++;
            }
        }

        while (!pendingNodes.isEmpty()) {
            boolean placedAny = false;
            Iterator<GridNode> it = pendingNodes.iterator();
            while (it.hasNext()) {
                GridNode node = it.next();
                if (!hasPlacedNeighbor(node, planById)) {
                    continue;
                }
                NodePlan plan = placeNode(player, node, null, rootCenter, planById, snapshot, debugDungeon,
                        straightWallsSinceGate, towerCount, gateCount, graph, graphMode);
                if (plan == null) {
                    return;
                }
                plans.add(plan);
                planById.put(plan.id, plan);
                noteTemplateUsage(plan.template);
                EnumSet<Direction> dirs = plan.node.directions();
                boolean isOpposite = dirs.equals(EnumSet.of(Direction.NORTH, Direction.SOUTH))
                        || dirs.equals(EnumSet.of(Direction.EAST, Direction.WEST));
                if (gateTemplates.contains(plan.template)) {
                    gateCount++;
                    straightWallsSinceGate = 0;
                } else if (towerTemplates.contains(plan.template)) {
                    towerCount++;
                    straightWallsSinceGate++;
                } else if (isOpposite) {
                    straightWallsSinceGate++;
                }
                it.remove();
                placedAny = true;
            }
            if (!placedAny) {
                rollbackAndFail(player, snapshot, "Failed to resolve template alignment order for all graph nodes.");
                return;
            }
        }

        List<ConnectorPlan> connectorPlans = buildConnectorPlans(plans, snapshot, debugDungeon);
        if (connectorPlans == null) {
            rollbackAndFail(player, snapshot, "Failed to align stronghold connectors without overlap.");
            return;
        }

        ActiveStronghold active = new ActiveStronghold(player.getWorld(), snapshot, plans, connectorPlans, debugDungeon, null);
        if (stepDelayTicks > 0) {
            restoreSnapshot(snapshot);
            BukkitTask task = runStepPlacement(player, plans, connectorPlans, snapshot, debugDungeon, stepDelayTicks);
            activeByPlayer.put(player.getUniqueId(), new ActiveStronghold(player.getWorld(), snapshot, plans, connectorPlans, debugDungeon, task));
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Stronghold step spawn started (" + plans.size() + " rooms).");
            return;
        }

        activeByPlayer.put(player.getUniqueId(), active);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Stronghold spawned with " + plans.size() + " rooms.");
    }

    private BukkitTask runStepPlacement(Player player, List<NodePlan> plans, List<ConnectorPlan> connectorPlans, Map<Location, BlockData> snapshot, Dungeon dungeon, long delayTicks) {
        List<PlacementPlan> placements = new ArrayList<>(plans.size() + connectorPlans.size());
        for (NodePlan p : plans) placements.add(new PlacementPlan(p.template, p.rotation, p.center));
        for (ConnectorPlan p : connectorPlans) placements.add(new PlacementPlan(p.template, p.rotation, p.center));
        final int[] idx = {0};
        return Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                ActiveStronghold active = activeByPlayer.remove(player.getUniqueId());
                if (active != null && active.task != null) active.task.cancel();
                restoreSnapshot(snapshot);
                return;
            }
            if (idx[0] >= placements.size()) {
                ActiveStronghold active = activeByPlayer.get(player.getUniqueId());
                if (active != null && active.task != null) {
                    active.task.cancel();
                    activeByPlayer.put(player.getUniqueId(), new ActiveStronghold(active.world, active.restoreSnapshot, active.placed, active.connectors, active.dungeon, null));
                }
                return;
            }
            PlacementPlan p = placements.get(idx[0]++);
            dungeonManager.pasteRoom(dungeon, p.template, p.rotation, p.center, null, false, TEMPLATE_IGNORE);
        }, 1L, delayTicks);
    }

    private void rollbackAndFail(Player player, Map<Location, BlockData> snapshot, String reason) {
        restoreSnapshot(snapshot);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, reason);
    }

    private boolean hasPlacedNeighbor(GridNode node, Map<Integer, NodePlan> placed) {
        for (Integer nid : node.neighbors().values()) {
            if (placed.containsKey(nid)) return true;
        }
        return false;
    }

    private NodePlan placeNode(Player player,
                               GridNode node,
                               Location fixedCenter,
                               Location fallback,
                               Map<Integer, NodePlan> planById,
                               Map<Location, BlockData> snapshot,
                               Dungeon debugDungeon,
                               int straightWallsSinceGate,
                               int towerCount,
                               int gateCount,
                               List<GridNode> graph,
                               GraphMode graphMode) {
        EnumSet<Direction> dirs = node.directions();
        RoomTemplate template = selectTemplate(dirs, straightWallsSinceGate, towerCount, gateCount, planById, node, graph, graphMode);
        if (template == null) {
            rollbackAndFail(player, snapshot, "No template matched connector pattern " + dirs + ".");
            return null;
        }
        int rotation = findRotationForPlacement(template, dirs);
        Location center = fixedCenter != null ? fixedCenter.clone() : solveCenter(node, template, rotation, planById, fallback);
        if (center == null) {
            rollbackAndFail(player, snapshot, "Failed to align template connectors for node " + node.id() + ".");
            return null;
        }
        DungeonManager.PasteResult preview = dungeonManager.pasteRoom(debugDungeon, template, rotation, center, null, true,
                TEMPLATE_IGNORE, overlapAllowance);
        if (!preview.success()) {
            rollbackAndFail(player, snapshot, "Template overlap too high (" + String.format(Locale.US, "%.1f", preview.overlap() * 100.0D)
                    + "% > " + String.format(Locale.US, "%.1f", overlapAllowance * 100.0D) + "%) at node " + node.id() + ".");
            return null;
        }
        captureForRestore(snapshot, template, rotation, center);
        DungeonManager.PasteResult result = dungeonManager.pasteRoom(debugDungeon, template, rotation, center, null, false, TEMPLATE_IGNORE);
        if (!result.success()) {
            rollbackAndFail(player, snapshot, "Failed to paste stronghold node " + node.id() + ".");
            return null;
        }
        return new NodePlan(node.id(), node, template, rotation, center);
    }

    private void captureForRestore(Map<Location, BlockData> snapshot, RoomTemplate template, int rotation, Location center) {
        World world = center.getWorld();
        if (world == null) return;
        for (RoomTemplate.BlockDef b : template.getBlocks()) {
            Material mat = b.data.getMaterial();
            if (TEMPLATE_IGNORE.contains(mat) || STRONGHOLD_SKIP.contains(mat)) continue;
            Location loc = blockLocationFor(template, b.x, b.y, b.z, rotation, center);
            snapshot.putIfAbsent(loc, world.getBlockAt(loc).getBlockData());
        }
        for (RoomTemplate.Marker m : template.getPortals()) {
            Location loc = blockLocationFor(template, m.x, m.y, m.z, rotation, center);
            snapshot.putIfAbsent(loc, world.getBlockAt(loc).getBlockData());
        }
        if (template.getBossSpawn() != null) {
            RoomTemplate.Marker m = template.getBossSpawn();
            Location loc = blockLocationFor(template, m.x, m.y, m.z, rotation, center);
            snapshot.putIfAbsent(loc, world.getBlockAt(loc).getBlockData());
        }
    }

    private Location blockLocationFor(RoomTemplate template, int x, int y, int z, int rotation, Location center) {
        int[] vec = RoomTemplate.rotate(x - (int) Math.round(template.getCenterX()),
                z - (int) Math.round(template.getCenterZ()), rotation);
        int wx = center.getBlockX() + vec[0];
        int wy = center.getBlockY() + (y - template.getConnectorMinY());
        int wz = center.getBlockZ() + vec[1];
        return new Location(center.getWorld(), wx, wy, wz);
    }

    private List<ConnectorPlan> buildConnectorPlans(List<NodePlan> plans, Map<Location, BlockData> snapshot, Dungeon dungeon) {
        List<ConnectorPlan> connectorPlans = new ArrayList<>();
        Map<Integer, NodePlan> byId = new HashMap<>();
        for (NodePlan p : plans) byId.put(p.id, p);

        for (NodePlan p : plans) {
            for (Direction d : p.node.directions()) {
                Integer nid = p.node.neighbors().get(d);
                if (nid == null || p.id > nid) continue;
                NodePlan neighbor = byId.get(nid);
                if (neighbor == null) continue;

                ConnectorPlan connectorPlan = buildConnectorPlan(p, neighbor, d);
                if (connectorPlan == null) {
                    if (connectorsAlreadyTouching(p, neighbor, d)) {
                        continue;
                    }
                    return null;
                }
                DungeonManager.PasteResult preview = dungeonManager.pasteRoom(dungeon, connectorPlan.template,
                        connectorPlan.rotation, connectorPlan.center, null, true, TEMPLATE_IGNORE, overlapAllowance);
                if (!preview.success()) {
                    return null;
                }
                captureForRestore(snapshot, connectorPlan.template, connectorPlan.rotation, connectorPlan.center);
                dungeonManager.pasteRoom(dungeon, connectorPlan.template, connectorPlan.rotation,
                        connectorPlan.center, null, false, TEMPLATE_IGNORE);
                connectorPlans.add(connectorPlan);
            }
        }

        return connectorPlans;
    }

    private boolean connectorsAlreadyTouching(NodePlan a, NodePlan b, Direction directionFromA) {
        Location aTarget = connectorAnchorLocation(a, directionFromA);
        Location bTarget = connectorAnchorLocation(b, directionFromA.opposite());
        if (aTarget == null || bTarget == null) {
            return false;
        }
        if (!Objects.equals(aTarget.getWorld(), bTarget.getWorld())) {
            return false;
        }
        int dx = Math.abs(aTarget.getBlockX() - bTarget.getBlockX());
        int dy = Math.abs(aTarget.getBlockY() - bTarget.getBlockY());
        int dz = Math.abs(aTarget.getBlockZ() - bTarget.getBlockZ());
        return dy == 0 && (dx + dz) == 0;
    }

    private ConnectorPlan buildConnectorPlan(NodePlan a, NodePlan b, Direction directionFromA) {
        Location aTarget = connectorAnchorLocation(a, directionFromA);
        Location bTarget = connectorAnchorLocation(b, directionFromA.opposite());
        if (aTarget == null || bTarget == null) return null;

        for (RoomTemplate connectorTemplate : connectorTemplates) {
            int rotation = findRotation(connectorTemplate, EnumSet.of(directionFromA, directionFromA.opposite()));
            if (rotation < 0) continue;

            RoomTemplate.Connector enter = findConnector(connectorTemplate, rotation, directionFromA);
            RoomTemplate.Connector exit = findConnector(connectorTemplate, rotation, directionFromA.opposite());
            if (enter == null || exit == null) continue;

            Location center = centerFromAnchor(connectorTemplate, enter, rotation, aTarget, a.center);
            if (center == null) continue;
            Location resolvedExit = connectorAnchorLocation(connectorTemplate, exit, rotation, center);
            if (resolvedExit != null
                    && resolvedExit.getBlockX() == bTarget.getBlockX()
                    && resolvedExit.getBlockY() == bTarget.getBlockY()
                    && resolvedExit.getBlockZ() == bTarget.getBlockZ()) {
                return new ConnectorPlan(connectorTemplate, rotation, center);
            }
        }
        return null;
    }

    private Location connectorWorldLocation(NodePlan plan, Direction direction) {
        for (RoomTemplate.Connector c : plan.template.getConnectors()) {
            Direction facing = rotateDirection(c.facing, plan.rotation);
            if (facing != direction) continue;
            return blockLocationFor(plan.template, c.x, c.bottomY, c.z, plan.rotation, plan.center);
        }
        return null;
    }

    private Location connectorAnchorLocation(NodePlan plan, Direction direction) {
        for (RoomTemplate.Connector c : plan.template.getConnectors()) {
            Direction facing = rotateDirection(c.facing, plan.rotation);
            if (facing != direction) continue;
            return connectorAnchorLocation(plan.template, c, plan.rotation, plan.center);
        }
        return null;
    }

    private Location connectorAnchorLocation(RoomTemplate template, RoomTemplate.Connector connector, int rotation, Location center) {
        Location marker = blockLocationFor(template, connector.x, connector.bottomY, connector.z, rotation, center);
        Direction outward = rotateDirection(connector.facing, rotation);
        int[] vec = directionVector(outward);
        return marker.add(vec[0], 0, vec[1]);
    }

    private Location centerFromAnchor(RoomTemplate template, RoomTemplate.Connector connector, int rotation, Location anchor, Location fallback) {
        if (anchor == null || fallback == null || fallback.getWorld() == null) return null;
        int[] vec = RoomTemplate.rotate(connector.x - (int) Math.round(template.getCenterX()),
                connector.z - (int) Math.round(template.getCenterZ()), rotation);
        int cx = anchor.getBlockX() - vec[0];
        int cy = anchor.getBlockY() - (connector.bottomY - template.getConnectorMinY());
        int cz = anchor.getBlockZ() - vec[1];
        return new Location(fallback.getWorld(), cx, cy, cz);
    }

    private Location solveCenter(GridNode node, RoomTemplate template, int rotation, Map<Integer, NodePlan> placed, Location fallback) {
        List<Map.Entry<Direction, Integer>> attached = new ArrayList<>();
        for (Map.Entry<Direction, Integer> edge : node.neighbors().entrySet()) {
            NodePlan neighbor = placed.get(edge.getValue());
            if (neighbor != null) {
                attached.add(edge);
            }
        }
        if (attached.isEmpty()) return null;

        for (Map.Entry<Direction, Integer> edge : attached) {
            Direction dirToNeighbor = edge.getKey();
            NodePlan neighbor = placed.get(edge.getValue());
            if (neighbor == null) continue;
            List<RoomTemplate.Connector> thisOptions = connectorsFacing(template, rotation, dirToNeighbor);
            List<RoomTemplate.Connector> otherOptions = connectorsFacing(neighbor.template, neighbor.rotation, dirToNeighbor.opposite());
            for (RoomTemplate.Connector thisConn : thisOptions) {
                for (RoomTemplate.Connector otherConn : otherOptions) {
                    Location target = connectorAnchorLocation(neighbor.template, otherConn, neighbor.rotation, neighbor.center);
                    Location center = centerFromAnchor(template, thisConn, rotation, target, fallback);
                    if (center == null) continue;
                    if (centerAlignsToPlacedNeighbors(node, template, rotation, center, placed)) {
                        return center;
                    }
                }
            }
        }
        return null;
    }

    private boolean centerAlignsToPlacedNeighbors(GridNode node,
                                                  RoomTemplate template,
                                                  int rotation,
                                                  Location center,
                                                  Map<Integer, NodePlan> placed) {
        for (Map.Entry<Direction, Integer> edge : node.neighbors().entrySet()) {
            NodePlan neighbor = placed.get(edge.getValue());
            if (neighbor == null) continue;
            Direction dir = edge.getKey();
            List<RoomTemplate.Connector> ours = connectorsFacing(template, rotation, dir);
            List<RoomTemplate.Connector> theirs = connectorsFacing(neighbor.template, neighbor.rotation, dir.opposite());
            if (ours.isEmpty() || theirs.isEmpty()) return false;
            boolean matched = false;
            for (RoomTemplate.Connector a : ours) {
                Location aLoc = connectorAnchorLocation(template, a, rotation, center);
                for (RoomTemplate.Connector b : theirs) {
                    Location bLoc = connectorAnchorLocation(neighbor.template, b, neighbor.rotation, neighbor.center);
                    if (aLoc.getBlockX() == bLoc.getBlockX()
                            && aLoc.getBlockY() == bLoc.getBlockY()
                            && aLoc.getBlockZ() == bLoc.getBlockZ()) {
                        matched = true;
                        break;
                    }
                }
                if (matched) break;
            }
            if (!matched) return false;
        }
        return true;
    }

    private List<RoomTemplate.Connector> connectorsFacing(RoomTemplate template, int rotation, Direction direction) {
        List<RoomTemplate.Connector> out = new ArrayList<>();
        for (RoomTemplate.Connector c : template.getConnectors()) {
            if (rotateDirection(c.facing, rotation) == direction) {
                out.add(c);
            }
        }
        out.sort(Comparator.comparing((RoomTemplate.Connector c) -> !c.entrance).thenComparingInt(c -> c.bottomY));
        return out;
    }

    private int[] directionVector(Direction direction) {
        return switch (direction) {
            case NORTH -> new int[]{0, -1};
            case SOUTH -> new int[]{0, 1};
            case EAST -> new int[]{1, 0};
            case WEST -> new int[]{-1, 0};
        };
    }

    private RoomTemplate.Connector findConnector(RoomTemplate t, int rotation, Direction want) {
        for (RoomTemplate.Connector c : t.getConnectors()) {
            if (rotateDirection(c.facing, rotation) == want) return c;
        }
        return null;
    }

    private List<GridNode> generateGraphForTemplates(GraphMode mode, int size) {
        if (mode == GraphMode.TEST) {
            return buildTestGraph(size);
        }
        List<GridNode> graph = mode.generator.generate(size, random);
        if (isGraphTemplateCompatible(graph)) {
            return graph;
        }
        // Branching can exceed available connector patterns; retry with conservative cap.
        if (mode == GraphMode.BRANCHING) {
            DungeonGraphGenerator fallback = new BranchingRandomGraphGenerator(2);
            for (int attempt = 0; attempt < 6; attempt++) {
                List<GridNode> candidate = fallback.generate(size, random);
                if (isGraphTemplateCompatible(candidate)) {
                    return candidate;
                }
            }
        }
        return graph;
    }

    private List<GridNode> buildTestGraph(int size) {
        int targetSize = Math.max(9, size);
        GridNode center = new GridNode(0, 0, 0);
        GridNode northArm = new GridNode(1, 0, -1);
        GridNode eastArm = new GridNode(2, 1, 0);
        GridNode southArm = new GridNode(3, 0, 1);
        GridNode westArm = new GridNode(4, -1, 0);
        GridNode northEnd = new GridNode(5, 0, -2);
        GridNode eastEnd = new GridNode(6, 2, 0);
        GridNode southEnd = new GridNode(7, 0, 2);
        GridNode westEnd = new GridNode(8, -2, 0);

        link(center, northArm, Direction.NORTH);
        link(center, eastArm, Direction.EAST);
        link(center, southArm, Direction.SOUTH);
        link(center, westArm, Direction.WEST);
        link(northArm, northEnd, Direction.NORTH);
        link(eastArm, eastEnd, Direction.EAST);
        link(southArm, southEnd, Direction.SOUTH);
        link(westArm, westEnd, Direction.WEST);

        List<GridNode> ordered = new ArrayList<>(List.of(
                center, northArm, eastArm, southArm, westArm,
                northEnd, eastEnd, southEnd, westEnd
        ));
        if (targetSize <= ordered.size()) {
            return new ArrayList<>(ordered.subList(0, targetSize));
        }

        EnumMap<Direction, GridNode> endpoints = new EnumMap<>(Direction.class);
        endpoints.put(Direction.NORTH, northEnd);
        endpoints.put(Direction.EAST, eastEnd);
        endpoints.put(Direction.SOUTH, southEnd);
        endpoints.put(Direction.WEST, westEnd);

        Direction[] order = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        int nextId = ordered.size();
        int step = 0;
        while (ordered.size() < targetSize) {
            Direction direction = order[step++ % order.length];
            GridNode tail = endpoints.get(direction);
            int[] vec = directionVector(direction);
            GridNode extension = new GridNode(nextId++, tail.gx() + vec[0], tail.gz() + vec[1]);
            link(tail, extension, direction);
            endpoints.put(direction, extension);
            ordered.add(extension);
        }
        return ordered;
    }

    private void link(GridNode a, GridNode b, Direction fromAToB) {
        a.link(fromAToB, b.id());
        b.link(fromAToB.opposite(), a.id());
    }

    private boolean isGraphTemplateCompatible(List<GridNode> graph) {
        if (graph == null || graph.isEmpty()) {
            return false;
        }
        for (GridNode node : graph) {
            if (selectTemplate(node.directions()) == null) {
                return false;
            }
        }
        return true;
    }

    private RoomTemplate selectTemplate(EnumSet<Direction> dirs,
                                        int straightWallsSinceGate,
                                        int towerCount,
                                        int gateCount,
                                        Map<Integer, NodePlan> placed,
                                        GridNode node,
                                        List<GridNode> graph,
                                        GraphMode mode) {
        RoomTemplate standard = selectTemplate(dirs);
        int degree = dirs.size();
        boolean bigAllowed = canPlaceBigStructure(node, placed, graph, 2);
        RoomTemplate bigCandidate = null;
        if (degree >= 4) {
            RoomTemplate tower = pickTemplateWithVariety(towerTemplates, dirs);
            if (tower != null && canPlaceTower(node, placed, graph) && findRotationForPlacement(tower, dirs) >= 0) {
                bigCandidate = tower;
            }
            return chooseBigOrStandard(bigCandidate, standard, bigAllowed, 0.25D);
        }
        boolean opposite = dirs.equals(EnumSet.of(Direction.NORTH, Direction.SOUTH))
                || dirs.equals(EnumSet.of(Direction.EAST, Direction.WEST));
        if (degree == 2 && opposite) {
            if (mode == GraphMode.TEST) {
                return standard;
            }
            if (straightWallsSinceGate >= 2 && towerCount > gateCount && canPlaceGate(node, placed, graph)) {
                RoomTemplate gate = pickTemplateWithVariety(gateTemplates, dirs);
                if (gate != null && findRotationForPlacement(gate, dirs) >= 0) {
                    bigCandidate = gate;
                }
            }
            if (bigCandidate == null) {
                RoomTemplate tower = pickTemplateWithVariety(towerTemplates, dirs);
                if (tower != null && canPlaceTower(node, placed, graph) && findRotationForPlacement(tower, dirs) >= 0) {
                    bigCandidate = tower;
                }
            }
            return chooseBigOrStandard(bigCandidate, standard, bigAllowed, 0.35D);
        }
        if (degree == 2) {
            return standard;
        }
        if (degree == 1) {
            return standard;
        }
        if (degree == 3) {
            RoomTemplate tower = pickTemplateWithVariety(towerTemplates, dirs);
            if (tower != null && canPlaceTower(node, placed, graph) && findRotationForPlacement(tower, dirs) >= 0) {
                bigCandidate = tower;
            }
            RoomTemplate gate = pickTemplateWithVariety(gateTemplates, dirs);
            if (bigCandidate == null && gate != null && canPlaceGate(node, placed, graph) && findRotationForPlacement(gate, dirs) >= 0) {
                bigCandidate = gate;
            }
            return chooseBigOrStandard(bigCandidate, standard, bigAllowed, 0.30D);
        }
        return standard;
    }

    private boolean canPlaceGate(GridNode node, Map<Integer, NodePlan> placed, List<GridNode> graph) {
        for (Integer nid : node.neighbors().values()) {
            NodePlan p = placed.get(nid);
            if (p == null) continue;
            if (gateTemplates.contains(p.template) || towerTemplates.contains(p.template)) return false;
        }
        return true;
    }

    private boolean canPlaceTower(GridNode node, Map<Integer, NodePlan> placed, List<GridNode> graph) {
        for (Integer nid : node.neighbors().values()) {
            NodePlan p = placed.get(nid);
            if (p != null && gateTemplates.contains(p.template)) return false;
        }
        return true;
    }

    private RoomTemplate selectTemplate(EnumSet<Direction> dirs) {
        int degree = dirs.size();
        if (degree == 1) {
            RoomTemplate dead = pickTemplateWithVariety(deadEndTemplates, dirs);
            if (dead != null && findRotationForPlacement(dead, dirs) >= 0) return dead;
        }
        if (degree == 2) {
            boolean opposite = dirs.equals(EnumSet.of(Direction.NORTH, Direction.SOUTH))
                    || dirs.equals(EnumSet.of(Direction.EAST, Direction.WEST));
            RoomTemplate candidate = opposite ? pickTemplateWithVariety(straightTemplates, dirs) : pickTemplateWithVariety(cornerTemplates, dirs);
            if (candidate != null && findRotationForPlacement(candidate, dirs) >= 0) return candidate;
        }
        RoomTemplate fallback = pickTemplateWithVariety(straightTemplates, dirs);
        if (fallback != null && findRotationForPlacement(fallback, dirs) >= 0) return fallback;
        for (RoomTemplate t : allTemplates()) {
            if (!isTemplateEnabled(t)) continue;
            if (findRotationForPlacement(t, dirs) >= 0) return t;
        }
        return null;
    }

    private List<RoomTemplate> allTemplates() {
        List<RoomTemplate> all = new ArrayList<>();
        all.addAll(cornerTemplates);
        all.addAll(straightTemplates);
        all.addAll(deadEndTemplates);
        all.addAll(connectorTemplates);
        all.addAll(towerTemplates);
        all.addAll(gateTemplates);
        return all;
    }

    private int findRotationForPlacement(RoomTemplate template, Set<Direction> required) {
        int exact = findRotation(template, required);
        if (exact >= 0) {
            return exact;
        }
        if (!supportsOptionalConnectors(template)) {
            return -1;
        }
        return findRotationContaining(template, required);
    }

    private boolean supportsOptionalConnectors(RoomTemplate template) {
        return towerTemplates.contains(template) || gateTemplates.contains(template);
    }

    private int findRotationContaining(RoomTemplate template, Set<Direction> required) {
        for (int r = 0; r < 4; r++) {
            Set<Direction> rotated = template.getRotatedDirections(r);
            if (rotated.containsAll(required)) {
                return r;
            }
        }
        return -1;
    }

    private int findRotation(RoomTemplate template, Set<Direction> target) {
        for (int r = 0; r < 4; r++) {
            if (template.getRotatedDirections(r).equals(target)) return r;
        }
        return -1;
    }

    private Direction rotateDirection(Direction dir, int rotation) {
        Direction out = dir;
        for (int i = 0; i < (rotation & 3); i++) {
            out = switch (out) {
                case NORTH -> Direction.EAST;
                case EAST -> Direction.SOUTH;
                case SOUTH -> Direction.WEST;
                case WEST -> Direction.NORTH;
            };
        }
        return out;
    }

    private boolean ensureTemplatesLoaded(Player player) {
        if (templatesLoaded) return true;
        World flatland = Bukkit.getWorld("flatland");
        if (flatland == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "World 'flatland' is required for stronghold debug templates.");
            return false;
        }
        cornerTemplates.clear(); straightTemplates.clear(); deadEndTemplates.clear(); connectorTemplates.clear(); towerTemplates.clear(); gateTemplates.clear();
        templateEntries.clear();
        templateIds.clear();

        load(cornerTemplates, "corner", flatland, 473, -38, -5346, 543, -61, -5276);
        load(cornerTemplates, "corner", flatland, 544, -38, -5631, 614, -61, -5701);
        load(cornerTemplates, "corner", flatland, 614, -61, -5630, 544, -38, -5560);

        load(straightTemplates, "straight", flatland, 402, -38, -5276, 472, -61, -5346);
        load(straightTemplates, "straight", flatland, 472, -61, -5347, 402, -38, -5417);
        load(straightTemplates, "straight", flatland, 402, -38, -5418, 472, -61, -5488);
        load(straightTemplates, "straight", flatland, 472, -61, -5489, 402, -38, -5559);
        load(straightTemplates, "straight", flatland, 402, -38, -5560, 472, -61, -5630);
        load(straightTemplates, "straight", flatland, 472, -61, -5631, 402, -38, -5701);
        load(straightTemplates, "straight", flatland, 473, -38, -5701, 543, -61, -5631);
        load(straightTemplates, "straight", flatland, 543, -61, -5630, 473, -38, -5560);
        load(straightTemplates, "straight", flatland, 473, -38, -5417, 543, -61, -5347);

        load(deadEndTemplates, "deadend", flatland, 543, -38, -5418, 473, -61, -5488);
        load(deadEndTemplates, "deadend", flatland, 473, -61, -5489, 543, -38, -5559);

        load(connectorTemplates, "connector", flatland, 412, -61, -5711, 402, -38, -5701);
        load(connectorTemplates, "connector", flatland, 402, -38, -5721, 412, -61, -5711);

        load(towerTemplates, "tower", flatland, 615, -61, -5488, 685, -7, -5418);
        load(towerTemplates, "tower", flatland, 615, -61, -5276, 685, -7, -5206);

        load(gateTemplates, "gate", flatland, 686, -61, -5346, 614, -10, -5418);
        load(gateTemplates, "gate", flatland, 686, -61, -5276, 614, -10, -5346);

        templatesLoaded = !straightTemplates.isEmpty();
        if (!templatesLoaded) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Failed to load stronghold templates from flatland.");
        }
        return templatesLoaded;
    }

    private void load(List<RoomTemplate> target, String category, World world, int x1, int y1, int z1, int x2, int y2, int z2) {
        RoomTemplate captured = RoomTemplate.capture(world, x1, y1, z1, x2, y2, z2, false);
        target.add(captured);
        String id = category + "_" + target.size();
        TemplateEntry entry = new TemplateEntry(id, category);
        templateEntries.put(id, entry);
        templateIds.put(captured, id);
    }

    private void restoreSnapshot(Map<Location, BlockData> snapshot) {
        for (Map.Entry<Location, BlockData> e : snapshot.entrySet()) {
            Location l = e.getKey();
            if (l.getWorld() == null) continue;
            l.getWorld().getBlockAt(l).setBlockData(e.getValue(), false);
        }
    }

    private <T> T pickRandom(List<T> list) {
        if (list == null || list.isEmpty()) return null;
        return list.get(random.nextInt(list.size()));
    }

    private RoomTemplate pickTemplateWithVariety(List<RoomTemplate> list, EnumSet<Direction> dirs) {
        if (list == null || list.isEmpty()) return null;
        List<RoomTemplate> eligible = new ArrayList<>();
        int minScore = Integer.MAX_VALUE;
        for (RoomTemplate template : list) {
            if (!isTemplateEnabled(template)) continue;
            if (findRotationForPlacement(template, dirs) < 0) continue;
            int score = templateUsage.getOrDefault(template, 0);
            if (recentTemplates.contains(template)) score += 2;
            if (score < minScore) {
                minScore = score;
                eligible.clear();
                eligible.add(template);
            } else if (score == minScore) {
                eligible.add(template);
            }
        }
        if (eligible.isEmpty()) return null;
        return pickRandom(eligible);
    }

    private RoomTemplate chooseBigOrStandard(RoomTemplate bigCandidate, RoomTemplate standard, boolean bigAllowed, double bigChance) {
        if (standard == null) return bigCandidate;
        if (!bigAllowed || bigCandidate == null) return standard;
        if (random.nextDouble() <= Math.max(0.0D, Math.min(1.0D, bigChance))) {
            return bigCandidate;
        }
        return standard;
    }

    private boolean canPlaceBigStructure(GridNode node, Map<Integer, NodePlan> placed, List<GridNode> graph, int minStraightRoomsBetween) {
        int requiredDistance = Math.max(1, minStraightRoomsBetween + 1);
        return distanceToNearestBig(node, placed, graph, requiredDistance) >= requiredDistance;
    }

    private int distanceToNearestBig(GridNode start, Map<Integer, NodePlan> placed, List<GridNode> graph, int cutoffDistance) {
        Map<Integer, GridNode> lookup = new HashMap<>();
        for (GridNode n : graph) lookup.put(n.id(), n);
        Deque<int[]> queue = new ArrayDeque<>();
        Set<Integer> visited = new HashSet<>();
        queue.add(new int[]{start.id(), 0});
        visited.add(start.id());
        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            GridNode node = lookup.get(current[0]);
            if (node == null) continue;
            int distance = current[1];
            NodePlan placedNode = placed.get(node.id());
            if (placedNode != null && isBigStructure(placedNode.template)) {
                return distance;
            }
            if (distance >= cutoffDistance) continue;
            for (Integer nid : node.neighbors().values()) {
                if (nid == null || !visited.add(nid)) continue;
                GridNode neighbor = lookup.get(nid);
                if (neighbor == null) continue;
                queue.add(new int[]{nid, distance + 1});
            }
        }
        return Integer.MAX_VALUE;
    }

    private boolean isBigStructure(RoomTemplate template) {
        return towerTemplates.contains(template) || gateTemplates.contains(template);
    }

    private boolean isTemplateEnabled(RoomTemplate template) {
        String id = templateIds.get(template);
        if (id == null) return true;
        TemplateEntry entry = templateEntries.get(id);
        return entry == null || entry.enabled;
    }

    private void noteTemplateUsage(RoomTemplate template) {
        templateUsage.merge(template, 1, Integer::sum);
        recentTemplates.addLast(template);
        while (recentTemplates.size() > 4) {
            recentTemplates.removeFirst();
        }
    }

    private ItemStack createTemplateToggleItem(TemplateEntry entry) {
        Material material = switch (entry.category) {
            case "straight" -> Material.STONE_BRICKS;
            case "corner" -> Material.CHISELED_STONE_BRICKS;
            case "deadend" -> Material.MOSSY_STONE_BRICKS;
            case "connector" -> Material.COBBLESTONE_WALL;
            case "tower" -> Material.POLISHED_DEEPSLATE;
            case "gate" -> Material.IRON_BARS;
            default -> Material.PAPER;
        };
        return GuiUtil.createToggleItem(entry.enabled,
                ChatColor.AQUA + entry.id,
                ChatColor.GRAY + "Category: " + ChatColor.WHITE + entry.category,
                ChatColor.GRAY + "Currently: " + (entry.enabled ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"),
                ChatColor.DARK_GRAY + "Click to toggle.");
    }

    private record ActiveStronghold(World world,
                                    Map<Location, BlockData> restoreSnapshot,
                                    List<NodePlan> placed,
                                    List<ConnectorPlan> connectors,
                                    Dungeon dungeon,
                                    BukkitTask task) {}

    private record NodePlan(int id, GridNode node, RoomTemplate template, int rotation, Location center) {}

    private record ConnectorPlan(RoomTemplate template, int rotation, Location center) {}

    private record PlacementPlan(RoomTemplate template, int rotation, Location center) {}

    private static final class TemplateEntry {
        private final String id;
        private final String category;
        private boolean enabled = true;

        private TemplateEntry(String id, String category) {
            this.id = id.toLowerCase(Locale.ROOT);
            this.category = category.toLowerCase(Locale.ROOT);
        }
    }

    public enum GraphMode {
        SNAKE(new SnakeGraphGenerator()),
        BRANCHING(new BranchingRandomGraphGenerator(3)),
        TEST(null);

        private final DungeonGraphGenerator generator;

        GraphMode(DungeonGraphGenerator generator) {
            this.generator = generator;
        }

        public static GraphMode fromArg(String raw) {
            if (raw == null || raw.isBlank()) {
                return SNAKE;
            }
            String normalized = raw.trim().toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "branch", "branches", "branching", "random" -> BRANCHING;
                case "snake", "serpentine" -> SNAKE;
                case "test", "towertest", "cross" -> TEST;
                default -> null;
            };
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static List<String> ids() {
            return Arrays.stream(values()).map(GraphMode::id).toList();
        }
    }

}
