package me.nakilex.levelplugin.environment;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.environment.stage.BuildingStageManager;
import me.nakilex.levelplugin.environment.BuildingNPCManager;
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
    private final BuildingNPCManager buildingNPCManager;
    private final FakeBlockManager fakeBlockManager;
    private final Map<UUID, EnvironmentState> states = new HashMap<>();
    private final Map<UUID, Location> origins = new HashMap<>();
    private final Map<UUID, String> towns = new HashMap<>();
    private final Map<UUID, Map<String, EnvironmentState>> buildingStates = new HashMap<>();
    private final Map<UUID, java.util.List<BukkitTask>> buildTasks = new HashMap<>();
    private final Map<UUID, Map<String, org.bukkit.entity.ArmorStand>> buildingHolograms = new HashMap<>();
    private final Map<UUID, UUID> coopOwners = new HashMap<>();
    private final Map<UUID, UUID> coopPartners = new HashMap<>();
    private final Map<UUID, UUID> pendingInvites = new HashMap<>();

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
                              BuildingNPCManager buildingNPCManager,
                              FakeBlockManager blockManager) {
        this.playerConfig = config;
        this.stageManager = stageManager;
        this.buildingStageManager = buildingStageManager;
        this.buildingNPCManager = buildingNPCManager;
        this.fakeBlockManager = blockManager;
    }

    public TownStageManager getStageManager() {
        return stageManager;
    }

    public me.nakilex.levelplugin.environment.stage.BuildingStageManager getBuildingStageManager() {
        return buildingStageManager;
    }

    public BuildingNPCManager getBuildingNPCManager() {
        return buildingNPCManager;
    }

    public String getTown(UUID uuid) {
        return towns.get(uuid);
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
    private void loadPlayerState(Player player) {
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

        UUID uuid = player.getUniqueId();
        EnvironmentState es = states.get(uuid);
        Location origin = origins.get(uuid);
        if (origin != null) {
            Map<String, EnvironmentState> bMap = buildingStates.get(uuid);
            final Map<String, EnvironmentState> finalBMap = bMap == null ? null : new java.util.HashMap<>(bMap);
            final Location finalOrigin = origin;
            final Runnable after;
            if (finalBMap != null) {
                after = () -> {
                    for (var e : finalBMap.entrySet()) {
                        Location bOrig = getBuildingOrigin(towns.get(uuid), e.getKey(), finalOrigin);
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
        return states.get(uuid);
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

    /** Spawn an upgrade hologram for the given building stage. */
    private void spawnBuildingHologram(Player player, String building,
                                       me.nakilex.levelplugin.environment.stage.BuildingStageManager.BuildingStage data,
                                       Location origin) {
        UUID uuid = player.getUniqueId();
        Location holo = origin.clone().add(
                data.hx - data.ox + 0.5,
                data.hy - data.oy,
                data.hz - data.oz + 0.5);
        org.bukkit.entity.ArmorStand stand = holo.getWorld().spawn(holo, org.bukkit.entity.ArmorStand.class);
        stand.addScoreboardTag("building_hologram:" + building.toLowerCase());
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setCustomName(org.bukkit.ChatColor.YELLOW + "Upgrade " + building + " - 1 Oak Log");
        stand.setCustomNameVisible(true);
        stand.setSilent(true);
        stand.setSmall(true);
        for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (!p.equals(player)) p.hideEntity(Main.getInstance(), stand);
        }
        buildingHolograms.computeIfAbsent(uuid, k -> new java.util.HashMap<>())
                .put(building.toLowerCase(), stand);
    }

    private void removeMemberData(UUID member, String town, EnvironmentState st, Map<String, EnvironmentState> bMap) {
        cancelTasks(member);
        removeAllBuildingHolograms(member);
        stageManager.despawnForStage(member, town, st.level, st.stage);
        if (bMap != null) {
            for (var e : bMap.entrySet()) {
                buildingStageManager.despawnForStage(member, e.getKey(), e.getValue().level, e.getValue().stage);
                buildingNPCManager.despawnForStage(member, e.getKey(), e.getValue().level, e.getValue().stage);
            }
        }
        fakeBlockManager.clear(Bukkit.getPlayer(member));
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
                            buildingNPCManager.despawnForStage(player.getUniqueId(), entry.getKey(), oldL, oldS);
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
                buildingNPCManager.despawnForStage(player.getUniqueId(), building, oldL, oldS);
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
        origins.remove(uuid);
        Map<String, EnvironmentState> bMap = buildingStates.remove(uuid);
        removeAllBuildingHolograms(uuid);
        if (town != null && st != null) {
            stageManager.despawnForStage(uuid, town, st.level, st.stage);
            if (bMap != null) {
                for (var e : bMap.entrySet()) {
                    buildingStageManager.despawnForStage(uuid, e.getKey(), e.getValue().level, e.getValue().stage);
                    buildingNPCManager.despawnForStage(uuid, e.getKey(), e.getValue().level, e.getValue().stage);
                }
            }
        }
        playerConfig.clearEnvironmentData(uuid);
        playerConfig.clearCoop(uuid);
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
        String town = towns.get(player.getUniqueId());
        if (town == null) return;
        var stageData = stageManager.getStage(town, level, stage);
        if (stageData == null) return;

        if (stageData.schematic != null && stageData.schematic.exists()) {
            Location pasteOrigin = origin.clone().add(-stageData.ox, -stageData.oy, -stageData.oz);
            stageManager.pasteSchematic(stageData.schematic, pasteOrigin);
            stageManager.spawnForStage(player, town, level, stage, origin);
            if (after != null) after.run();
            return;
        }

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

    /**
     * Upgrade the main town structure, replacing old blocks layer by layer.
     */
    private void spawnStructureUpgrade(Player player, Location origin,
                                       int oldLevel, int oldStage,
                                       int newLevel, int newStage) {
        UUID uuid = player.getUniqueId();
        cancelTasks(uuid);
        fakeBlockManager.clear(player);
        String town = towns.get(uuid);
        if (town == null) return;

        var newData = stageManager.getStage(town, newLevel, newStage);
        if (newData == null) return;
        var oldData = stageManager.getStage(town, oldLevel, oldStage);

        if (newData.schematic != null && newData.schematic.exists()) {
            stageManager.despawnForStage(uuid, town, oldLevel, oldStage);
            Location pasteOrigin = origin.clone().add(-newData.ox, -newData.oy, -newData.oz);
            stageManager.pasteSchematic(newData.schematic, pasteOrigin);
            stageManager.spawnForStage(player, town, newLevel, newStage, origin);
            return;
        }

        java.util.Map<Integer, java.util.List<TownStageManager.BlockDef>> newLayers = new java.util.HashMap<>();
        for (var b : newData.blocks) {
            newLayers.computeIfAbsent(b.y, k -> new java.util.ArrayList<>()).add(b);
        }

        java.util.Map<Integer, java.util.List<Location>> oldLayers = new java.util.HashMap<>();
        if (oldData != null) {
            for (var b : oldData.blocks) {
                Location l = origin.clone().add(b.x - oldData.ox, b.y - oldData.oy, b.z - oldData.oz);
                oldLayers.computeIfAbsent(b.y, k -> new java.util.ArrayList<>()).add(l);
            }
        }

        java.util.Set<Integer> allY = new java.util.TreeSet<>(newLayers.keySet());
        allY.addAll(oldLayers.keySet());

        java.util.Random rand = new java.util.Random();
        org.bukkit.Sound[] breakSounds = { org.bukkit.Sound.BLOCK_STONE_BREAK, org.bukkit.Sound.BLOCK_DEEPSLATE_BREAK, org.bukkit.Sound.BLOCK_WOOD_BREAK };
        org.bukkit.Sound[] placeSounds = { org.bukkit.Sound.BLOCK_STONE_PLACE, org.bukkit.Sound.BLOCK_DEEPSLATE_PLACE, org.bukkit.Sound.BLOCK_WOOD_PLACE };

        stageManager.despawnForStage(uuid, town, oldLevel, oldStage);

        BukkitTask task = new BukkitRunnable() {
            final java.util.Iterator<Integer> it = allY.iterator();
            @Override public void run() {
                if (!player.isOnline()) { cancel(); return; }
                if (!it.hasNext()) {
                    player.playSound(origin, org.bukkit.Sound.BLOCK_ANVIL_USE, 1f, 1f);
                    stageManager.spawnForStage(player, town, newLevel, newStage, origin);
                    cancel();
                    return;
                }
                int y = it.next();
                java.util.List<Location> toRemove = oldLayers.getOrDefault(y, java.util.Collections.emptyList());
                fakeBlockManager.hideFakeBlocks(player, toRemove);

                java.util.Map<Location, org.bukkit.block.data.BlockData> batch = new java.util.HashMap<>();
                java.util.List<TownStageManager.BlockDef> add = newLayers.getOrDefault(y, java.util.Collections.emptyList());
                for (var b : add) {
                    Location loc = origin.clone().add(b.x - newData.ox, b.y - newData.oy, b.z - newData.oz);
                    batch.put(loc, b.data);
                    org.bukkit.Sound breakS = breakSounds[rand.nextInt(breakSounds.length)];
                    org.bukkit.Sound placeS = placeSounds[rand.nextInt(placeSounds.length)];
                    player.getWorld().playSound(loc, breakS, 0.7f, 1f);
                    player.getWorld().playSound(loc, placeS, 0.7f, 1f);
                }
                fakeBlockManager.showFakeBlocks(player, batch);
            }
        }.runTaskTimer(Main.getInstance(), 0L, 5L);

        java.util.List<BukkitTask> tasks = new java.util.ArrayList<>();
        tasks.add(task);
        buildTasks.put(uuid, tasks);
    }

    /** Spawn a specific building stage relative to the town origin. */
    private void spawnBuilding(Player player, String building, Location origin, int level, int stage, Runnable after) {
        UUID uuid = player.getUniqueId();
        removeBuildingHologram(uuid, building);
        String town = towns.get(player.getUniqueId());
        if (town == null) return;
        var stageData = buildingStageManager.getStage(building, level, stage);
        if (stageData == null) return;

        if (stageData.schematic != null && stageData.schematic.exists()) {
            Location pasteOrigin = origin.clone().add(-stageData.ox, -stageData.oy, -stageData.oz);
            buildingStageManager.pasteSchematic(stageData.schematic, pasteOrigin);
            buildingStageManager.spawnForStage(player, building, level, stage, origin);
            buildingNPCManager.spawnForStage(player, building, level, stage, origin);
            spawnBuildingHologram(player, building, stageData, origin);
            if (after != null) after.run();
            return;
        }

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
                    buildingNPCManager.spawnForStage(player, building, level, stage, origin);
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

    /**
     * Upgrade a building by replacing the old stage with the new one layer by layer.
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

        if (newData.schematic != null && newData.schematic.exists()) {
            buildingStageManager.despawnForStage(uuid, building, oldLevel, oldStage);
            buildingNPCManager.despawnForStage(uuid, building, oldLevel, oldStage);
            Location pasteOrigin = origin.clone().add(-newData.ox, -newData.oy, -newData.oz);
            buildingStageManager.pasteSchematic(newData.schematic, pasteOrigin);
            buildingStageManager.spawnForStage(player, building, newLevel, newStage, origin);
            buildingNPCManager.spawnForStage(player, building, newLevel, newStage, origin);
            spawnBuildingHologram(player, building, newData, origin);
            if (after != null) after.run();
            return;
        }

        java.util.Map<Integer, java.util.List<BuildingStageManager.BlockDef>> newLayers = new java.util.HashMap<>();
        for (var b : newData.blocks) {
            newLayers.computeIfAbsent(b.y, k -> new java.util.ArrayList<>()).add(b);
        }

        java.util.Map<Integer, java.util.List<Location>> oldLayers = new java.util.HashMap<>();
        if (oldData != null) {
            for (var b : oldData.blocks) {
                Location l = origin.clone().add(b.x - oldData.ox, b.y - oldData.oy, b.z - oldData.oz);
                oldLayers.computeIfAbsent(b.y, k -> new java.util.ArrayList<>()).add(l);
            }
        }

        java.util.Set<Integer> allY = new java.util.TreeSet<>(newLayers.keySet());
        allY.addAll(oldLayers.keySet());

        java.util.Random rand = new java.util.Random();
        org.bukkit.Sound[] breakSounds = { org.bukkit.Sound.BLOCK_STONE_BREAK, org.bukkit.Sound.BLOCK_DEEPSLATE_BREAK, org.bukkit.Sound.BLOCK_WOOD_BREAK };
        org.bukkit.Sound[] placeSounds = { org.bukkit.Sound.BLOCK_STONE_PLACE, org.bukkit.Sound.BLOCK_DEEPSLATE_PLACE, org.bukkit.Sound.BLOCK_WOOD_PLACE };

        buildingStageManager.despawnForStage(uuid, building, oldLevel, oldStage);
        buildingNPCManager.despawnForStage(uuid, building, oldLevel, oldStage);

        BukkitTask task = new BukkitRunnable() {
            final java.util.Iterator<Integer> it = allY.iterator();
            @Override public void run() {
                if (!player.isOnline()) { cancel(); return; }
                if (!it.hasNext()) {
                    player.playSound(origin, org.bukkit.Sound.BLOCK_ANVIL_USE, 1f, 1f);
                    buildingStageManager.spawnForStage(player, building, newLevel, newStage, origin);
                    buildingNPCManager.spawnForStage(player, building, newLevel, newStage, origin);
                    Location holo = origin.clone().add(
                            newData.hx - newData.ox + 0.5,
                            newData.hy - newData.oy,
                            newData.hz - newData.oz + 0.5);
                    org.bukkit.entity.ArmorStand stand = holo.getWorld().spawn(holo, org.bukkit.entity.ArmorStand.class);
                    stand.addScoreboardTag("building_hologram:" + building.toLowerCase());
                    stand.setVisible(false);
                    stand.setGravity(false);
                    stand.setCustomName(org.bukkit.ChatColor.YELLOW + "Upgrade " + building + " - 1 Oak Log");
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
                    return;
                }
                int y = it.next();
                java.util.List<Location> toRemove = oldLayers.getOrDefault(y, java.util.Collections.emptyList());
                fakeBlockManager.hideFakeBlocks(player, toRemove);

                java.util.Map<Location, org.bukkit.block.data.BlockData> batch = new java.util.HashMap<>();
                java.util.List<BuildingStageManager.BlockDef> add = newLayers.getOrDefault(y, java.util.Collections.emptyList());
                for (var b : add) {
                    Location loc = origin.clone().add(b.x - newData.ox, b.y - newData.oy, b.z - newData.oz);
                    batch.put(loc, b.data);
                    org.bukkit.Sound breakS = breakSounds[rand.nextInt(breakSounds.length)];
                    org.bukkit.Sound placeS = placeSounds[rand.nextInt(placeSounds.length)];
                    player.getWorld().playSound(loc, breakS, 0.7f, 1f);
                    player.getWorld().playSound(loc, placeS, 0.7f, 1f);
                }
                fakeBlockManager.showFakeBlocks(player, batch);
            }
        }.runTaskTimer(Main.getInstance(), 0L, 5L);

        java.util.List<BukkitTask> tasks = new java.util.ArrayList<>();
        tasks.add(task);
        buildTasks.put(uuid, tasks);
    }

    /** Remove any fake blocks from a previous building stage before upgrading. */
    private void clearBuildingStage(Player player, String building, Location origin, int level, int stage) {
        var st = buildingStageManager.getStage(building, level, stage);
        if (st == null) return;
        java.util.List<Location> locs = new java.util.ArrayList<>();
        for (var b : st.blocks) {
            Location l = origin.clone().add(b.x - st.ox, b.y - st.oy, b.z - st.oz);
            locs.add(l);
        }
        fakeBlockManager.hideFakeBlocks(player, locs);
    }

    /** Remove fake blocks from a previous town stage before upgrading. */
    private void clearTownStage(Player player, String town, Location origin, int level, int stage) {
        var st = stageManager.getStage(town, level, stage);
        if (st == null) return;
        java.util.List<Location> locs = new java.util.ArrayList<>();
        for (var b : st.blocks) {
            Location l = origin.clone().add(b.x - st.ox, b.y - st.oy, b.z - st.oz);
            locs.add(l);
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
