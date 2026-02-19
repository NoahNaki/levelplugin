package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.listeners.StatsEffectListener;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.utils.ModelEngineUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.List;

public class MageBasicFireballSpell implements SpellHandler, Listener {
    private static final String MODEL_ID = "fireball";
    private static final String BASIC_ATTACK_META = "BasicAttack";
    private static final String MAGE_FIREBALL_META = "MageBasicFireball";
    private static final double SPEED = 1.6;
    private static final double BASE_DAMAGE = 1.0;
    private static final double INTELLIGENCE_SCALE = 0.5;
    private static final double TECHNIQUE_SCALE = 0.001;

    private final Main plugin;

    public MageBasicFireballSpell(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        Vector direction = caster.getEyeLocation().getDirection().normalize();
        Snowball projectile = caster.launchProjectile(Snowball.class);
        projectile.setGravity(false);
        projectile.setVelocity(direction.multiply(SPEED));
        projectile.setMetadata(BASIC_ATTACK_META, new FixedMetadataValue(plugin, caster.getUniqueId()));
        projectile.setMetadata(MAGE_FIREBALL_META, new FixedMetadataValue(plugin, true));

        ModelEngineUtil.ModelApplyResult result = ModelEngineUtil.applyModels(projectile, List.of(MODEL_ID), plugin);
        if (!result.failed().isEmpty()) {
            plugin.getLogger().warning("Mage fireball failed to apply model: " + String.join(", ", result.failed()));
        }
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.6f, 1.35f);
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball projectile)) {
            return;
        }
        if (!projectile.hasMetadata(MAGE_FIREBALL_META)) {
            return;
        }
        if (!(projectile.getShooter() instanceof Player caster)) {
            projectile.remove();
            return;
        }

        Location impact = projectile.getLocation();
        spawnImpactEffects(impact);

        Entity hitEntity = event.getHitEntity();
        if (hitEntity instanceof LivingEntity target && !target.getUniqueId().equals(caster.getUniqueId())) {
            double damage = SpellEffectUtil.computeIntTechniqueDamage(caster, BASE_DAMAGE, INTELLIGENCE_SCALE,
                    TECHNIQUE_SCALE) * StatsEffectListener.BASIC_ATTACK_MULTIPLIER;
            target.damage(damage, projectile);
        }
        projectile.remove();
    }

    private void spawnImpactEffects(Location impact) {
        if (impact.getWorld() == null) {
            return;
        }
        impact.getWorld().spawnParticle(Particle.FLAME, impact, 20, 0.25, 0.25, 0.25, 0.01);
        impact.getWorld().spawnParticle(Particle.SMALL_FLAME, impact, 12, 0.2, 0.2, 0.2, 0.01);
        impact.getWorld().spawnParticle(Particle.SMOKE, impact, 8, 0.2, 0.2, 0.2, 0.01);
        impact.getWorld().playSound(impact, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.75f, 1.4f);
    }
}
