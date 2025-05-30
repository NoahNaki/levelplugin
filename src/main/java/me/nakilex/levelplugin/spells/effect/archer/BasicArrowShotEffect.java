package me.nakilex.levelplugin.spells.effect.archer;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import java.util.UUID;

/**
 * Fires a basic arrow attack, calculating damage from weapon and STR.
 */
public class BasicArrowShotEffect implements SpellEffect {
    private static final String META_KEY = "BasicAttack";

    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        UUID pid = player.getUniqueId();

        // Calculate damage: weapon base + STR modifier
        double baseAtk = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue();
        int str = me.nakilex.levelplugin.player.attributes.managers.StatsManager
            .getInstance()
            .getStatValue(player, me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType.STR);
        double damage = baseAtk + (str * 0.5);

        // Launch arrow
        Arrow arrow = player.launchProjectile(Arrow.class);
        arrow.setDamage(damage);
        arrow.setCustomName("BasicArcherArrow");
        arrow.setCustomNameVisible(false);
        arrow.setMetadata(META_KEY, new FixedMetadataValue(Main.getInstance(), pid));

        // Effects
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 1f);
        player.getWorld().spawnParticle(Particle.INSTANT_EFFECT, player.getLocation(), 20, 0.5, 1, 0.5);
    }
}
