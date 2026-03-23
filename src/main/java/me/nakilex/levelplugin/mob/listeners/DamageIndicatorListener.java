package me.nakilex.levelplugin.mob.listeners;

import me.nakilex.levelplugin.mob.managers.ChatToggleManager;
import me.nakilex.levelplugin.mob.managers.PlayerToggleManager;
import me.nakilex.levelplugin.player.attributes.listeners.StatsEffectListener;
import me.nakilex.levelplugin.utils.HologramUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class DamageIndicatorListener implements Listener {

    private final PlayerToggleManager toggleManager;
    private final ChatToggleManager chatToggleManager = ChatToggleManager.getInstance();

    public DamageIndicatorListener(PlayerToggleManager toggleManager) {
        this.toggleManager = toggleManager;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Player damager = resolveDamager(event.getDamager());
        if (damager == null) return;
        Entity target = event.getEntity();
        if (!(target instanceof LivingEntity)) return;

        if (!toggleManager.isEnabled(damager)) return;

        double damage = event.getFinalDamage();
        boolean isCrit = StatsEffectListener.consumeLastCrit(damager);

        // Color code
        ChatColor color = isCrit ? ChatColor.GOLD : ChatColor.RED;
        String text     = color + String.format(isCrit ? "-%.1f✦" : "-%.1f", damage);

        // Spawn the floating text
        LivingEntity mob = (LivingEntity) target;
        HologramUtil.spawnDamageHologram(damager, mob.getEyeLocation(), text);
        if (chatToggleManager.isEnabled(damager)) {
            damager.sendMessage(ChatColor.DARK_RED + "Damage: " + ChatColor.RED + String.format("%.1f", damage));
        }
    }

    private Player resolveDamager(Entity rawDamager) {
        if (rawDamager instanceof Player player) {
            return player;
        }
        if (!(rawDamager instanceof Projectile projectile)) {
            return null;
        }
        if (projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }
}
