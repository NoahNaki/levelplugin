package me.nakilex.levelplugin.mob.listeners;

import me.nakilex.levelplugin.mob.utils.MobNameUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.SlimeSplitEvent;

public class SlimeSplitListener implements Listener {
    private static final String COMMON_SLIME_KEY = "Slime_Common";
    private static final String COMMON_SLIME_CANONICAL = MobNameUtil.canonicalMobKey(COMMON_SLIME_KEY);

    @EventHandler
    public void onSlimeSplit(SlimeSplitEvent event) {
        if (event.isCancelled()) {
            return;
        }
        Slime slime = event.getEntity();
        if (slime == null) {
            return;
        }
        String mobId = MobNameUtil.resolveCustomMobId(slime).orElse(null);
        if (isCommonSlime(mobId)) {
            event.setCancelled(true);
            return;
        }
        String name = slime.getCustomName();
        if (name == null || name.isBlank()) {
            name = slime.getName();
        }
        if (isCommonSlime(ChatColor.stripColor(name))) {
            event.setCancelled(true);
        }
    }

    private boolean isCommonSlime(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String canonical = MobNameUtil.canonicalMobKey(value);
        return !canonical.isEmpty() && canonical.equalsIgnoreCase(COMMON_SLIME_CANONICAL);
    }
}
