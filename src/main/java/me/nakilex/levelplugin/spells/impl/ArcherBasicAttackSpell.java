package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.ArcherArrowUtil;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.server.PluginDisableEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ArcherBasicAttackSpell implements SpellHandler, Listener {
    private static final double ARROW_SPEED = 3.2;
    private static final double BASE_DAMAGE = 3.4;
    private static final double DEX_SCALE = 0.30;
    private static final double TECHNIQUE_SCALE = 0.001;
    private static final Map<UUID, ExplosivePayload> EXPLOSIVE_ARROWS = new ConcurrentHashMap<>();
    private static boolean listenerRegistered;

    private final Main plugin;
    private final double homingStrength;
    private final double explosionRadius;
    private final double splashDamageFactor;

    public ArcherBasicAttackSpell(Main plugin) {
        this(plugin, 0.0, 0.0, 0.0);
    }

    public ArcherBasicAttackSpell(Main plugin,
                                  double homingStrength,
                                  double explosionRadius,
                                  double splashDamageFactor) {
        this.plugin = plugin;
        this.homingStrength = Math.max(0.0, homingStrength);
        this.explosionRadius = Math.max(0.0, explosionRadius);
        this.splashDamageFactor = Math.max(0.0, splashDamageFactor);
        if (!listenerRegistered) {
            this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
            listenerRegistered = true;
        }
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        double damage = SpellEffectUtil.computeDexTecScaledDamage(caster, BASE_DAMAGE, DEX_SCALE, TECHNIQUE_SCALE);
        Arrow arrow = ArcherArrowUtil.launchClassArrow(plugin, caster, caster.getEyeLocation().getDirection(), ARROW_SPEED, damage);
        if (arrow != null && homingStrength > 0.0) {
            ArcherArrowUtil.attachHomingTask(plugin, caster, arrow, homingStrength, 28, 16.0, 0.65);
        }
        if (arrow != null && explosionRadius > 0.0 && splashDamageFactor > 0.0) {
            EXPLOSIVE_ARROWS.put(arrow.getUniqueId(),
                    new ExplosivePayload(caster.getUniqueId(), damage, explosionRadius, splashDamageFactor));
        }
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.75f, 1.22f);
    }

    @EventHandler
    public void onArrowImpact(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) {
            return;
        }
        ExplosivePayload payload = EXPLOSIVE_ARROWS.remove(arrow.getUniqueId());
        if (payload == null) {
            return;
        }
        Player source = plugin.getServer().getPlayer(payload.ownerId());
        Location impact = arrow.getLocation();
        if (impact.getWorld() == null) {
            return;
        }

        impact.getWorld().spawnParticle(Particle.EXPLOSION, impact, 1, 0.0, 0.0, 0.0, 0.0);
        impact.getWorld().spawnParticle(Particle.FLAME, impact, 24, payload.radius() * 0.4, 0.2, payload.radius() * 0.4, 0.01);
        impact.getWorld().playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.35f);

        if (source == null || !source.isOnline()) {
            return;
        }
        double splashDamage = payload.damage() * payload.splashFactor();
        for (LivingEntity target : SpellEffectUtil.getLivingTargets(impact, payload.radius(),
                living -> !living.equals(source))) {
            Entity directHit = event.getHitEntity();
            if (directHit != null && directHit.getUniqueId().equals(target.getUniqueId())) {
                continue;
            }
            SpellEffectUtil.applyDirectSpellDamage(plugin, source, target, splashDamage, true);
        }
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin().equals(plugin)) {
            EXPLOSIVE_ARROWS.clear();
        }
    }

    private record ExplosivePayload(UUID ownerId, double damage, double radius, double splashFactor) {
    }
}
