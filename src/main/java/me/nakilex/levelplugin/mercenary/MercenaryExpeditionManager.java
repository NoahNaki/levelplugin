package me.nakilex.levelplugin.mercenary;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.time.Instant;
import java.util.*;

/**
 * Handles mercenary expeditions that leverage the dungeon list. The manager is
 * intentionally lightweight: it tracks timing and success chances while leaving
 * combat to the existing mercenary framework.
 */
public class MercenaryExpeditionManager {
    private final Plugin plugin;
    private final MercenaryAffinityManager affinityManager;
    private final Map<String, ExpeditionDefinition> expeditions = new HashMap<>();
    private final Map<UUID, ActiveExpedition> active = new HashMap<>();

    public MercenaryExpeditionManager(Plugin plugin, MercenaryAffinityManager affinityManager) {
        this.plugin = plugin;
        this.affinityManager = affinityManager;
        reload();
        startTick();
    }

    public void reload() {
        File cfgFile = new File(plugin.getDataFolder(), "mercenaries.yml");
        if (!cfgFile.exists()) {
            plugin.saveResource("mercenaries.yml", false);
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(cfgFile);
        expeditions.clear();
        ConfigurationSection section = cfg.getConfigurationSection("dungeons");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            String name = ChatColor.translateAlternateColorCodes('&', section.getString(id + ".name", id));
            int threat = section.getInt(id + ".threat", 0);
            int duration = section.getInt(id + ".base-duration-seconds", 600);
            expeditions.put(id, new ExpeditionDefinition(id, name, threat, duration));
        }
    }

    public Collection<ExpeditionDefinition> getExpeditions() {
        return expeditions.values();
    }

    public boolean isOnExpedition(UUID playerId) {
        return active.containsKey(playerId);
    }

    public ActiveExpedition getActive(UUID playerId) {
        return active.get(playerId);
    }

    public ActiveExpedition startExpedition(Player player, int npcId, ExpeditionDefinition definition) {
        int gs = affinityManager.getGearScore(npcId);
        double success = successChance(gs, definition.threat());
        int seconds = adjustedDuration(gs, definition.threat(), definition.baseDurationSeconds(),
                affinityManager.getFriendship(player.getUniqueId(), npcId).getLevel());
        ActiveExpedition expedition = new ActiveExpedition(npcId, definition, Instant.now().plusSeconds(seconds), success);
        active.put(player.getUniqueId(), expedition);
        player.sendMessage(ChatColor.GREEN + "Sent mercenary " + npcId + " to " + definition.displayName());
        return expedition;
    }

    public void complete(Player player) {
        ActiveExpedition expedition = active.remove(player.getUniqueId());
        if (expedition == null) {
            return;
        }
        boolean success = Math.random() * 100 <= expedition.getSuccessChance();
        if (success) {
            player.sendMessage(ChatColor.GREEN + "Expedition success! Rewards delivered from " + expedition.getDefinition().displayName());
        } else {
            player.sendMessage(ChatColor.RED + "Expedition failed. Your mercenary returns empty handed.");
        }
    }

    private double successChance(int gs, int threat) {
        if (threat <= 0) {
            return 100.0;
        }
        if (gs >= threat) {
            return 100.0;
        }
        double deficit = threat - gs;
        return Math.max(25.0, 100.0 - (deficit * 0.05));
    }

    private int adjustedDuration(int gs, int threat, int baseSeconds, int friendshipLevel) {
        double modifier = 1.0;
        if (gs > threat && threat > 0) {
            double ratio = (double) gs / (double) threat;
            modifier -= Math.min(0.35, (ratio - 1.0) * 0.15);
        }
        if (friendshipLevel >= 4) {
            modifier -= 0.1;
        }
        modifier = Math.max(0.5, modifier);
        return (int) Math.round(baseSeconds * modifier);
    }

    private void startTick() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Instant now = Instant.now();
                Iterator<Map.Entry<UUID, ActiveExpedition>> it = active.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<UUID, ActiveExpedition> entry = it.next();
                    if (now.isAfter(entry.getValue().getEndTime())) {
                        Player player = Bukkit.getPlayer(entry.getKey());
                        if (player != null && player.isOnline()) {
                            complete(player);
                        }
                        it.remove();
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L * 5);
    }
}
