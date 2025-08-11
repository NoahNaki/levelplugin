package me.nakilex.levelplugin.npc.wandering;

import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

    public WanderingMerchantListener(WanderingMerchantManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEntityEvent e) {
        if (!manager.isActive()) return;
        if (e.getRightClicked().equals(manager.getMerchant())) {
            e.setCancelled(true);
            manager.openShop(e.getPlayer());
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!manager.isActive()) return;
        if (!e.getEntity().equals(manager.getMerchant())) return;
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
            Player killer = e.getEntity().getKiller();
            String killerName = killer != null ? killer.getName() : "unknown";
            Bukkit.broadcastMessage(ChatColor.YELLOW + "[WM DEBUG] Merchant died (killer: " + killerName + ")");

            WanderingMerchantGUI gui = manager.getGui();
            if (gui == null) {
                Bukkit.broadcastMessage(ChatColor.YELLOW + "[WM DEBUG] GUI was null, no items dropped");
            } else {
                Bukkit.broadcastMessage(ChatColor.YELLOW + "[WM DEBUG] Offers: " + gui.getOffers().size());
                for (WanderingMerchantOffer offer : gui.getOffers()) {
                    if (offer.getStock() > 0) {
                        Bukkit.broadcastMessage(ChatColor.YELLOW + "[WM DEBUG] Dropping "
                                + offer.getStock() + "x " + offer.getItem().getType());
                        for (int i = 0; i < offer.getStock(); i++) {
                            e.getEntity().getWorld().dropItemNaturally(
                                    e.getEntity().getLocation(),
                                    offer.getItem().clone()
                            );
                        }
                    } else {
                        Bukkit.broadcastMessage(ChatColor.YELLOW + "[WM DEBUG] Offer "
                                + offer.getItem().getType() + " had no stock");
                    }
                }
            }
            Bukkit.broadcastMessage(ChatColor.YELLOW + "[WM DEBUG] Scheduling merchant despawn");
            // despawn on next tick so drops are not cleared
            Bukkit.getScheduler().runTask(Main.getInstance(), manager::despawn);
        }
        // handle llama deaths so fleeing can still work
        Entity entity = e.getEntity();
        if (entity instanceof TraderLlama llama) {
            manager.handleLlamaDeath(llama);
        }
    }
}
