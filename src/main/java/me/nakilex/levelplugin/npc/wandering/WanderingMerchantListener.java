package me.nakilex.levelplugin.npc.wandering;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Player;
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

    private boolean isMerchantNPC(NPC npc) {
        return npc != null && manager.isActive() && npc.equals(manager.getMerchant());
    }

    @EventHandler
    public void onRightClick(PlayerInteractEntityEvent e) {
        if (!CitizensAPI.getNPCRegistry().isNPC(e.getRightClicked())) return;
        NPC npc = CitizensAPI.getNPCRegistry().getNPC(e.getRightClicked());
        if (manager.isActive() && npc.equals(manager.getMerchant())) {
            e.setCancelled(true);
            manager.openShop(e.getPlayer());
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!manager.isActive()) return;
        if (!e.getEntity().getUniqueId().equals(manager.getMerchant().getEntity().getUniqueId())) return;
        Player damager = (e.getDamager() instanceof Player p) ? p : null;
        manager.handleDamage(damager);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        if (manager.isActive() && e.getEntity().getUniqueId().equals(manager.getMerchant().getEntity().getUniqueId())) {
            for (WanderingMerchantOffer of : manager.getGui().getOffers()) {
                if (of.getStock() > 0) {
                    e.getDrops().add(of.getItem());
                }
            }
            manager.despawn();
        }
    }
}
