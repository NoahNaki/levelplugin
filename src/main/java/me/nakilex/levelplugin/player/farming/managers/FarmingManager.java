package me.nakilex.levelplugin.player.farming.managers;

import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FarmingManager {

    private static FarmingManager instance;

    private final Main plugin;
    private final HashMap<UUID, Integer> farmingLevels = new HashMap<>();
    private final HashMap<UUID, Integer> farmingXp     = new HashMap<>();
    private final HashMap<UUID, BossBar> xpBars       = new HashMap<>();
    private final Map<UUID, org.bukkit.scheduler.BukkitTask> hideTasks = new HashMap<>();
    private final Map<UUID, Boolean> activeBars = new HashMap<>();
    private final Map<UUID, ConsistencyState> consistencyStates = new HashMap<>();

    private final int MAX_LEVEL = 100;
    private final int XP_PER_LEVEL_MULTIPLIER = 1200;
    private static final long CONSISTENCY_TIMEOUT_MS = 3000L;
    private static final int CONSISTENCY_STEP = 150;
    private static final int CONSISTENCY_MAX_SIZE = 6;

    public FarmingManager(Main plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static FarmingManager getInstance() {
        return instance;
    }

    public void initializePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        farmingLevels.putIfAbsent(uuid, 1);
        farmingXp.putIfAbsent(uuid, 0);
        updateBossBar(player);
    }

    public void addXP(Player player, int amount) {
        if (player == null) return;
        addXP(player.getUniqueId(), amount);
    }

    public void addXP(UUID uuid, int amount) {
        if (getLevel(uuid) >= MAX_LEVEL) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                markFarmingActive(player);
                updateBossBar(player);
            }
            return;
        }
        int newXP = getXP(uuid) + amount;
        farmingXp.put(uuid, newXP);
        checkLevelUp(uuid);
    }

    private void checkLevelUp(UUID uuid) {
        int level = getLevel(uuid);
        int xp    = getXP(uuid);

        int xpNeeded = getXpRequired(level);
        boolean leveled = false;
        while (level < MAX_LEVEL && xp >= xpNeeded) {
            xp -= xpNeeded;
            level++;
            leveled = true;

            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                sendLevelUpMessage(player, level, getXpRequired(level));
            }

            xpNeeded = getXpRequired(level);
        }

        farmingLevels.put(uuid, level);
        farmingXp.put(uuid, xp);

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            markFarmingActive(player);
            updateBossBar(player);
            if (leveled) {
                updatePlayerTooltips(player);
            }
        }
        if (leveled && plugin.getPlayerConfig() != null) {
            plugin.getPlayerConfig().savePlayerData(uuid);
        }
    }

    public int getXpRequired(int level) {
        return level * XP_PER_LEVEL_MULTIPLIER;
    }

    public int getConsistencySize(Player player) {
        if (player == null) return 1;
        ConsistencyState state = getConsistencyState(player.getUniqueId());
        if (state == null) return 1;
        return Math.max(1, Math.min(CONSISTENCY_MAX_SIZE, state.size));
    }

    public void recordConsistencyHarvest(Player player, int amount) {
        if (player == null || amount <= 0) return;
        UUID uuid = player.getUniqueId();
        ConsistencyState state = consistencyStates.computeIfAbsent(uuid, id -> new ConsistencyState());
        long now = System.currentTimeMillis();
        applyConsistencyDecay(state, now);
        state.harvestedSinceIncrease += amount;
        while (state.harvestedSinceIncrease >= CONSISTENCY_STEP && state.size < CONSISTENCY_MAX_SIZE) {
            state.harvestedSinceIncrease -= CONSISTENCY_STEP;
            state.size++;
            playConsistencyLevelUpSound(player, state.size);
        }
        state.lastHarvestAt = now;
        state.lastDecayAt = now;
    }

    public String getConsistencyIndicator(Player player) {
        if (player == null) return null;
        ConsistencyState state = getConsistencyState(player.getUniqueId());
        if (state == null) return null;
        int size = Math.max(1, Math.min(CONSISTENCY_MAX_SIZE, state.size));
        return ChatColor.LIGHT_PURPLE + "Consistency "
                + ChatColor.YELLOW + size + "x" + size;
    }

    private ConsistencyState getConsistencyState(UUID uuid) {
        ConsistencyState state = consistencyStates.get(uuid);
        if (state == null) return null;
        long now = System.currentTimeMillis();
        applyConsistencyDecay(state, now);
        return state;
    }

    private void applyConsistencyDecay(ConsistencyState state, long now) {
        if (state.size <= 1) {
            return;
        }
        long sinceDecay = now - state.lastDecayAt;
        if (sinceDecay < CONSISTENCY_TIMEOUT_MS) {
            return;
        }
        int steps = (int) (sinceDecay / CONSISTENCY_TIMEOUT_MS);
        state.size = Math.max(1, state.size - steps);
        state.harvestedSinceIncrease = 0;
        state.lastDecayAt += steps * CONSISTENCY_TIMEOUT_MS;
    }

    private void playConsistencyLevelUpSound(Player player, int size) {
        if (player == null) return;
        float base = 0.6f + ((size - 1) * 0.12f);
        for (int i = 0; i < 3; i++) {
            float pitch = Math.min(2.0f, base + (i * 0.08f));
            player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, pitch);
        }
    }

    private static class ConsistencyState {
        private int size = 1;
        private int harvestedSinceIncrease;
        private long lastHarvestAt;
        private long lastDecayAt;
    }

    private void sendLevelUpMessage(Player player, int newLevel, int nextXp) {
        me.nakilex.levelplugin.utils.ChatFormatter.constructDivider(player, "§e§l-", 45);
        me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "§e§lFARMING LEVEL UP!");
        me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "");
        me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "§7You are now Farming level §e§l" + newLevel + "§7!");
        if (newLevel < MAX_LEVEL) {
            me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "§7You need §e" + nextXp + " XP §7to reach level §e" + (newLevel + 1) + "§7.");
        }
        me.nakilex.levelplugin.utils.ChatFormatter.constructDivider(player, "§e§l-", 45);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        player.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, player.getLocation(), 20);
    }

    private void updatePlayerTooltips(Player player) {
        player.getInventory().forEach(stack -> {
            if (stack != null && me.nakilex.levelplugin.items.tools.ToolManager.getInstance().isToolMaterial(stack.getType())) {
                me.nakilex.levelplugin.items.utils.ItemUtil.updateCustomToolTooltip(stack, player);
            }
        });
        player.updateInventory();
    }

    private void updateBossBar(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        BossBar bar = xpBars.computeIfAbsent(uuid, id -> {
            BossBar created = Bukkit.createBossBar("", BarColor.YELLOW, BarStyle.SOLID);
            created.addPlayer(player);
            created.setVisible(false);
            return created;
        });

        int level = getLevel(uuid);
        int xp = getXP(uuid);
        boolean atMax = level >= MAX_LEVEL;
        int required = atMax ? getXpRequired(MAX_LEVEL) : getXpRequired(level);
        double progress = atMax ? 1.0 : required <= 0 ? 1.0 : Math.min(1.0, Math.max(0.0, xp / (double) required));
        boolean showBar = activeBars.getOrDefault(uuid, false);
        if (!showBar) {
            bar.removePlayer(player);
            bar.setVisible(false);
            return;
        }
        String progressLabel = atMax ? (ChatColor.GREEN + "MAX") : (ChatColor.WHITE + String.valueOf(xp)
                + ChatColor.GRAY + "/" + ChatColor.WHITE + required);

        String title = ChatColor.GOLD + "" + ChatColor.BOLD + "Farming "
                + ChatColor.GRAY + "(Lv. " + ChatColor.WHITE + level + ChatColor.GRAY + ") "
                + ChatColor.DARK_GRAY + "| "
                + progressLabel;

        bar.setTitle(title);
        bar.setProgress(progress);
        if (!bar.getPlayers().contains(player)) {
            bar.addPlayer(player);
        }
        bar.setVisible(true);
    }

    private void markFarmingActive(Player player) {
        UUID uuid = player.getUniqueId();
        activeBars.put(uuid, true);
        org.bukkit.scheduler.BukkitTask existing = hideTasks.remove(uuid);
        if (existing != null) {
            existing.cancel();
        }
        updateBossBar(player);
        hideTasks.put(uuid, Bukkit.getScheduler().runTaskLater(plugin, () -> {
            activeBars.put(uuid, false);
            Player online = Bukkit.getPlayer(uuid);
            if (online != null) {
                updateBossBar(online);
            }
        }, 20L * 6));
    }

    public int getLevel(Player player) {
        if (player == null) return 1;
        return getLevel(player.getUniqueId());
    }

    public int getLevel(UUID uuid) {
        return farmingLevels.getOrDefault(uuid, 1);
    }

    public int getXP(Player player) {
        if (player == null) return 0;
        return getXP(player.getUniqueId());
    }

    public int getXP(UUID uuid) {
        return farmingXp.getOrDefault(uuid, 0);
    }

    public void setLevel(UUID uuid, int level) {
        farmingLevels.put(uuid, level);
        farmingXp.put(uuid, 0);
    }

    public void clearPlayerData(UUID uuid) {
        farmingLevels.remove(uuid);
        farmingXp.remove(uuid);
        if (plugin.getPlayerConfig() != null) {
            String path = "players." + uuid + ".farming";
            plugin.getPlayerConfig().getConfig().set(path, null);
            plugin.getPlayerConfig().saveConfigFile();
        }
    }

    public int getMaxLevel() {
        return MAX_LEVEL;
    }
}
