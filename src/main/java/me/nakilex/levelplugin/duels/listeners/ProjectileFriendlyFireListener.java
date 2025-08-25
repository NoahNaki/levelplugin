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
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

public class ProjectileFriendlyFireListener implements Listener {

    public static final String ARCHER_META = "ArcherSpell";   // ⇠ keep in one place
    public static final String MYTHIC_META = "MythicSkill";

    private final DuelManager duels = DuelManager.getInstance();
    private final Plugin plugin;

    public ProjectileFriendlyFireListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent e) {
        Projectile proj = e.getEntity();
        if (!(proj.getShooter() instanceof Player shooter)) return;
        if (!shooter.hasMetadata(MYTHIC_META)) return;
        String skill = shooter.getMetadata(MYTHIC_META).get(0).asString();
        proj.setMetadata(MYTHIC_META, new FixedMetadataValue(plugin, skill));
        shooter.removeMetadata(MYTHIC_META, plugin);
        plugin.getLogger().info("[DuelSkillDebug] Tagged projectile from " + shooter.getName() + " for skill " + skill);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCollide(ProjectileHitEvent e) {
        Projectile proj = e.getEntity();
        Entity hit = e.getHitEntity();
        if (!(hit instanceof Player victim)) return;
        if (!proj.hasMetadata(MYTHIC_META) && !proj.hasMetadata(ARCHER_META)) return;
        Object s = proj.getShooter();
        if (!(s instanceof Player attacker)) return;
        if (duels.areInDuel(attacker.getUniqueId(), victim.getUniqueId())) return;

        plugin.getLogger().info("[DuelSkillDebug] Detected projectile collision from " + attacker.getName() + " with bystander " + victim.getName());
        proj.remove();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjPvp(EntityDamageByEntityEvent e) {

        // Only players taking damage
        if (!(e.getEntity() instanceof Player victim)) return;

        Entity rawDamager = e.getDamager();
        if (!(rawDamager instanceof Projectile proj)) return;
        if (!proj.hasMetadata(ARCHER_META) && !proj.hasMetadata(MYTHIC_META)) return; // our filter

        Object s = proj.getShooter();
        if (!(s instanceof Player attacker)) return;           // shooter must be a player
        if (attacker.equals(victim)) return;                   // self-hits allowed

        if (!duels.areInDuel(attacker.getUniqueId(), victim.getUniqueId())) {
            e.setCancelled(true);
            attacker.sendMessage(ChatColor.RED + "You can only damage players you’re duelling!");
            plugin.getLogger().info("[DuelSkillDebug] Cancelled damage from projectile between " + attacker.getName() + " and bystander " + victim.getName());
        }
    }
}
