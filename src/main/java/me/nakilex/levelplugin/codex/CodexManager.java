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
    private final Set<String> npcKeys = new java.util.HashSet<>();
    private final Set<String> locationKeys = new java.util.HashSet<>();

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

    /* ----- Registration of known NPCs and Locations ----- */

    public void registerNpcKeys(java.util.Collection<String> keys) {
        npcKeys.addAll(keys);
    }

    public void registerLocationKeys(java.util.Collection<String> keys) {
        locationKeys.addAll(keys);
    }

    /* ----- Generic record helpers ----- */

    private void record(Player player, String category, String key) {
        UUID id = player.getUniqueId();
        String path = "players." + id + ".codex." + category + "." + key.toLowerCase();
        boolean first = !playerConfig.getConfig().getBoolean(path, false);
        playerConfig.getConfig().set(path, true);
        if (first) {
            player.sendTitle(ChatColor.GOLD + "CODEX UPDATED",
                             ChatColor.YELLOW + key + " discovered",
                             10, 40, 10);
        }
    }

    public void recordKill(Player player, String key) {
        String killsPath = "players." + player.getUniqueId() + ".codex.mobs." + key.toLowerCase() + ".kills";
        int kills = playerConfig.getConfig().getInt(killsPath, 0) + 1;
        playerConfig.getConfig().set(killsPath, kills);
        record(player, "mobs", key);
        playerConfig.saveConfigFile();
    }

    public void recordNpc(Player player, String name) {
        record(player, "npcs", name);
        playerConfig.saveConfigFile();
    }

    public void recordLocation(Player player, String name) {
        record(player, "locations", name);
        playerConfig.saveConfigFile();
    }

    public boolean hasDiscovered(UUID id, String key) {
        String path = "players." + id + ".codex.mobs." + key.toLowerCase() + ".kills";
        return playerConfig.getConfig().contains(path);
    }

    public boolean hasDiscoveredNpc(UUID id, String name) {
        String path = "players." + id + ".codex.npcs." + name.toLowerCase();
        return playerConfig.getConfig().getBoolean(path, false);
    }

    public boolean hasDiscoveredLocation(UUID id, String name) {
        String path = "players." + id + ".codex.locations." + name.toLowerCase();
        return playerConfig.getConfig().getBoolean(path, false);
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

    public int getDiscoveredNpcCount(UUID id) {
        String base = "players." + id + ".codex.npcs";
        if (!playerConfig.getConfig().isConfigurationSection(base)) return 0;
        return playerConfig.getConfig().getConfigurationSection(base).getKeys(false).size();
    }

    public int getDiscoveredLocationCount(UUID id) {
        String base = "players." + id + ".codex.locations";
        if (!playerConfig.getConfig().isConfigurationSection(base)) return 0;
        return playerConfig.getConfig().getConfigurationSection(base).getKeys(false).size();
    }

    public int getTotalMobCount() {
        return mobKeys.size() + bossKeys.size();
    }

    public int getTotalNpcCount() { return npcKeys.size(); }
    public int getTotalLocationCount() { return locationKeys.size(); }

    public List<String> getAllMobKeys() {
        List<String> all = new ArrayList<>();
        all.addAll(mobKeys);
        all.addAll(bossKeys);
        return all;
    }

    public java.util.List<String> getAllNpcKeys() { return new java.util.ArrayList<>(npcKeys); }
    public java.util.List<String> getAllLocationKeys() { return new java.util.ArrayList<>(locationKeys); }

    /* ----- Essence Management ----- */

    public void addEssence(UUID id, String key, MobEssence essence) {
        String path = "players." + id + ".essences." + key.toLowerCase();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>)(List<?>)
                playerConfig.getConfig().getMapList(path);
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
