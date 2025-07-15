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
    public static final int MAX_LEVEL = 3;
    private static final int STAGES_PER_LEVEL = 3;
    private final PlayerConfig playerConfig;
    private final TownStageManager stageManager;
    private final me.nakilex.levelplugin.environment.stage.BuildingStageManager buildingStageManager;
    private final FakeBlockManager fakeBlockManager;
    private final Map<UUID, EnvironmentState> states = new HashMap<>();
    private final Map<UUID, Location> origins = new HashMap<>();
    private final Map<UUID, String> towns = new HashMap<>();
    private final Map<UUID, Map<String, EnvironmentState>> buildingStates = new HashMap<>();
    private final Map<UUID, java.util.List<BukkitTask>> buildTasks = new HashMap<>();
    /** Hologram lines per building per player. */
    private final Map<UUID, Map<String, java.util.List<org.bukkit.entity.TextDisplay>>> buildingHolograms = new HashMap<>();
    private final Map<UUID, UUID> coopOwners = new HashMap<>();
    private final Map<UUID, UUID> coopPartners = new HashMap<>();
    private final Map<UUID, UUID> pendingInvites = new HashMap<>();
    /** Track placed block priorities for each player (location key -> priority). */
    private final Map<UUID, Map<String, Integer>> blockPriorities = new HashMap<>();
    /** Keep track of chunks force-loaded for each player so they can be released later. */
    private final Map<UUID, java.util.Set<Long>> loadedChunks = new java.util.HashMap<>();
    /** Repeating tasks that ensure chunks finish loading. */
    private final Map<UUID, BukkitTask> chunkLoadTasks = new java.util.HashMap<>();

    /** Players currently viewing their town (fake blocks active). */
    private final java.util.Set<UUID> loadedPlayers = new java.util.HashSet<>();

    /** Players that already saw the initial build animation. */
    private final java.util.Set<UUID> playedInitAnimation = new java.util.HashSet<>();

    private static String key(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
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

    /** Build the hologram text for a building upgrade based on the player's
     *  current resources. */
    private java.util.List<String> formatBuildingHologram(Player player, String building, int level, int stage) {
        int nextLevel = level;
        int nextStage = stage + 1;
        if (nextStage > STAGES_PER_LEVEL) {
            nextStage = 1;
            nextLevel++;
        }

        // Example requirements - currently hardcoded to 1 oak log and no coins
        int logCost = 1;
        boolean hasLog = player.getInventory().containsAtLeast(
                new org.bukkit.inventory.ItemStack(org.bukkit.Material.OAK_LOG, logCost), logCost);
        String logLine = (hasLog
                ? ChatColor.GREEN.toString() + "✔"
                : ChatColor.RED.toString() + "✘")
                + ChatColor.GRAY + " - " + ChatColor.WHITE + "Oak Log"
                + ChatColor.GRAY + " x" + ChatColor.WHITE + logCost;

        int coinCost = 0;
        int coins = Main.getInstance().getEconomyManager().getBalance(player);
        boolean hasCoins = coins >= coinCost;
        String coinLine = (hasCoins
                ? ChatColor.GREEN.toString() + "✔"
                : ChatColor.RED.toString() + "✘")
                + ChatColor.GRAY + " - " + ChatColor.WHITE + coinCost + " coins "
                + ChatColor.GOLD + "\u26C3";

        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Upgrade " + ChatColor.WHITE + building);
        lines.add(ChatColor.GOLD.toString() + ChatColor.BOLD + "STAGE "
                + ChatColor.YELLOW + stage + " "
                + ChatColor.GREEN + ">" + ChatColor.DARK_GREEN + ">"
                + ChatColor.GREEN + ">" + ChatColor.DARK_GREEN + "> "
                + ChatColor.GOLD + "STAGE " + ChatColor.YELLOW + nextStage);
        lines.add(ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH.toString() + "--------------------");
        lines.add(ChatColor.AQUA + "Requirements:");
        lines.add(logLine);
        lines.add(coinLine);
        return lines;
    }

    /** Spawn TextDisplay hologram lines at the given location. */
    private java.util.List<TextDisplay> spawnHologramLines(Player player, Location base, java.util.List<String> lines, String tag) {
        java.util.List<TextDisplay> displays = new java.util.ArrayList<>();
        double offset = 0.0;
        for (String text : lines) {
            Location loc = base.clone().add(0, offset, 0);
            TextDisplay td = (TextDisplay) base.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
            td.setBillboard(Display.Billboard.CENTER);
            td.setShadowRadius(0f);
            td.setShadowStrength(0f);
            td.setText(text);
            td.addScoreboardTag("building_hologram:" + tag.toLowerCase());
            displays.add(td);
            offset -= 0.25;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(player)) p.hideEntity(Main.getInstance(), td);
            }
        }
        return displays;
    }

    private UUID getBase(UUID uuid) {
        return coopOwners.getOrDefault(uuid, uuid);
    }

    private void shareData(UUID member, UUID owner) {
        states.put(member, states.get(owner));
        origins.put(member, origins.get(owner));
        towns.put(member, towns.get(owner));
        Map<String, EnvironmentState> map = buildingStates.get(owner);
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
            Map<String, EnvironmentState> map = new java.util.HashMap<>();
            for (String b : playerConfig.getStoredBuildings(uuid)) {
                int bl = playerConfig.getBuildingLevel(uuid, b);
                int bs = playerConfig.getBuildingStage(uuid, b);
                map.put(b.toLowerCase(), new EnvironmentState(bl, bs));
            }
            if (!map.isEmpty()) buildingStates.put(uuid, map);
        }
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
        loadPlayerState(player);
        preloadTownChunks(player);

        UUID uuid = player.getUniqueId();
        EnvironmentState es = states.get(uuid);
        Location origin = origins.get(uuid);
        if (origin != null) {
            Map<String, EnvironmentState> bMap = buildingStates.get(uuid);
            final Map<String, EnvironmentState> finalBMap = bMap == null ? null : new java.util.HashMap<>(bMap);
            final Location finalOrigin = origin;
            if (finalBMap != null) {
                for (var e : finalBMap.entrySet()) {
                    Location bOrig = getBuildingOrigin(towns.get(uuid), e.getKey(), finalOrigin);
                    spawnBuildingInstant(player, e.getKey(), bOrig, e.getValue().level, e.getValue().stage);
                }
            }
            spawnStructureInstant(player, finalOrigin, es.level, es.stage);
        }
    }

    /** Load player state and spawn their town with a short animation. */
    public void initializePlayerAnimated(Player player, int ticks) {
        loadPlayerState(player);
        preloadTownChunks(player);

        UUID uuid = player.getUniqueId();
        EnvironmentState es = states.get(uuid);
        Location origin = origins.get(uuid);
        if (origin != null) {
            Map<String, EnvironmentState> bMap = buildingStates.get(uuid);
            if (bMap != null) {
                for (var e : bMap.entrySet()) {
                    Location bOrig = getBuildingOrigin(towns.get(uuid), e.getKey(), origin);
                    spawnBuildingTimed(player, e.getKey(), bOrig, e.getValue().level, e.getValue().stage, null, ticks);
                }
            }
            spawnStructureTimed(player, origin, es.level, es.stage, null, ticks);
        }
    }

    public EnvironmentState getState(UUID uuid) {
        return states.get(uuid);
    }

    private void cancelTasks(UUID uuid) {
        java.util.List<BukkitTask> tasks = buildTasks.remove(uuid);
        if (tasks != null) {
            for (BukkitTask t : tasks) {
                t.cancel();
            }
        }
        BukkitTask chk = chunkLoadTasks.remove(uuid);
        if (chk != null) {
            chk.cancel();
        }
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

    private void removeMemberData(UUID member, String town, EnvironmentState st, Map<String, EnvironmentState> bMap) {
        cancelTasks(member);
        removeAllBuildingHolograms(member);
        stageManager.despawnForStage(member, town, st.level, st.stage);
        if (bMap != null) {
            for (var e : bMap.entrySet()) {
                buildingStageManager.despawnForStage(member, e.getKey(), e.getValue().level, e.getValue().stage);
            }
        }
        fakeBlockManager.clear(Bukkit.getPlayer(member));
        blockPriorities.remove(member);
        towns.remove(member);
        origins.remove(member);
        states.remove(member);
        buildingStates.remove(member);
        coopOwners.remove(member);
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
            Map<String, EnvironmentState> bMap = buildingStates.get(base);
            if (bMap != null) {
                for (var e : bMap.entrySet()) {
                    playerConfig.setBuildingState(base, e.getKey(), e.getValue().level, e.getValue().stage);
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
            Map<String, EnvironmentState> bMap = buildingStates.get(base);
            if (bMap != null && !bMap.isEmpty()) {
                for (var entry : bMap.entrySet()) {
                    EnvironmentState bs = entry.getValue();
                    if (bs.level < MAX_LEVEL || bs.stage < STAGES_PER_LEVEL) {
                        int oldL = bs.level;
                        int oldS = bs.stage;
                        advance(bs);
                        player.sendMessage(ChatColor.GREEN + "" + entry.getKey() + " upgraded to L" + bs.level + " S" + bs.stage);
                        String town = towns.get(base);
                        Location origin = origins.get(base);
                        if (town != null && origin != null) {
                            Location bOrig = getBuildingOrigin(town, entry.getKey(), origin);
                            buildingStageManager.despawnForStage(player.getUniqueId(), entry.getKey(), oldL, oldS);
                            spawnBuildingUpgrade(player, entry.getKey(), bOrig, oldL, oldS, bs.level, bs.stage);
                        }
                        Main.getInstance().getQuestManager().handleTownUpgrade(player);
                        saveState(base);
                        return;
                    }
                }
                // all buildings maxed -> upgrade town
            }
            int oldLevel = state.level;
            int oldStage = state.stage;
            advance(state);
            player.sendMessage(ChatColor.GREEN + "Settlement upgraded to Level " + state.level + " Stage " + state.stage + "!");
            String town = towns.get(base);
            Location origin = origins.get(base);
            if (town != null && origin != null) {
                stageManager.despawnForStage(player.getUniqueId(), town, oldLevel, oldStage);
                spawnStructureUpgrade(player, origin, oldLevel, oldStage, state.level, state.stage);
                // reset building progress for new level
                Map<String, EnvironmentState> reset = buildingStates.get(base);
                if (reset != null) {
                    for (var e : reset.values()) {
                        e.level = 1;
                        e.stage = 1;
                    }
                }
            }
            Main.getInstance().getQuestManager().handleTownUpgrade(player);
            saveState(base);
        } else {
            player.sendMessage(ChatColor.GREEN + "Invested " + amount + " oak log.");
        }
    }

    /** Invest materials towards upgrading a specific building. */
    public void investBuilding(Player player, String building, int amount) {
        loadPlayerState(player);
        UUID base = getBase(player.getUniqueId());
        Map<String, EnvironmentState> bMap = buildingStates.get(base);
        if (bMap == null) {
            player.sendMessage(ChatColor.RED + "You have no settlement buildings.");
            return;
        }
        EnvironmentState bs = bMap.get(building.toLowerCase());
        if (bs == null) {
            player.sendMessage(ChatColor.RED + "Unknown building.");
            return;
        }
        bs.invested += amount;
        if (bs.invested >= 1) {
            bs.invested = 0;
            int oldL = bs.level;
            int oldS = bs.stage;
            advance(bs);
            player.sendMessage(ChatColor.GREEN + building + " upgraded to L" + bs.level + " S" + bs.stage);
            String town = towns.get(base);
            Location origin = origins.get(base);
            if (town != null && origin != null) {
                Location bOrig = getBuildingOrigin(town, building, origin);
                buildingStageManager.despawnForStage(player.getUniqueId(), building, oldL, oldS);
                spawnBuildingUpgrade(player, building, bOrig, oldL, oldS, bs.level, bs.stage);
            }
            Main.getInstance().getQuestManager().handleTownUpgrade(player);
            saveState(base);
        } else {
            player.sendMessage(ChatColor.GREEN + "Invested " + amount + " oak log.");
        }
    }

    private void advance(EnvironmentState state) {
        state.stage++;
        if (state.stage > STAGES_PER_LEVEL) {
            state.stage = 1;
            if (state.level < MAX_LEVEL) {
                state.level++;
            } else {
                state.stage = STAGES_PER_LEVEL;
            }
        }
    }

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
     * are loaded. This prevents fake block updates from forcing asynchronous
     * chunk loads which can cause errors on some servers.
     */
    public void preloadTownChunks(Player player) {
        UUID base = getBase(player.getUniqueId());
        EnvironmentState st = states.get(base);
        Location origin = origins.get(base);
        if (st == null || origin == null) return;

        java.util.Set<Long> chunks = new java.util.HashSet<>();

        String town = towns.get(base);
        var stage = stageManager.getStage(town, st.level, st.stage);
        if (stage != null) {
            Location baseOrigin = origin.clone().add(0, stage.oy, 0);
            for (var b : stage.blocks) {
                int wx = baseOrigin.getBlockX() + b.x - stage.ox;
                int wz = baseOrigin.getBlockZ() + b.z - stage.oz;
                long key = (((long) (wx >> 4)) << 32) ^ (wz >> 4 & 0xffffffffL);
                chunks.add(key);
            }
        }

        Map<String, EnvironmentState> bMap = buildingStates.get(base);
        if (bMap != null) {
            for (var e : bMap.entrySet()) {
                var bStage = buildingStageManager.getStage(e.getKey(), e.getValue().level, e.getValue().stage);
                if (bStage == null) continue;
                Location bo = getBuildingOrigin(town, e.getKey(), origin).add(0, bStage.oy, 0);
                for (var b : bStage.blocks) {
                    int wx = bo.getBlockX() + b.x - bStage.ox;
                    int wz = bo.getBlockZ() + b.z - bStage.oz;
                    long key = (((long) (wx >> 4)) << 32) ^ (wz >> 4 & 0xffffffffL);
                    chunks.add(key);
                }
            }
        }

        java.util.Set<Long> loaded = loadedChunks.computeIfAbsent(base, k -> new java.util.HashSet<>());
        loaded.addAll(chunks);

        final java.util.List<Long> sorted = new java.util.ArrayList<>(chunks);
        final int ox = origin.getBlockX() >> 4;
        final int oz = origin.getBlockZ() >> 4;
        sorted.sort(java.util.Comparator.comparingInt(k -> {
            int cx = (int) (k >> 32);
            int cz = (int) k;
            return Math.abs(cx - ox) + Math.abs(cz - oz);
        }));
        final java.util.Deque<Long> queue = new java.util.ArrayDeque<>(sorted);
        final Location baseOrigin = origin;

        Runnable loader = () -> {
            int batch = 0;
            java.util.Iterator<Long> it = queue.iterator();
            while (it.hasNext() && batch < 5) {
                long key = it.next();
                int cx = (int) (key >> 32);
                int cz = (int) key;
                org.bukkit.Chunk chunk = baseOrigin.getWorld().getChunkAt(cx, cz);
                if (!chunk.isLoaded()) {
                    chunk.load(true);
                }
                if (chunk.isLoaded()) {
                    it.remove();
                }
                chunk.addPluginChunkTicket(Main.getInstance());
                batch++;
            }
            if (queue.isEmpty()) {
                BukkitTask t = chunkLoadTasks.remove(base);
                if (t != null) t.cancel();
            }
        };

        loader.run();

        if (!queue.isEmpty() && !chunkLoadTasks.containsKey(base)) {
            BukkitTask task = new BukkitRunnable() {
                @Override public void run() {
                    Player p = Bukkit.getPlayer(base);
                    if (p == null || !p.isOnline()) {
                        BukkitTask t = chunkLoadTasks.remove(base);
                        if (t != null) t.cancel();
                        return;
                    }
                    loader.run();
                    if (queue.isEmpty()) cancel();
                }
            }.runTaskTimer(Main.getInstance(), 2L, 2L);
            chunkLoadTasks.put(base, task);
        }
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
            Map<String, EnvironmentState> map = new java.util.HashMap<>();
            for (String b : buildingNames) {
                map.put(b.toLowerCase(), new EnvironmentState(1,1));
            }
            buildingStates.put(uuid, map);
        }
        playerConfig.setEnvironmentOrigin(uuid, origin);
        playerConfig.setEnvironmentTown(uuid, townName.toLowerCase());
        playerConfig.saveConfigFile();

        final EnvironmentState state = states.computeIfAbsent(uuid, id -> new EnvironmentState(1, 1));
        Map<String, EnvironmentState> bMap = buildingStates.get(uuid);
        final Map<String, EnvironmentState> finalBMap = bMap == null ? null : new java.util.HashMap<>(bMap);
        final Runnable after;
        if (finalBMap != null) {
            after = () -> {
                for (var e : finalBMap.entrySet()) {
                    Location bo = getBuildingOrigin(townName.toLowerCase(), e.getKey(), origin);
                    spawnBuilding(player, e.getKey(), bo, e.getValue().level, e.getValue().stage, null);
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
            Map<String, EnvironmentState> bMap = buildingStates.get(base);
            removeMemberData(uuid, town, st, bMap);
            coopPartners.remove(base);
            player.sendMessage(ChatColor.RED + "You have left the town.");
            return;
        }

        UUID partner = coopPartners.remove(uuid);
        if (partner != null) {
            EnvironmentState st = states.get(uuid);
            String town = towns.get(uuid);
            Map<String, EnvironmentState> bMap = buildingStates.get(uuid);
            removeMemberData(partner, town, st, bMap);
        }

        cancelTasks(uuid);
        fakeBlockManager.clear(player);
        EnvironmentState st = states.remove(uuid);
        String town = towns.remove(uuid);
        Location origin = origins.remove(uuid);
        Map<String, EnvironmentState> bMap = buildingStates.remove(uuid);
        removeAllBuildingHolograms(uuid);
        if (town != null && st != null) {
            stageManager.despawnForStage(uuid, town, st.level, st.stage);
            if (bMap != null) {
                for (var e : bMap.entrySet()) {
                    buildingStageManager.despawnForStage(uuid, e.getKey(), e.getValue().level, e.getValue().stage);
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
    }

    /** Remove all fake blocks and NPCs for this player's view without deleting data. */
    public void unloadPlayerTown(Player player) {
        UUID base = getBase(player.getUniqueId());
        cancelTasks(base);
        EnvironmentState st = states.get(base);
        String town = towns.get(base);
        Location origin = origins.get(base);
        Map<String, EnvironmentState> bMap = buildingStates.get(base);
        if (town != null && st != null && origin != null) {
            stageManager.despawnForStage(player.getUniqueId(), town, st.level, st.stage);
            if (bMap != null) {
                for (var e : bMap.entrySet()) {
                    buildingStageManager.despawnForStage(player.getUniqueId(), e.getKey(), e.getValue().level, e.getValue().stage);
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
        cancelTasks(uuid);
        fakeBlockManager.clear(player);
        String town = towns.get(player.getUniqueId());
        if (town == null) return;
        var stageData = stageManager.getStage(town, level, stage);
        if (stageData == null) return;
        // Adjust origin so Y is based on the stage's recorded offset
        Location baseOrigin = origin.clone().add(0, stageData.oy, 0);

        java.util.List<TownStageManager.BlockDef> blocks = new java.util.ArrayList<>(stageData.blocks);
        blocks.sort(java.util.Comparator.comparingInt(b -> b.y));

        final int blocksPerTick = Math.max(1, blocks.size() / totalTime);

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
                    cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);

        java.util.List<BukkitTask> tasks = new java.util.ArrayList<>();
        tasks.add(task);
        buildTasks.put(uuid, tasks);
    }

    private void spawnStructure(Player player, Location origin, int level, int stage, Runnable after) {
        spawnStructureTimed(player, origin, level, stage, after, 5 * 20);
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
        Location baseOrigin = origin.clone().add(0, stageData.oy, 0);

        Map<Location, org.bukkit.block.data.BlockData> batch = new java.util.HashMap<>();
        Map<String, Integer> priMap = blockPriorities.computeIfAbsent(uuid, k -> new java.util.HashMap<>());
        for (var b : stageData.blocks) {
            Location loc = baseOrigin.clone().add(b.x - stageData.ox, b.y - stageData.oy, b.z - stageData.oz);
            int lcx = loc.getBlockX() >> 4;
            int lcz = loc.getBlockZ() >> 4;
            if (lcx == cx && lcz == cz) {
                batch.put(loc, b.data);
                priMap.put(key(loc), stageData.priority);
            }
        }
        if (!batch.isEmpty()) {
            fakeBlockManager.showFakeBlocks(player, batch);
        }
    }

    /**
     * Upgrade the main town structure with a progressive build animation
     * instead of swapping blocks instantly.
     */
    private void spawnStructureUpgrade(Player player, Location origin,
                                       int oldLevel, int oldStage,
                                       int newLevel, int newStage) {
        UUID uuid = player.getUniqueId();
        cancelTasks(uuid);

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
            String key = loc.getBlockX()+":"+loc.getBlockY()+":"+loc.getBlockZ();
            newKeys.add(key);
            changes.add(new Change(loc, b.data));
        }

        if (oldData != null) {
            var air = org.bukkit.Bukkit.createBlockData(org.bukkit.Material.AIR);
            for (var b : oldData.blocks) {
                Location loc = oldOrigin.clone().add(b.x - oldData.ox, b.y - oldData.oy, b.z - oldData.oz);
                String key = loc.getBlockX()+":"+loc.getBlockY()+":"+loc.getBlockZ();
                if (!newKeys.contains(key)) {
                    changes.add(new Change(loc, air));
                }
            }
        }

        // sort changes bottom-up for a nicer effect
        changes.sort(java.util.Comparator.comparingInt(c -> c.loc.getBlockY()));

        final int totalTime = 5 * 20; // 5 seconds in ticks
        final int blocksPerTick = Math.max(1, changes.size() / totalTime);

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
                    cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);

        java.util.List<BukkitTask> tasks = new java.util.ArrayList<>();
        tasks.add(task);
        buildTasks.put(uuid, tasks);
    }

    /** Spawn a specific building stage relative to the town origin. */
    private void spawnBuildingTimed(Player player, String building, Location origin, int level, int stage, Runnable after, int totalTime) {
        UUID uuid = player.getUniqueId();
        removeBuildingHologram(uuid, building);
        String town = towns.get(player.getUniqueId());
        if (town == null) return;
        var stageData = buildingStageManager.getStage(building, level, stage);
        if (stageData == null) return;
        // Adjust origin so Y is based on the stage's recorded offset
        Location baseOrigin = origin.clone().add(0, stageData.oy, 0);

        java.util.List<BuildingStageManager.BlockDef> blocks = new java.util.ArrayList<>(stageData.blocks);
        blocks.sort(java.util.Comparator.comparingInt(b -> b.y));

        final int blocksPerTick = Math.max(1, blocks.size() / totalTime);

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
                    buildingStageManager.spawnForStage(player, building, level, stage, baseOrigin);
                    // Place the hologram where the stage was defined (+1 Y already stored)
                    Location holo = baseOrigin.clone().add(
                            stageData.hx - stageData.ox + 0.5,
                            stageData.hy - stageData.oy,
                            stageData.hz - stageData.oz + 0.5);
                    java.util.List<String> textLines = formatBuildingHologram(player, building, level, stage);
                    java.util.List<TextDisplay> displays = spawnHologramLines(player, holo, textLines, building);
                    buildingHolograms.computeIfAbsent(uuid, k -> new java.util.HashMap<>())
                            .put(building.toLowerCase(), displays);
                    if (after != null) after.run();
                    cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);

        java.util.List<BukkitTask> tasks = new java.util.ArrayList<>();
        tasks.add(task);
        buildTasks.put(uuid, tasks);
    }

    private void spawnBuilding(Player player, String building, Location origin, int level, int stage, Runnable after) {
        spawnBuildingTimed(player, building, origin, level, stage, after, 5 * 20);
    }

    private void spawnBuilding(Player player, String building, Location origin, int level, int stage) {
        spawnBuilding(player, building, origin, level, stage, null);
    }

    private void spawnBuildingQuick(Player player, String building, Location origin, int level, int stage) {
        spawnBuildingTimed(player, building, origin, level, stage, null, 20);
    }

    /**
     * Spawn a building stage instantly without animation. Used when chunks are
     * reloaded so players continue to see their buildings.
     */
    private void spawnBuildingInstant(Player player, String building, Location origin, int level, int stage) {
        UUID uuid = player.getUniqueId();
        removeBuildingHologram(uuid, building);
        String town = towns.get(uuid);
        if (town == null) return;
        var stageData = buildingStageManager.getStage(building, level, stage);
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
        buildingStageManager.spawnForStage(player, building, level, stage, baseOrigin);

        Location holo = baseOrigin.clone().add(
                stageData.hx - stageData.ox + 0.5,
                stageData.hy - stageData.oy,
                stageData.hz - stageData.oz + 0.5);
        java.util.List<String> textLines = formatBuildingHologram(player, building, level, stage);
        java.util.List<TextDisplay> displays = spawnHologramLines(player, holo, textLines, building);
        buildingHolograms.computeIfAbsent(uuid, k -> new java.util.HashMap<>())
                .put(building.toLowerCase(), displays);
    }

    /**
     * Resend fake blocks for a building only within the specified chunk.
     */
    private void resendBuildingForChunk(Player player, String building, Location origin,
                                        int level, int stage, int cx, int cz) {
        UUID uuid = player.getUniqueId();
        var stageData = buildingStageManager.getStage(building, level, stage);
        if (stageData == null) return;
        Location baseOrigin = origin.clone().add(0, stageData.oy, 0);

        Map<Location, org.bukkit.block.data.BlockData> batch = new java.util.HashMap<>();
        Map<String, Integer> priMap = blockPriorities.computeIfAbsent(uuid, k -> new java.util.HashMap<>());
        for (var b : stageData.blocks) {
            Location loc = baseOrigin.clone().add(b.x - stageData.ox, b.y - stageData.oy, b.z - stageData.oz);
            int lcx = loc.getBlockX() >> 4;
            int lcz = loc.getBlockZ() >> 4;
            if (lcx == cx && lcz == cz) {
                batch.put(loc, b.data);
                priMap.put(key(loc), stageData.priority);
            }
        }
        if (!batch.isEmpty()) {
            fakeBlockManager.showFakeBlocks(player, batch);
        }
    }

    /**
     * Upgrade a building with a progressive build animation rather than
     * swapping blocks instantly.
     */
    private void spawnBuildingUpgrade(Player player, String building, Location origin,
                                      int oldLevel, int oldStage,
                                      int newLevel, int newStage) {
        spawnBuildingUpgrade(player, building, origin, oldLevel, oldStage, newLevel, newStage, null);
    }

    private void spawnBuildingUpgrade(Player player, String building, Location origin,
                                      int oldLevel, int oldStage,
                                      int newLevel, int newStage,
                                      Runnable after) {
        UUID uuid = player.getUniqueId();
        removeBuildingHologram(uuid, building);
        String town = towns.get(uuid);
        if (town == null) return;

        var newData = buildingStageManager.getStage(building, newLevel, newStage);
        if (newData == null) return;
        var oldData = buildingStageManager.getStage(building, oldLevel, oldStage);

        // Adjust origin so Y is based on each stage's stored offset
        Location newOrigin = origin.clone().add(0, newData.oy, 0);
        Location oldOrigin = origin.clone();
        if (oldData != null) oldOrigin.add(0, oldData.oy, 0);

        class Change { Location loc; org.bukkit.block.data.BlockData data; Change(Location l, org.bukkit.block.data.BlockData d){this.loc=l;this.data=d;} }
        java.util.List<Change> changes = new java.util.ArrayList<>();
        java.util.Set<String> newKeys = new java.util.HashSet<>();

        for (var b : newData.blocks) {
            Location loc = newOrigin.clone().add(b.x - newData.ox, b.y - newData.oy, b.z - newData.oz);
            String key = loc.getBlockX()+":"+loc.getBlockY()+":"+loc.getBlockZ();
            newKeys.add(key);
            changes.add(new Change(loc, b.data));
        }

        if (oldData != null) {
            var air = org.bukkit.Bukkit.createBlockData(org.bukkit.Material.AIR);
            for (var b : oldData.blocks) {
                Location loc = oldOrigin.clone().add(b.x - oldData.ox, b.y - oldData.oy, b.z - oldData.oz);
                String key = loc.getBlockX()+":"+loc.getBlockY()+":"+loc.getBlockZ();
                if (!newKeys.contains(key)) {
                    changes.add(new Change(loc, air));
                }
            }
        }

        // sort bottom-up for nicer effect
        changes.sort(java.util.Comparator.comparingInt(c -> c.loc.getBlockY()));

        final int totalTime = 5 * 20; // 5 seconds in ticks
        final int blocksPerTick = Math.max(1, changes.size() / totalTime);

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
                    buildingStageManager.spawnForStage(player, building, newLevel, newStage, newOrigin);
                    Location holo = newOrigin.clone().add(
                            newData.hx - newData.ox + 0.5,
                            newData.hy - newData.oy,
                            newData.hz - newData.oz + 0.5);
                    java.util.List<String> textLines = formatBuildingHologram(player, building, newLevel, newStage);
                    java.util.List<TextDisplay> displays = spawnHologramLines(player, holo, textLines, building);
                    buildingHolograms.computeIfAbsent(uuid, k -> new java.util.HashMap<>())
                            .put(building.toLowerCase(), displays);
                    if (after != null) after.run();
                    cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);

        java.util.List<BukkitTask> tasks = new java.util.ArrayList<>();
        tasks.add(task);
        buildTasks.put(uuid, tasks);
    }

    /** Remove any fake blocks from a previous building stage before upgrading. */
    private void clearBuildingStage(Player player, String building, Location origin, int level, int stage) {
        var st = buildingStageManager.getStage(building, level, stage);
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
        Map<String, EnvironmentState> bMap = buildingStates.get(ownerId);
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
        int cx = chunk.getX();
        int cz = chunk.getZ();

        // resend structure blocks inside the chunk
        resendStructureForChunk(player, origin, st.level, st.stage, cx, cz);

        Map<String, EnvironmentState> bMap = buildingStates.get(base);
        if (bMap != null) {
            for (var e : bMap.entrySet()) {
                Location bOrigin = getBuildingOrigin(towns.get(base), e.getKey(), origin);
                resendBuildingForChunk(player, e.getKey(), bOrigin, e.getValue().level,
                        e.getValue().stage, cx, cz);
            }
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
        Map<String, EnvironmentState> bMap = buildingStates.remove(ownerId);
        if (bMap != null) buildingStates.put(partner, bMap);
        Location origin = origins.remove(ownerId);
        if (origin != null) origins.put(partner, origin);
        String town = towns.remove(ownerId);
        if (town != null) towns.put(partner, town);

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
                playerConfig.setBuildingState(partner, e.getKey(), e.getValue().level, e.getValue().stage);
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
