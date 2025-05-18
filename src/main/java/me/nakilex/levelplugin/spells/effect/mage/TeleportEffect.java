package me.nakilex.levelplugin.spells.effect.mage;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.PlayerStats;
import me.nakilex.levelplugin.player.listener.ClickComboListener;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Exact reincarnation of original teleport logic as a SpellEffect.
 */
public class TeleportEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getCaster();
        // 1) Compute scaled distance based on agility
        PlayerStats stats = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        int totalAgi = stats.baseAgility + stats.bonusAgility;
        final int baseDistance = 8;
        final double agiMultiplier = 0.05;
        int scaledDistance = baseDistance + (int) (totalAgi * agiMultiplier);
        scaledDistance = Math.max(baseDistance, Math.min(scaledDistance, 30));

        // 2) Determine target location
        Location origin = player.getLocation();
        Location target = origin.clone().add(origin.getDirection().multiply(scaledDistance));

        // 3) Find a safe spot near target
        Location safe = findSafeLocation(target, player);

        // 4) Perform teleport with particles & sound
        if (safe != null) {
            player.getWorld().spawnParticle(Particle.DRAGON_BREATH, player.getLocation(), 150, 0.5, 1, 0.5);
            player.teleport(safe);
            player.getWorld().spawnParticle(Particle.DRAGON_BREATH, safe, 150, 0.5, 1, 0.5);
            player.getWorld().playSound(safe, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        } else {
            player.sendMessage("§cTeleportation failed! Destination is unsafe.");
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1f, 1f);
        }
    }

    /**
     * Checks three vertical positions around the target for safety.
     */
    private Location findSafeLocation(Location target, Player player) {
        for (int dy = -1; dy <= 1; dy++) {
            Location temp = target.clone().add(0, dy, 0);
            if (ClickComboListener.isLocTpSafe(temp)) {
                return temp;
            }
        }
        return null;
    }
}
