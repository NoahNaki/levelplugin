package me.nakilex.levelplugin.mob.managers;

import io.lumine.mythic.bukkit.events.MythicMobSpawnEvent;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import me.nakilex.levelplugin.mob.utils.MobNameUtil;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

public class MythicMobNameManager implements Listener {

    private final Main plugin;
    private final BukkitAPIHelper mythicHelper = MythicBukkit.inst().getAPIHelper();
    private final Set<ActiveMob> trackedMobs = new HashSet<>();
    /** Holds all boss‑keys exactly as in your YAML (e.g. "KING SLIME", "TERRACOTTA GENERAL", etc.) */
    private final Set<String> fieldBossKeys;
    /** MythicMob IDs that should display HP bars (from mob_rewards.yml). */
    private final Set<String> rewardMobKeys;

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
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet());
        } else {
            this.fieldBossKeys = new HashSet<>();
        }

        FileConfiguration rewardsCfg = plugin.getMobRewardsConfig().getConfig();
        if (rewardsCfg.isConfigurationSection("mobs")) {
            this.rewardMobKeys = rewardsCfg.getConfigurationSection("mobs")
                    .getKeys(false)
                    .stream()
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet());
        } else {
            this.rewardMobKeys = new HashSet<>();
        }

        // ─── Schedule the name‐updater ────────────────────────────────────────────────
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateMobNames, 5L, 5L);

        // ─── Register listener ───────────────────────────────────────────────────────
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onMythicMobSpawn(MythicMobSpawnEvent event) {
        ActiveMob mob = event.getMob();
        String type = mob.getMobType();
        if (!rewardMobKeys.contains(type.toUpperCase())) {
            return;
        }
        trackedMobs.add(mob);
        setDisplayName(mob);
    }

    @EventHandler
    public void onMythicMobDeath(EntityDeathEvent event) {
        ActiveMob mob = mythicHelper.getMythicMobInstance(event.getEntity());
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
        if (!rewardMobKeys.contains(mob.getMobType().toUpperCase())) {
            return;
        }
        int    level     = (int) mob.getLevel();
        double currentHP = mob.getEntity().getHealth();
        double maxHP     = mob.getEntity().getMaxHealth();

        String rawType    = mob.getMobType();        // e.g. "KING_SLIME"
        String prettyType = MobNameUtil.toPrettyName(rawType);  // → "King Slime"

        // ─── normalize for lookup using the raw mob ID
        String lookupKey = rawType.toUpperCase();

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

}
