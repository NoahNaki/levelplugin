package me.nakilex.levelplugin.mob.managers;

import io.lumine.mythic.bukkit.events.MythicMobSpawnEvent;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

public class MythicMobNameManager implements Listener {

    private final Main plugin;
    private final Set<ActiveMob> trackedMobs = new HashSet<>();
    /** Holds all boss‑keys exactly as in your YAML (e.g. "KING SLIME", "TERRACOTTA GENERAL", etc.) */
    private final Set<String> fieldBossKeys;

    public MythicMobNameManager(Main plugin) {
        this.plugin = plugin;

        // ─── Load your field bosses from field_bosses.yml ─────────────────────────────
        File bossesFile = new File(plugin.getDataFolder(), "field_bosses.yml");
        FileConfiguration bossCfg = YamlConfiguration.loadConfiguration(bossesFile);
        if (bossCfg.isConfigurationSection("mobs")) {
            this.fieldBossKeys = bossCfg
                .getConfigurationSection("mobs")
                .getKeys(false)
                .stream()
                .map(String::toUpperCase)    // normalize to uppercase
                .collect(Collectors.toSet());
        } else {
            this.fieldBossKeys = new HashSet<>();
        }

        // ─── Schedule the name‐updater ────────────────────────────────────────────────
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateMobNames, 5L, 5L);

        // ─── Register listener ───────────────────────────────────────────────────────
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onMythicMobSpawn(MythicMobSpawnEvent event) {
        ActiveMob mob = event.getMob();
        trackedMobs.add(mob);
        setDisplayName(mob);
    }

    @EventHandler
    public void onMythicMobDeath(EntityDeathEvent event) {
        ActiveMob mob = plugin.getMythicHelper().getMythicMobInstance(event.getEntity());
        if (mob != null) {
            trackedMobs.remove(mob);
        }
    }

    private void updateMobNames() {
        Iterator<ActiveMob> it = trackedMobs.iterator();
        while (it.hasNext()) {
            ActiveMob mob = it.next();
            if (mob == null
                || mob.getEntity() == null
                || mob.getEntity().isDead()) {
                it.remove();
            } else {
                setDisplayName(mob);
            }
        }
    }

    private void setDisplayName(ActiveMob mob) {
        int    level     = (int) mob.getLevel();
        double currentHP = mob.getEntity().getHealth();
        double maxHP     = mob.getEntity().getMaxHealth();

        String rawType    = mob.getMobType();        // e.g. "KING_SLIME"
        String prettyType = formatMobName(rawType);  // → "King Slime"

        // ─── normalize for lookup: uppercase with spaces
        String lookupKey = prettyType.toUpperCase(); // → "KING SLIME"

        // ─── if it's in your field_boss list, color yellow, otherwise white
        ChatColor nameColor = fieldBossKeys.contains(lookupKey)
            ? ChatColor.YELLOW
            : ChatColor.WHITE;

        String displayName = ChatColor.GRAY + "[Lv " + level + "] "
            + nameColor + prettyType + " "
            + ChatColor.RED + (int)currentHP + "/" + (int)maxHP + " \u2764";

        mob.getEntity().getBukkitEntity().setCustomName(displayName);
        mob.getEntity().getBukkitEntity().setCustomNameVisible(true);
    }

    /**
     * Converts "RANCID_PIG_ZOMBIE" → "Rancid Pig Zombie"
     */
    private String formatMobName(String rawName) {
        String[] parts = rawName.split("_");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].toLowerCase();
            if (!part.isEmpty()) {
                parts[i] = part.substring(0, 1).toUpperCase() + part.substring(1);
            }
        }
        return String.join(" ", parts);
    }
}
