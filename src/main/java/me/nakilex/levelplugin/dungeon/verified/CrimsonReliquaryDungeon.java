package me.nakilex.levelplugin.dungeon.verified;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dungeon.Dungeon;
import me.nakilex.levelplugin.dungeon.DungeonLayout;
import me.nakilex.levelplugin.dungeon.DungeonManager;
import me.nakilex.levelplugin.dungeon.RoomTemplate;
import me.nakilex.levelplugin.mob.utils.MythicMobModifier;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import me.nakilex.levelplugin.utils.MultiLineHologram;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.items.ItemUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.CurrentLocation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;

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
    private static final List<NpcPlacement> NPCS = List.of(
            new NpcPlacement(-174, -43, -5805, 1420),
            new NpcPlacement(-166, -43, -5794, 658),
            new NpcPlacement(-167, -43, -5806, 671)
    );

    private final Main plugin;
    private final Map<World, InstanceState> activeInstances = new HashMap<>();
    private volatile RoomTemplate cachedTemplate;

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
        final List<Player> participants = new ArrayList<>();
        final List<MobMarker> mobMarkers = new ArrayList<>();
        final List<NPC> npcs = new ArrayList<>();
        long startTime;
        double damageTaken;
        boolean bossDefeated;
        boolean puzzleComplete;
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
        manager.registerVerifiedLayout(KEY, DISPLAY, 10, layout);
    }

    private RoomTemplate getTemplate() {
        if (cachedTemplate != null) {
            return cachedTemplate;
        }
        plugin.getWorldManager().ensureWorldsLoaded(SOURCE_WORLD);
        World source = Bukkit.getWorld(SOURCE_WORLD);
        if (source == null) {
            return null;
        }
        cachedTemplate = RoomTemplate.capture(source, MIN_X, MIN_Y, MIN_Z, MAX_X, MAX_Y, MAX_Z, false);
        return cachedTemplate;
    }

    @Override
    public void startInstance(DungeonManager manager, Player player) {
        RoomTemplate template = getTemplate();
        if (template == null) {
            player.sendMessage(Component.text("Verified dungeon template world is missing (flatland).", NamedTextColor.RED));
            return;
        }

        String worldName = "dgn_verified_" + System.currentTimeMillis();
        World world = manager.createVoidWorld(worldName);
        if (world == null) return;

        Dungeon dungeon = new Dungeon(world, KEY);
        DungeonManager.Instance inst = manager.createTrackedInstance(dungeon, KEY, world);

        List<Player> participants = new ArrayList<>();
        me.nakilex.levelplugin.party.PartyManager pm = plugin.getPartyManager();
        me.nakilex.levelplugin.party.Party party = pm.getParty(player.getUniqueId());
        if (party != null && party.isLeader(player.getUniqueId())) {
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

        world.setDifficulty(org.bukkit.Difficulty.HARD);
        world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
        world.setSpawnLocation(spawn);

        TemplateMarkers markers = new TemplateMarkers();
        Map<Player, PlayerState> prevStates = captureStates(participants);

        pasteTemplateAsync(template, world, origin, spawn, markers,
                () -> teleportParticipantsEarly(participants, prevStates, spawn),
                () -> finalizeInstance(manager, inst, origin, spawn, participants, markers, prevStates));
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

    private void handleTemplateBlock(World dest, Location destLoc, BlockData data, TemplateMarkers markers) {
        Material mat = data.getMaterial();
        switch (mat) {
            case BLACK_WOOL -> {
                markers.bossMarkers.add(destLoc.clone().add(0.5, 0, 0.5));
                dest.getBlockAt(destLoc).setType(Material.AIR, false);
            }
            case CYAN_WOOL -> {
                markers.miniBossMarkers.add(destLoc.clone().add(0.5, 0, 0.5));
                dest.getBlockAt(destLoc).setType(Material.AIR, false);
            }
            case MAGENTA_WOOL -> {
                markers.normalMarkers.add(destLoc.clone().add(0.5, 0, 0.5));
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

    private Map<Player, PlayerState> captureStates(List<Player> participants) {
        Map<Player, PlayerState> states = new HashMap<>();
        for (Player p : participants) {
            states.put(p, new PlayerState(p));
        }
        return states;
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
        state.startTime = System.currentTimeMillis();
        activeInstances.put(origin.getWorld(), state);

        List<Location> flowerSpots = new ArrayList<>(markers.yellowFlowers);
        Collections.shuffle(flowerSpots);
        int pluckableCount = Math.min(5, flowerSpots.size());
        List<FlowerType> flowers = new ArrayList<>(Arrays.asList(FlowerType.values()));
        Collections.shuffle(flowers);
        for (int i = 0; i < flowerSpots.size(); i++) {
            Location yellow = flowerSpots.get(i);
            FlowerType choice = flowers.get(i % flowers.size());
            yellow.getBlock().setType(choice.block, false);
            if (i < pluckableCount) {
                state.pluckable.put(yellow, choice);
                MultiLineHologram holo = new MultiLineHologram(yellow.clone().add(0.5, 1.25, 0.5), "crimson_flower_pluck");
                holo.spawn(List.of(ChatColor.GOLD + "Mystic Bloom", ChatColor.GRAY + "Right-click to pluck"));
                state.pluckHolograms.put(yellow, holo);
            }
        }

        List<Location> placementSpots = new ArrayList<>(markers.bluePlacements);
        Collections.shuffle(placementSpots);
        int placementCount = Math.min(4, placementSpots.size());
        for (int i = 0; i < placementCount; i++) {
            Location marker = placementSpots.get(i);
            marker.getBlock().setType(Material.AIR, false);
            marker.getWorld().spawn(marker.clone().add(0.5, 0.1, 0.5), ArmorStand.class, as -> {
                as.setVisible(false);
                as.setGravity(false);
                as.setMarker(false);
                as.addScoreboardTag("dungeon_flower_slot");
            });
            MultiLineHologram holo = new MultiLineHologram(marker.clone().add(0.5, 1.2, 0.5), "crimson_flower_slot");
            holo.spawn(List.of(ChatColor.AQUA + "Place Flower", ChatColor.GRAY + "Right-click with a bloom"));
            state.placementHolograms.put(marker, holo);
            state.placements.put(marker, null);
        }

        for (Location brown : markers.brownRewards) {
            brown.getBlock().setType(Material.AIR, false);
            state.rewardFountains.add(brown);
        }

        int tier = Math.max(1, manager.getThreatLevel(KEY));
        for (TemplateChest chest : markers.chestMarkers) {
            chest.loc().getBlock().setType(Material.AIR, false);
            int id = manager.getLootChestManager().createAndSpawnChest(chest.loc(), tier, getFacingFromData(chest.data()));
            inst.addChestId(id);
        }

        logMarkerSummary(markers);
        queueMobs(state, markers);
        startMobWatcher(state);

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
                ChatMessageUtil.send(p, MessageType.SUCCESS, "Crimson Reliquary is ready.");
            }
        }
    }

    private ItemStack createFlowerItem(FlowerType type) {
        ItemStack stack = new ItemStack(type.block);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + type.display);
            meta.setLore(TooltipUtil.dungeonItemLore(type.description, true));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(ItemUtil.SOULBOUND_KEY, PersistentDataType.BYTE, (byte) 1);
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

        // Remove any placement armor stands nearby to avoid duplicate holograms lingering.
        base.getWorld().getNearbyEntities(base.clone().add(0.5, 0.5, 0.5), 0.75, 1.5, 0.75, ent ->
                ent instanceof ArmorStand && ent.getScoreboardTags().contains("dungeon_flower_slot"))
                .forEach(org.bukkit.entity.Entity::remove);

        MultiLineHologram holo = state.placementHolograms.remove(base);
        if (holo != null) holo.despawn();
        held.setAmount(held.getAmount() - 1);
        state.placements.put(base, type);
        ChatMessageUtil.send(player, MessageType.SUCCESS, "Flower placed.");
        base.getWorld().spawnParticle(Particle.END_ROD, base.clone().add(0.5, 1.1, 0.5), 16, 0.2, 0.35, 0.2, 0.01);
        base.getWorld().playSound(base, Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.4f);
        long filled = state.placements.values().stream().filter(java.util.Objects::nonNull).count();
        long distinct = state.placements.values().stream().filter(java.util.Objects::nonNull).map(ft -> ft.block).distinct().count();
        if (filled >= required && distinct >= Math.min(required, FlowerType.values().length)) {
            completePuzzle(state);
        }
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
            state.mobMarkers.add(new MobMarker(cyan, "Nocsy_Ganon"));
        }
        for (Location boss : markers.bossMarkers) {
            state.mobMarkers.add(new MobMarker(boss, "MSO_Demon_General"));
        }
    }

    private void startMobWatcher(InstanceState state) {
        final double radiusSq = 140 * 140;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (state.participants.stream().noneMatch(p -> p != null && p.isOnline())) {
                    cancel();
                    return;
                }
                int waiting = 0;
                double nearestSq = Double.MAX_VALUE;
                for (MobMarker marker : state.mobMarkers) {
                    if (marker.spawned) continue;
                    waiting++;
                    for (Player p : state.participants) {
                        if (p == null || !p.isOnline() || p.getWorld() != marker.loc.getWorld()) continue;
                        double distSq = p.getLocation().distanceSquared(marker.loc);
                        nearestSq = Math.min(nearestSq, distSq);
                        if (distSq <= radiusSq) {
                            if (!marker.proximityTriggered) {
                                marker.proximityTriggered = true;
                                plugin.getLogger().info("[Dungeon] Player " + p.getName() + " within spawn radius for " + marker.mobId + " at " + marker.loc);
                            }
                            trySpawnMob(marker);
                            break;
                        }
                    }
                }
                if (waiting > 0 && nearestSq < Double.MAX_VALUE && nearestSq > radiusSq) {
                    plugin.getLogger().fine("[Dungeon] Mob spawns pending=" + waiting + " nearestDistSq=" + nearestSq);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void trySpawnMob(MobMarker marker) {
        if (marker.spawned || marker.spawning) return;
        marker.spawning = true;
        new BukkitRunnable() {
            int attempts = 0;

            @Override
            public void run() {
                if (marker.spawned) {
                    cancel();
                    return;
                }
                org.bukkit.Chunk chunk = marker.loc.getChunk();
                if (!chunk.isLoaded()) {
                    chunk.load(true);
                    plugin.getLogger().info("[Dungeon] Loading chunk for mob " + marker.mobId + " at " + marker.loc);
                    if (++attempts >= 5) {
                        marker.spawning = false;
                        plugin.getLogger().warning("[Dungeon] Unable to load chunk for mob " + marker.mobId + " at " + marker.loc);
                        cancel();
                    }
                    return;
                }
                var mob = MythicMobModifier.spawnModifiedMob(marker.mobId, marker.loc, null, null, null, null);
                if (mob != null) {
                    if (marker.mobId.equals("MSO_Demon_General")) {
                        mob.getEntity().getBukkitEntity().addScoreboardTag("dungeon_boss");
                    }
                    marker.spawned = true;
                    plugin.getLogger().info("[Dungeon] Spawned mob " + marker.mobId + " at " + marker.loc);
                    cancel();
                    return;
                }
                plugin.getLogger().warning("[Dungeon] Failed to spawn mob " + marker.mobId + " at attempt " + attempts + " location=" + marker.loc);
                if (++attempts >= 5) {
                    marker.spawning = false;
                    plugin.getLogger().warning("[Dungeon] Giving up spawning mob " + marker.mobId + " at " + marker.loc);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
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
        for (Player p : state.participants) {
            if (p != null && p.isOnline()) {
                ChatMessageUtil.send(p, MessageType.SUCCESS, "Puzzle complete!");
            }
        }
        int coinsAward = ThreadLocalRandom.current().nextInt(300, 601);
        int gemsAward = ThreadLocalRandom.current().nextInt(3, 8);
        for (Player p : state.participants) {
            if (p != null && p.isOnline()) {
                plugin.getEconomyManager().addCoins(p, coinsAward);
                plugin.getGemsManager().addUnits(p, gemsAward);
                ChatMessageUtil.send(p, MessageType.SUCCESS, "Rewards: +" + coinsAward + " coins, +" + gemsAward + " gems");
            }
        }
        for (Location fountain : state.rewardFountains) {
            for (Player p : state.participants) {
                if (p != null && p.isOnline()) {
                    RewardBombUtil.startRewardBomb(plugin, fountain, this::createFountainReward, 100, p);
                }
            }
        }
    }

    private ItemStack createFountainReward() {
        int roll = ThreadLocalRandom.current().nextInt(3);
        return switch (roll) {
            case 0 -> {
                var generator = plugin.getItemManager();
                var generated = generator.generateItem("dungeon", 20 + ThreadLocalRandom.current().nextInt(10));
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
        public void onDeath(EntityDeathEvent event) {
            if (!event.getEntity().getScoreboardTags().contains("dungeon_boss")) return;
            InstanceState state = activeInstances.get(event.getEntity().getWorld());
            if (state == null || state.bossDefeated) return;
            state.bossDefeated = true;
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
                    ChatMessageUtil.send(p, MessageType.SUCCESS, "Dungeon complete! Time: " + seconds + "s | Score: " + score);
                }
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
        }

        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            World world = event.getPlayer().getWorld();
            if (activeInstances.containsKey(world)) {
                removeDungeonItems(event.getPlayer());
            }
        }
    }
}
