package me.nakilex.levelplugin.npc.wandering;

import org.bukkit.entity.Player;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TraderLlama;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.attribute.Attribute;

/** Handles interactions with the wandering merchant NPC. */
public class WanderingMerchantListener implements Listener {
    private final WanderingMerchantManager manager;

    public WanderingMerchantListener(WanderingMerchantManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEntityEvent e) {
        if (!manager.isActive()) return;
        if (!CitizensAPI.getNPCRegistry().isNPC(e.getRightClicked())) return;
        NPC npc = CitizensAPI.getNPCRegistry().getNPC(e.getRightClicked());
        if (npc.getId() == manager.getMerchant().getId()) {
            e.setCancelled(true);
            manager.openShop(e.getPlayer());
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!manager.isActive()) return;
        if (!CitizensAPI.getNPCRegistry().isNPC(e.getEntity())) return;
        NPC npc = CitizensAPI.getNPCRegistry().getNPC(e.getEntity());
        if (npc.getId() != manager.getMerchant().getId()) return;
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
        if (!manager.isActive()) return;
        if (!CitizensAPI.getNPCRegistry().isNPC(e.getEntity())) return;
        NPC npc = CitizensAPI.getNPCRegistry().getNPC(e.getEntity());
        if (npc.getId() == manager.getMerchant().getId()) {
            for (WanderingMerchantOffer of : manager.getGui().getOffers()) {
                if (of.getStock() > 0) {
                    e.getDrops().add(of.getItem());
                }
            }
            manager.despawn();
        }
        // handle llama deaths so fleeing can still work
        Entity entity = e.getEntity();
        if (entity instanceof TraderLlama llama) {
            manager.handleLlamaDeath(llama);
        }
    }
}
