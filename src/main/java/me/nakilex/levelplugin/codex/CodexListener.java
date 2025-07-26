package me.nakilex.levelplugin.codex;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.nakilex.levelplugin.mob.config.MobRewardsConfig;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class CodexListener implements Listener {
    private final MobRewardsConfig mobCfg;
    private final FileConfiguration bossCfg;
    private final CodexManager manager;

    public CodexListener(MobRewardsConfig mobCfg,
                         FileConfiguration bossCfg,
                         CodexManager manager) {
        this.mobCfg = mobCfg;
        this.bossCfg = bossCfg;
        this.manager = manager;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        LivingEntity ent = event.getEntity();
        ActiveMob mob = MythicBukkit.inst().getAPIHelper().getMythicMobInstance(ent);
        if (mob == null) return;

        String mobType = mob.getMobType().replaceAll("§.", "");
        if (mobCfg.getConfig().contains("mobs." + mobType)) {
            manager.recordKill(killer, mobType);
            return;
        }

        if (bossCfg.isConfigurationSection("mobs")) {
            String name = ChatColor.stripColor(ent.getName());
            for (String key : bossCfg.getConfigurationSection("mobs").getKeys(false)) {
                if (key.equalsIgnoreCase(name)) {
                    manager.recordKill(killer, key);
                    break;
                }
            }
        }
    }
}
