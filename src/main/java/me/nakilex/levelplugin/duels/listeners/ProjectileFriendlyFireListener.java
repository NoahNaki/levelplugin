package me.nakilex.levelplugin.duels.listeners;

import me.nakilex.levelplugin.duels.managers.DuelManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class ProjectileFriendlyFireListener implements Listener {

    private static final String ARCHER_META = "ArcherSpell";   // ⇠ keep in one place

    private final DuelManager duels = DuelManager.getInstance();

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjPvp(EntityDamageByEntityEvent e) {

        // Only players taking damage
        if (!(e.getEntity() instanceof Player victim)) return;

        Entity rawDamager = e.getDamager();
        if (!(rawDamager instanceof Projectile proj)) return;
        if (!proj.hasMetadata(ARCHER_META)) return;            // <— our filter

        Object s = proj.getShooter();
        if (!(s instanceof Player attacker)) return;           // shooter must be a player
        if (attacker.equals(victim)) return;                   // self-hits allowed

        if (!duels.areInDuel(attacker.getUniqueId(), victim.getUniqueId())) {
            e.setCancelled(true);
            attacker.sendMessage(ChatColor.RED + "You can only damage players you’re duelling!");
        }
    }
}
