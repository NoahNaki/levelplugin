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
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.server.PluginDisableEvent;
import me.nakilex.levelplugin.mob.utils.MobNameUtil;
import me.nakilex.levelplugin.utils.EntityTextDisplay;
import org.bukkit.metadata.FixedMetadataValue;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;

public class MythicMobNameManager implements Listener {

    private final Main plugin;
    private final BukkitAPIHelper mythicHelper = MythicBukkit.inst().getAPIHelper();
    private final Set<ActiveMob> trackedMobs = new HashSet<>();
    private final Set<LivingEntity> trackedZombies = new HashSet<>();
    private final Map<UUID, EntityTextDisplay> healthDisplays = new HashMap<>();
    private static final Set<EntityType> ZOMBIE_TYPES = java.util.EnumSet.of(
            EntityType.ZOMBIE,
            EntityType.HUSK,
            EntityType.DROWNED,
            EntityType.ZOMBIE_VILLAGER,
            EntityType.ZOMBIFIED_PIGLIN
    );
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
                    .map(String::toUpperCase)
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
        // Only track mobs that have rewards configured
        if (plugin.getMobRewardsConfig().getMobSection(mob.getMobType()) == null) {
            return;
        }
        Entity base = mob.getEntity().getBukkitEntity();
        trackedMobs.add(mob);
        setDisplayName(mob);
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        if (ZOMBIE_TYPES.contains(entity.getType())) {
            trackedZombies.add(entity);
            setDisplayName(entity);
        }
    }

    @EventHandler
    public void onMythicMobDeath(EntityDeathEvent event) {
        ActiveMob mob = mythicHelper.getMythicMobInstance(event.getEntity());
        if (mob != null) {
            trackedMobs.remove(mob);
        }
        LivingEntity entity = event.getEntity();
        trackedZombies.remove(entity);
        EntityTextDisplay disp = healthDisplays.remove(entity.getUniqueId());
        if (disp != null) {
            disp.remove();
        }
        entity.removeMetadata("lp_numeric_hp", plugin);
    }

    private void updateMobNames() {
        Iterator<ActiveMob> it = trackedMobs.iterator();
        while (it.hasNext()) {
            ActiveMob mob = it.next();
            if (mob == null || mob.getEntity() == null || mob.getEntity().isDead()) {
                if (mob != null && mob.getEntity() != null) {
                    Entity base = mob.getEntity().getBukkitEntity();
                    if (base instanceof LivingEntity be) {
                        EntityTextDisplay disp = healthDisplays.remove(be.getUniqueId());
                        if (disp != null) {
                            disp.remove();
                        }
                        be.removeMetadata("lp_numeric_hp", plugin);
                    }
                }
                it.remove();
            } else {
                setDisplayName(mob);
            }
        }

        Iterator<LivingEntity> itZ = trackedZombies.iterator();
        while (itZ.hasNext()) {
            LivingEntity mob = itZ.next();
            if (mob == null || mob.isDead()) {
                itZ.remove();
            } else {
                setDisplayName(mob);
            }
        }
    }

    private void setDisplayName(ActiveMob mob) {
        if (plugin.getMobRewardsConfig().getMobSection(mob.getMobType()) == null) {
            return;
        }
        Entity raw = mob.getEntity().getBukkitEntity();
        if (!(raw instanceof LivingEntity entity)) {
            plugin.getLogger().warning("[NameManager] Cannot set name for " + mob.getMobType()
                    + " because bukkit entity is " + raw.getType()
                    + " (" + raw.getClass().getSimpleName() + ")");
            return;
        }

        int    level     = (int) mob.getLevel();
        double currentHP = entity.getHealth();
        double maxHP     = entity.getMaxHealth();

        String rawType    = mob.getMobType();        // e.g. "KING_SLIME"
        String prettyType = MobNameUtil.getPlainDisplayName(rawType);  // → Mythic display name

        // ─── normalize for lookup using the raw mob ID
        String lookupKey = rawType.toUpperCase();

        // ─── if it's in your field_boss list, color yellow, otherwise white
        ChatColor nameColor = fieldBossKeys.contains(lookupKey)
            ? ChatColor.YELLOW
            : ChatColor.WHITE;
        String displayName = MobNameUtil.buildHealthName(level, nameColor, prettyType, currentHP, maxHP);

        entity.setCustomName(displayName);
        entity.setCustomNameVisible(false);
        entity.setMetadata("lp_numeric_hp", new FixedMetadataValue(plugin, true));
        EntityTextDisplay disp = healthDisplays.computeIfAbsent(entity.getUniqueId(),
                id -> new EntityTextDisplay(plugin, entity, 0.5));
        disp.update(displayName);
    }

    private void setDisplayName(LivingEntity mob) {
        double currentHP = mob.getHealth();
        double maxHP = mob.getMaxHealth();
        String prettyType = MobNameUtil.toPrettyName(mob.getType().name());
        String displayName = MobNameUtil.buildHealthName(1, ChatColor.WHITE, prettyType, currentHP, maxHP);
        mob.setCustomName(displayName);
        mob.setCustomNameVisible(true);
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() != plugin) return;
        for (EntityTextDisplay disp : healthDisplays.values()) {
            disp.remove();
        }
        EntityTextDisplay.removeAllDisplays();
        healthDisplays.clear();
        trackedMobs.forEach(m -> {
            Entity base = m.getEntity().getBukkitEntity();
            if (base instanceof LivingEntity be) {
                be.removeMetadata("lp_numeric_hp", plugin);
            }
        });
        trackedZombies.forEach(m -> m.removeMetadata("lp_numeric_hp", plugin));
        trackedMobs.clear();
        trackedZombies.clear();
    }

}
