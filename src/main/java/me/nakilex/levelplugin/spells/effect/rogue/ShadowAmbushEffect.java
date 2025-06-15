package me.nakilex.levelplugin.spells.effect.rogue;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Teleport behind the nearest target and deliver a powerful strike.
 */
public class ShadowAmbushEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        double range = 8.0;
        Object r = ctx.getExtraParam("targetRange");
        if (r instanceof Number num) range += num.doubleValue();

        LivingEntity target = null;
        for (Entity e : player.getWorld().getNearbyEntities(player.getEyeLocation(), range, range, range)) {
            if (!(e instanceof LivingEntity le) || le.equals(player)) continue;
            if (le instanceof Player p && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId()))
                continue;
            target = le;
            break;
        }

        if (target == null) {
            player.sendMessage("§cNo valid target in range!");
            return;
        }

        World world = player.getWorld();
        double damage = ctx.getFinalDamage();
        Location behind = target.getLocation().clone().add(target.getLocation().getDirection().normalize().multiply(-1));

        world.spawnParticle(Particle.LARGE_SMOKE, player.getLocation(), 20, 0.5, 0.5, 0.5, 0.05);
        world.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.8f);
        player.teleport(behind);
        world.spawnParticle(Particle.SQUID_INK, behind, 20, 0.5, 0.5, 0.5, 0.05);
        world.playSound(behind, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);

        SpellUtils.dealWithChat(player, target, damage * 1.5, "Shadow Ambush");
        world.spawnParticle(Particle.CRIT, target.getLocation(), 20, 0.5, 1, 0.5);
        world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
    }
}
