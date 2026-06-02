package me.nakilex.levelplugin.player.fishing.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.pet.PetEffectType;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.player.fishing.data.FishingQuality;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import me.nakilex.levelplugin.player.attributes.lifeskill.LifeSkillProgression;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles Fishing profession XP and levels.
 */
public class FishingManager implements LifeSkillProgression {

    private static FishingManager instance;

    private final Main plugin;
    private final HashMap<UUID, Integer> fishingLevels = new HashMap<>();
    private final HashMap<UUID, Integer> fishingXp     = new HashMap<>();
    private final HashMap<UUID, BossBar> xpBars       = new HashMap<>();
    private final Map<UUID, org.bukkit.scheduler.BukkitTask> hideTasks = new HashMap<>();
    private final Map<UUID, Boolean> activeBars = new HashMap<>();
    private final Map<UUID, java.util.Set<String>> discoveredFish = new HashMap<>();
    private final Map<UUID, Map<String, FishRecord>> fishRecords = new HashMap<>();
    private final Map<UUID, Double> debugBiteSpeedMultipliers = new HashMap<>();

    private final int MAX_LEVEL = 100;
    private final int XP_PER_LEVEL_MULTIPLIER = 200;

    public FishingManager(Main plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static FishingManager getInstance() {
        return instance;
    }

    public void initializePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        fishingLevels.putIfAbsent(uuid, 1);
        fishingXp.putIfAbsent(uuid, 0);
        discoveredFish.putIfAbsent(uuid, new java.util.HashSet<>());
        fishRecords.putIfAbsent(uuid, new HashMap<>());
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
                markFishingActive(player);
                updateBossBar(player);
            }
            return;
        }
        int adjusted = amount;
        if (plugin.getPetManager() != null) {
            adjusted = plugin.getPetManager().applyActiveEffectMultiplier(uuid, PetEffectType.GATHERING_XP_BOOST, adjusted);
        }
        int newXP = getXP(uuid) + adjusted;
        fishingXp.put(uuid, newXP);
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

        fishingLevels.put(uuid, level);
        fishingXp.put(uuid, xp);

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            markFishingActive(player);
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

    private void sendLevelUpMessage(Player player, int newLevel, int nextXp) {
        me.nakilex.levelplugin.utils.ChatFormatter.constructDivider(player, "§b§l-", 45);
        me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "§b§lFISHING LEVEL UP!");
        me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "");
        me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "§7You are now Fishing level §e§l" + newLevel + "§7!");
        if (newLevel < MAX_LEVEL) {
            me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "§7You need §e" + nextXp + " XP §7to reach level §e" + (newLevel + 1) + "§7.");
        }
        me.nakilex.levelplugin.utils.ChatFormatter.constructDivider(player, "§b§l-", 45);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        player.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, player.getLocation(), 20);
    }

    private void updatePlayerTooltips(Player player) {
        player.getInventory().forEach(stack -> {
            if (stack != null && me.nakilex.levelplugin.items.tools.ToolManager.getInstance().isToolMaterial(stack.getType())) {
                ItemUtil.updateCustomToolTooltip(stack, player);
            }
        });
        player.updateInventory();
    }

    private void updateBossBar(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        int level = getLevel(uuid);
        int xp = getXP(uuid);
        boolean atMax = level >= MAX_LEVEL;
        int required = atMax ? getXpRequired(MAX_LEVEL) : getXpRequired(level);
        me.nakilex.levelplugin.utils.LifeSkillBossBarUtil.updateBossBar(
                player,
                uuid,
                xpBars,
                activeBars,
                level,
                xp,
                atMax,
                required,
                "Fishing",
                ChatColor.BLUE,
                BarColor.BLUE
        );
    }

    private void markFishingActive(Player player) {
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
        return fishingLevels.getOrDefault(uuid, 1);
    }

    public int getXP(Player player) {
        if (player == null) return 0;
        return getXP(player.getUniqueId());
    }

    public int getXP(UUID uuid) {
        return fishingXp.getOrDefault(uuid, 0);
    }

    public void setLevel(UUID uuid, int level) {
        fishingLevels.put(uuid, level);
        fishingXp.put(uuid, 0);
    }

    public boolean discoverFish(UUID uuid, String fishId) {
        if (uuid == null || fishId == null || fishId.isBlank()) {
            return false;
        }
        java.util.Set<String> discovered = discoveredFish.computeIfAbsent(uuid, k -> new java.util.HashSet<>());
        boolean added = discovered.add(fishId.toLowerCase());
        if (added && plugin.getPlayerConfig() != null) {
            plugin.getPlayerConfig().savePlayerData(uuid);
        }
        return added;
    }

    public boolean isFishDiscovered(UUID uuid, String fishId) {
        if (uuid == null || fishId == null) {
            return false;
        }
        return discoveredFish.getOrDefault(uuid, java.util.Collections.emptySet())
                .contains(fishId.toLowerCase());
    }

    public java.util.Set<String> getDiscoveredFish(UUID uuid) {
        return new java.util.HashSet<>(discoveredFish.getOrDefault(uuid, java.util.Collections.emptySet()));
    }

    public CatchResult recordCatch(UUID uuid, String fishId, double size, FishingQuality quality) {
        if (uuid == null || fishId == null || fishId.isBlank()) return new CatchResult(false, false);
        FishingQuality safeQuality = quality == null ? FishingQuality.NORMAL : quality;
        Map<String, FishRecord> records = fishRecords.computeIfAbsent(uuid, ignored -> new HashMap<>());
        FishRecord previous = records.getOrDefault(fishId, new FishRecord(0, 0.0, FishingQuality.NORMAL));
        boolean personalBest = size > previous.largestSize();
        boolean qualityUpgrade = safeQuality.ordinal() > previous.bestQuality().ordinal();
        records.put(fishId, new FishRecord(previous.caughtCount() + 1, Math.max(size, previous.largestSize()),
                qualityUpgrade ? safeQuality : previous.bestQuality()));
        return new CatchResult(personalBest, qualityUpgrade);
    }

    public FishRecord getFishRecord(UUID uuid, String fishId) {
        return fishRecords.getOrDefault(uuid, Map.of()).getOrDefault(fishId, new FishRecord(0, 0.0, FishingQuality.NORMAL));
    }

    public Map<String, FishRecord> getFishRecords(UUID uuid) {
        return new HashMap<>(fishRecords.getOrDefault(uuid, Map.of()));
    }

    public void setFishRecords(UUID uuid, Map<String, FishRecord> records) {
        fishRecords.put(uuid, records == null ? new HashMap<>() : new HashMap<>(records));
    }


    public double getDebugBiteSpeedMultiplier(UUID uuid) {
        if (uuid == null) return 1.0;
        return debugBiteSpeedMultipliers.getOrDefault(uuid, 1.0);
    }

    public void setDebugBiteSpeedMultiplier(UUID uuid, double multiplier) {
        if (uuid == null) return;
        if (multiplier <= 1.0) debugBiteSpeedMultipliers.remove(uuid);
        else debugBiteSpeedMultipliers.put(uuid, Math.min(100.0, multiplier));
    }

    public void clearDebugBiteSpeedMultiplier(UUID uuid) {
        if (uuid != null) debugBiteSpeedMultipliers.remove(uuid);
    }

    public record FishRecord(int caughtCount, double largestSize, FishingQuality bestQuality) { }
    public record CatchResult(boolean personalBest, boolean qualityUpgrade) { }

    public void setDiscoveredFish(UUID uuid, java.util.Collection<String> fishIds) {
        java.util.Set<String> set = new java.util.HashSet<>();
        if (fishIds != null) {
            for (String id : fishIds) {
                if (id != null && !id.isBlank()) {
                    set.add(id.toLowerCase());
                }
            }
        }
        discoveredFish.put(uuid, set);
    }

    /** Remove all fishing progress for a player. */
    public void clearPlayerData(UUID uuid) {
        fishingLevels.remove(uuid);
        fishingXp.remove(uuid);
        discoveredFish.remove(uuid);
        fishRecords.remove(uuid);
        debugBiteSpeedMultipliers.remove(uuid);
        if (plugin.getPlayerConfig() != null) {
            String path = "players." + uuid + ".fishing";
            plugin.getPlayerConfig().getConfig().set(path, null);
            plugin.getPlayerConfig().saveConfigFile();
        }
    }

    public int getMaxLevel() {
        return MAX_LEVEL;
    }
}
