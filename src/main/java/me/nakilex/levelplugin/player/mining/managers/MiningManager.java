package me.nakilex.levelplugin.player.mining.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.pet.PetEffectType;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.progression.TimedTierProgression;
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
 * Handles Mining profession XP and levels.
 */
public class MiningManager implements LifeSkillProgression {

    private static MiningManager instance;

    private final Main plugin;
    private final HashMap<UUID, Integer> miningLevels = new HashMap<>();
    private final HashMap<UUID, Integer> miningXp     = new HashMap<>();
    private final HashMap<UUID, BossBar> xpBars       = new HashMap<>();
    private final Map<UUID, org.bukkit.scheduler.BukkitTask> hideTasks = new HashMap<>();
    private final Map<UUID, Boolean> activeBars = new HashMap<>();
    private final Map<UUID, TimedTierProgression> momentumStates = new HashMap<>();

    private static final int MOMENTUM_MAX_TIER = 5;
    private static final int MOMENTUM_ORES_PER_TIER = 3;
    private static final long MOMENTUM_TIMEOUT_MS = 12_000L;
    private static final double MOMENTUM_DAMAGE_PER_TIER = 0.08;

    private final int MAX_LEVEL = 100;
    private final int XP_PER_LEVEL_MULTIPLIER = 200;

    public MiningManager(Main plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static MiningManager getInstance() {
        return instance;
    }

    public void initializePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        miningLevels.putIfAbsent(uuid, 1);
        miningXp.putIfAbsent(uuid, 0);
        momentumStates.computeIfAbsent(uuid, ignored -> createMomentumState());
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
                markMiningActive(player);
                updateBossBar(player);
            }
            return;
        }
        int adjusted = amount;
        if (plugin.getPetManager() != null) {
            adjusted = plugin.getPetManager().applyActiveEffectMultiplier(uuid, PetEffectType.GATHERING_XP_BOOST, adjusted);
        }
        int newXP = getXP(uuid) + adjusted;
        miningXp.put(uuid, newXP);
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

        miningLevels.put(uuid, level);
        miningXp.put(uuid, xp);

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            markMiningActive(player);
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


    public TimedTierProgression.Update recordMomentumOre(Player player) {
        if (player == null) return new TimedTierProgression.Update(1, 0, false);
        TimedTierProgression.Update update = momentumState(player.getUniqueId()).recordActivity(System.currentTimeMillis());
        if (update.tierIncreased()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.REWARD,
                    "Mining Momentum reached " + ChatColor.YELLOW + "Tier " + update.tier() + ChatColor.GOLD + "!");
            player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f,
                    Math.min(2.0f, 0.8f + (update.tier() * 0.15f)));
        }
        return update;
    }

    public int getMomentumTier(Player player) {
        if (player == null) return 1;
        return momentumState(player.getUniqueId()).getTier(System.currentTimeMillis());
    }

    public double getMomentumDamageMultiplier(Player player) {
        return 1.0 + ((getMomentumTier(player) - 1) * MOMENTUM_DAMAGE_PER_TIER);
    }

    public String getMomentumIndicator(Player player) {
        if (player == null) return null;
        TimedTierProgression state = momentumState(player.getUniqueId());
        long now = System.currentTimeMillis();
        int tier = state.getTier(now);
        int progress = state.getProgress(now);
        return ChatColor.AQUA + "Momentum " + ChatColor.YELLOW + "T" + tier
                + ChatColor.DARK_GRAY + " [" + progress + "/" + state.getActivitiesPerTier() + "]";
    }

    private TimedTierProgression momentumState(UUID uuid) {
        return momentumStates.computeIfAbsent(uuid, ignored -> createMomentumState());
    }

    private TimedTierProgression createMomentumState() {
        return new TimedTierProgression(MOMENTUM_MAX_TIER, MOMENTUM_ORES_PER_TIER, MOMENTUM_TIMEOUT_MS);
    }

    private void sendLevelUpMessage(Player player, int newLevel, int nextXp) {
        me.nakilex.levelplugin.utils.ChatFormatter.constructDivider(player, "§b§l-", 45);
        me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "§b§lMINING LEVEL UP!");
        me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "");
        me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "§7You are now Mining level §e§l" + newLevel + "§7!");
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
                me.nakilex.levelplugin.items.utils.ItemUtil.updateCustomToolTooltip(stack, player);
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
                "Mining",
                ChatColor.GRAY,
                BarColor.WHITE
        );
    }

    private void markMiningActive(Player player) {
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
        return miningLevels.getOrDefault(uuid, 1);
    }

    public int getXP(Player player) {
        if (player == null) return 0;
        return getXP(player.getUniqueId());
    }

    public int getXP(UUID uuid) {
        return miningXp.getOrDefault(uuid, 0);
    }

    public void setLevel(UUID uuid, int level) {
        miningLevels.put(uuid, level);
        miningXp.put(uuid, 0);
    }

    /** Remove all mining progress for a player. */
    public void clearPlayerData(UUID uuid) {
        miningLevels.remove(uuid);
        miningXp.remove(uuid);
        momentumStates.remove(uuid);
        if (plugin.getPlayerConfig() != null) {
            String path = "players." + uuid + ".mining";
            plugin.getPlayerConfig().getConfig().set(path, null);
            plugin.getPlayerConfig().saveConfigFile();
        }
    }

    public int getMaxLevel() {
        return MAX_LEVEL;
    }
}
