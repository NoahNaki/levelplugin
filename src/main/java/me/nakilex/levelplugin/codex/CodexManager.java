package me.nakilex.levelplugin.codex;

import me.nakilex.levelplugin.mob.config.MobRewardsConfig;
import me.nakilex.levelplugin.player.config.PlayerConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.util.*;
import java.util.stream.Collectors;

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
            player.sendTitle(ChatColor.GOLD + "CODEX UPDATED",
                             ChatColor.YELLOW + key + " discovered",
                             10, 40, 10);
        }
        playerConfig.getConfig().set(path, kills + 1);
        playerConfig.saveConfigFile();
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

    /* ----- Essence Management ----- */

    public void addEssence(UUID id, String key, MobEssence essence) {
        String path = "players." + id + ".essences." + key.toLowerCase();
        List<Map<String,Object>> list = playerConfig.getConfig().getMapList(path);
        if (list == null) list = new ArrayList<>();
        list.add(essence.toMap());
        playerConfig.getConfig().set(path, list);
        playerConfig.saveConfigFile();
    }

    public List<MobEssence> getEssences(UUID id, String key) {
        String path = "players." + id + ".essences." + key.toLowerCase();
        List<Map<?,?>> list = playerConfig.getConfig().getMapList(path);
        if (list == null) return List.of();
        return list.stream().map(MobEssence::fromMap).collect(Collectors.toList());
    }

    /** 10% chance to generate and store a new essence on kill. */
    public void maybeGrantEssence(Player player, String key) {
        if (Math.random() < 0.10) {
            MobEssence essence = MobEssence.randomEssence();
            addEssence(player.getUniqueId(), key, essence);
            player.sendMessage(ChatColor.LIGHT_PURPLE + "New essence discovered for " + key + "!");
        }
    }
}
