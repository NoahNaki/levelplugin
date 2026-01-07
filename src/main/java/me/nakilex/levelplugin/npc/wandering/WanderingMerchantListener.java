package me.nakilex.levelplugin.npc.wandering;

import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TraderLlama;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/** Handles interactions with the wandering merchant NPC. */
public class WanderingMerchantListener implements Listener {
    private final WanderingMerchantManager manager;
    private final java.util.Map<java.util.UUID, Long> recentInteractions = new java.util.HashMap<>();
    private static final long INTERACT_SUPPRESSION_MS = 200L;

    public WanderingMerchantListener(WanderingMerchantManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEntityEvent e) {
        if (!manager.isActive()) return;
        if (e.getRightClicked().equals(manager.getMerchant())) {
            e.setCancelled(true);
            recentInteractions.put(e.getPlayer().getUniqueId(), System.currentTimeMillis());
            manager.openShop(e.getPlayer());
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!manager.isActive()) return;
        if (!e.getEntity().equals(manager.getMerchant())) return;
        if (e.getDamager() instanceof Player player) {
            Long last = recentInteractions.get(player.getUniqueId());
            if (last != null && System.currentTimeMillis() - last < INTERACT_SUPPRESSION_MS) {
                e.setCancelled(true);
                return;
            }
        }
        manager.recordHit();

        if (e.getEntity() instanceof org.bukkit.entity.LivingEntity le) {
            var attr = le.getAttribute(Attribute.MAX_HEALTH);
            if (attr != null) {
                e.setDamage(attr.getValue() * 0.10);
            }
        }

        if (e.getDamager() instanceof Player p) {
            manager.damage(p);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        var merchant = manager.getMerchant();
        if (merchant == null) return;
        if (e.getEntity().getUniqueId().equals(merchant.getUniqueId())) {
            e.setDroppedExp(0);
            // despawn on next tick so drops are not cleared
            Bukkit.getScheduler().runTask(Main.getInstance(), manager::despawn);
        }
        // handle llama deaths so fleeing can still work
        Entity entity = e.getEntity();
        if (entity instanceof TraderLlama llama) {
            manager.handleLlamaDeath(llama);
            e.setDroppedExp(0);
        }
    }
}
