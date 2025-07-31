package me.nakilex.levelplugin.codex;

import me.nakilex.levelplugin.mob.config.MobRewardsConfig;
import me.nakilex.levelplugin.player.config.PlayerConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.util.*;

public class CodexManager {
    private final PlayerConfig playerConfig;
    private final Set<String> mobKeys;
    private final Set<String> bossKeys;

    public CodexManager(PlayerConfig playerConfig,
                        MobRewardsConfig mobCfg,
                        FileConfiguration bossCfg) {
        this.playerConfig = playerConfig;
        this.mobKeys = mobCfg.getConfig().isConfigurationSection("mobs")
                ? mobCfg.getConfig().getConfigurationSection("mobs").getKeys(false)
                : Collections.emptySet();
        this.bossKeys = bossCfg.isConfigurationSection("mobs")
                ? bossCfg.getConfigurationSection("mobs").getKeys(false)
                : Collections.emptySet();
    }

    public void recordKill(Player player, String key) {
        UUID id = player.getUniqueId();
        String path = "players." + id + ".codex.mobs." + key.toLowerCase() + ".kills";
        int kills = playerConfig.getConfig().getInt(path, 0);
        if (kills == 0) {
            notifyDiscovery(player, "Monster", key);
        }
        playerConfig.getConfig().set(path, kills + 1);
        playerConfig.saveConfigFile();
    }

    private void notifyDiscovery(Player player, String category, String name) {
        String title = ChatColor.WHITE + "" + ChatColor.BOLD + "CODEX UPDATED";
        String subtitle = ChatColor.GRAY + category + ": " + ChatColor.YELLOW + name;
        player.sendTitle(title, subtitle, 10, 40, 10);
        me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player,
                ChatColor.WHITE + "" + ChatColor.BOLD + "CODEX UPDATED" +
                ChatColor.GRAY + category + ": " + ChatColor.YELLOW + ChatColor.BOLD + name +
                ChatColor.GRAY + " Check it now by using " + ChatColor.WHITE + "/codex" +
                ChatColor.GRAY + " Rewards: " + ChatColor.GREEN + "50XP");
    }

    public boolean hasDiscovered(UUID id, String key) {
        String path = "players." + id + ".codex.mobs." + key.toLowerCase() + ".kills";
        return playerConfig.getConfig().contains(path);
    }

    public int getKillCount(UUID id, String key) {
        String path = "players." + id + ".codex.mobs." + key.toLowerCase() + ".kills";
        return playerConfig.getConfig().getInt(path, 0);
    }

    public int getDiscoveredMobCount(UUID id) {
        String base = "players." + id + ".codex.mobs";
        if (!playerConfig.getConfig().isConfigurationSection(base)) return 0;
        return playerConfig.getConfig().getConfigurationSection(base).getKeys(false).size();
    }

    public int getTotalMobCount() {
        return mobKeys.size() + bossKeys.size();
    }

    public List<String> getAllMobKeys() {
        List<String> all = new ArrayList<>();
        all.addAll(mobKeys);
        all.addAll(bossKeys);
        return all;
    }
}
