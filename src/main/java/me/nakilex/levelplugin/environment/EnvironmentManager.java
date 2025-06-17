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
    private final Map<UUID, Map<String, org.bukkit.entity.ArmorStand>> buildingHolograms = new HashMap<>();
    private final Map<UUID, UUID> coopOwners = new HashMap<>(); // player -> owner
    private final Map<UUID, java.util.Set<UUID>> coopMembers = new HashMap<>(); // owner -> members

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
        UUID owner = getOwner(uuid);
        return towns.get(owner);
    }

    /** Get the owner of a player's town (returns self if none). */
    public UUID getOwner(UUID player) {
        return coopOwners.getOrDefault(player, player);
    }

    /** Get online players belonging to a town owner. */
    private java.util.List<Player> getOnlineMembers(UUID owner) {
        java.util.List<Player> list = new java.util.ArrayList<>();
        Player p = Bukkit.getPlayer(owner);
        if (p != null) list.add(p);
        for (UUID m : coopMembers.getOrDefault(owner, java.util.Collections.emptySet())) {
            Player pm = Bukkit.getPlayer(m);
            if (pm != null) list.add(pm);
        }
        return list;
    }

    private void storeMembers(UUID owner) {
        java.util.Set<UUID> set = coopMembers.get(owner);
        if (set != null) playerConfig.setTownMembers(owner, set);
    }

    private java.util.List<UUID> getViewerIds(UUID owner) {
        java.util.List<UUID> ids = new java.util.ArrayList<>();
        ids.add(owner);
        ids.addAll(coopMembers.getOrDefault(owner, java.util.Collections.emptySet()));
        return ids;
    }

    private void loadOwnerState(UUID owner) {
        states.computeIfAbsent(owner, id -> {
            int lvl = playerConfig.getEnvironmentLevel(id);
            int stg = playerConfig.getEnvironmentStage(id);
            if (lvl <= 0) lvl = 1;
            if (stg <= 0) stg = 1;
            return new EnvironmentState(lvl, stg);
        });

        if (!origins.containsKey(owner)) {
            Location origin = playerConfig.getEnvironmentOrigin(owner);
            if (origin != null) origins.put(owner, origin);
        }

        if (!towns.containsKey(owner)) {
            String town = playerConfig.getEnvironmentTown(owner);
            if (town != null) towns.put(owner, town);
        }

        if (towns.containsKey(owner) && !buildingStates.containsKey(owner)) {
            Map<String, EnvironmentState> map = new java.util.HashMap<>();
            for (String b : playerConfig.getStoredBuildings(owner)) {
                int bl = playerConfig.getBuildingLevel(owner, b);
                int bs = playerConfig.getBuildingStage(owner, b);
                map.put(b.toLowerCase(), new EnvironmentState(bl, bs));
            }
            if (!map.isEmpty()) buildingStates.put(owner, map);
        }

        if (!coopMembers.containsKey(owner)) {
            var list = playerConfig.getTownMembers(owner);
            if (!list.isEmpty()) {
                coopMembers.put(owner, new java.util.HashSet<>(list));
                for (UUID m : list) coopOwners.putIfAbsent(m, owner);
            }
        }

        coopOwners.putIfAbsent(owner, owner);
    }

    /** Load state for player from config without spawning any structures. */
    private void loadPlayerState(Player player) {
        UUID uuid = player.getUniqueId();
        UUID owner = playerConfig.getTownOwner(uuid);
        if (owner != null && !owner.equals(uuid)) {
            coopOwners.put(uuid, owner);
            loadOwnerState(owner);
        } else {
            coopOwners.put(uuid, uuid);
            loadOwnerState(uuid);
        }
    }

    /** Load state for player if not present and spawn their structures/NPCs. */
    public void initializePlayer(Player player) {
        loadPlayerState(player);

        UUID owner = getOwner(player.getUniqueId());
        EnvironmentState es = states.get(owner);
        Location origin = origins.get(owner);
        if (origin != null) {
            Map<String, EnvironmentState> bMap = buildingStates.get(owner);
            final Map<String, EnvironmentState> finalBMap = bMap == null ? null : new java.util.HashMap<>(bMap);
            final Location finalOrigin = origin;
            final Runnable after;
            if (finalBMap != null) {
                after = () -> {
                    for (var e : finalBMap.entrySet()) {
                        Location bOrig = getBuildingOrigin(towns.get(owner), e.getKey(), finalOrigin);
                        spawnBuilding(player, e.getKey(), bOrig, e.getValue().level, e.getValue().stage, null);
                    }
                };
            } else {
                after = null;
            }
            spawnStructure(player, finalOrigin, es.level, es.stage, after);
        }
    }

    public EnvironmentState getState(UUID uuid) {
        return states.get(getOwner(uuid));
    }

    private void cancelTasks(UUID uuid) {
        java.util.List<BukkitTask> tasks = buildTasks.remove(uuid);
        if (tasks != null) {
            for (BukkitTask t : tasks) {
                t.cancel();
            }
        }
    }

    private void removeBuildingHologram(UUID uuid, String building) {
        var map = buildingHolograms.get(uuid);
        if (map != null) {
            var stand = map.remove(building.toLowerCase());
            if (stand != null && !stand.isDead()) {
                stand.remove();
            }
            if (map.isEmpty()) buildingHolograms.remove(uuid);
        }
    }

    private void removeAllBuildingHolograms(UUID uuid) {
        var map = buildingHolograms.remove(uuid);
        if (map != null) {
            for (var stand : map.values()) {
                if (stand != null && !stand.isDead()) stand.remove();
            }
        }
    }

    private void despawnForPlayer(Player player) {
        UUID pid = player.getUniqueId();
        cancelTasks(pid);
        fakeBlockManager.clear(player);
        removeAllBuildingHolograms(pid);
        UUID owner = getOwner(pid);
        EnvironmentState st = states.get(owner);
        String town = towns.get(owner);
        Map<String, EnvironmentState> bMap = buildingStates.get(owner);
        if (town != null && st != null) {
            stageManager.despawnForStage(pid, town, st.level, st.stage);
            if (bMap != null) {
                for (var e : bMap.entrySet()) {
                    buildingStageManager.despawnForStage(pid, e.getKey(), e.getValue().level, e.getValue().stage);
                }
            }
        }
    }

    public void saveState(UUID uuid) {
        UUID owner = getOwner(uuid);
        EnvironmentState s = states.get(owner);
        if (s != null) {
            playerConfig.setEnvironmentState(owner, s.level, s.stage);
            String town = towns.get(owner);
            if (town != null) playerConfig.setEnvironmentTown(owner, town);
            Map<String, EnvironmentState> bMap = buildingStates.get(owner);
            if (bMap != null) {
                for (var e : bMap.entrySet()) {
                    playerConfig.setBuildingState(owner, e.getKey(), e.getValue().level, e.getValue().stage);
                }
            }
            storeMembers(owner);
            playerConfig.setTownOwner(owner, owner);
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
        UUID owner = getOwner(player.getUniqueId());
        EnvironmentState state = states.get(owner);
        state.invested += amount;
        if (state.invested >= 1) {
            state.invested = 0;
            Map<String, EnvironmentState> bMap = buildingStates.get(owner);
            if (bMap != null && !bMap.isEmpty()) {
                for (var entry : bMap.entrySet()) {
                    EnvironmentState bs = entry.getValue();
                    if (bs.level < MAX_LEVEL || bs.stage < STAGES_PER_LEVEL) {
                        int oldL = bs.level;
                        int oldS = bs.stage;
                        advance(bs);
                        player.sendMessage(ChatColor.GREEN + "" + entry.getKey() + " upgraded to L" + bs.level + " S" + bs.stage);
                        String town = towns.get(owner);
                        Location origin = origins.get(owner);
                        if (town != null && origin != null) {
                            for (Player p : getOnlineMembers(owner)) {
                                buildingStageManager.despawnForStage(p.getUniqueId(), entry.getKey(), oldL, oldS);
                                Location bOrig = getBuildingOrigin(town, entry.getKey(), origin);
                                spawnBuilding(p, entry.getKey(), bOrig, bs.level, bs.stage, null);
                            }
                        }
                        saveState(owner);
                        return;
                    }
                }
                // all buildings maxed -> upgrade town
            }
            int oldLevel = state.level;
            int oldStage = state.stage;
            advance(state);
            player.sendMessage(ChatColor.GREEN + "Settlement upgraded to Level " + state.level + " Stage " + state.stage + "!");
            String town = towns.get(owner);
            Location origin = origins.get(owner);
            if (town != null && origin != null) {
                for (Player p : getOnlineMembers(owner)) {
                    stageManager.despawnForStage(p.getUniqueId(), town, oldLevel, oldStage);
                    spawnStructure(p, origin, state.level, state.stage, null);
                }
                // reset building progress for new level
                Map<String, EnvironmentState> reset = buildingStates.get(owner);
                if (reset != null) {
                    for (var e : reset.values()) {
                        e.level = 1;
                        e.stage = 1;
                    }
                }
            }
            saveState(owner);
        } else {
            player.sendMessage(ChatColor.GREEN + "Invested " + amount + " oak log.");
        }
    }

    /** Invest materials towards upgrading a specific building. */
    public void investBuilding(Player player, String building, int amount) {
        loadPlayerState(player);
        UUID owner = getOwner(player.getUniqueId());
        Map<String, EnvironmentState> bMap = buildingStates.get(owner);
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
            String town = towns.get(owner);
            Location origin = origins.get(owner);
            if (town != null && origin != null) {
                for (Player p : getOnlineMembers(owner)) {
                    buildingStageManager.despawnForStage(p.getUniqueId(), building, oldL, oldS);
                    Location bOrig = getBuildingOrigin(town, building, origin);
                    spawnBuilding(p, building, bOrig, bs.level, bs.stage, null);
                }
            }
            saveState(owner);
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
    private static final int TOWN_Y = -59;
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

    /** Start a settlement for the player at a fixed location using the given town name. */
    public void startTown(Player player, String townName) {
        UUID uuid = player.getUniqueId();
        if (origins.containsKey(uuid) || coopOwners.getOrDefault(uuid, uuid) != uuid) {
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
        coopOwners.put(uuid, uuid);
        coopMembers.put(uuid, new java.util.HashSet<>());
        playerConfig.setTownOwner(uuid, uuid);
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
        playerConfig.setTownOwner(uuid, uuid);
        playerConfig.setTownMembers(uuid, java.util.Collections.emptyList());
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
        UUID owner = getOwner(uuid);
        if (!owner.equals(uuid)) {
            player.sendMessage(ChatColor.RED + "Only town owners can reset the town.");
            return;
        }

        // remove members
        java.util.Set<UUID> mems = coopMembers.remove(owner);
        if (mems != null) {
            for (UUID m : mems) {
                coopOwners.remove(m);
                Player pl = Bukkit.getPlayer(m);
                if (pl != null) despawnForPlayer(pl);
                playerConfig.setTownOwner(m, null);
            }
        }

        cancelTasks(owner);
        fakeBlockManager.clear(player);
        EnvironmentState st = states.remove(owner);
        String town = towns.remove(owner);
        origins.remove(owner);
        Map<String, EnvironmentState> bMap = buildingStates.remove(owner);
        removeAllBuildingHolograms(owner);
        if (town != null && st != null) {
            for (UUID viewer : getViewerIds(owner)) {
                stageManager.despawnForStage(viewer, town, st.level, st.stage);
                if (bMap != null) {
                    for (var e : bMap.entrySet()) {
                        buildingStageManager.despawnForStage(viewer, e.getKey(), e.getValue().level, e.getValue().stage);
                    }
                }
            }
        }
        playerConfig.clearEnvironmentData(owner);
        playerConfig.setTownMembers(owner, java.util.Collections.emptyList());
        playerConfig.saveConfigFile();
        player.sendMessage(ChatColor.RED + "Your settlement has been reset.");
    }

    /**
     * Spawn the structure for the given player and stage with a simple build
     * animation and sound effects.
     */
    private void spawnStructure(Player player, Location origin, int level, int stage, Runnable after) {
        UUID uuid = player.getUniqueId();
        cancelTasks(uuid);
        fakeBlockManager.clear(player);
        String town = towns.get(getOwner(player.getUniqueId()));
        if (town == null) return;
        var stageData = stageManager.getStage(town, level, stage);
        if (stageData == null) return;

        java.util.List<TownStageManager.BlockDef> blocks = new java.util.ArrayList<>(stageData.blocks);
        blocks.sort(java.util.Comparator.comparingInt(b -> b.y));

        final int totalTime = 20 * 20; // 20 seconds in ticks
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
                    Location loc = origin.clone().add(b.x - stageData.ox, b.y - stageData.oy, b.z - stageData.oz);
                    batch.put(loc, b.data);
                    Sound breakS = breakSounds[rand.nextInt(breakSounds.length)];
                    Sound placeS = placeSounds[rand.nextInt(placeSounds.length)];
                    player.getWorld().playSound(loc, breakS, 0.7f, 1f);
                    player.getWorld().playSound(loc, placeS, 0.7f, 1f);
                }
                fakeBlockManager.showFakeBlocks(player, batch);
                if (index >= blocks.size()) {
                    player.playSound(origin, Sound.BLOCK_ANVIL_USE, 1f, 1f);
                    stageManager.spawnForStage(player, town, level, stage, origin);
                    if (after != null) after.run();
                    cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);

        java.util.List<BukkitTask> tasks = new java.util.ArrayList<>();
        tasks.add(task);
        buildTasks.put(uuid, tasks);
    }

    private void spawnStructure(Player player, Location origin, int level, int stage) {
        spawnStructure(player, origin, level, stage, null);
    }

    /** Spawn a specific building stage relative to the town origin. */
    private void spawnBuilding(Player player, String building, Location origin, int level, int stage, Runnable after) {
        UUID uuid = player.getUniqueId();
        removeBuildingHologram(uuid, building);
        String town = towns.get(getOwner(player.getUniqueId()));
        if (town == null) return;
        var stageData = buildingStageManager.getStage(building, level, stage);
        if (stageData == null) return;

        java.util.List<BuildingStageManager.BlockDef> blocks = new java.util.ArrayList<>(stageData.blocks);
        blocks.sort(java.util.Comparator.comparingInt(b -> b.y));

        final int totalTime = 20 * 20;
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
                    BuildingStageManager.BlockDef b = blocks.get(index);
                    Location loc = origin.clone().add(
                            b.x - stageData.ox,
                            b.y - stageData.oy,
                            b.z - stageData.oz);
                    batch.put(loc, b.data);
                    Sound breakS = breakSounds[rand.nextInt(breakSounds.length)];
                    Sound placeS = placeSounds[rand.nextInt(placeSounds.length)];
                    player.getWorld().playSound(loc, breakS, 0.7f, 1f);
                    player.getWorld().playSound(loc, placeS, 0.7f, 1f);
                }
                fakeBlockManager.showFakeBlocks(player, batch);
                if (index >= blocks.size()) {
                    player.playSound(origin, Sound.BLOCK_ANVIL_USE, 1f, 1f);
                    buildingStageManager.spawnForStage(player, building, level, stage, origin);
                    // Place the hologram where the stage was defined (+1 Y already stored)
                    Location holo = origin.clone().add(
                            stageData.hx - stageData.ox + 0.5,
                            stageData.hy - stageData.oy,
                            stageData.hz - stageData.oz + 0.5);
                    org.bukkit.entity.ArmorStand stand = holo.getWorld().spawn(holo, org.bukkit.entity.ArmorStand.class);
                    stand.addScoreboardTag("building_hologram:" + building.toLowerCase());
                    stand.setVisible(false);
                    stand.setGravity(false);
                    stand.setCustomName(ChatColor.YELLOW + "Upgrade " + building + " - 1 Oak Log");
                    stand.setCustomNameVisible(true);
                    stand.setSilent(true);
                    stand.setSmall(true);
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (!p.equals(player)) p.hideEntity(Main.getInstance(), stand);
                    }
                    buildingHolograms.computeIfAbsent(uuid, k -> new java.util.HashMap<>())
                            .put(building.toLowerCase(), stand);
                    if (after != null) after.run();
                    cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);

        java.util.List<BukkitTask> tasks = new java.util.ArrayList<>();
        tasks.add(task);
        buildTasks.put(uuid, tasks);
    }

    private void spawnBuilding(Player player, String building, Location origin, int level, int stage) {
        spawnBuilding(player, building, origin, level, stage, null);
    }

    // ----- Coop management -----

    public void invite(Player ownerPlayer, Player target) {
        UUID owner = getOwner(ownerPlayer.getUniqueId());
        if (!owner.equals(ownerPlayer.getUniqueId())) {
            ownerPlayer.sendMessage(ChatColor.RED + "Only the town owner can invite players.");
            return;
        }
        UUID tid = target.getUniqueId();
        if (coopOwners.containsKey(tid) && !coopOwners.get(tid).equals(tid)) {
            ownerPlayer.sendMessage(ChatColor.RED + "That player is already in a town.");
            return;
        }
        coopOwners.put(tid, owner);
        coopMembers.computeIfAbsent(owner, k -> new java.util.HashSet<>()).add(tid);
        playerConfig.setTownOwner(tid, owner);
        storeMembers(owner);
        playerConfig.saveConfigFile();
        initializePlayer(target);
        target.sendMessage(ChatColor.GREEN + "You joined " + ownerPlayer.getName() + "'s town!");
        ownerPlayer.sendMessage(ChatColor.GREEN + target.getName() + " added to your town.");
    }

    public void kick(Player ownerPlayer, Player target) {
        UUID owner = getOwner(ownerPlayer.getUniqueId());
        if (!owner.equals(ownerPlayer.getUniqueId())) {
            ownerPlayer.sendMessage(ChatColor.RED + "Only the town owner can kick players.");
            return;
        }
        UUID tid = target.getUniqueId();
        java.util.Set<UUID> set = coopMembers.get(owner);
        if (set == null || !set.remove(tid)) {
            ownerPlayer.sendMessage(ChatColor.RED + "That player is not in your town.");
            return;
        }
        coopOwners.remove(tid);
        storeMembers(owner);
        playerConfig.setTownOwner(tid, null);
        playerConfig.saveConfigFile();
        despawnForPlayer(target);
        target.sendMessage(ChatColor.RED + "You were removed from the town.");
        ownerPlayer.sendMessage(ChatColor.YELLOW + target.getName() + " kicked from your town.");
    }

    public void leave(Player player) {
        UUID uuid = player.getUniqueId();
        UUID owner = getOwner(uuid);
        if (owner.equals(uuid)) {
            player.sendMessage(ChatColor.RED + "You are the town owner. Use /town reset to disband.");
            return;
        }
        java.util.Set<UUID> set = coopMembers.get(owner);
        if (set != null) set.remove(uuid);
        coopOwners.remove(uuid);
        storeMembers(owner);
        playerConfig.setTownOwner(uuid, null);
        playerConfig.saveConfigFile();
        despawnForPlayer(player);
        player.sendMessage(ChatColor.YELLOW + "You left the town.");
    }

    public void sendInfo(Player player) {
        loadPlayerState(player);
        UUID owner = getOwner(player.getUniqueId());
        String town = towns.get(owner);
        if (town == null) {
            player.sendMessage(ChatColor.RED + "You are not in a town.");
            return;
        }
        player.sendMessage(ChatColor.AQUA + "Town: " + town);
        player.sendMessage(ChatColor.AQUA + "Owner: " + Bukkit.getOfflinePlayer(owner).getName());
        java.util.Set<UUID> set = coopMembers.getOrDefault(owner, java.util.Collections.emptySet());
        if (!set.isEmpty()) {
            player.sendMessage(ChatColor.AQUA + "Members:");
            for (UUID m : set) {
                player.sendMessage(ChatColor.GRAY + "- " + Bukkit.getOfflinePlayer(m).getName());
            }
        }
    }
}
