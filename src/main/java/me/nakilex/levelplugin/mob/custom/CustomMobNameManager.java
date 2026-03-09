package me.nakilex.levelplugin.mob.custom;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.mob.utils.MobNameUtil;
import me.nakilex.levelplugin.utils.EntityTextDisplay;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class CustomMobNameManager implements Listener {

    private final Main plugin;
    private final CustomMobManager mobManager;
    private final Map<UUID, EntityTextDisplay> healthDisplays = new HashMap<>();

    public CustomMobNameManager(Main plugin, CustomMobManager mobManager) {
        this.plugin = plugin;
        this.mobManager = mobManager;
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateMobNames, 5L, 5L);
    }

    public void track(CustomMobInstance instance) {
        updateDisplay(instance);
    }

    public void untrack(UUID uuid) {
        EntityTextDisplay display = healthDisplays.remove(uuid);
        if (display != null) {
            display.remove();
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        mobManager.getInstance(entity).ifPresent(inst -> {
            untrack(entity.getUniqueId());
            entity.removeMetadata("lp_numeric_hp", plugin);
        });
    }

    private void updateMobNames() {
        Iterator<Map.Entry<UUID, CustomMobInstance>> it = mobManager.getActiveMobs().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, CustomMobInstance> entry = it.next();
            CustomMobInstance instance = entry.getValue();
            if (instance == null || instance.entity() == null || instance.entity().isDead()) {
                if (instance != null && instance.entity() != null) {
                    untrack(instance.entity().getUniqueId());
                    instance.entity().removeMetadata("lp_numeric_hp", plugin);
                }
                it.remove();
            } else {
                updateDisplay(instance);
            }
        }
    }

    private void updateDisplay(CustomMobInstance instance) {
        LivingEntity entity = instance.entity();
        if (entity == null) {
            return;
        }
        double currentHP = entity.getHealth();
        double maxHP = entity.getMaxHealth();
        String prettyType = ChatColor.stripColor(instance.definition().displayName());
        ChatColor nameColor = instance.definition().boss() ? ChatColor.YELLOW : ChatColor.WHITE;
        String displayName = MobNameUtil.buildHealthName(instance.level(), nameColor, prettyType, currentHP, maxHP);
        // Keep vanilla nameplate empty; we only want the hologram display.
        entity.setCustomName(null);
        entity.setCustomNameVisible(false);
        entity.setMetadata("lp_numeric_hp", new FixedMetadataValue(plugin, true));
        EntityTextDisplay disp = healthDisplays.computeIfAbsent(entity.getUniqueId(),
                id -> new EntityTextDisplay(plugin, entity, 0.5));
        disp.update(displayName);
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() != plugin) return;
        for (EntityTextDisplay display : healthDisplays.values()) {
            display.remove();
        }
        EntityTextDisplay.removeAllDisplays();
        healthDisplays.clear();
    }
}
