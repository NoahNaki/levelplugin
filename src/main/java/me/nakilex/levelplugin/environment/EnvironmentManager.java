package me.nakilex.levelplugin.environment;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.environment.stage.BuildingStageManager;
import me.nakilex.levelplugin.player.config.PlayerConfig;
import me.nakilex.levelplugin.environment.stage.TownStageManager;
import me.nakilex.levelplugin.fakeblock.FakeBlockManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Particle;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles per-player settlement levels and upgrades.
 */
public class EnvironmentManager {
    /** Maximum times we try loading a single chunk before giving up. */
    private static final int MAX_CHUNK_ATTEMPTS = 5;
    /** Upper limit of block updates sent per tick during build animations. */
    private static final int MAX_BLOCKS_PER_TICK = 32;
    private static final String TOWN_TASK_KEY = "__town__";
    private final PlayerConfig playerConfig;
    private final TownStageManager stageManager;
    private final me.nakilex.levelplugin.environment.stage.BuildingStageManager buildingStageManager;
    private final FakeBlockManager fakeBlockManager;
    private final Map<UUID, EnvironmentState> states = new HashMap<>();
    private final Map<UUID, Location> origins = new HashMap<>();
    private final Map<UUID, String> towns = new HashMap<>();
    private final Map<UUID, Map<String, BuildingState>> buildingStates = new HashMap<>();
    /** Active build tasks per player and building key. */
    private final Map<UUID, Map<String, BukkitTask>> buildTasks = new HashMap<>();
    /** Hologram entities per building per player. */
    private final Map<UUID, Map<String, java.util.List<org.bukkit.entity.Entity>>> buildingHolograms = new HashMap<>();
    private final Map<UUID, UUID> coopOwners = new HashMap<>();
    private final Map<UUID, UUID> coopPartners = new HashMap<>();
    private final Map<UUID, UUID> pendingInvites = new HashMap<>();
    /** Track placed block priorities for each player (location key -> priority). */
    private final Map<UUID, Map<String, Integer>> blockPriorities = new HashMap<>();
    /** Keep track of chunks force-loaded for each player so they can be released later. */
    private final Map<UUID, java.util.Set<Long>> loadedChunks = new java.util.HashMap<>();
    /** Chunks still waiting to load for each player. */
    private final Map<UUID, java.util.Set<Long>> pendingChunkLoads = new java.util.HashMap<>();
    /** Attempts made per chunk so we can retry a few times. */
    private final Map<UUID, java.util.Map<Long, Integer>> chunkRetryCount = new java.util.HashMap<>();
    /** Repeating tasks that ensure chunks finish loading. */
    private final Map<UUID, BukkitTask> chunkLoadTasks = new java.util.HashMap<>();
    /** Players that require periodic checks for chunk loading. */
    private final java.util.Set<UUID> townCheckList = new java.util.HashSet<>();
    /** Single repeating task that verifies chunks for all players. */
    private BukkitTask globalTownCheckTask;
    /** Cached set of chunk keys for each player's town to avoid recomputation. */
    private final Map<UUID, java.util.Set<Long>> townChunkCache = new java.util.HashMap<>();
    /** Cached mapping of chunk -> block map for each player's main structure. */
    private final Map<UUID, Map<Long, Map<Location, org.bukkit.block.data.BlockData>>> townChunkBlocks = new java.util.HashMap<>();
    /** Cached mapping of chunk -> block map for each building per player. */
    private final Map<UUID, Map<String, Map<Long, Map<Location, org.bukkit.block.data.BlockData>>>> buildingChunkBlocks = new java.util.HashMap<>();
    /** Players whose settlement chunks have fully loaded. */
    private final java.util.Set<UUID> fullyLoaded = new java.util.HashSet<>();

    /** Players currently viewing their town (fake blocks active). */
    private final java.util.Set<UUID> loadedPlayers = new java.util.HashSet<>();

    /** Players that already saw the initial build animation. */
    private final java.util.Set<UUID> playedInitAnimation = new java.util.HashSet<>();

    /** Players for whom chunk tasks are temporarily paused. */
    private final java.util.Set<UUID> chunkPaused = new java.util.HashSet<>();
    /** Timers that auto-resume chunk tasks after a delay. */
    private final java.util.Map<UUID, BukkitTask> chunkResumeTimers = new java.util.HashMap<>();

    private static String key(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    /** Whether chunk loading debug is enabled. */
    public static boolean isDebug() {
        return Main.getInstance().getCustomConfig().getBoolean("debug.chunk-loading", false);
    }

    private static void debugLog(String msg) {
        if (isDebug()) {
            Main.getInstance().getLogger().info("[ChunkDebug] " + msg);
        }
    }

    /** Convert a lowercase, space-separated name into capitalized words. */
    public static String beautifyWords(String name) {
        String[] parts = name.toLowerCase().split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            sb.append(Character.toUpperCase(parts[i].charAt(0)))
              .append(parts[i].substring(1));
            if (i < parts.length - 1) sb.append(' ');
        }
        return sb.toString();
    }

    public static class EnvironmentState {
        public int level;
        public int stage;
        public int invested;
        public EnvironmentState(int level, int stage) {
            this.level = level;
            this.stage = stage;
        }
    }

    public static class BuildingState {
        public int stage;
        public int invested;
        public BuildingState(int stage) {
            this.stage = stage;
        }
    }

    public EnvironmentManager(PlayerConfig config,
                              TownStageManager stageManager,
                              me.nakilex.levelplugin.environment.stage.BuildingStageManager buildingStageManager,
                              FakeBlockManager blockManager) {
        this.playerConfig = config;
        this.stageManager = stageManager;
        this.buildingStageManager = buildingStageManager;
        this.fakeBlockManager = blockManager;
    }

    public TownStageManager getStageManager() {
        return stageManager;
    }

    public me.nakilex.levelplugin.environment.stage.BuildingStageManager getBuildingStageManager() {
        return buildingStageManager;
    }

    public String getTown(UUID uuid) {
        return towns.get(uuid);
    }

    /** Get the origin location for the player's town if it exists. */
    public Location getOrigin(UUID uuid) {
        return origins.get(getBase(uuid));
    }

    /** Get the current stage of a player's building. */
    public int getPlayerBuildingStage(Player player, String building) {
        loadPlayerState(player);
        java.util.UUID base = getBase(player.getUniqueId());
        java.util.Map<String, BuildingState> map = buildingStates.get(base);
        if (map == null) return 1;
        BuildingState bs = map.get(building.toLowerCase());
        if (bs == null) return 1;
        return bs.stage;
    }

    /** Whether the player's town is currently loaded for them. */
    public boolean isTownLoaded(Player player) {
        return loadedPlayers.contains(player.getUniqueId());
    }

    /** Internal setter for loaded state. */
    public void markTownLoaded(Player player, boolean loaded) {
        UUID id = player.getUniqueId();
        if (loaded) loadedPlayers.add(id); else loadedPlayers.remove(id);
    }

    /** Track that the initial animation has played for this player. */
    public void markAnimationPlayed(Player player) {
        playedInitAnimation.add(getBase(player.getUniqueId()));
    }

    /** Check if the player already saw the initial build animation. */
    public boolean hasPlayedInitAnimation(Player player) {
        return playedInitAnimation.contains(getBase(player.getUniqueId()));
    }

    /** Check if all chunks for the player's town are currently loaded. */
    public boolean areTownChunksLoaded(Player player) {
        UUID base = getBase(player.getUniqueId());
        Location origin = origins.get(base);
        if (origin == null) return true;
        for (long key : collectTownChunks(base)) {
            int cx = (int) (key >> 32);
            int cz = (int) key;
            if (!origin.getWorld().getChunkAt(cx, cz).isLoaded()) {
                return false;
            }
        }
        return true;
    }

    /*
     * Previous versions compared real blocks against the schematic on every
     * chunk load to detect desynchronization. These checks proved extremely
     * expensive and rarely provided useful recovery, so they have been removed
     * in favor of simply ensuring chunks are loaded.
     */

    /** Build the hologram text for a building upgrade based on the player's
     *  current resources. */
    private java.util.List<String> formatBuildingHologram(Player player, String building, int stage) {
        int nextStage = stage + 1;

        var nextData = buildingStageManager.getStage(building, nextStage);
        String niceName = java.util.Arrays.stream(building.replace('_', ' ').split(" "))
                .filter(part -> !part.isEmpty())
                .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1).toLowerCase())
                .collect(java.util.stream.Collectors.joining(" "));

        if (nextData == null) {
            return null; // no further upgrades, don't show hologram
        }

        java.util.List<String> reqLines = new java.util.ArrayList<>();
        int coins = Main.getInstance().getEconomyManager().getBalance(player);
        for (var entry : nextData.materialCost.entrySet()) {
            org.bukkit.Material mat = entry.getKey();
            int amt = entry.getValue();
            boolean has = player.getInventory().containsAtLeast(new org.bukkit.inventory.ItemStack(mat, amt), amt);
            String matName = beautifyWords(mat.name().toLowerCase().replace('_', ' '));
            String line = (has ? ChatColor.GREEN + "\u2714" : ChatColor.RED + "\u2718")
                    + ChatColor.GRAY + " - " + ChatColor.WHITE + matName
                    + ChatColor.GRAY + " x" + ChatColor.WHITE + amt;
            reqLines.add(line);
        }
        int coinCost = nextData.coinCost;
        boolean hasCoins = coins >= coinCost;
        String coinLine = (hasCoins ? ChatColor.GREEN + "\u2714" : ChatColor.RED + "\u2718")
                + ChatColor.GRAY + " - " + ChatColor.WHITE + "" + coinCost + " coins "
                + ChatColor.GOLD + " <glyph:coins_icon>";
        reqLines.add(coinLine);

        java.util.List<String> lines = new java.util.ArrayList<>();
        String verb = stage == 1 ? "CONSTRUCT" : "UPGRADE";
        lines.add(ChatColor.GREEN + "" + ChatColor.BOLD + verb + " " + ChatColor.WHITE + niceName);
        lines.add(ChatColor.GOLD.toString() + ChatColor.BOLD + "STAGE "
            + ChatColor.YELLOW + stage + " "
            + ChatColor.GREEN + ">" + ChatColor.DARK_GREEN + ">"
            + ChatColor.GREEN + ">" + ChatColor.DARK_GREEN + "> "
            + ChatColor.GOLD + ChatColor.BOLD.toString() + "STAGE " + ChatColor.YELLOW + nextStage);
        lines.add(ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH.toString() + "--------------------");
        lines.add(ChatColor.AQUA + "Requirements:");
        lines.addAll(reqLines);
        lines.add(ChatColor.YELLOW.toString() + ChatColor.UNDERLINE + "Right-click to upgrade!");
        return lines;
    }

    /** Spawn hologram entities at the given location. */
    private java.util.List<org.bukkit.entity.Entity> spawnHologramLines(Player player, Location base,
                                                                       java.util.List<String> lines, String tag) {
        if (lines == null || lines.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        java.util.List<org.bukkit.entity.Entity> entities = new java.util.ArrayList<>();

        // Spawn an invisible interaction entity for reliable clicking
        double bottomOffset = -(lines.size() - 1) * 0.25;
        Location clickLoc = base.clone().add(0, bottomOffset, 0);
        org.bukkit.entity.Interaction clicker = clickLoc.getWorld().spawn(
                clickLoc, org.bukkit.entity.Interaction.class, it -> {
                    // Make the clickable area large so players don't miss the hologram
                    it.setInteractionWidth(2.0f);
                    it.setInteractionHeight(2.0f);
                    it.addScoreboardTag("building_hologram:" + tag.toLowerCase());
                });
        entities.add(clicker);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(player)) p.hideEntity(Main.getInstance(), clicker);
        }

        double offset = 0.0;
        for (String text : lines) {
            Location loc = base.clone().add(0, offset, 0);
            TextDisplay td = (TextDisplay) base.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
            td.setBillboard(Display.Billboard.CENTER);
            td.setShadowRadius(0f);
            td.setShadowStrength(0f);
            td.setText(text);
            td.addScoreboardTag("building_hologram:" + tag.toLowerCase());
            entities.add(td);
            offset -= 0.25;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(player)) p.hideEntity(Main.getInstance(), td);
            }
        }
        return entities;
    }

    private UUID getBase(UUID uuid) {
        return coopOwners.getOrDefault(uuid, uuid);
    }

    private void shareData(UUID member, UUID owner) {
        states.put(member, states.get(owner));
        origins.put(member, origins.get(owner));
        towns.put(member, towns.get(owner));
        Map<String, BuildingState> map = buildingStates.get(owner);
        if (map != null) {
            buildingStates.put(member, map);
        }
    }

    private void loadPlayerData(UUID uuid) {
        states.computeIfAbsent(uuid, id -> {
            int lvl = playerConfig.getEnvironmentLevel(id);
            int stg = playerConfig.getEnvironmentStage(id);
            if (lvl <= 0) lvl = 1;
            if (stg <= 0) stg = 1;
            return new EnvironmentState(lvl, stg);
        });

        if (!origins.containsKey(uuid)) {
            Location origin = playerConfig.getEnvironmentOrigin(uuid);
            if (origin != null) origins.put(uuid, origin);
        }

        if (!towns.containsKey(uuid)) {
            String town = playerConfig.getEnvironmentTown(uuid);
            if (town != null) towns.put(uuid, town);
        }

        if (towns.containsKey(uuid) && !buildingStates.containsKey(uuid)) {
            Map<String, BuildingState> map = new java.util.HashMap<>();
            for (String b : playerConfig.getStoredBuildings(uuid)) {
                int bs = playerConfig.getBuildingStage(uuid, b);
                map.put(b.toLowerCase(), new BuildingState(bs));
            }
            if (!map.isEmpty()) buildingStates.put(uuid, map);
        }
    }

    /** Compute all chunk coordinates for the player's town and buildings. */
    private java.util.Set<Long> computeTownChunks(UUID base) {
        EnvironmentState st = states.get(base);
        Location origin = origins.get(base);
        String town = towns.get(base);
        if (st == null || origin == null || town == null) {
            return java.util.Collections.emptySet();
        }

        java.util.Set<Long> chunks = new java.util.HashSet<>();
        Map<Long, Map<Location, org.bukkit.block.data.BlockData>> structMap = new java.util.HashMap<>();
        Map<String, Map<Long, Map<Location, org.bukkit.block.data.BlockData>>> buildMap = new java.util.HashMap<>();

        var stage = stageManager.getStage(town, st.level, st.stage);
        if (stage != null) {
            Location baseOrigin = origin.clone().add(0, stage.oy, 0);
            for (var b : stage.blocks) {
                Location loc = baseOrigin.clone().add(b.x - stage.ox, b.y - stage.oy, b.z - stage.oz);
                long key = (((long) (loc.getBlockX() >> 4)) << 32) ^ (loc.getBlockZ() >> 4 & 0xffffffffL);
                chunks.add(key);
                structMap.computeIfAbsent(key, k -> new java.util.HashMap<>()).put(loc, b.data);
            }
        }

        Map<String, BuildingState> bMap = buildingStates.get(base);
        if (bMap != null) {
            for (var e : bMap.entrySet()) {
                var bStage = buildingStageManager.getStage(e.getKey(), e.getValue().stage);
                if (bStage == null) continue;
                Location bo = getBuildingOrigin(town, e.getKey(), origin).add(0, bStage.oy, 0);
                Map<Long, Map<Location, org.bukkit.block.data.BlockData>> chunkMap = new java.util.HashMap<>();
                for (var b : bStage.blocks) {
                    Location loc = bo.clone().add(b.x - bStage.ox, b.y - bStage.oy, b.z - bStage.oz);
                    long key = (((long) (loc.getBlockX() >> 4)) << 32) ^ (loc.getBlockZ() >> 4 & 0xffffffffL);
                    chunks.add(key);
                    chunkMap.computeIfAbsent(key, k -> new java.util.HashMap<>()).put(loc, b.data);
                }
                buildMap.put(e.getKey().toLowerCase(), chunkMap);
            }
        }

        townChunkBlocks.put(base, structMap);
        buildingChunkBlocks.put(base, buildMap);

        return chunks;
    }

    /**
     * Get the cached chunk set for this player, computing if necessary.
     */
    private java.util.Set<Long> collectTownChunks(UUID base) {
        return townChunkCache.computeIfAbsent(base, this::computeTownChunks);
    }

    /** Invalidate cached chunk coordinates for the player. */
    private void invalidateTownChunks(UUID base) {
        townChunkCache.remove(base);
        townChunkBlocks.remove(base);
        buildingChunkBlocks.remove(base);
        fullyLoaded.remove(base);
    }

    /** Load state for player from config without spawning any structures. */
    public void loadPlayerState(Player player) {
        UUID uuid = player.getUniqueId();

        UUID owner = playerConfig.getCoopOwner(uuid);
        if (owner != null) {
            coopOwners.put(uuid, owner);
            loadPlayerData(owner);
            shareData(uuid, owner);
            return;
        }

        loadPlayerData(uuid);
        UUID partner = playerConfig.getCoopPartner(uuid);
        if (partner != null) {
            coopPartners.put(uuid, partner);
        }
    }

    /** Load state for player if not present and spawn their structures/NPCs. */
    public void initializePlayer(Player player) {
        initializePlayerInternal(player, false, 20);
    }

    /** Load player state and spawn their town with a short animation. */
    public void initializePlayerAnimated(Player player, int ticks) {
        initializePlayerInternal(player, true, ticks);
    }

    private void initializePlayerInternal(Player player, boolean animated, int ticks) {
        loadPlayerState(player);
        boolean ready = preloadTownChunks(player);

        Runnable spawn = () -> {
            UUID uuid = player.getUniqueId();
            EnvironmentState es = states.get(uuid);
            Location origin = origins.get(uuid);
            if (origin != null && es != null) {
                Map<String, BuildingState> bMap = buildingStates.get(uuid);
                final Map<String, BuildingState> finalBMap = bMap == null ? null : new java.util.HashMap<>(bMap);
                final Location finalOrigin = origin;
                if (finalBMap != null) {
                    for (var e : finalBMap.entrySet()) {
                        Location bOrig = getBuildingOrigin(towns.get(uuid), e.getKey(), finalOrigin);
                        if (animated) {
                            spawnBuildingTimed(player, e.getKey(), bOrig, e.getValue().stage, null, ticks);
                        } else {
                            spawnBuildingInstant(player, e.getKey(), bOrig, e.getValue().stage);
                        }
                    }
                }
                if (animated) {
                    spawnStructureTimed(player, finalOrigin, es.level, es.stage, null, ticks);
                } else {
                    spawnStructureInstant(player, finalOrigin, es.level, es.stage);
                }
            }
        };

        if (ready) {
            spawn.run();
            // Verify chunks are correctly loaded after spawning
            startTownLoadCheck(player);
        } else {
            waitForChunks(player, spawn);
            // Start periodic checks so the town is spawned once chunks load
            startTownLoadCheck(player);
        }
    }

    /** Repeatedly check for chunk completion then run the given action. */
    private void waitForChunks(Player player, Runnable action) {
        UUID base = getBase(player.getUniqueId());
        new BukkitRunnable() {
            @Override public void run() {
                if (chunkPaused.contains(base)) return;
                Player p = Bukkit.getPlayer(base);
                if (p == null || !p.isOnline()) { cancel(); return; }
                if (areTownChunksLoaded(p)) {
                    action.run();
                    cancel();
                } else {
                    preloadTownChunks(p);
                }
            }
        }.runTaskTimer(Main.getInstance(), 20L, 20L);
    }

    public EnvironmentState getState(UUID uuid) {
        return states.get(uuid);
    }

    /** Cancel all active build tasks for the player. */
    private void cancelBuildTasks(UUID uuid) {
        Map<String, BukkitTask> map = buildTasks.remove(uuid);
        if (map != null) {
            for (BukkitTask t : map.values()) {
                t.cancel();
            }
        }
    }

    /** Cancel a build task for a specific building if present. */
    private void cancelBuildTask(UUID uuid, String key) {
        Map<String, BukkitTask> map = buildTasks.get(uuid);
        if (map == null) return;
        BukkitTask t = map.remove(key);
        if (t != null) t.cancel();
        if (map.isEmpty()) buildTasks.remove(uuid);
    }

    /** Remove task mapping without cancelling (called after completion). */
    private void clearFinishedTask(UUID uuid, String key) {
        Map<String, BukkitTask> map = buildTasks.get(uuid);
        if (map == null) return;
        map.remove(key);
        if (map.isEmpty()) buildTasks.remove(uuid);
    }

    private void cancelTasks(UUID uuid) {
        cancelBuildTasks(uuid);
        BukkitTask chk = chunkLoadTasks.remove(uuid);
        if (chk != null) {
            chk.cancel();
        }
        cancelTownLoadCheck(uuid);
    }

    /** Periodically verify that a player's town chunks are loaded and correct. */
    private void startTownLoadCheck(Player player) {
        UUID base = getBase(player.getUniqueId());
        if (!townCheckList.add(base)) return;
        if (globalTownCheckTask != null) return;
        globalTownCheckTask = new BukkitRunnable() {
            @Override public void run() {
                java.util.Iterator<UUID> it = townCheckList.iterator();
                while (it.hasNext()) {
                    UUID b = it.next();
                    if (chunkPaused.contains(b)) continue;
                    Player p = Bukkit.getPlayer(b);
                    if (p == null || !p.isOnline()) {
                        it.remove();
                        continue;
                    }

                    Location origin = origins.get(b);
                    if (origin == null) {
                        it.remove();
                        continue;
                    }

                    boolean allLoaded = true;
                    for (long key : collectTownChunks(b)) {
                        int cx = (int) (key >> 32);
                        int cz = (int) key;
                        if (!origin.getWorld().isChunkLoaded(cx, cz)) {
                            allLoaded = false;
                            preloadTownChunks(p);
                            break;
                        }
                    }
                    if (allLoaded) {
                        it.remove();
                    }
                }
                if (townCheckList.isEmpty()) {
                    globalTownCheckTask.cancel();
                    globalTownCheckTask = null;
                }
            }
        }.runTaskTimer(Main.getInstance(), 60L, 60L);
    }

    private void cancelTownLoadCheck(UUID base) {
        townCheckList.remove(base);
        if (townCheckList.isEmpty() && globalTownCheckTask != null) {
            globalTownCheckTask.cancel();
            globalTownCheckTask = null;
        }
    }

    /** Temporarily stop chunk-related tasks for the player. */
    private void pauseChunkTasks(UUID base) {
        chunkPaused.add(base);
        BukkitTask load = chunkLoadTasks.remove(base);
        if (load != null) load.cancel();
        cancelTownLoadCheck(base);
        BukkitTask timer = chunkResumeTimers.remove(base);
        if (timer != null) timer.cancel();
        timer = new BukkitRunnable() {
            @Override public void run() {
                chunkResumeTimers.remove(base);
                resumeChunkTasks(base);
            }
        }.runTaskLater(Main.getInstance(), 120L);
        chunkResumeTimers.put(base, timer);
    }

    /** Resume chunk tasks after a pause. */
    private void resumeChunkTasks(UUID base) {
        chunkPaused.remove(base);
        BukkitTask timer = chunkResumeTimers.remove(base);
        if (timer != null) timer.cancel();
        Player p = Bukkit.getPlayer(base);
        if (p != null && p.isOnline()) {
            preloadTownChunks(p);
            startTownLoadCheck(p);
        }
    }

    private void resumeChunkTasks(Player player) {
        if (player != null)
            resumeChunkTasks(getBase(player.getUniqueId()));
    }

    private void removeBuildingHologram(UUID uuid, String building) {
        var map = buildingHolograms.get(uuid);
        if (map != null) {
            var list = map.remove(building.toLowerCase());
            if (list != null) {
                for (var disp : list) {
                    if (disp != null && !disp.isDead()) disp.remove();
                }
            }
            if (map.isEmpty()) buildingHolograms.remove(uuid);
        }
    }

    /** Remove a hologram for all town members. */
    private void removeTownHologram(UUID member, String building) {
        UUID base = getBase(member);
        removeBuildingHologram(base, building);
        UUID partner = coopPartners.get(base);
        if (partner != null) removeBuildingHologram(partner, building);
    }

    /** Remove every hologram currently spawned. */
    public void removeAllHolograms() {
        for (var map : buildingHolograms.values()) {
            for (var list : map.values()) {
                if (list != null) {
                    for (var disp : list) {
                        if (disp != null && !disp.isDead()) disp.remove();
                    }
                }
            }
        }
        buildingHolograms.clear();
    }

    private void removeAllBuildingHolograms(UUID uuid) {
        var map = buildingHolograms.remove(uuid);
        if (map != null) {
            for (var list : map.values()) {
                if (list != null) {
                    for (var disp : list) {
                        if (disp != null && !disp.isDead()) disp.remove();
                    }
                }
            }
        }
    }

    private void removeMemberData(UUID member, String town, EnvironmentState st, Map<String, BuildingState> bMap) {
        cancelTasks(member);
        removeAllBuildingHolograms(member);
        stageManager.despawnForStage(member, town, st.level, st.stage);
        if (bMap != null) {
            for (var e : bMap.entrySet()) {
                buildingStageManager.despawnForStage(member, e.getKey(), e.getValue().stage);
            }
        }
        fakeBlockManager.clear(Bukkit.getPlayer(member));
        blockPriorities.remove(member);
        towns.remove(member);
        origins.remove(member);
        states.remove(member);
        buildingStates.remove(member);
        coopOwners.remove(member);
        invalidateTownChunks(member);
        playerConfig.clearEnvironmentData(member);
        playerConfig.saveConfigFile();
    }

    public void saveState(UUID uuid) {
        UUID base = getBase(uuid);
        if (!base.equals(uuid)) {
            return; // members rely on owner save
        }
        EnvironmentState s = states.get(base);
        if (s != null) {
            playerConfig.setEnvironmentState(base, s.level, s.stage);
            String town = towns.get(base);
            if (town != null) playerConfig.setEnvironmentTown(base, town);
            Map<String, BuildingState> bMap = buildingStates.get(base);
            if (bMap != null) {
                for (var e : bMap.entrySet()) {
                    playerConfig.setBuildingStage(base, e.getKey(), e.getValue().stage);
                }
            }
            UUID partner = coopPartners.get(base);
            playerConfig.setCoopPartner(base, partner);
            playerConfig.saveConfigFile();
        }
    }

    public void saveAll() {
        for (UUID id : states.keySet()) {
            saveState(id);
        }
    }

    /**
     * Invest materials towards the next upgrade. Currently costs 1 oak log.
     */
    public void invest(Player player, int amount) {
        loadPlayerState(player);
        UUID base = getBase(player.getUniqueId());
        EnvironmentState state = states.get(base);
        state.invested += amount;
        if (state.invested >= 1) {
            state.invested = 0;
            int oldLevel = state.level;
            int oldStage = state.stage;
            advance(state);
            invalidateTownChunks(base);
            player.sendMessage(ChatColor.GREEN + "Settlement upgraded to Level " + state.level + " Stage " + state.stage + "!");
            String town = towns.get(base);
            Location origin = origins.get(base);
            if (town != null && origin != null) {
                stageManager.despawnForStage(player.getUniqueId(), town, oldLevel, oldStage);
                spawnStructureUpgrade(player, origin, oldLevel, oldStage, state.level, state.stage);
            }
            Main.getInstance().getQuestManager().handleTownUpgrade(player);
            computeTownChunks(base);
            saveState(base);
        } else {
            player.sendMessage(ChatColor.GREEN + "Invested " + amount + " oak log.");
        }
    }

    /** Invest materials towards upgrading a specific building. */
    public void investBuilding(Player player, String building, int amount) {
        loadPlayerState(player);
        UUID base = getBase(player.getUniqueId());
        Map<String, BuildingState> bMap = buildingStates.get(base);
        if (bMap == null) {
            player.sendMessage(ChatColor.RED + "You have no settlement buildings.");
            return;
        }
        BuildingState bs = bMap.get(building.toLowerCase());
        if (bs == null) {
            player.sendMessage(ChatColor.RED + "Unknown building.");
            return;
        }
        bs.invested += amount;
        if (bs.invested >= 1) {
            bs.invested = 0;
            int oldS = bs.stage;
            advance(bs);
            invalidateTownChunks(base);
            player.sendMessage(ChatColor.GREEN + building + " upgraded to Stage " + bs.stage);
            String town = towns.get(base);
            Location origin = origins.get(base);
            if (town != null && origin != null) {
                Location bOrig = getBuildingOrigin(town, building, origin);
                buildingStageManager.despawnForStage(player.getUniqueId(), building, oldS);
                spawnBuildingUpgrade(player, building, bOrig, oldS, bs.stage);
            }
            Main.getInstance().getQuestManager().handleTownUpgrade(player);
            computeTownChunks(base);
            saveState(base);
        } else {
            player.sendMessage(ChatColor.GREEN + "Invested " + amount + " oak log.");
        }
    }

    /** Attempt to upgrade a building by paying its configured cost. */
    public void attemptUpgradeBuilding(Player player, String building) {
        int currentStage = getPlayerBuildingStage(player, building);
        var nextStageData = buildingStageManager.getStage(building, currentStage + 1);
        if (nextStageData == null) {
            player.sendMessage(ChatColor.RED + "Building is fully upgraded.");
            return;
        }
        // Check materials
        for (var entry : nextStageData.materialCost.entrySet()) {
            org.bukkit.Material mat = entry.getKey();
            int amt = entry.getValue();
            if (!player.getInventory().containsAtLeast(new org.bukkit.inventory.ItemStack(mat, amt), amt)) {
                player.sendMessage(ChatColor.RED + "Missing required materials for upgrade.");
                return;
            }
        }
        int coinCost = nextStageData.coinCost;
        int balance = Main.getInstance().getEconomyManager().getBalance(player);
        if (balance < coinCost) {
            player.sendMessage(ChatColor.RED + "You need " + coinCost + " coins.");
            return;
        }
        // Deduct items
        for (var entry : nextStageData.materialCost.entrySet()) {
            player.getInventory().removeItem(new org.bukkit.inventory.ItemStack(entry.getKey(), entry.getValue()));
        }
        if (coinCost > 0) {
            Main.getInstance().getEconomyManager().deductCoins(player, coinCost);
        }
        investBuilding(player, building, 1);
    }

    private void advance(EnvironmentState state) {
        state.stage++;
    }

    private void advance(BuildingState state) {
        state.stage++;
    }

    // All towns reside in the "flatland" world for now
    private static final String TOWN_WORLD = "flatland";
    private static final int TOWN_X = 2010;
    // Raise the starting Y coordinate by 5 blocks
    private static final int TOWN_Y = -54;
    private static final int TOWN_Z = -1242;

    public Location getTownStartLocation() {
        return new Location(Bukkit.getWorld(TOWN_WORLD), TOWN_X, TOWN_Y, TOWN_Z);
    }

    private Location getBuildingOrigin(String town, String building, Location townOrigin) {
        var pl = buildingStageManager.getPlacement(town, building);
        if (pl == null) return townOrigin;
        return townOrigin.clone().add(pl.x, pl.y, pl.z);
    }

    private void teleportWithEffect(Player player, Location dest, Runnable after) {
        var startLoc = player.getLocation().clone();
        new org.bukkit.scheduler.BukkitRunnable() {
            int t = 60;
            @Override public void run() {
                if(!player.isOnline()) { cancel(); return; }
                if(player.getLocation().distanceSquared(startLoc) > 0.1) {
                    player.sendMessage(ChatColor.RED + "Teleport cancelled.");
                    cancel();
                    return;
                }
                double radius = 3.0*(t/60.0);
                for(int i=0;i<20;i++) {
                    double angle = 2*Math.PI*i/20.0;
                    double x = radius*Math.cos(angle);
                    double z = radius*Math.sin(angle);
                    player.getWorld().spawnParticle(org.bukkit.Particle.DRAGON_BREATH,startLoc.clone().add(x,1,z),0,0,0,0,0);
                }
                if(--t <= 0) {
                    player.teleport(dest);
                    player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS,40,0,false,false));
                    player.getWorld().spawnParticle(org.bukkit.Particle.FLASH, player.getLocation(), 20, 0.5,0.5,0.5,0);
                    if(after != null) after.run();
                    cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(),0L,1L);
    }

    /**
     * Ensure all chunks that contain the player's town and unlocked buildings
     * are loaded. Each chunk will be requested multiple times until it is
     * reported loaded by the server to mirror how Minecraft loads chunks
     * naturally.
     */
    public boolean preloadTownChunks(Player player) {
        UUID base = getBase(player.getUniqueId());
        EnvironmentState st = states.get(base);
        Location origin = origins.get(base);
        if (st == null || origin == null) return true;
        if (chunkPaused.contains(base)) return false;
        if (fullyLoaded.contains(base)) return true;

        java.util.Set<Long> required = collectTownChunks(base);
        java.util.Set<Long> loaded = loadedChunks.computeIfAbsent(base, k -> new java.util.HashSet<>());
        java.util.Set<Long> pending = pendingChunkLoads.computeIfAbsent(base, k -> new java.util.HashSet<>());
        java.util.Map<Long, Integer> attempts = chunkRetryCount.computeIfAbsent(base, k -> new java.util.HashMap<>());

        debugLog("Preloading " + required.size() + " chunks in "
            + origin.getWorld().getName() + " for " + player.getName());

        boolean allLoaded = true;

        for (long key : required) {
            int cx = (int) (key >> 32);
            int cz = (int) key;
            org.bukkit.World world = origin.getWorld();
            org.bukkit.Chunk chunk = world.isChunkLoaded(cx, cz) ? world.getChunkAt(cx, cz) : null;

            if (chunk == null) {
                allLoaded = false;
                pending.add(key);
                int tries = attempts.getOrDefault(key, 0);
                if (tries < MAX_CHUNK_ATTEMPTS) {
                    debugLog("Request load for chunk " + cx + "," + cz + " attempt " + (tries + 1));
                    world.getChunkAtAsync(cx, cz, true).thenAccept(ch ->
                        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                            loaded.add(key);
                            ch.addPluginChunkTicket(Main.getInstance());
                            pending.remove(key);
                            attempts.remove(key);
                        }));
                    attempts.put(key, tries + 1);
                } else if (tries == MAX_CHUNK_ATTEMPTS) {
                    debugLog("Chunk " + cx + "," + cz + " still not loaded after " + MAX_CHUNK_ATTEMPTS + " tries");
                    attempts.put(key, tries + 1); // avoid repeated log
                }
            } else {
                loaded.add(key);
                chunk.addPluginChunkTicket(Main.getInstance());
                pending.remove(key);
                attempts.remove(key);
            }
        }

        if (!allLoaded) {
            fullyLoaded.remove(base);
            if (!chunkLoadTasks.containsKey(base)) {
                debugLog("Scheduling chunk verify task for " + player.getName());
                BukkitTask task = new BukkitRunnable() {
                    @Override public void run() {
                        Player p = Bukkit.getPlayer(base);
                        if (p == null || !p.isOnline()) { cancelTownLoadCheck(base); cancel(); return; }
                        if (preloadTownChunks(p)) cancel();
                    }
                }.runTaskTimer(Main.getInstance(), 60L, 60L);
                chunkLoadTasks.put(base, task);
            }
            startTownLoadCheck(player);
        } else {
            BukkitTask t = chunkLoadTasks.remove(base);
            if (t != null) t.cancel();
            cancelTownLoadCheck(base);
            pendingChunkLoads.remove(base);
            chunkRetryCount.remove(base);
            debugLog("Completed chunk preload for " + player.getName());
            fullyLoaded.add(base);
        }

        return allLoaded;
    }

    /** Start a settlement for the player at a fixed location using the given town name. */
    public void startTown(Player player, String townName) {
        UUID uuid = player.getUniqueId();
        if (origins.containsKey(uuid) || coopOwners.containsKey(uuid) || coopPartners.containsKey(uuid)) {
            player.sendMessage(ChatColor.RED + "You already started a settlement.");
            return;
        }
        if (townName == null || stageManager.getStage(townName, 1, 1) == null) {
            player.sendMessage(ChatColor.RED + "Unknown town type.");
            return;
        }
        Location origin = getTownStartLocation();
        origins.put(uuid, origin);
        towns.put(uuid, townName.toLowerCase());
        // initialize building progress for all defined buildings of this town
        var buildingNames = buildingStageManager.getBuildings(townName);
        if (!buildingNames.isEmpty()) {
            Map<String, BuildingState> map = new java.util.HashMap<>();
            for (String b : buildingNames) {
                map.put(b.toLowerCase(), new BuildingState(1));
            }
            buildingStates.put(uuid, map);
        }
        playerConfig.setEnvironmentOrigin(uuid, origin);
        playerConfig.setEnvironmentTown(uuid, townName.toLowerCase());
        playerConfig.saveConfigFile();
        invalidateTownChunks(uuid);

        final EnvironmentState state = states.computeIfAbsent(uuid, id -> new EnvironmentState(1, 1));
        Map<String, BuildingState> bMap = buildingStates.get(uuid);
        final Map<String, BuildingState> finalBMap = bMap == null ? null : new java.util.HashMap<>(bMap);
        final Runnable after;
        if (finalBMap != null) {
            after = () -> {
                for (var e : finalBMap.entrySet()) {
                    Location bo = getBuildingOrigin(townName.toLowerCase(), e.getKey(), origin);
                    spawnBuilding(player, e.getKey(), bo, e.getValue().stage, null);
                }
            };
        } else {
            after = null;
        }
        final Location finalOrigin = origin;
        final Runnable spawn = () -> {
            spawnStructure(player, finalOrigin, state.level, state.stage, after);
            player.sendMessage(ChatColor.YELLOW + "Settlement created at " + finalOrigin.getBlockX()+","+finalOrigin.getBlockY()+","+finalOrigin.getBlockZ());
        };
        teleportWithEffect(player, origin, spawn);
    }

    /** Remove the player's settlement so they can start over. */
    public void resetTown(Player player) {
        UUID uuid = player.getUniqueId();
        UUID base = getBase(uuid);
        if (!base.equals(uuid)) {
            // member leaving
            EnvironmentState st = states.get(base);
            String town = towns.get(base);
            Map<String, BuildingState> bMap = buildingStates.get(base);
            removeMemberData(uuid, town, st, bMap);
            coopPartners.remove(base);
            player.sendMessage(ChatColor.RED + "You have left the town.");
            invalidateTownChunks(base);
            return;
        }

        UUID partner = coopPartners.remove(uuid);
        if (partner != null) {
            EnvironmentState st = states.get(uuid);
            String town = towns.get(uuid);
            Map<String, BuildingState> bMap = buildingStates.get(uuid);
            removeMemberData(partner, town, st, bMap);
            invalidateTownChunks(uuid);
        }

        cancelTasks(uuid);
        fakeBlockManager.clear(player);
        EnvironmentState st = states.remove(uuid);
        String town = towns.remove(uuid);
        Location origin = origins.remove(uuid);
        Map<String, BuildingState> bMap = buildingStates.remove(uuid);
        removeAllBuildingHolograms(uuid);
        if (town != null && st != null) {
            stageManager.despawnForStage(uuid, town, st.level, st.stage);
            if (bMap != null) {
                for (var e : bMap.entrySet()) {
                    buildingStageManager.despawnForStage(uuid, e.getKey(), e.getValue().stage);
                }
            }
        }
        java.util.Set<Long> loaded = loadedChunks.remove(uuid);
        if (loaded != null && origin != null) {
            for (long key : loaded) {
                int cx = (int) (key >> 32);
                int cz = (int) key;
                org.bukkit.Chunk chunk = origin.getWorld().getChunkAt(cx, cz);
                chunk.removePluginChunkTicket(Main.getInstance());
            }
        }
        playerConfig.clearEnvironmentData(uuid);
        playerConfig.clearCoop(uuid);
        playerConfig.saveConfigFile();
        player.sendMessage(ChatColor.RED + "Your settlement has been reset.");
        invalidateTownChunks(uuid);
    }

    /** Remove all fake blocks and NPCs for this player's view without deleting data. */
    public void unloadPlayerTown(Player player) {
        UUID base = getBase(player.getUniqueId());
        cancelTasks(base);
        EnvironmentState st = states.get(base);
        String town = towns.get(base);
        Location origin = origins.get(base);
        Map<String, BuildingState> bMap = buildingStates.get(base);
        if (town != null && st != null && origin != null) {
            stageManager.despawnForStage(player.getUniqueId(), town, st.level, st.stage);
            if (bMap != null) {
                for (var e : bMap.entrySet()) {
                    buildingStageManager.despawnForStage(player.getUniqueId(), e.getKey(), e.getValue().stage);
                }
            }
        }
        removeAllBuildingHolograms(player.getUniqueId());
        fakeBlockManager.clear(player);
        blockPriorities.remove(player.getUniqueId());
        java.util.Set<Long> loaded = loadedChunks.remove(base);
        if (loaded != null) {
            for (long key : loaded) {
                int cx = (int) (key >> 32);
                int cz = (int) key;
                org.bukkit.Chunk chunk = origin.getWorld().getChunkAt(cx, cz);
                chunk.removePluginChunkTicket(Main.getInstance());
                // allow chunk to unload naturally
            }
        }
    }

    /**
     * Spawn the structure for the given player and stage with a simple build
     * animation and sound effects.
     */
    private void spawnStructureTimed(Player player, Location origin, int level, int stage, Runnable after, int totalTime) {
        UUID uuid = player.getUniqueId();
        cancelBuildTask(uuid, TOWN_TASK_KEY);

        fakeBlockManager.clear(player);
        String town = towns.get(player.getUniqueId());
        if (town == null) return;
        var stageData = stageManager.getStage(town, level, stage);
        if (stageData == null) return;
        // Adjust origin so Y is based on the stage's recorded offset
        Location baseOrigin = origin.clone().add(0, stageData.oy, 0);

        java.util.List<TownStageManager.BlockDef> blocks = new java.util.ArrayList<>(stageData.blocks);
        blocks.sort(java.util.Comparator.comparingInt(b -> b.y));

        int calc = Math.max(1, blocks.size() / totalTime);
        final int blocksPerTick = Math.min(MAX_BLOCKS_PER_TICK, calc);

        java.util.Random rand = new java.util.Random();
        Sound[] breakSounds = { Sound.BLOCK_STONE_BREAK, Sound.BLOCK_DEEPSLATE_BREAK, Sound.BLOCK_WOOD_BREAK };
        Sound[] placeSounds = { Sound.BLOCK_STONE_PLACE, Sound.BLOCK_DEEPSLATE_PLACE, Sound.BLOCK_WOOD_PLACE };

        BukkitTask task = new BukkitRunnable() {
            int index = 0;
            @Override public void run() {
                if (!player.isOnline()) { cancel(); return; }
                Map<Location, org.bukkit.block.data.BlockData> batch = new java.util.HashMap<>();
                for (int i = 0; i < blocksPerTick && index < blocks.size(); i++, index++) {
                    TownStageManager.BlockDef b = blocks.get(index);
                    Location loc = baseOrigin.clone().add(b.x - stageData.ox, b.y - stageData.oy, b.z - stageData.oz);
                    batch.put(loc, b.data);
                    Sound breakS = breakSounds[rand.nextInt(breakSounds.length)];
                    Sound placeS = placeSounds[rand.nextInt(placeSounds.length)];
                    player.getWorld().playSound(loc, breakS, 0.7f, 1f);
                    player.getWorld().playSound(loc, placeS, 0.7f, 1f);
                }
                fakeBlockManager.showFakeBlocks(player, batch);
                if (index >= blocks.size()) {
                    player.playSound(baseOrigin, Sound.BLOCK_ANVIL_USE, 1f, 1f);
                    stageManager.spawnForStage(player, town, level, stage, baseOrigin);
                    if (after != null) after.run();
                    clearFinishedTask(uuid, TOWN_TASK_KEY);
                    cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);

        buildTasks.computeIfAbsent(uuid, k -> new java.util.HashMap<>())
            .put(TOWN_TASK_KEY, task);
    }

    private void spawnStructure(Player player, Location origin, int level, int stage, Runnable after) {
        // run the build animation over ~6 seconds
        spawnStructureTimed(player, origin, level, stage, after, 6 * 20);
    }

    private void spawnStructure(Player player, Location origin, int level, int stage) {
        spawnStructure(player, origin, level, stage, null);
    }

    private void spawnStructureQuick(Player player, Location origin, int level, int stage) {
        spawnStructureTimed(player, origin, level, stage, null, 20);
    }

    /**
     * Spawn the structure instantly without any build animation. Used when
     * reloading chunks so the player still sees the correct town stage.
     */
    private void spawnStructureInstant(Player player, Location origin, int level, int stage) {
        UUID uuid = player.getUniqueId();
        String town = towns.get(uuid);
        if (town == null) return;

        UUID base = getBase(uuid);
        pauseChunkTasks(base);
        var stageData = stageManager.getStage(town, level, stage);
        if (stageData == null) return;
        Location baseOrigin = origin.clone().add(0, stageData.oy, 0);

        Map<Location, org.bukkit.block.data.BlockData> batch = new java.util.HashMap<>();
        Map<String, Integer> priMap = blockPriorities.computeIfAbsent(uuid, k -> new java.util.HashMap<>());
        for (var b : stageData.blocks) {
            Location loc = baseOrigin.clone().add(b.x - stageData.ox, b.y - stageData.oy, b.z - stageData.oz);
            batch.put(loc, b.data);
            priMap.put(key(loc), stageData.priority);
        }
        fakeBlockManager.showFakeBlocks(player, batch);
        stageManager.spawnForStage(player, town, level, stage, baseOrigin);
        resumeChunkTasks(player);
    }

    /**
     * Resend fake blocks for the main structure inside a specific chunk.
     */
    private void resendStructureForChunk(Player player, Location origin, int level, int stage,
                                         int cx, int cz) {
        UUID uuid = player.getUniqueId();
        String town = towns.get(uuid);
        if (town == null) return;
        var stageData = stageManager.getStage(town, level, stage);
        if (stageData == null) return;

        Map<Long, Map<Location, org.bukkit.block.data.BlockData>> chunkMap = townChunkBlocks.get(getBase(uuid));
        if (chunkMap == null) return;
        long cKey = (((long) cx) << 32) ^ (cz & 0xffffffffL);
        Map<Location, org.bukkit.block.data.BlockData> blocks = chunkMap.get(cKey);
        if (blocks == null || blocks.isEmpty()) return;

        Map<Location, org.bukkit.block.data.BlockData> batch = new java.util.HashMap<>();
        Map<String, Integer> priMap = blockPriorities.computeIfAbsent(uuid, k -> new java.util.HashMap<>());
        for (var e : blocks.entrySet()) {
            batch.put(e.getKey(), e.getValue());
            priMap.put(key(e.getKey()), stageData.priority);
        }
        fakeBlockManager.showFakeBlocks(player, batch);
    }

    /**
     * Upgrade the main town structure with a progressive build animation
     * instead of swapping blocks instantly.
     */
    private void spawnStructureUpgrade(Player player, Location origin,
                                       int oldLevel, int oldStage,
                                       int newLevel, int newStage) {
        UUID uuid = player.getUniqueId();
        cancelBuildTask(uuid, TOWN_TASK_KEY);

        UUID base = getBase(uuid);
        pauseChunkTasks(base);

        String town = towns.get(uuid);
        if (town == null) return;

        var newData = stageManager.getStage(town, newLevel, newStage);
        if (newData == null) return;
        var oldData = stageManager.getStage(town, oldLevel, oldStage);

        // Adjust origin so Y is based on each stage's stored offset
        Location newOrigin = origin.clone().add(0, newData.oy, 0);
        Location oldOrigin = origin.clone();
        if (oldData != null) oldOrigin.add(0, oldData.oy, 0);

        class Change { Location loc; org.bukkit.block.data.BlockData data; Change(Location l, org.bukkit.block.data.BlockData d){this.loc=l;this.data=d;} }
        java.util.List<Change> changes = new java.util.ArrayList<>();
        java.util.Set<String> newKeys = new java.util.HashSet<>();

        for (var b : newData.blocks) {
            Location loc = newOrigin.clone().add(b.x - newData.ox, b.y - newData.oy, b.z - newData.oz);
            String locKey = loc.getBlockX()+":"+loc.getBlockY()+":"+loc.getBlockZ();
            newKeys.add(locKey);
            changes.add(new Change(loc, b.data));
        }

        if (oldData != null) {
            var air = org.bukkit.Bukkit.createBlockData(org.bukkit.Material.AIR);
            for (var b : oldData.blocks) {
                Location loc = oldOrigin.clone().add(b.x - oldData.ox, b.y - oldData.oy, b.z - oldData.oz);
                String locKey = loc.getBlockX()+":"+loc.getBlockY()+":"+loc.getBlockZ();
                if (!newKeys.contains(locKey)) {
                    changes.add(new Change(loc, air));
                }
            }
        }

        // sort changes bottom-up for a nicer effect
        changes.sort(java.util.Comparator.comparingInt(c -> c.loc.getBlockY()));

        // play upgrade animation over ~6 seconds
        final int totalTime = 6 * 20; // 6 seconds in ticks
        int calc2 = Math.max(1, changes.size() / totalTime);
        final int blocksPerTick = Math.min(MAX_BLOCKS_PER_TICK, calc2);

        java.util.Random rand = new java.util.Random();
        Sound[] breakSounds = { Sound.BLOCK_STONE_BREAK, Sound.BLOCK_DEEPSLATE_BREAK, Sound.BLOCK_WOOD_BREAK };
        Sound[] placeSounds = { Sound.BLOCK_STONE_PLACE, Sound.BLOCK_DEEPSLATE_PLACE, Sound.BLOCK_WOOD_PLACE };
        Map<String, Integer> priMap = blockPriorities.computeIfAbsent(uuid, k -> new HashMap<>());

        BukkitTask task = new BukkitRunnable() {
            int index = 0;
            @Override public void run() {
                if (!player.isOnline()) { cancel(); return; }
                Map<Location, org.bukkit.block.data.BlockData> batch = new java.util.HashMap<>();
                for (int i = 0; i < blocksPerTick && index < changes.size(); i++, index++) {
                    Change c = changes.get(index);
                    String k = key(c.loc);
                    int exist = priMap.getOrDefault(k, Integer.MIN_VALUE);
                    int newPr = newData.priority;
                    if (exist > newPr && priMap.containsKey(k)) continue;
                    batch.put(c.loc, c.data);
                    if (c.data.getMaterial() == org.bukkit.Material.AIR) {
                        priMap.remove(k);
                    } else {
                        priMap.put(k, newPr);
                    }
                    Sound breakS = breakSounds[rand.nextInt(breakSounds.length)];
                    Sound placeS = placeSounds[rand.nextInt(placeSounds.length)];
                    player.getWorld().playSound(c.loc, breakS, 0.7f, 1f);
                    player.getWorld().playSound(c.loc, placeS, 0.7f, 1f);
                }
                fakeBlockManager.showFakeBlocks(player, batch);
                if (index >= changes.size()) {
                    player.playSound(newOrigin, Sound.BLOCK_ANVIL_USE, 1f, 1f);
                    stageManager.spawnForStage(player, town, newLevel, newStage, newOrigin);
                    resumeChunkTasks(player);
                    cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);

        buildTasks.computeIfAbsent(uuid, k -> new java.util.HashMap<>())
            .put(TOWN_TASK_KEY, task);
    }

    /** Spawn a specific building stage relative to the town origin. */
    private void spawnBuildingTimed(Player player, String building, Location origin, int stage, Runnable after, int totalTime) {
        UUID uuid = player.getUniqueId();
        String key = building.toLowerCase();
        cancelBuildTask(uuid, key);
        removeTownHologram(uuid, building);

        String town = towns.get(player.getUniqueId());
        if (town == null) return;
        var stageData = buildingStageManager.getStage(building, stage);
        if (stageData == null) return;
        // Adjust origin so Y is based on the stage's recorded offset
        Location baseOrigin = origin.clone().add(0, stageData.oy, 0);

        java.util.List<BuildingStageManager.BlockDef> blocks = new java.util.ArrayList<>(stageData.blocks);
        blocks.sort(java.util.Comparator.comparingInt(b -> b.y));

        int calc3 = Math.max(1, blocks.size() / totalTime);
        final int blocksPerTick = Math.min(MAX_BLOCKS_PER_TICK, calc3);

        java.util.Random rand = new java.util.Random();
        Sound[] breakSounds = { Sound.BLOCK_STONE_BREAK, Sound.BLOCK_DEEPSLATE_BREAK, Sound.BLOCK_WOOD_BREAK };
        Sound[] placeSounds = { Sound.BLOCK_STONE_PLACE, Sound.BLOCK_DEEPSLATE_PLACE, Sound.BLOCK_WOOD_PLACE };

        Map<String, Integer> priMap = blockPriorities.computeIfAbsent(uuid, k -> new HashMap<>());

        BukkitTask task = new BukkitRunnable() {
            int index = 0;
            @Override public void run() {
                if (!player.isOnline()) { cancel(); return; }
                Map<Location, org.bukkit.block.data.BlockData> batch = new java.util.HashMap<>();
                for (int i = 0; i < blocksPerTick && index < blocks.size(); i++, index++) {
                    BuildingStageManager.BlockDef b = blocks.get(index);
                    Location loc = baseOrigin.clone().add(
                        b.x - stageData.ox,
                        b.y - stageData.oy,
                        b.z - stageData.oz);
                    String k = key(loc);
                    int exist = priMap.getOrDefault(k, Integer.MIN_VALUE);
                    if (exist > stageData.priority && priMap.containsKey(k)) continue;
                    batch.put(loc, b.data);
                    priMap.put(k, stageData.priority);
                    Sound breakS = breakSounds[rand.nextInt(breakSounds.length)];
                    Sound placeS = placeSounds[rand.nextInt(placeSounds.length)];
                    player.getWorld().playSound(loc, breakS, 0.7f, 1f);
                    player.getWorld().playSound(loc, placeS, 0.7f, 1f);
                }
                fakeBlockManager.showFakeBlocks(player, batch);
                if (index >= blocks.size()) {
                    player.playSound(baseOrigin, Sound.BLOCK_ANVIL_USE, 1f, 1f);
                    buildingStageManager.spawnForStage(player, building, stage, baseOrigin);
                    // Place the hologram where the stage was defined (+1 Y already stored)
                    Location holo = baseOrigin.clone().add(
                        stageData.hx - stageData.ox + 0.5,
                        stageData.hy - stageData.oy + 2,
                        stageData.hz - stageData.oz + 0.5);
                    java.util.List<String> textLines = formatBuildingHologram(player, building, stage);
                    if (textLines != null && !textLines.isEmpty()) {
                        java.util.List<org.bukkit.entity.Entity> displays = spawnHologramLines(player, holo, textLines, building);
                        buildingHolograms.computeIfAbsent(uuid, k -> new java.util.HashMap<>())
                            .put(building.toLowerCase(), displays);
                    }
                    if (after != null) after.run();
                    clearFinishedTask(uuid, key);
                    cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);

        buildTasks.computeIfAbsent(uuid, k -> new java.util.HashMap<>())
            .put(key, task);
    }

    private void spawnBuilding(Player player, String building, Location origin, int stage, Runnable after) {
        // building construction animation lasts ~6 seconds
        spawnBuildingTimed(player, building, origin, stage, after, 6 * 20);
    }

    private void spawnBuilding(Player player, String building, Location origin, int stage) {
        spawnBuilding(player, building, origin, stage, null);
    }

    private void spawnBuildingQuick(Player player, String building, Location origin, int stage) {
        spawnBuildingTimed(player, building, origin, stage, null, 20);
    }

    /**
     * Spawn a building stage instantly without animation. Used when chunks are
     * reloaded so players continue to see their buildings.
     */
    private void spawnBuildingInstant(Player player, String building, Location origin, int stage) {
        UUID uuid = player.getUniqueId();
        String key = building.toLowerCase();
        cancelBuildTask(uuid, key);
        removeTownHologram(uuid, building);

        UUID base = getBase(uuid);
        pauseChunkTasks(base);

        String town = towns.get(uuid);
        if (town == null) return;
        var stageData = buildingStageManager.getStage(building, stage);
        if (stageData == null) return;
        Location baseOrigin = origin.clone().add(0, stageData.oy, 0);

        Map<Location, org.bukkit.block.data.BlockData> batch = new java.util.HashMap<>();
        Map<String, Integer> priMap = blockPriorities.computeIfAbsent(uuid, k -> new java.util.HashMap<>());
        for (var b : stageData.blocks) {
            Location loc = baseOrigin.clone().add(b.x - stageData.ox, b.y - stageData.oy, b.z - stageData.oz);
            batch.put(loc, b.data);
            priMap.put(key(loc), stageData.priority);
        }
        fakeBlockManager.showFakeBlocks(player, batch);
        buildingStageManager.spawnForStage(player, building, stage, baseOrigin);
        resumeChunkTasks(player);

        Location holo = baseOrigin.clone().add(
            stageData.hx - stageData.ox + 0.5,
            stageData.hy - stageData.oy + 2,
            stageData.hz - stageData.oz + 0.5);
        java.util.List<String> textLines = formatBuildingHologram(player, building, stage);
        if (textLines != null && !textLines.isEmpty()) {
            java.util.List<org.bukkit.entity.Entity> displays = spawnHologramLines(player, holo, textLines, building);
            buildingHolograms.computeIfAbsent(uuid, k -> new java.util.HashMap<>())
                .put(building.toLowerCase(), displays);
        }
    }

    /**
     * Resend fake blocks for a building only within the specified chunk.
     */
    private void resendBuildingForChunk(Player player, String building, Location origin,
                                        int stage, int cx, int cz) {
        UUID uuid = player.getUniqueId();
        var stageData = buildingStageManager.getStage(building, stage);
        if (stageData == null) return;

        Map<String, Map<Long, Map<Location, org.bukkit.block.data.BlockData>>> pMap = buildingChunkBlocks.get(getBase(uuid));
        if (pMap == null) return;
        Map<Long, Map<Location, org.bukkit.block.data.BlockData>> chunkMap = pMap.get(building.toLowerCase());
        if (chunkMap == null) return;
        long cKey = (((long) cx) << 32) ^ (cz & 0xffffffffL);
        Map<Location, org.bukkit.block.data.BlockData> blocks = chunkMap.get(cKey);
        if (blocks == null || blocks.isEmpty()) return;

        Map<Location, org.bukkit.block.data.BlockData> batch = new java.util.HashMap<>();
        Map<String, Integer> priMap = blockPriorities.computeIfAbsent(uuid, k -> new java.util.HashMap<>());
        for (var e : blocks.entrySet()) {
            batch.put(e.getKey(), e.getValue());
            priMap.put(key(e.getKey()), stageData.priority);
        }
        fakeBlockManager.showFakeBlocks(player, batch);
    }

    /**
     * Upgrade a building with a progressive build animation rather than
     * swapping blocks instantly.
     */
    private void spawnBuildingUpgrade(Player player, String building, Location origin,
                                      int oldStage, int newStage) {
        spawnBuildingUpgrade(player, building, origin, oldStage, newStage, null);
    }

    private void spawnBuildingUpgrade(Player player, String building, Location origin,
                                      int oldStage, int newStage,
                                      Runnable after) {
        UUID uuid = player.getUniqueId();
        String key = building.toLowerCase();
        cancelBuildTask(uuid, key);
        removeTownHologram(uuid, building);

        UUID base = getBase(uuid);
        pauseChunkTasks(base);

        String town = towns.get(uuid);
        if (town == null) return;

        var newData = buildingStageManager.getStage(building, newStage);
        if (newData == null) return;
        var oldData = buildingStageManager.getStage(building, oldStage);

        // Adjust origin so Y is based on each stage's stored offset
        Location newOrigin = origin.clone().add(0, newData.oy, 0);
        Location oldOrigin = origin.clone();
        if (oldData != null) oldOrigin.add(0, oldData.oy, 0);

        class Change { Location loc; org.bukkit.block.data.BlockData data; Change(Location l, org.bukkit.block.data.BlockData d){this.loc=l;this.data=d;} }
        java.util.List<Change> changes = new java.util.ArrayList<>();
        java.util.Set<String> newKeys = new java.util.HashSet<>();

        for (var b : newData.blocks) {
            Location loc = newOrigin.clone().add(b.x - newData.ox, b.y - newData.oy, b.z - newData.oz);
            String locKey = loc.getBlockX()+":"+loc.getBlockY()+":"+loc.getBlockZ();
            newKeys.add(locKey);
            changes.add(new Change(loc, b.data));
        }

        if (oldData != null) {
            var air = org.bukkit.Bukkit.createBlockData(org.bukkit.Material.AIR);
            for (var b : oldData.blocks) {
                Location loc = oldOrigin.clone().add(b.x - oldData.ox, b.y - oldData.oy, b.z - oldData.oz);
                String locKey = loc.getBlockX()+":"+loc.getBlockY()+":"+loc.getBlockZ();
                if (!newKeys.contains(locKey)) {
                    changes.add(new Change(loc, air));
                }
            }
        }

        // sort bottom-up for nicer effect
        changes.sort(java.util.Comparator.comparingInt(c -> c.loc.getBlockY()));

        // build upgrade animation runs for ~6 seconds
        final int totalTime = 6 * 20; // 6 seconds in ticks
        int calc4 = Math.max(1, changes.size() / totalTime);
        final int blocksPerTick = Math.min(MAX_BLOCKS_PER_TICK, calc4);

        java.util.Random rand = new java.util.Random();
        Sound[] breakSounds = { Sound.BLOCK_STONE_BREAK, Sound.BLOCK_DEEPSLATE_BREAK, Sound.BLOCK_WOOD_BREAK };
        Sound[] placeSounds = { Sound.BLOCK_STONE_PLACE, Sound.BLOCK_DEEPSLATE_PLACE, Sound.BLOCK_WOOD_PLACE };

        Map<String, Integer> priMap = blockPriorities.computeIfAbsent(uuid, k -> new HashMap<>());

        BukkitTask task = new BukkitRunnable() {
            int index = 0;
            @Override public void run() {
                if (!player.isOnline()) { cancel(); return; }
                Map<Location, org.bukkit.block.data.BlockData> batch = new java.util.HashMap<>();
                for (int i = 0; i < blocksPerTick && index < changes.size(); i++, index++) {
                    Change c = changes.get(index);
                    String k = key(c.loc);
                    int exist = priMap.getOrDefault(k, Integer.MIN_VALUE);
                    int newPr = newData.priority;
                    if (exist > newPr && priMap.containsKey(k)) continue;
                    batch.put(c.loc, c.data);
                    if (c.data.getMaterial() == org.bukkit.Material.AIR) {
                        priMap.remove(k);
                    } else {
                        priMap.put(k, newPr);
                    }
                    Sound breakS = breakSounds[rand.nextInt(breakSounds.length)];
                    Sound placeS = placeSounds[rand.nextInt(placeSounds.length)];
                    player.getWorld().playSound(c.loc, breakS, 0.7f, 1f);
                    player.getWorld().playSound(c.loc, placeS, 0.7f, 1f);
                }
                fakeBlockManager.showFakeBlocks(player, batch);
                if (index >= changes.size()) {
                    player.playSound(newOrigin, Sound.BLOCK_ANVIL_USE, 1f, 1f);
                    buildingStageManager.spawnForStage(player, building, newStage, newOrigin);
                    Location holo = newOrigin.clone().add(
                        newData.hx - newData.ox + 0.5,
                        newData.hy - newData.oy + 2,
                        newData.hz - newData.oz + 0.5);
                    java.util.List<String> textLines = formatBuildingHologram(player, building, newStage);
                    if (textLines != null && !textLines.isEmpty()) {
                        java.util.List<org.bukkit.entity.Entity> displays = spawnHologramLines(player, holo, textLines, building);
                        buildingHolograms.computeIfAbsent(uuid, k -> new java.util.HashMap<>())
                            .put(building.toLowerCase(), displays);
                    }
                    if (after != null) after.run();
                    resumeChunkTasks(player);
                    clearFinishedTask(uuid, key);
                    cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);

        buildTasks.computeIfAbsent(uuid, k -> new java.util.HashMap<>())
            .put(key, task);
    }

    /** Remove any fake blocks from a previous building stage before upgrading. */
    private void clearBuildingStage(Player player, String building, Location origin, int stage) {
        var st = buildingStageManager.getStage(building, stage);
        if (st == null) return;
        Location baseOrigin = origin.clone().add(0, st.oy, 0);
        java.util.List<Location> locs = new java.util.ArrayList<>();
        for (var b : st.blocks) {
            Location l = baseOrigin.clone().add(b.x - st.ox, b.y - st.oy, b.z - st.oz);
            locs.add(l);
            Map<String, Integer> priMap = blockPriorities.get(player.getUniqueId());
            if (priMap != null) priMap.remove(key(l));
        }
        fakeBlockManager.hideFakeBlocks(player, locs);
    }

    /** Remove fake blocks from a previous town stage before upgrading. */
    private void clearTownStage(Player player, String town, Location origin, int level, int stage) {
        var st = stageManager.getStage(town, level, stage);
        if (st == null) return;
        Location baseOrigin = origin.clone().add(0, st.oy, 0);
        java.util.List<Location> locs = new java.util.ArrayList<>();
        for (var b : st.blocks) {
            Location l = baseOrigin.clone().add(b.x - st.ox, b.y - st.oy, b.z - st.oz);
            locs.add(l);
            Map<String, Integer> priMap = blockPriorities.get(player.getUniqueId());
            if (priMap != null) priMap.remove(key(l));
        }
        fakeBlockManager.hideFakeBlocks(player, locs);
    }

    // ----- Coop management -----

    public void invite(Player owner, Player target) {
        UUID ownerId = owner.getUniqueId();
        if (coopOwners.containsKey(ownerId)) {
            owner.sendMessage(ChatColor.RED + "You are not the town owner.");
            return;
        }
        if (!origins.containsKey(ownerId)) {
            owner.sendMessage(ChatColor.RED + "You don't have a town.");
            return;
        }
        if (coopPartners.containsKey(ownerId)) {
            owner.sendMessage(ChatColor.RED + "You already have a partner.");
            return;
        }

        UUID targetId = target.getUniqueId();
        pendingInvites.put(targetId, ownerId);
        owner.sendMessage(ChatColor.GREEN + "Invited " + target.getName() + " to your town.");
        target.sendMessage(ChatColor.YELLOW + owner.getName() + " has invited you to join their town. Type /town accept or /town deny.");
    }

    public void accept(Player player) {
        UUID playerId = player.getUniqueId();
        UUID ownerId = pendingInvites.remove(playerId);
        if (ownerId == null) {
            player.sendMessage(ChatColor.RED + "You have no pending town invites.");
            return;
        }
        if (origins.containsKey(playerId) || coopOwners.containsKey(playerId) || coopPartners.containsKey(playerId)) {
            player.sendMessage(ChatColor.RED + "You must /town reset before joining another town.");
            pendingInvites.put(playerId, ownerId); // keep invite
            return;
        }
        coopOwners.put(playerId, ownerId);
        coopPartners.put(ownerId, playerId);
        shareData(playerId, ownerId);
        playerConfig.setCoopOwner(playerId, ownerId);
        playerConfig.setCoopPartner(ownerId, playerId);
        playerConfig.saveConfigFile();
        initializePlayer(player);
        Player owner = Bukkit.getPlayer(ownerId);
        if (owner != null) owner.sendMessage(ChatColor.GREEN + player.getName() + " joined your town.");
        player.sendMessage(ChatColor.GREEN + "You joined " + (owner != null ? owner.getName() : "the owner") + "'s town!");
    }

    public void deny(Player player) {
        UUID playerId = player.getUniqueId();
        UUID ownerId = pendingInvites.remove(playerId);
        if (ownerId == null) {
            player.sendMessage(ChatColor.RED + "You have no pending town invites.");
            return;
        }
        Player owner = Bukkit.getPlayer(ownerId);
        if (owner != null) owner.sendMessage(ChatColor.RED + player.getName() + " declined your town invite.");
        player.sendMessage(ChatColor.RED + "You declined the town invite.");
    }

    public void kick(Player owner, Player target) {
        UUID ownerId = owner.getUniqueId();
        UUID partner = coopPartners.get(ownerId);
        if (partner == null || !partner.equals(target.getUniqueId())) {
            owner.sendMessage(ChatColor.RED + "That player is not your partner.");
            return;
        }
        EnvironmentState st = states.get(ownerId);
        String town = towns.get(ownerId);
        Map<String, BuildingState> bMap = buildingStates.get(ownerId);
        removeMemberData(partner, town, st, bMap);
        coopPartners.remove(ownerId);
        owner.sendMessage(ChatColor.RED + "Removed " + target.getName() + " from the town.");
        Player tp = Bukkit.getPlayer(partner);
        if (tp != null) tp.sendMessage(ChatColor.RED + "You were removed from the town.");
    }

    public void leave(Player player) {
        resetTown(player);
    }

    /**
     * Resend the player's town and building fake blocks for a specific chunk.
     * Only blocks within the loaded chunk are resent to avoid triggering
     * additional chunk loads.
     */
    public void handleChunkLoad(Player player, org.bukkit.Chunk chunk) {
        UUID base = getBase(player.getUniqueId());
        EnvironmentState st = states.get(base);
        Location origin = origins.get(base);
        if (st == null || origin == null) return;
        if (chunkPaused.contains(base)) return;
        int cx = chunk.getX();
        int cz = chunk.getZ();

        long key = (((long) cx) << 32) ^ (cz & 0xffffffffL);

        // mark this chunk as loaded for retry tracking
        java.util.Set<Long> pending = pendingChunkLoads.get(base);
        if (pending != null) {
            pending.remove(key);
            java.util.Map<Long, Integer> tries = chunkRetryCount.get(base);
            if (tries != null) tries.remove(key);
        }

        // keep the chunk loaded if it is part of the town
        if (collectTownChunks(base).contains(key)) {
            chunk.addPluginChunkTicket(Main.getInstance());
            loadedChunks.computeIfAbsent(base, k -> new java.util.HashSet<>()).add(key);
        }

        // skip resending blocks if a build animation is active to avoid
        // overriding the visual effect
        if (!buildTasks.containsKey(base) || buildTasks.get(base).isEmpty()) {
            // resend structure blocks inside the chunk
            resendStructureForChunk(player, origin, st.level, st.stage, cx, cz);

            Map<String, BuildingState> bMap = buildingStates.get(base);
            if (bMap != null) {
                for (var e : bMap.entrySet()) {
                    Location bOrigin = getBuildingOrigin(towns.get(base), e.getKey(), origin);
                    resendBuildingForChunk(player, e.getKey(), bOrigin, e.getValue().stage, cx, cz);
                }
            }

            // Ensure any missing structures are respawned once the chunk is ready
            startTownLoadCheck(player);
        }
    }

    public void transfer(Player owner, Player newOwner) {
        UUID ownerId = owner.getUniqueId();
        UUID partner = coopPartners.get(ownerId);
        if (partner == null || !partner.equals(newOwner.getUniqueId())) {
            owner.sendMessage(ChatColor.RED + "That player is not your partner.");
            return;
        }
        // Move data
        EnvironmentState st = states.remove(ownerId);
        states.put(partner, st);
        Map<String, BuildingState> bMap = buildingStates.remove(ownerId);
        if (bMap != null) buildingStates.put(partner, bMap);
        Location origin = origins.remove(ownerId);
        if (origin != null) origins.put(partner, origin);
        String town = towns.remove(ownerId);
        if (town != null) towns.put(partner, town);

        invalidateTownChunks(ownerId);
        invalidateTownChunks(partner);

        coopOwners.put(ownerId, partner);
        coopPartners.remove(ownerId);
        coopPartners.put(partner, ownerId);

        playerConfig.clearEnvironmentData(ownerId);
        playerConfig.setCoopOwner(ownerId, partner);
        playerConfig.setCoopPartner(partner, ownerId);
        playerConfig.setEnvironmentOrigin(partner, origin);
        playerConfig.setEnvironmentTown(partner, town);
        if (bMap != null) {
            for (var e : bMap.entrySet()) {
                playerConfig.setBuildingStage(partner, e.getKey(), e.getValue().stage);
            }
        }
        playerConfig.setEnvironmentState(partner, st.level, st.stage);
        playerConfig.saveConfigFile();

        owner.sendMessage(ChatColor.GREEN + "Transferred town ownership to " + newOwner.getName() + ".");
        newOwner.sendMessage(ChatColor.GREEN + "You are now the town owner.");
    }

    public void sendInfo(Player player) {
        UUID base = getBase(player.getUniqueId());
        EnvironmentState st = states.get(base);
        String town = towns.get(base);
        UUID ownerId = base;
        UUID partner = coopPartners.get(base);
        player.sendMessage(ChatColor.YELLOW + "Town: " + (town != null ? town : "None"));
        if (st != null) {
            player.sendMessage(ChatColor.YELLOW + "Level " + st.level + " Stage " + st.stage);
        }
        player.sendMessage(ChatColor.YELLOW + "Owner: " + Bukkit.getOfflinePlayer(ownerId).getName());
        if (partner != null) {
            player.sendMessage(ChatColor.YELLOW + "Partner: " + Bukkit.getOfflinePlayer(partner).getName());
        }
    }
}