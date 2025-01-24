package me.nakilex.levelplugin.mob.listeners;

import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class MythicMobDamageListener implements Listener {

    private final BukkitAPIHelper mythicHelper;

    public MythicMobDamageListener() {
        this.mythicHelper = MythicBukkit.inst().getAPIHelper();
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Check if the damager is a player
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getDamager();

        // Check if the entity being damaged is a MythicMob
        ActiveMob mythicMob = mythicHelper.getMythicMobInstance(event.getEntity());
        if (mythicMob == null) {
            return;
        }

        // Calculate damage dealt
        double damage = event.getFinalDamage();

        // Format and send a message to the player
        player.sendMessage(String.format("§eYou dealt §c%.2f §edamage to §6%s§e!",
            damage,
            mythicMob.getMobType()));
    }
}
