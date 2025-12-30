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

    private final int MAX_LEVEL = 100;
    private final int XP_PER_LEVEL_MULTIPLIER = 200;

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
        if (getLevel(uuid) >= MAX_LEVEL) return;
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
        int required = getXpRequired(level);
        double progress = required <= 0 ? 1.0 : Math.min(1.0, Math.max(0.0, xp / (double) required));

        String title = ChatColor.GOLD + "" + ChatColor.BOLD + "Farming "
                + ChatColor.GRAY + "(Lv. " + ChatColor.WHITE + level + ChatColor.GRAY + ") "
                + ChatColor.DARK_GRAY + "| "
                + ChatColor.WHITE + xp + ChatColor.GRAY + "/" + ChatColor.WHITE + required;

        bar.setTitle(title);
        bar.setProgress(progress);
        if (!bar.getPlayers().contains(player)) {
            bar.addPlayer(player);
        }
        boolean visible = activeBars.getOrDefault(uuid, false);
        bar.setVisible(visible);
    }

    private void markFarmingActive(Player player) {
        UUID uuid = player.getUniqueId();
        activeBars.put(uuid, true);
        org.bukkit.scheduler.BukkitTask existing = hideTasks.remove(uuid);
        if (existing != null) {
            existing.cancel();
        }
        BossBar bar = xpBars.get(uuid);
        if (bar != null) {
            bar.setVisible(true);
        }
        hideTasks.put(uuid, Bukkit.getScheduler().runTaskLater(plugin, () -> {
            activeBars.put(uuid, false);
            BossBar b = xpBars.get(uuid);
            if (b != null) {
                b.setVisible(false);
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
