package me.nakilex.levelplugin.codex;

import me.nakilex.levelplugin.mob.config.MobRewardsConfig;
import me.nakilex.levelplugin.mob.utils.MobNameUtil;
import me.nakilex.levelplugin.player.config.PlayerConfig;
import me.nakilex.levelplugin.utils.NpcNameUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class CodexManager {
    private final PlayerConfig playerConfig;
    private final Set<String> mobKeys = new HashSet<>();
    private final Set<String> bossKeys = new HashSet<>();
    private final Set<String> canonicalBossKeys = new HashSet<>();
    // No predefined sets for NPCs or locations; they are tracked dynamically

    /** Kill milestones used for per-mob codex levels. */
    private static final int[] KILL_MILESTONES = {
            10, 25, 50, 100, 250, 500, 1000, 1500, 2500, 5000,
            10000, 15000, 25000, 50000
    };

    public CodexManager(PlayerConfig playerConfig,
                        MobRewardsConfig mobCfg,
                        FileConfiguration bossCfg) {
        this.playerConfig = playerConfig;
        reload(mobCfg, bossCfg);
    }

    public synchronized void reload(MobRewardsConfig mobCfg, FileConfiguration bossCfg) {
        mobKeys.clear();
        bossKeys.clear();
        canonicalBossKeys.clear();

        if (mobCfg != null && mobCfg.getConfig().isConfigurationSection("mobs")) {
            mobKeys.addAll(mobCfg.getConfig().getConfigurationSection("mobs").getKeys(false));
        }
        if (bossCfg != null && bossCfg.isConfigurationSection("mobs")) {
            bossCfg.getConfigurationSection("mobs").getKeys(false).forEach(key -> {
                bossKeys.add(key);
                String canonical = MobNameUtil.canonicalMobKey(key);
                if (!canonical.isEmpty()) {
                    canonicalBossKeys.add(canonical);
                }
            });
        }
    }

    public void recordKill(Player player, String key) {
        UUID id = player.getUniqueId();
        String path = "players." + id + ".codex.mobs." + key.toLowerCase() + ".kills";
        int kills = playerConfig.getConfig().getInt(path, 0);
        if (kills == 0) {
            String display = MobNameUtil.getPlainDisplayName(key);
            notifyDiscovery(player, "Monster", display);
        }
        playerConfig.getConfig().set(path, kills + 1);
        playerConfig.saveConfigFile();
    }

    /**
     * Notify a player that they have discovered a new codex entry using the
     * same styled message format as level ups and quest completion.
     */
    private void notifyDiscovery(Player player, String category, String name) {
        String title = ChatColor.WHITE + "" + ChatColor.BOLD + "CODEX UPDATED";
        String subtitle = ChatColor.GRAY + category + ": " + ChatColor.YELLOW + name;
        player.sendTitle(title, subtitle, 10, 40, 10);

        me.nakilex.levelplugin.utils.ChatFormatter.constructDivider(player, "§6§l-", 45);
        me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "§6§lCODEX UPDATED!");
        me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player,
                "§e" + category + ": §f" + name);
        me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player,
                "§7Use §f/codex §7to view your discoveries.");
        me.nakilex.levelplugin.utils.ChatFormatter.constructDivider(player, "§6§l-", 45);
    }

    public boolean hasDiscovered(UUID id, String key) {
        String path = "players." + id + ".codex.mobs." + key.toLowerCase() + ".kills";
        return playerConfig.getConfig().contains(path);
    }

    /**
     * Determine whether a player has discovered a mob that matches the canonical
     * identity of the given key. This allows configuration files that reference
     * the same MythicMob using different cases or word ordering (e.g. "Slime_King"
     * vs. "KING_SLIME") to still count as discovered once the player records the
     * entry in their codex.
     */
    public boolean hasDiscoveredIdentity(UUID id, String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        if (hasDiscovered(id, key)) {
            return true;
        }

        String canonical = MobNameUtil.canonicalMobKey(key);
        if (canonical.isEmpty()) {
            return false;
        }

        String base = "players." + id + ".codex.mobs";
        var section = playerConfig.getConfig().getConfigurationSection(base);
        if (section == null) {
            return false;
        }

        for (String discoveredKey : section.getKeys(false)) {
            if (canonical.equals(MobNameUtil.canonicalMobKey(discoveredKey))) {
                return true;
            }
        }
        return false;
    }

    public int getKillCount(UUID id, String key) {
        String path = "players." + id + ".codex.mobs." + key.toLowerCase() + ".kills";
        return playerConfig.getConfig().getInt(path, 0);
    }

    /**
     * Current codex level for a mob based on total kills.
     */
    public int getMobLevel(UUID id, String key) {
        int kills = getKillCount(id, key);
        int level = 0;
        while (level < KILL_MILESTONES.length && kills >= KILL_MILESTONES[level]) {
            level++;
        }
        return level;
    }

    /** Kills required to reach a given codex level (1-indexed). */
    public int getKillsForLevel(int level) {
        if (level <= 0) return 0;
        if (level > KILL_MILESTONES.length) return KILL_MILESTONES[KILL_MILESTONES.length - 1];
        return KILL_MILESTONES[level - 1];
    }

    /** Fractional progress toward the next codex level for a mob. */
    public double getMobProgress(UUID id, String key) {
        int kills = getKillCount(id, key);
        int level = getMobLevel(id, key);
        if (level >= KILL_MILESTONES.length) return 1.0;
        int prev = getKillsForLevel(level);
        int next = getKillsForLevel(level + 1);
        return (double) (kills - prev) / (next - prev);
    }

    public int getMaxMobLevel() {
        return KILL_MILESTONES.length;
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

    /** Determine whether the given mob key represents a field boss. */
    public boolean isFieldBoss(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        if (bossKeys.stream().anyMatch(b -> b.equalsIgnoreCase(key))) {
            return true;
        }

        String canonical = MobNameUtil.canonicalMobKey(key);
        if (canonical.isEmpty()) {
            return false;
        }
        return canonicalBossKeys.contains(canonical);
    }

    /* ----- NPC Tracking ----- */
    public void recordNpc(Player player, String name) {
        UUID id = player.getUniqueId();
        String normalized = normalizeNpcName(name);
        if (normalized.isEmpty()) {
            return;
        }
        String path = "players." + id + ".codex.npcs." + normalized;
        if (!playerConfig.getConfig().contains(path)) {
            playerConfig.getConfig().set(path, true);
            playerConfig.saveConfigFile();
            notifyDiscovery(player, "NPC", name);
        }
    }

    public List<String> getDiscoveredNpcs(UUID id) {
        String base = "players." + id + ".codex.npcs";
        if (!playerConfig.getConfig().isConfigurationSection(base)) return java.util.Collections.emptyList();
        List<String> discovered = new ArrayList<>();
        for (String key : playerConfig.getConfig().getConfigurationSection(base).getKeys(false)) {
            String normalized = normalizeNpcName(key);
            if (!normalized.isEmpty() && !discovered.contains(normalized)) {
                discovered.add(normalized);
            }
        }
        return discovered;
    }

    public int getDiscoveredNpcCount(UUID id) {
        return getDiscoveredNpcs(id).size();
    }

    public int getTotalNpcCount() {
        return getAllNpcs().size();
    }

    public List<NPC> getAllNpcs() {
        Map<String, NPC> uniqueByName = new LinkedHashMap<>();
        CitizensAPI.getNPCRegistry().forEach(npc -> {
            String normalized = normalizeNpcName(npc.getName());
            if (!normalized.isEmpty() && !uniqueByName.containsKey(normalized)) {
                uniqueByName.put(normalized, npc);
            }
        });
        return new ArrayList<>(uniqueByName.values());
    }

    private String normalizeNpcName(String name) {
        String normalized = NpcNameUtil.normalize(name);
        return normalized == null ? "" : normalized;
    }

    /* ----- Location Tracking ----- */
    public void recordLocation(Player player, String name) {
        UUID id = player.getUniqueId();
        String path = "players." + id + ".codex.locations." + name.toLowerCase();
        if (!playerConfig.getConfig().contains(path)) {
            playerConfig.getConfig().set(path, true);
            playerConfig.saveConfigFile();
            notifyDiscovery(player, "Location", name);
        }
    }

    public List<String> getDiscoveredLocations(UUID id) {
        String base = "players." + id + ".codex.locations";
        if (!playerConfig.getConfig().isConfigurationSection(base)) return java.util.Collections.emptyList();
        return new ArrayList<>(playerConfig.getConfig().getConfigurationSection(base).getKeys(false));
    }

    /**
     * Remove all codex discovery data for a player.
     */
    public void clearPlayerData(UUID id) {
        String path = "players." + id + ".codex";
        playerConfig.getConfig().set(path, null);
        playerConfig.saveConfigFile();
    }
}
