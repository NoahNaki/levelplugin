package me.nakilex.levelplugin.environment;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.config.PlayerConfig;
import me.nakilex.levelplugin.environment.stage.TownStageManager;
import me.nakilex.levelplugin.fakeblock.FakeBlockManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

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
    private final FakeBlockManager fakeBlockManager;
    private final Map<UUID, EnvironmentState> states = new HashMap<>();
    private final Map<UUID, Location> origins = new HashMap<>();
    private final Map<UUID, String> towns = new HashMap<>();
    private final Map<UUID, java.util.List<BukkitTask>> buildTasks = new HashMap<>();

    public static class EnvironmentState {
        public int level;
        public int stage;
        public int invested;
        public EnvironmentState(int level, int stage) {
            this.level = level;
            this.stage = stage;
        }
    }

    public EnvironmentManager(PlayerConfig config, TownStageManager stageManager, FakeBlockManager blockManager) {
        this.playerConfig = config;
        this.stageManager = stageManager;
        this.fakeBlockManager = blockManager;
    }

    public TownStageManager getStageManager() {
        return stageManager;
    }

    public String getTown(UUID uuid) {
        return towns.get(uuid);
    }

    /** Load state for player if not present and spawn their structures/NPCs. */
    public void initializePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        EnvironmentState es = states.computeIfAbsent(uuid, id -> {
            int lvl = playerConfig.getEnvironmentLevel(id);
            int stg = playerConfig.getEnvironmentStage(id);
            if (lvl <= 0) lvl = 1;
            if (stg <= 0) stg = 1;
            return new EnvironmentState(lvl, stg);
        });

        Location origin = origins.get(uuid);
        if (origin == null) {
            origin = playerConfig.getEnvironmentOrigin(uuid);
            if (origin != null) {
                origins.put(uuid, origin);
            }
        }
        String town = towns.get(uuid);
        if (town == null) {
            town = playerConfig.getEnvironmentTown(uuid);
            if (town != null) towns.put(uuid, town);
        }

        if (origin != null) {
            spawnStructure(player, origin, es.level, es.stage);
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

    public void saveState(UUID uuid) {
        EnvironmentState s = states.get(uuid);
        if (s != null) {
            playerConfig.setEnvironmentState(uuid, s.level, s.stage);
            String town = towns.get(uuid);
            if (town != null) playerConfig.setEnvironmentTown(uuid, town);
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
        initializePlayer(player);
        EnvironmentState state = states.get(player.getUniqueId());
        state.invested += amount;
        if (state.invested >= 1) {
            state.invested = 0;
            int oldLevel = state.level;
            int oldStage = state.stage;
            advance(state);
            player.sendMessage(ChatColor.GREEN + "Settlement upgraded to Level "
                    + state.level + " Stage " + state.stage + "!");
            String town = towns.get(player.getUniqueId());
            Location origin = origins.get(player.getUniqueId());
            if (town != null && origin != null) {
                stageManager.despawnForStage(player.getUniqueId(), town, oldLevel, oldStage);
                spawnStructure(player, origin, state.level, state.stage);
            }
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

    /** Start a settlement for the player at their current location using the given town name. */
    public void startTown(Player player, String townName) {
        UUID uuid = player.getUniqueId();
        if (origins.containsKey(uuid)) {
            player.sendMessage(ChatColor.RED + "You already started a settlement.");
            return;
        }
        if (townName == null || stageManager.getStage(townName, 1, 1) == null) {
            player.sendMessage(ChatColor.RED + "Unknown town type.");
            return;
        }
        Location origin = player.getLocation().getBlock().getLocation();
        origins.put(uuid, origin);
        towns.put(uuid, townName.toLowerCase());
        playerConfig.setEnvironmentOrigin(uuid, origin);
        playerConfig.setEnvironmentTown(uuid, townName.toLowerCase());
        playerConfig.saveConfigFile();
        initializePlayer(player); // ensure state
        EnvironmentState s = states.get(uuid);
        spawnStructure(player, origin, s.level, s.stage);
        player.sendMessage(ChatColor.YELLOW + "Settlement created at " + origin.getBlockX()+","+origin.getBlockY()+","+origin.getBlockZ());
    }

    /** Remove the player's settlement so they can start over. */
    public void resetTown(Player player) {
        UUID uuid = player.getUniqueId();
        cancelTasks(uuid);
        fakeBlockManager.clear(player);
        EnvironmentState st = states.remove(uuid);
        String town = towns.remove(uuid);
        origins.remove(uuid);
        if (town != null && st != null) {
            stageManager.despawnForStage(uuid, town, st.level, st.stage);
        }
        playerConfig.clearEnvironmentData(uuid);
        playerConfig.saveConfigFile();
        player.sendMessage(ChatColor.RED + "Your settlement has been reset.");
    }

    /**
     * Spawn the structure for the given player and stage with a simple build
     * animation and sound effects.
     */
    private void spawnStructure(Player player, Location origin, int level, int stage) {
        UUID uuid = player.getUniqueId();
        cancelTasks(uuid);
        fakeBlockManager.clear(player);
        String town = towns.get(player.getUniqueId());
        if (town == null) return;
        var stageData = stageManager.getStage(town, level, stage);
        if (stageData == null) return;

        java.util.List<BukkitTask> tasks = new java.util.ArrayList<>();

        java.util.List<TownStageManager.BlockDef> blocks = new java.util.ArrayList<>(stageData.blocks);
        blocks.sort(java.util.Comparator.comparingInt(b -> b.y));

        final int totalTime = 20 * 20; // 20 seconds in ticks
        double step = blocks.isEmpty() ? totalTime : (double) totalTime / blocks.size();
        double current = 0;

        java.util.Random rand = new java.util.Random();
        Sound[] breakSounds = { Sound.BLOCK_STONE_BREAK, Sound.BLOCK_DEEPSLATE_BREAK, Sound.BLOCK_WOOD_BREAK };
        Sound[] placeSounds = { Sound.BLOCK_STONE_PLACE, Sound.BLOCK_DEEPSLATE_PLACE, Sound.BLOCK_WOOD_PLACE };

        for (TownStageManager.BlockDef b : blocks) {
            long delay = Math.round(current);
            current += step;
            Location loc = origin.clone().add(b.x, b.y, b.z);
            BukkitTask task = Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                fakeBlockManager.showFakeBlock(player, loc, b.data);
                Sound breakS = breakSounds[rand.nextInt(breakSounds.length)];
                Sound placeS = placeSounds[rand.nextInt(placeSounds.length)];
                player.getWorld().playSound(loc, breakS, 0.7f, 1f);
                player.getWorld().playSound(loc, placeS, 0.7f, 1f);
            }, delay);
            tasks.add(task);
        }

        BukkitTask finalTask = Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            player.playSound(origin, Sound.BLOCK_ANVIL_USE, 1f, 1f);
            stageManager.spawnForStage(player, town, level, stage, origin);
        }, Math.round(current));
        tasks.add(finalTask);
        buildTasks.put(uuid, tasks);
    }
}
