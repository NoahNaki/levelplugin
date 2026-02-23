package me.nakilex.levelplugin.player.woodcutting.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.pet.PetEffectType;
import me.nakilex.levelplugin.player.attributes.lifeskill.LifeSkillProgression;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Handles Woodcutting profession XP and levels. */
public class WoodcuttingManager implements LifeSkillProgression {

    private static WoodcuttingManager instance;

    private final Main plugin;
    private final HashMap<UUID, Integer> levels = new HashMap<>();
    private final HashMap<UUID, Integer> xp = new HashMap<>();
    private final HashMap<UUID, BossBar> xpBars = new HashMap<>();
    private final Map<UUID, org.bukkit.scheduler.BukkitTask> hideTasks = new HashMap<>();
    private final Map<UUID, Boolean> activeBars = new HashMap<>();

    private final int MAX_LEVEL = 100;
    private final int XP_PER_LEVEL_MULTIPLIER = 220;

    public WoodcuttingManager(Main plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static WoodcuttingManager getInstance() {
        return instance;
    }

    public void initializePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        levels.putIfAbsent(uuid, 1);
        xp.putIfAbsent(uuid, 0);
        updateBossBar(player);
    }

    public void addXP(Player player, int amount) {
        if (player == null) return;
        addXP(player.getUniqueId(), amount);
    }

    @Override
    public void addXP(UUID uuid, int amount) {
        if (getLevel(uuid) >= MAX_LEVEL) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                markActive(player);
                updateBossBar(player);
            }
            return;
        }
        int adjusted = amount;
        if (plugin.getPetManager() != null) {
            adjusted = plugin.getPetManager().applyActiveEffectMultiplier(uuid, PetEffectType.GATHERING_XP_BOOST, adjusted);
        }
        int newXP = getXP(uuid) + adjusted;
        xp.put(uuid, newXP);
        checkLevelUp(uuid);
    }

    private void checkLevelUp(UUID uuid) {
        int level = getLevel(uuid);
        int currentXp = getXP(uuid);
        int xpNeeded = getXpRequired(level);
        boolean leveled = false;

        while (level < MAX_LEVEL && currentXp >= xpNeeded) {
            currentXp -= xpNeeded;
            level++;
            leveled = true;
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                sendLevelUpMessage(player, level, getXpRequired(level));
            }
            xpNeeded = getXpRequired(level);
        }

        levels.put(uuid, level);
        xp.put(uuid, currentXp);

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            markActive(player);
            updateBossBar(player);
            if (leveled) {
                updatePlayerTooltips(player);
            }
        }
        if (leveled && plugin.getPlayerConfig() != null) {
            plugin.getPlayerConfig().savePlayerData(uuid);
        }
    }

    @Override
    public int getXpRequired(int level) {
        return level * XP_PER_LEVEL_MULTIPLIER;
    }

    private void sendLevelUpMessage(Player player, int newLevel, int nextXp) {
        me.nakilex.levelplugin.utils.ChatFormatter.constructDivider(player, "§6§l-", 45);
        me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "§6§lWOODCUTTING LEVEL UP!");
        me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "");
        me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "§7You are now Woodcutting level §e§l" + newLevel + "§7!");
        if (newLevel < MAX_LEVEL) {
            me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "§7You need §e" + nextXp + " XP §7to reach level §e" + (newLevel + 1) + "§7.");
        }
        me.nakilex.levelplugin.utils.ChatFormatter.constructDivider(player, "§6§l-", 45);
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
        int currentXp = getXP(uuid);
        boolean atMax = level >= MAX_LEVEL;
        int required = atMax ? getXpRequired(MAX_LEVEL) : getXpRequired(level);
        me.nakilex.levelplugin.utils.LifeSkillBossBarUtil.updateBossBar(
                player,
                uuid,
                xpBars,
                activeBars,
                level,
                currentXp,
                atMax,
                required,
                "Woodcutting",
                ChatColor.GOLD,
                BarColor.GREEN
                BarColor.YELLOW
        );
    }

    private void markActive(Player player) {
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

    @Override
    public int getLevel(UUID uuid) {
        return levels.getOrDefault(uuid, 1);
    }

    public int getXP(Player player) {
        if (player == null) return 0;
        return getXP(player.getUniqueId());
    }

    @Override
    public int getXP(UUID uuid) {
        return xp.getOrDefault(uuid, 0);
    }

    @Override
    public void setLevel(UUID uuid, int level) {
        levels.put(uuid, level);
        xp.put(uuid, 0);
    }

    public void clearPlayerData(UUID uuid) {
        levels.remove(uuid);
        xp.remove(uuid);
        if (plugin.getPlayerConfig() != null) {
            String path = "players." + uuid + ".woodcutting";
            plugin.getPlayerConfig().getConfig().set(path, null);
            plugin.getPlayerConfig().saveConfigFile();
        }
    }

    @Override
    public int getMaxLevel() {
        return MAX_LEVEL;
    }
}
