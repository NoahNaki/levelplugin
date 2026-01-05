package me.nakilex.levelplugin.dungeon.verified;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import com.nexomc.nexo.api.NexoFurniture;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dungeon.Dungeon;
import me.nakilex.levelplugin.dungeon.DungeonLayout;
import me.nakilex.levelplugin.dungeon.DungeonManager;
import me.nakilex.levelplugin.dungeon.RoomTemplate;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.mob.utils.MythicMobModifier;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.essence.ClassEssence;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.utils.*;
import me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.CurrentLocation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

public class CrimsonReliquaryDungeon implements VerifiedDungeonDefinition {
    private static final String DISPLAY = "Crimson Reliquary";
    private static final String KEY = DungeonManager.normalizeKey(DISPLAY);
    private static final String SOURCE_WORLD = "flatland";
    private static final int MIN_X = -245;
    private static final int MIN_Y = -57;
    private static final int MIN_Z = -5936;
    private static final int MAX_X = 138;
    private static final int MAX_Y = 107;
    private static final int MAX_Z = -5387;
    private static final Location TEMPLATE_SPAWN = new Location(null, -211, -34, -5801);
    private static final NamespacedKey DUNGEON_FLOWER_KEY = new NamespacedKey(Main.getInstance(), "crimson_flower");
    private static final String DUNGEON_MOB_TAG = "reliquary_dungeon_mob";
    private static final List<NpcPlacement> NPCS = List.of(
            new NpcPlacement(-174, -43, -5805, 1420),
            new NpcPlacement(-166, -43, -5794, 658),
            new NpcPlacement(-167, -43, -5806, 671)
    );

    private final Main plugin;
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();
    private final Map<World, InstanceState> activeInstances = new HashMap<>();
    private volatile RoomTemplate cachedTemplate;
    private java.util.concurrent.CompletableFuture<RoomTemplate> templateFuture;

    public CrimsonReliquaryDungeon(Main plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(new InteractionListener(), plugin);
    }

    private static final class TemplateMarkers {
        private final List<Location> bossMarkers = new ArrayList<>();
        private final List<Location> miniBossMarkers = new ArrayList<>();
        private final List<Location> normalMarkers = new ArrayList<>();
        private final List<Location> yellowFlowers = new ArrayList<>();
        private final List<Location> bluePlacements = new ArrayList<>();
        private final List<Location> brownRewards = new ArrayList<>();
        private final List<TemplateChest> chestMarkers = new ArrayList<>();
    }

    private record TemplateChest(Location loc, BlockData data) {}

    private static final class PlayerState {
        final boolean allowFlight;
        final boolean flying;
        final boolean invulnerable;

        PlayerState(Player p) {
            this.allowFlight = p.getAllowFlight();
            this.flying = p.isFlying();
            this.invulnerable = p.isInvulnerable();
        }
    }

    private static final class MobMarker {
        private final Location loc;
        private final String mobId;
        private boolean spawned;
        private boolean spawning;
        private boolean proximityTriggered;
        private int ticksWaited;

        private MobMarker(Location loc, String mobId) {
            this.loc = loc;
            this.mobId = mobId;
        }
    }

    private enum FlowerType {
        POPPY(Material.POPPY, "Scarlet Poppy", "Blood-red petals hide a subtle glow."),
        DANDELION(Material.DANDELION, "Sunlit Dandelion", "Warm to the touch despite the chill."),
        BLUE_ORCHID(Material.BLUE_ORCHID, "Azure Orchid", "Its perfume hums with latent mana."),
        ALLIUM(Material.ALLIUM, "Amethyst Allium", "A violet bloom that refuses to wilt.");

        final Material block;
        final String display;
        final String description;

        FlowerType(Material block, String display, String description) {
            this.block = block;
            this.display = display;
            this.description = description;
        }
    }

    private static final class InstanceState {
        final Map<Location, FlowerType> pluckable = new HashMap<>();
        final Map<Location, FlowerType> placements = new HashMap<>();
        final Map<Location, MultiLineHologram> pluckHolograms = new HashMap<>();
        final Map<Location, MultiLineHologram> placementHolograms = new HashMap<>();
        final List<Location> rewardFountains = new ArrayList<>();
        final List<Location> expeditionPath = new ArrayList<>();
        Location bossPortalLocation;
        Location spawnLocation;
        final List<Player> participants = new ArrayList<>();
        final List<MobMarker> mobMarkers = new ArrayList<>();
        final List<NPC> npcs = new ArrayList<>();
        final Set<UUID> activeMobIds = new HashSet<>();
        BoundingBox exitPortalRegion;
        DungeonManager.Instance trackedInstance;
        int averageGearScore;
        int highestAverageGearScore;
        int lootTier;
        long startTime;
        double damageTaken;
        boolean bossDefeated;
        boolean puzzleComplete;
        boolean ready;
    }

    public record ExpeditionRoute(Location spawn, List<Location> path) {
    }

    private record NpcPlacement(int x, int y, int z, int id) { }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY;
    }

    @Override
    public void register(DungeonManager manager) {
        DungeonLayout layout = new DungeonLayout();
        layout.setStep(0);
        manager.registerVerifiedLayout(KEY, DISPLAY, 5, layout);
    }

    private RoomTemplate getTemplate() {
        if (cachedTemplate != null) {
            return cachedTemplate;
        }
        if (templateFuture != null) {
            if (templateFuture.isDone()) {
                try {
                    cachedTemplate = templateFuture.get();
                } catch (Exception ex) {
                    plugin.getLogger().warning("[Dungeon] Failed to load Crimson Reliquary template: " + ex.getMessage());
                    templateFuture = null;
                }
                return cachedTemplate;
            }
            return null;
        }
        plugin.getWorldManager().ensureWorldsLoaded(SOURCE_WORLD);
        World source = Bukkit.getWorld(SOURCE_WORLD);
        if (source == null) {
            return null;
        }
        templateFuture = loadTemplateAsync(source);
        return null;
    }

    private java.util.concurrent.CompletableFuture<RoomTemplate> loadTemplateAsync(World source) {
        int minChunkX = Math.floorDiv(Math.min(MIN_X, MAX_X), 16);
        int maxChunkX = Math.floorDiv(Math.max(MIN_X, MAX_X), 16);
        int minChunkZ = Math.floorDiv(Math.min(MIN_Z, MAX_Z), 16);
        int maxChunkZ = Math.floorDiv(Math.max(MIN_Z, MAX_Z), 16);

        List<java.util.concurrent.CompletableFuture<org.bukkit.Chunk>> futures = new ArrayList<>();
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                futures.add(source.getChunkAtAsync(cx, cz, true));
            }
        }

        java.util.concurrent.CompletableFuture<RoomTemplate> future = new java.util.concurrent.CompletableFuture<>();
        java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0]))
                .whenComplete((ignored, err) -> {
                    if (err != null) {
                        future.completeExceptionally(err);
                        return;
                    }
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            future.complete(RoomTemplate.capture(
                                    source, MIN_X, MIN_Y, MIN_Z, MAX_X, MAX_Y, MAX_Z, false));
                        } catch (Exception ex) {
                            future.completeExceptionally(ex);
                        }
                    });
                });
        return future;
    }

    @Override
    public void startInstance(DungeonManager manager, Player player) {
        RoomTemplate template = getTemplate();
        if (template == null) {
            String message = templateFuture != null
                    ? "Crimson Reliquary is still loading. Please try again in a moment."
                    : "Verified dungeon template world is missing (flatland).";
            player.sendMessage(Component.text(message, NamedTextColor.RED));
            return;
        }

        String worldName = "dgn_verified_" + System.currentTimeMillis();
        World world = manager.createVoidWorld(worldName, org.bukkit.Difficulty.HARD);
        if (world == null) return;

        Dungeon dungeon = new Dungeon(world, KEY);
        DungeonManager.Instance inst = manager.createTrackedInstance(dungeon, KEY, world);

        List<Player> participants = new ArrayList<>();
        me.nakilex.levelplugin.party.PartyManager pm = plugin.getPartyManager();
        me.nakilex.levelplugin.party.Party party = pm.getParty(player.getUniqueId());
        if (party != null) {
            for (UUID id : party.getMembers()) {
                Player mem = Bukkit.getPlayer(id);
                if (mem != null && mem.isOnline()) {
                    participants.add(mem);
                    inst.addReturnLocation(id, mem.getLocation());
                }
            }
        } else {
            participants.add(player);
            inst.addReturnLocation(player.getUniqueId(), player.getLocation());
        }

        for (Player p : participants) ChatMessageUtil.send(p, MessageType.INFO, "Preparing Crimson Reliquary instance. Please wait...");

        int width = MAX_X - MIN_X + 1;
        int height = MAX_Y - MIN_Y + 1;
        int depth = MAX_Z - MIN_Z + 1;
        int offsetX = 0;
        int offsetZ = 0;
        int offsetY = 64 - MIN_Y;
        Location origin = new Location(world, offsetX, offsetY, offsetZ);

        int minX = offsetX;
        int minY = offsetY;
        int minZ = offsetZ;
        int maxX = offsetX + width - 1;
        int maxY = offsetY + height - 1;
        int maxZ = offsetZ + depth - 1;
        Location spawn = origin.clone().add(TEMPLATE_SPAWN.getX() - MIN_X + 0.5, TEMPLATE_SPAWN.getY() - MIN_Y, TEMPLATE_SPAWN.getZ() - MIN_Z + 0.5);
        Dungeon.RoomInstance bounds = new Dungeon.RoomInstance(null, 0, spawn, minX, minY, minZ, maxX, maxY, maxZ, null, List.of(), null);
        dungeon.addRoom(bounds);

        world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
        world.setSpawnLocation(spawn);
        inst.setSpawnLocation(spawn);

        TemplateMarkers markers = new TemplateMarkers();
        Map<Player, PlayerState> prevStates = captureStates(participants);

        seedMarkersFromTemplate(template, origin, markers);
        plugin.getLogger().info("[Dungeon] Seeded template markers: normal=" + markers.normalMarkers.size()
                + " boss=" + markers.bossMarkers.size());
        pasteTemplateAsync(template, world, origin, spawn, markers,
                () -> teleportParticipantsEarly(participants, prevStates, spawn),
                () -> finalizeInstance(manager, inst, origin, spawn, participants, markers, prevStates));
    }

    public java.util.Optional<ExpeditionRoute> getExpeditionRoute(World world) {
        InstanceState state = activeInstances.get(world);
        if (state == null || !state.ready || state.spawnLocation == null || state.expeditionPath.isEmpty()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new ExpeditionRoute(state.spawnLocation.clone(), List.copyOf(state.expeditionPath)));
    }

    private void seedMarkersFromTemplate(RoomTemplate template, Location origin, TemplateMarkers markers) {
        if (template == null) return;

        for (RoomTemplate.Marker portal : template.getPortals()) {
            Location loc = origin.clone().add(portal.x, portal.y, portal.z).add(0.5, 0, 0.5);
            addMarkerIfAbsent(markers.normalMarkers, loc);
        }

        RoomTemplate.Marker boss = template.getBossSpawn();
        if (boss != null) {
            Location loc = origin.clone().add(boss.x, boss.y, boss.z).add(0.5, 0, 0.5);
            addMarkerIfAbsent(markers.bossMarkers, loc);
        }
    }

    private void logMarkerSummary(TemplateMarkers markers) {
        plugin.getLogger().info("[Dungeon] Parsed markers - boss: " + markers.bossMarkers.size()
                + " miniboss: " + markers.miniBossMarkers.size()
                + " normal: " + markers.normalMarkers.size()
                + " flowers: " + markers.yellowFlowers.size()
                + " placements: " + markers.bluePlacements.size()
                + " chests: " + markers.chestMarkers.size());
        if (markers.normalMarkers.isEmpty()) {
            plugin.getLogger().warning("[Dungeon] No magenta wool markers found for normal mobs.");
        }
        if (markers.bossMarkers.isEmpty()) {
            plugin.getLogger().warning("[Dungeon] No black wool markers found for boss spawn.");
        }
    }

    private void addMarkerIfAbsent(List<Location> targets, Location loc) {
        boolean exists = targets.stream().anyMatch(existing ->
                existing.getBlockX() == loc.getBlockX()
                        && existing.getBlockY() == loc.getBlockY()
                        && existing.getBlockZ() == loc.getBlockZ());
        if (!exists) {
            targets.add(loc);
        }
    }

    private void handleTemplateBlock(World dest, Location destLoc, BlockData data, TemplateMarkers markers) {
        Material mat = data.getMaterial();
        switch (mat) {
            case BLACK_WOOL -> {
                addMarkerIfAbsent(markers.bossMarkers, destLoc.clone().add(0.5, 0, 0.5));
                dest.getBlockAt(destLoc).setType(Material.AIR, false);
            }
            case CYAN_WOOL -> {
                markers.miniBossMarkers.add(destLoc.clone().add(0.5, 0, 0.5));
                dest.getBlockAt(destLoc).setType(Material.AIR, false);
            }
            case MAGENTA_WOOL, PURPLE_WOOL -> {
                addMarkerIfAbsent(markers.normalMarkers, destLoc.clone().add(0.5, 0, 0.5));
                dest.getBlockAt(destLoc).setType(Material.AIR, false);
            }
            case YELLOW_WOOL -> {
                markers.yellowFlowers.add(destLoc);
                dest.getBlockAt(destLoc).setType(Material.AIR, false);
            }
            case BLUE_WOOL -> {
                markers.bluePlacements.add(destLoc);
                dest.getBlockAt(destLoc).setType(Material.AIR, false);
            }
            case BROWN_WOOL -> {
                markers.brownRewards.add(destLoc);
                dest.getBlockAt(destLoc).setType(Material.AIR, false);
            }
            default -> dest.getBlockAt(destLoc).setBlockData(data, false);
        }
    }

    private void pasteTemplateAsync(RoomTemplate template, World dest, Location origin, Location spawn, TemplateMarkers markers, Runnable onSpawnReady, Runnable done) {
        for (RoomTemplate.ChestMarker chest : template.getChests()) {
            Location loc = origin.clone().add(chest.x, chest.y, chest.z);
            markers.chestMarkers.add(new TemplateChest(loc, chest.data));
        }

        List<RoomTemplate.BlockDef> blocks = template.getBlocks();
        Map<Long, List<RoomTemplate.BlockDef>> byChunk = blocks.stream()
                .collect(Collectors.groupingBy(b -> chunkKey(origin, b)));

        long spawnChunk = chunkKey(spawn);
        List<RoomTemplate.BlockDef> spawnBlocks = byChunk.remove(spawnChunk);
        if (spawnBlocks != null) {
            for (RoomTemplate.BlockDef b : spawnBlocks) {
                Location destLoc = origin.clone().add(b.x, b.y, b.z);
                handleTemplateBlock(dest, destLoc, b.data, markers);
            }
        }
        onSpawnReady.run();

        final int chunksPerTick = 12;
        new BukkitRunnable() {
            final List<Map.Entry<Long, List<RoomTemplate.BlockDef>>> chunkEntries;
            int idx = 0;

            {
                List<Map.Entry<Long, List<RoomTemplate.BlockDef>>> ordered = new ArrayList<>(byChunk.entrySet());
                ordered.sort(java.util.Map.Entry.comparingByKey());
                this.chunkEntries = ordered;
            }

            @Override
            public void run() {
                int processedChunks = 0;
                while (idx < chunkEntries.size() && processedChunks < chunksPerTick) {
                    Map.Entry<Long, List<RoomTemplate.BlockDef>> entry = chunkEntries.get(idx++);
                    for (RoomTemplate.BlockDef b : entry.getValue()) {
                        Location destLoc = origin.clone().add(b.x, b.y, b.z);
                        handleTemplateBlock(dest, destLoc, b.data, markers);
                    }
                    processedChunks++;
                }
                if (idx >= chunkEntries.size()) {
                    cancel();
                    done.run();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private long chunkKey(Location origin, RoomTemplate.BlockDef b) {
        int cx = (origin.getBlockX() + b.x) >> 4;
        int cz = (origin.getBlockZ() + b.z) >> 4;
        return (((long) cx) << 32) ^ (cz & 0xffffffffL);
    }

    private long chunkKey(Location loc) {
        return (((long) loc.getBlockX() >> 4) << 32) ^ ((loc.getBlockZ() >> 4) & 0xffffffffL);
    }

    private BoundingBox createExitPortalBounds(Location origin) {
        double worldX = origin.getX() + (-222 - MIN_X);
        double minY = origin.getY() + (-35 - MIN_Y);
        double maxY = origin.getY() + (-31 - MIN_Y) + 1;
        double minZ = origin.getZ() + (-5803 - MIN_Z);
        double maxZ = origin.getZ() + (-5798 - MIN_Z) + 1;
        return BoundingBox.of(new Location(origin.getWorld(), worldX, minY, minZ),
                new Location(origin.getWorld(), worldX + 1, maxY, maxZ));
    }

    private Map<Player, PlayerState> captureStates(List<Player> participants) {
        Map<Player, PlayerState> states = new HashMap<>();
        for (Player p : participants) {
            states.put(p, new PlayerState(p));
        }
        return states;
    }

    private int calculateAverageGearScore(List<Player> participants) {
        if (participants == null || participants.isEmpty()) {
            return 0;
        }
        int total = 0;
        int counted = 0;
        for (Player p : participants) {
            if (p != null && p.isOnline()) {
                total += ItemUtil.calculateTotalGearScore(p);
                counted++;
            }
        }
        return counted == 0 ? 0 : total / counted;
    }

    private int updatePeakAverageGearScore(InstanceState state) {
        if (state == null) return 0;
        int current = calculateAverageGearScore(state.participants);
        if (current > 0) {
            state.averageGearScore = current;
            state.highestAverageGearScore = Math.max(state.highestAverageGearScore, current);
        }
        return Math.max(state.highestAverageGearScore, state.averageGearScore);
    }

    private int calculateAveragePlayerLevel(List<Player> participants) {
        if (participants == null || participants.isEmpty()) {
            return 0;
        }
        int total = 0;
        int counted = 0;
        for (Player p : participants) {
            if (p != null && p.isOnline()) {
                total += LevelManager.getInstance().getLevel(p);
                counted++;
            }
        }
        return counted == 0 ? 0 : total / counted;
    }

    private int determineLootTier(DungeonManager manager) {
        return Math.min(8, Math.max(1, manager.getThreatLevel(KEY)));
    }

    private void teleportParticipantsEarly(List<Player> participants, Map<Player, PlayerState> prevStates, Location spawn) {
        for (Player p : participants) {
            if (p == null || !p.isOnline()) continue;
            p.setAllowFlight(true);
            p.setFlying(true);
            p.setInvulnerable(true);
            p.teleport(spawn);
            ChatMessageUtil.send(p, MessageType.INFO, "Instance spawn chunk ready. Remaining rooms are rendering...");
        }
    }

    private void finalizeInstance(DungeonManager manager,
                                  DungeonManager.Instance inst,
                                  Location origin,
                                  Location spawn,
                                  List<Player> participants,
                                  TemplateMarkers markers,
                                  Map<Player, PlayerState> prevStates) {
        InstanceState state = new InstanceState();
        state.participants.addAll(participants);
        state.averageGearScore = calculateAverageGearScore(participants);
        state.highestAverageGearScore = state.averageGearScore;
        state.lootTier = determineLootTier(manager);
        state.startTime = System.currentTimeMillis();
        state.exitPortalRegion = createExitPortalBounds(origin);
        state.bossPortalLocation = markers.bossMarkers.isEmpty() ? null : markers.bossMarkers.get(0);
        state.trackedInstance = inst;
        state.spawnLocation = spawn.clone();
        state.expeditionPath.addAll(buildExpeditionPath(spawn, markers));
        activeInstances.put(origin.getWorld(), state);
        manager.startRun(participants.stream().map(Player::getUniqueId).toList(), KEY, state.startTime);

        List<Location> flowerSpots = new ArrayList<>(markers.yellowFlowers);
        Collections.shuffle(flowerSpots);
        List<FlowerType> flowers = new ArrayList<>(Arrays.asList(FlowerType.values()));
        Collections.shuffle(flowers);
        for (int i = 0; i < flowerSpots.size(); i++) {
            Location yellow = flowerSpots.get(i);
            FlowerType choice = flowers.get(i % flowers.size());
            yellow.getBlock().setType(choice.block, false);
            state.pluckable.put(yellow, choice);
            MultiLineHologram holo = new MultiLineHologram(yellow.clone().add(0.5, 1.25, 0.5), "crimson_flower_pluck");
            holo.spawn(List.of(
                    legacy.serialize(Component.text(choice.display, NamedTextColor.GOLD)),
                    legacy.serialize(Component.text("Right-click to pluck", NamedTextColor.GRAY))));
            state.pluckHolograms.put(yellow, holo);
        }
        Map<FlowerType, Long> flowerCounts = state.pluckable.values().stream()
                .collect(Collectors.groupingBy(ft -> ft, Collectors.counting()));
        plugin.getLogger().info("[Dungeon] Flower placements prepared: total=" + state.pluckable.size()
                + " distinct=" + flowerCounts.size() + " " + flowerCounts);

        List<Location> placementSpots = new ArrayList<>(markers.bluePlacements);
        Collections.shuffle(placementSpots);
        int placementCount = Math.min(4, placementSpots.size());
        for (int i = 0; i < placementCount; i++) {
            Location marker = placementSpots.get(i);
            setupPlacementSlot(marker, state);
        }

        for (Location brown : markers.brownRewards) {
            brown.getBlock().setType(Material.AIR, false);
            state.rewardFountains.add(brown);
        }

        int tier = state.lootTier <= 0 ? Math.max(1, manager.getThreatLevel(KEY)) : state.lootTier;
        me.nakilex.levelplugin.lootchests.managers.LootChestManager lootManager = manager.getLootChestManager();
        if (lootManager != null) {
            for (TemplateChest chest : markers.chestMarkers) {
                chest.loc().getBlock().setType(Material.AIR, false);
                int id = lootManager.createAndSpawnChest(chest.loc(), getFacingFromData(chest.data()));
                inst.addChestId(id);
            }
        }

        logMarkerSummary(markers);
        queueMobs(state, markers);
        startMobSpawner(state);

        spawnInstanceNpcs(origin, state);

        for (int y = -35; y <= -31; y++) {
            for (int z = -5803; z <= -5798; z++) {
                Location dest = origin.clone().add(-222 - MIN_X, y - MIN_Y, z - MIN_Z);
                dest.getBlock().setType(Material.NETHER_PORTAL, false);
            }
        }

        for (Player p : participants) {
            PlayerState st = prevStates.get(p);
            if (st != null && p.isOnline()) {
                p.setInvulnerable(st.invulnerable);
                p.setAllowFlight(st.allowFlight);
                p.setFlying(st.allowFlight && st.flying);
                manager.disableInstanceFlight(p);
                ChatMessageUtil.send(p, MessageType.SUCCESS, "Crimson Reliquary is ready.");
            }
        }
        state.ready = true;
    }

    private List<Location> buildExpeditionPath(Location spawn, TemplateMarkers markers) {
        List<Location> path = new ArrayList<>();
        if (spawn != null) {
            path.add(spawn.clone());
        }
        if (!markers.bossMarkers.isEmpty()) {
            Location boss = markers.bossMarkers.get(0).clone();
            path.add(boss);
        }
        List<Location> targets = new ArrayList<>();
        targets.addAll(markers.normalMarkers);
        targets.addAll(markers.miniBossMarkers);
        Location current = path.size() > 1 ? path.get(1) : spawn;
        while (!targets.isEmpty()) {
            Location next = targets.get(0);
            if (current != null) {
                double best = current.distanceSquared(next);
                for (Location candidate : targets) {
                    double dist = current.distanceSquared(candidate);
                    if (dist < best) {
                        best = dist;
                        next = candidate;
                    }
                }
            }
            targets.remove(next);
            Location nextPoint = next.clone();
            path.add(nextPoint);
            current = nextPoint;
        }
        return path;
    }

    private ItemStack createFlowerItem(FlowerType type) {
        ItemStack stack = new ItemStack(type.block);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(type.display, NamedTextColor.AQUA));
            meta.lore(TooltipUtil.dungeonItemLore(type.description, true).stream()
                    .map(legacy::deserialize)
                    .collect(Collectors.toList()));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(ItemUtil.SOULBOUND_KEY, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(ItemUtil.DUNGEON_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(DUNGEON_FLOWER_KEY, PersistentDataType.STRING, type.name());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private void attemptPlaceFlower(Player player, InstanceState state, Location base, org.bukkit.block.Block clickedBlock) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!isDungeonFlower(held)) {
            ChatMessageUtil.send(player, MessageType.ERROR, "Hold a dungeon flower to place it.");
            return;
        }
        FlowerType type = getFlowerType(held);
        if (type == null) {
            ChatMessageUtil.send(player, MessageType.ERROR, "That flower has faded.");
            return;
        }
        if (!state.placements.containsKey(base)) {
            return;
        }
        int required = Math.min(4, state.placements.size());
        if (required == 0) return;

        if (clickedBlock != null) {
            clickedBlock.setType(Material.AIR, false);
        }
        base.getBlock().setType(type.block, false);

        held.setAmount(held.getAmount() - 1);
        state.placements.put(base, type);
        updatePlacementHologram(base, state, true);
        ChatMessageUtil.send(player, MessageType.SUCCESS, "Flower placed.");
        base.getWorld().spawnParticle(Particle.END_ROD, base.clone().add(0.5, 1.1, 0.5), 16, 0.2, 0.35, 0.2, 0.01);
        base.getWorld().playSound(base, Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.4f);
        long filled = state.placements.values().stream().filter(java.util.Objects::nonNull).count();
        long distinct = state.placements.values().stream().filter(java.util.Objects::nonNull).map(ft -> ft.block).distinct().count();
        if (filled >= required && distinct >= Math.min(required, FlowerType.values().length)) {
            completePuzzle(state);
        }
    }

    private boolean tryRemovePlacedFlower(Player player, InstanceState state, Location base) {
        if (player == null || state == null || base == null) {
            return false;
        }
        FlowerType placed = state.placements.get(base);
        if (placed == null) {
            return false;
        }
        base.getBlock().setType(Material.AIR, false);
        ItemStack reward = createFlowerItem(placed);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(reward);
        overflow.values().forEach(item -> base.getWorld().dropItemNaturally(player.getLocation(), item));
        state.placements.put(base, null);
        updatePlacementHologram(base, state, false);
        ChatMessageUtil.send(player, MessageType.INFO, "You retrieve the placed flower.");
        return true;
    }

    private void setupPlacementSlot(Location marker, InstanceState state) {
        if (marker == null || state == null) {
            return;
        }
        marker.getBlock().setType(Material.AIR, false);
        ensurePlacementMarker(marker);
        updatePlacementHologram(marker, state, false);
        state.placements.put(marker, null);
    }

    private void ensurePlacementMarker(Location marker) {
        if (marker == null || marker.getWorld() == null) {
            return;
        }
        boolean hasMarker = marker.getWorld().getNearbyEntities(marker.clone().add(0.5, 0.1, 0.5), 0.4, 0.8, 0.4, ent ->
                ent instanceof ArmorStand && ent.getScoreboardTags().contains("dungeon_flower_slot"))
                .stream()
                .findFirst()
                .isPresent();
        if (hasMarker) {
            return;
        }
        marker.getWorld().spawn(marker.clone().add(0.5, 0.1, 0.5), ArmorStand.class, as -> {
            as.setVisible(false);
            as.setGravity(false);
            as.setMarker(false);
            as.addScoreboardTag("dungeon_flower_slot");
        });
    }

    private void updatePlacementHologram(Location marker, InstanceState state, boolean placed) {
        if (marker == null || state == null) {
            return;
        }
        MultiLineHologram holo = state.placementHolograms.get(marker);
        if (holo == null) {
            holo = new MultiLineHologram(marker.clone().add(0.5, 1.2, 0.5), "crimson_flower_slot");
            state.placementHolograms.put(marker, holo);
        }
        List<String> lines = placed
                ? List.of(
                    legacy.serialize(Component.text("Flower Placed", NamedTextColor.AQUA)),
                    legacy.serialize(Component.text("Right-click to remove the flower", NamedTextColor.GRAY)))
                : List.of(
                    legacy.serialize(Component.text("Place Flower", NamedTextColor.AQUA)),
                    legacy.serialize(Component.text("Right-click with a flower", NamedTextColor.GRAY)));
        holo.setLines(lines);
    }

    private org.bukkit.block.BlockFace getFacingFromData(BlockData data) {
        if (data instanceof org.bukkit.block.data.Directional dir) {
            return dir.getFacing();
        }
        return org.bukkit.block.BlockFace.NORTH;
    }

    private void spawnInstanceNpcs(Location origin, InstanceState state) {
        for (NpcPlacement placement : NPCS) {
            NPC template = CitizensAPI.getNPCRegistry().getById(placement.id());
            if (template == null) {
                plugin.getLogger().warning("[Dungeon] Missing NPC template id=" + placement.id());
                continue;
            }
            NPC clone = template.copy();
            Location loc = origin.clone().add(placement.x() - MIN_X + 0.5, placement.y() - MIN_Y, placement.z() - MIN_Z + 0.5);
            clone.getOrAddTrait(CurrentLocation.class).setLocation(loc);
            clone.spawn(loc);
            if (clone.isSpawned()) {
                clone.getEntity().teleport(loc, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
                clone.getEntity().setGravity(false);
            }
            state.npcs.add(clone);
        }
    }

    private void queueMobs(InstanceState state, TemplateMarkers markers) {
        String[] mobs = new String[]{
                "Nocsy_Bokoblin_Shaman",
                "Nocsy_Bokoblin_Swordsman",
                "Nocsy_Bokoblin_Warrior"
        };
        plugin.getLogger().info("[Dungeon] queuing mob markers: normal=" + markers.normalMarkers.size()
                + " miniboss=" + markers.miniBossMarkers.size() + " boss=" + markers.bossMarkers.size());
        for (Location magenta : markers.normalMarkers) {
            for (int i = 0; i < 5; i++) {
                String mob = mobs[ThreadLocalRandom.current().nextInt(mobs.length)];
                Location spread = magenta.clone().add(ThreadLocalRandom.current().nextDouble(-0.7, 0.7), 0,
                        ThreadLocalRandom.current().nextDouble(-0.7, 0.7));
                state.mobMarkers.add(new MobMarker(spread, mob));
            }
        }
        for (Location cyan : markers.miniBossMarkers) {
            state.mobMarkers.add(new MobMarker(cyan, "LRD_eldric"));
        }
        for (Location boss : markers.bossMarkers) {
            state.mobMarkers.add(new MobMarker(boss, "MSO_Demon_General"));
        }
    }

    private void startMobSpawner(InstanceState state) {
        if (state.mobMarkers.isEmpty()) {
            plugin.getLogger().info("[Dungeon] No mob markers queued; skipping spawner.");
            return;
        }

        Queue<MobMarker> queue = new ArrayDeque<>(state.mobMarkers);
        final int attemptsBeforeGiveUp = 12;
        final int mobsPerTick = 10;

        World mobWorld = queue.peek() != null ? queue.peek().loc.getWorld() : null;
        if (mobWorld != null) {
            Boolean naturalSpawns = mobWorld.getGameRuleValue(org.bukkit.GameRule.DO_MOB_SPAWNING);
            plugin.getLogger().info("[Dungeon] Mob spawner initializing in " + mobWorld.getName()
                    + " difficulty=" + mobWorld.getDifficulty()
                    + " gamerule.DO_MOB_SPAWNING=" + naturalSpawns);
        }

        // Make sure all target chunks stay loaded while we attempt spawns.
        for (MobMarker marker : queue) {
            org.bukkit.Chunk chunk = marker.loc.getChunk();
            if (!chunk.isLoaded()) {
                chunk.load(true);
            }
            chunk.setForceLoaded(true);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (state.participants.stream().noneMatch(p -> p != null && p.isOnline())) {
                    cancel();
                    return;
                }
                int spawnedThisTick = 0;
                Iterator<MobMarker> it = queue.iterator();
                while (it.hasNext() && spawnedThisTick < mobsPerTick) {
                    MobMarker marker = it.next();
                    if (marker.spawned) {
                        it.remove();
                        continue;
                    }
                    org.bukkit.Chunk chunk = marker.loc.getChunk();
                    if (!chunk.isLoaded()) {
                        chunk.load(true);
                        plugin.getLogger().info("[Dungeon] Loading chunk " + chunk.getX() + "," + chunk.getZ() + " for mob " + marker.mobId);
                        continue;
                    }

                    marker.ticksWaited++;
                    var mob = MythicMobModifier.spawnModifiedMob(marker.mobId, marker.loc, null, null, null, null);
                    if (mob != null) {
                        var entity = mob.getEntity().getBukkitEntity();
                        entity.addScoreboardTag(DUNGEON_MOB_TAG);
                        if (entity instanceof org.bukkit.entity.LivingEntity living) {
                            living.setRemoveWhenFarAway(false);
                            living.setPersistent(true);
                        }
                        if (marker.mobId.equals("MSO_Demon_General")) {
                            entity.addScoreboardTag("dungeon_boss");
                        }
                        state.activeMobIds.add(entity.getUniqueId());
                        marker.spawned = true;
                        it.remove();
                        plugin.getLogger().info("[Dungeon] Spawned mob " + marker.mobId + " at " + marker.loc + " after " + marker.ticksWaited + " ticks waiting.");
                    } else {
                        if (marker.ticksWaited == 1 || marker.ticksWaited % 5 == 0) {
                            plugin.getLogger().warning("[Dungeon] Failed to spawn mob " + marker.mobId + " (attempt " + marker.ticksWaited + ") at " + marker.loc
                                    + " chunkLoaded=" + chunk.isLoaded()
                                    + " forceLoaded=" + chunk.isForceLoaded()
                                    + " difficulty=" + chunk.getWorld().getDifficulty());
                        }
                        if (marker.ticksWaited >= attemptsBeforeGiveUp) {
                            it.remove();
                            plugin.getLogger().warning("[Dungeon] Giving up on mob " + marker.mobId + " at " + marker.loc
                                    + " chunkLoaded=" + chunk.isLoaded()
                                    + " forceLoaded=" + chunk.isForceLoaded());
                        }
                    }

                    spawnedThisTick++;
                }

                if (queue.isEmpty()) {
                    cancel();
                    plugin.getLogger().info("[Dungeon] Mob spawner finished processing all markers.");
                }
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }

    private boolean isDungeonFlower(ItemStack stack) {
        if (stack == null) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        String type = meta.getPersistentDataContainer().get(DUNGEON_FLOWER_KEY, PersistentDataType.STRING);
        return type != null;
    }

    private FlowerType getFlowerType(ItemStack stack) {
        if (stack == null) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;
        String type = meta.getPersistentDataContainer().get(DUNGEON_FLOWER_KEY, PersistentDataType.STRING);
        if (type == null) return null;
        try {
            return FlowerType.valueOf(type);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void completePuzzle(InstanceState state) {
        if (state.puzzleComplete) return;
        state.puzzleComplete = true;
        plugin.getDungeonManager().markPuzzleComplete(state.participants.stream()
                .filter(java.util.Objects::nonNull)
                .map(Player::getUniqueId)
                .toList());
        int coinsAward = ThreadLocalRandom.current().nextInt(300, 601);
        int gemsAward = ThreadLocalRandom.current().nextInt(3, 8);
        for (Player p : state.participants) {
            if (p != null && p.isOnline()) {
                plugin.getEconomyManager().addCoins(p, coinsAward);
                plugin.getGemsManager().addUnits(p, gemsAward);
                ChatMessageUtil.send(p, MessageType.REWARD,
                        "Puzzle complete! "
                                + CurrencyMessageUtil.formatAmount(CurrencyMessageUtil.Currency.COINS, coinsAward)
                                + " & "
                                + CurrencyMessageUtil.formatAmount(CurrencyMessageUtil.Currency.GEMS, gemsAward));
            }
        }
        for (Location fountain : state.rewardFountains) {
            for (Player p : state.participants) {
                if (p != null && p.isOnline()) {
                    RewardBombUtil.startRewardBomb(plugin, fountain, () -> createFountainReward(state), 100, p);
                }
            }
        }
    }

    private ItemStack createFountainReward(InstanceState state) {
        me.nakilex.levelplugin.lootchests.managers.LootChestManager lootManager = plugin.getDungeonManager().getLootChestManager();
        int peakGearScore = Math.max(updatePeakAverageGearScore(state), state.lootTier * 40);
        int averageLevel = calculateAveragePlayerLevel(state.participants);
        int tier = state.lootTier <= 0 ? 1 : state.lootTier;

        if (lootManager != null) {
            Integer levelRequirement = averageLevel > 0 ? averageLevel : null;
            ItemStack scaledLoot = lootManager.getRandomLootForCombatPower(
                    Math.max(50, peakGearScore),
                    levelRequirement,
                    "dungeon",
                    null,
                    false);
            if (scaledLoot != null) {
                return scaledLoot;
            }
        }

        int baseLevel = averageLevel > 0 ? averageLevel : (tier * 8);
        int minLevel = Math.max(1, baseLevel - 2);
        int maxLevel = Math.max(minLevel, baseLevel + 4);
        int roll = ThreadLocalRandom.current().nextInt(3);
        return switch (roll) {
            case 0 -> {
                var generator = plugin.getItemManager();
                int level = ThreadLocalRandom.current().nextInt(minLevel, maxLevel + 1);
                var generated = generator.generateItem("dungeon", level);
                yield ItemUtil.createItemStackFromCustomItem(generated, 1, null);
            }
            case 1 -> {
                var gifts = plugin.getMercenaryAffinityManager().getGifts();
                ItemStack gift = null;
                if (!gifts.isEmpty()) {
                    int idx = ThreadLocalRandom.current().nextInt(gifts.size());
                    gift = gifts.stream().skip(idx).findFirst().map(me.nakilex.levelplugin.mercenary.MercenaryGift::getIcon).orElse(null);
                }
                yield gift == null ? new ItemStack(Material.PRISMARINE_CRYSTALS) : gift;
            }
            default -> {
                var templates = plugin.getPotionManager().getAllTemplates();
                if (!templates.isEmpty()) {
                    int idx = ThreadLocalRandom.current().nextInt(templates.size());
                    var template = templates.stream().skip(idx).findFirst().orElse(null);
                    if (template != null) {
                        var inst = plugin.getPotionManager().createInstance(template);
                        yield inst.toItemStack(plugin);
                    }
                }
                yield new ItemStack(Material.HONEY_BOTTLE);
            }
        };
    }

    private void removeDungeonItems(Player player) {
        if (player == null) return;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isDungeonFlower(stack)) {
                player.getInventory().clear(i);
            }
        }
    }

    private void spawnBossExitPortal(InstanceState state) {
        if (state == null || state.bossPortalLocation == null) return;
        Location target = findBossPortalLocation(state.bossPortalLocation, 10);
        if (target == null) {
            plugin.getLogger().warning("[Dungeon] Unable to find clear space for boss exit portal.");
            return;
        }
        if (!target.getChunk().isLoaded()) {
            target.getChunk().load();
        }
        clearPortalEntities(target);
        target.getBlock().setType(Material.AIR, false);
        FurnitureMechanic existing = NexoFurniture.furnitureMechanic(target.getBlock());
        if (existing != null && "portal_decoration_animated_v1_portal_5".equalsIgnoreCase(existing.getItemID())) {
            return;
        }
        FurnitureMechanic mechanic = NexoFurniture.furnitureMechanic("portal_decoration_animated_v1_portal_5");
        if (mechanic == null) {
            plugin.getLogger().warning("[Dungeon] Unable to spawn boss exit portal furniture (missing mechanic)");
            return;
        }
        NexoFurniture.place(mechanic.getItemID(), target, 0f, BlockFace.NORTH);
    }

    private Location findBossPortalLocation(Location base, int radius) {
        if (base == null || base.getWorld() == null) {
            return null;
        }
        for (int r = 0; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) {
                        continue;
                    }
                    Location candidate = base.clone().add(dx, 0, dz);
                    if (isPortalSpotClear(candidate)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private boolean isPortalSpotClear(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return false;
        }
        return loc.getBlock().isPassable() && loc.clone().add(0, 1, 0).getBlock().isPassable();
    }

    private void clearPortalEntities(Location loc) {
        loc.getWorld().getNearbyEntities(loc, 0.6, 1.4, 0.6).forEach(entity -> {
            if (entity instanceof ArmorStand || entity instanceof TextDisplay) {
                entity.remove();
            }
        });
    }

    private class InteractionListener implements Listener {
        @EventHandler(ignoreCancelled = true)
        public void onInteract(PlayerInteractEvent event) {
            if (event.getHand() == EquipmentSlot.OFF_HAND) return;
            if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
            if (event.getClickedBlock() == null) return;
            World world = event.getClickedBlock().getWorld();
            InstanceState state = activeInstances.get(world);
            if (state == null) return;
            Location loc = event.getClickedBlock().getLocation();
            FlowerType type = state.pluckable.get(loc);
            if (type == null) {
                if (state.placements.containsKey(loc)) {
                    event.setCancelled(true);
                    if (tryRemovePlacedFlower(event.getPlayer(), state, loc)) {
                        return;
                    }
                    attemptPlaceFlower(event.getPlayer(), state, loc, event.getClickedBlock());
                }
                return;
            }
            event.setCancelled(true);
            ItemStack reward = createFlowerItem(type);
            Map<Integer, ItemStack> overflow = event.getPlayer().getInventory().addItem(reward);
            overflow.values().forEach(item -> world.dropItemNaturally(event.getPlayer().getLocation(), item));
            event.getClickedBlock().setType(Material.AIR, false);
            MultiLineHologram holo = state.pluckHolograms.remove(loc);
            if (holo != null) holo.despawn();
            state.pluckable.remove(loc);
            ChatMessageUtil.send(event.getPlayer(), MessageType.SUCCESS, "You pluck the flower.");
            world.spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(0.5, 1, 0.5), 12, 0.25, 0.25, 0.25, 0.01);
            world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.2f);

            // If the clicked block is also a placement slot (e.g., flower pot under a hologram), process placement too.
            if (state.placements.containsKey(loc)) {
                if (tryRemovePlacedFlower(event.getPlayer(), state, loc)) {
                    return;
                }
                attemptPlaceFlower(event.getPlayer(), state, loc, event.getClickedBlock());
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onInteractEntity(PlayerInteractAtEntityEvent event) {
            if (event.getHand() == EquipmentSlot.OFF_HAND) return;
            if (!event.getRightClicked().getScoreboardTags().contains("dungeon_flower_slot")) return;
            World world = event.getRightClicked().getWorld();
            InstanceState state = activeInstances.get(world);
            if (state == null) return;
            event.setCancelled(true);
            Location base = event.getRightClicked().getLocation().getBlock().getLocation();
            if (tryRemovePlacedFlower(event.getPlayer(), state, base)) {
                return;
            }
            attemptPlaceFlower(event.getPlayer(), state, base, null);
        }

        @EventHandler(ignoreCancelled = true)
        public void onDamage(EntityDamageEvent event) {
            if (!(event.getEntity() instanceof Player player)) return;
            InstanceState state = activeInstances.get(player.getWorld());
            if (state == null) return;
            state.damageTaken += event.getFinalDamage();
        }

        @EventHandler(ignoreCancelled = true)
        public void onMove(PlayerMoveEvent event) {
            Location to = event.getTo();
            if (to == null) return;
            InstanceState state = activeInstances.get(to.getWorld());
            if (state == null || state.exitPortalRegion == null) return;
            boolean entered = state.exitPortalRegion.contains(to.getX(), to.getY(), to.getZ())
                    && (event.getFrom() == null || !state.exitPortalRegion.contains(event.getFrom().getX(),
                    event.getFrom().getY(), event.getFrom().getZ()));
            if (entered) {
                plugin.getDungeonManager().handleInstanceExit(to.getWorld(), event.getPlayer());
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onDeath(EntityDeathEvent event) {
            InstanceState state = activeInstances.get(event.getEntity().getWorld());
            if (state == null) return;
            state.activeMobIds.remove(event.getEntity().getUniqueId());
            if (!event.getEntity().getScoreboardTags().contains("dungeon_boss")) return;
            if (state.bossDefeated) return;
            state.bossDefeated = true;
            if (state.trackedInstance != null) {
                state.trackedInstance.getDungeon().setBossDefeated(true);
            }
            RewardBombUtil.startRewardBomb(plugin, event.getEntity().getLocation(),
                    createBossRewardBombSupplier(state), 120);
            spawnBossExitPortal(state);
            long durationMs = System.currentTimeMillis() - state.startTime;
            long seconds = Math.max(1, durationMs / 1000);
            double damage = state.damageTaken;
            int baseScore = 1000;
            int timePenalty = (int) Math.min(baseScore, seconds * 4);
            int damagePenalty = (int) Math.round(damage * 3);
            int puzzleBonus = state.puzzleComplete ? 200 : 0;
            int score = Math.max(0, baseScore - timePenalty - damagePenalty + puzzleBonus);
            for (Player p : state.participants) {
                if (p != null && p.isOnline()) {
                    DungeonManager.CompletionXp xp = plugin.getDungeonManager()
                            .awardCompletionRewards(p, KEY, seconds, false);
                    sendDungeonClearMessage(p, seconds, score, state.puzzleComplete, xp);
                    me.nakilex.levelplugin.quests.managers.QuestManager qm = Main.getInstance().getQuestManager();
                    if (qm != null) {
                        qm.handleDungeonComplete(p, KEY);
                    }
                }
            }
        }

        private java.util.function.Supplier<ItemStack> createBossRewardBombSupplier(InstanceState state) {
            boolean[] essenceServed = {false};
            return () -> {
                if (!essenceServed[0]) {
                    essenceServed[0] = true;
                    return ClassEssence.generateStandardPoolEssence(ItemRarity.RARE, 0);
                }
                return createFountainReward(state);
            };
        }

        @EventHandler
        public void onChunkUnload(org.bukkit.event.world.ChunkUnloadEvent event) {
            World world = event.getWorld();
            InstanceState state = activeInstances.get(world);
            if (state == null) return;

            boolean hasDungeonMob = Arrays.stream(event.getChunk().getEntities())
                    .anyMatch(ent -> ent.getScoreboardTags().contains(DUNGEON_MOB_TAG)
                            || state.activeMobIds.contains(ent.getUniqueId()));
            if (hasDungeonMob) {
                org.bukkit.Chunk chunk = event.getChunk();
                boolean ticketed = chunk.addPluginChunkTicket(plugin);
                chunk.setForceLoaded(true);
                plugin.getLogger().info("[Dungeon] Kept chunk " + chunk.getX() + "," + chunk.getZ()
                        + " loaded for active dungeon mobs (ticketAdded=" + ticketed + ").");
            }
        }

        @EventHandler
        public void onTeleport(PlayerTeleportEvent event) {
            if (event.getFrom() == null || event.getTo() == null) return;
            World from = event.getFrom().getWorld();
            World to = event.getTo().getWorld();
            if (from != null && to != from && activeInstances.containsKey(from)) {
                removeDungeonItems(event.getPlayer());
            }
            if (to != null && activeInstances.containsKey(to)) {
                plugin.getDungeonManager().disableInstanceFlight(event.getPlayer());
            }
        }

        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            World world = event.getPlayer().getWorld();
            if (activeInstances.containsKey(world)) {
                removeDungeonItems(event.getPlayer());
            }
        }
    }

    private void sendDungeonClearMessage(Player player, long seconds, int score, boolean puzzleComplete,
                                         DungeonManager.CompletionXp xp) {
        ChatFormatter.constructDivider(player, "§c§l-", 45);
        ChatFormatter.sendCenteredMessage(player, "§c§lCRIMSON RELIQUARY CLEARED");
        ChatFormatter.sendCenteredMessage(player, "");
        ChatFormatter.sendCenteredMessage(player, "");

        String expLabel = ChatFormatter.experienceLabel();
        int totalXp = xp != null ? xp.totalXp() : 0;
        int mobXp = xp != null ? xp.mobXp() : 0;
        int timeBonus = xp != null ? xp.timeBonus() : 0;
        double timeMult = xp != null ? xp.timeMultiplier() : 1.0;
        int puzzleBonus = xp != null ? xp.puzzleBonus() : 0;
        double puzzleMult = xp != null ? xp.puzzleMultiplier() : 1.0;
        int coins = xp != null ? xp.coins() : 0;

        ChatFormatter.sendIndentedMessage(player,
                ChatColor.GRAY + "Run Time: " + ChatColor.WHITE + seconds + ChatColor.GRAY + "s");
        ChatFormatter.sendIndentedMessage(player,
                ChatColor.GRAY + "Score: " + ChatColor.WHITE + score
                        + (puzzleComplete ? ChatColor.GRAY + " (puzzle bonus)" : ""));
        player.sendMessage("");
        ChatFormatter.sendIndentedMessage(player, ChatColor.GRAY + "XP Gained: " + ChatColor.WHITE + totalXp
                + ChatColor.GRAY + " <glyph:experience_orb_icon> " + expLabel);
        ChatFormatter.sendIndentedMessage(player, ChatColor.GRAY + "Mobs: " + ChatColor.WHITE + mobXp
                + ChatColor.GRAY + " <glyph:experience_orb_icon>");
        ChatFormatter.sendIndentedMessage(player, ChatColor.GRAY + "Time cleared: " + formatBonusLine(timeBonus, timeMult)
                + ChatColor.GRAY + " <glyph:experience_orb_icon>");
        ChatFormatter.sendIndentedMessage(player, ChatColor.GRAY + "Puzzle modifier: "
                + formatBonusLine(puzzleBonus, puzzleMult) + ChatColor.GRAY + " <glyph:experience_orb_icon>");
        ChatFormatter.sendIndentedMessage(player, ChatColor.GRAY + "Coins: " + ChatColor.WHITE + coins
                + ChatColor.GRAY + " <glyph:coins_icon>");
        player.sendMessage("");
        ChatFormatter.sendCenteredMessage(player, ChatColor.GRAY + "Great work, challenger.");
        ChatFormatter.constructDivider(player, "§c§l-", 45);
    }

    private String formatBonusLine(int bonus, double multiplier) {
        String mult = new java.text.DecimalFormat("0.00").format(multiplier);
        if (bonus == 0) {
            return ChatColor.GRAY + "x" + ChatColor.WHITE + mult + ChatColor.GRAY + " (+0 XP)";
        }
        String sign = bonus > 0 ? ChatColor.GREEN + "+" : ChatColor.RED + "-";
        return ChatColor.GRAY + "x" + ChatColor.WHITE + mult + ChatColor.GRAY + " (" + sign
                + ChatColor.WHITE + Math.abs(bonus) + ChatColor.GRAY + " XP)";
    }
}
