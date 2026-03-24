package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.utils.PotionEffectUtil;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WarriorGuardedResolveSpell implements SpellHandler {
    private static final Map<UUID, GuardState> ACTIVE_GUARDS = new ConcurrentHashMap<>();

    private final Main plugin;
    private final int durationTicks;
    private final double incomingDamageMultiplier;
    private final double absorbAmount;

    public WarriorGuardedResolveSpell(Main plugin, int durationTicks, double incomingDamageMultiplier, double absorbAmount) {
        this.plugin = plugin;
        this.durationTicks = Math.max(20, durationTicks);
        this.incomingDamageMultiplier = Math.max(0.2, Math.min(1.0, incomingDamageMultiplier));
        this.absorbAmount = Math.max(0.0, absorbAmount);
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        ACTIVE_GUARDS.put(caster.getUniqueId(),
                new GuardState(System.currentTimeMillis() + (durationTicks * 50L), incomingDamageMultiplier, absorbAmount));
        PotionEffectUtil.applyHiddenEffect(caster, PotionEffectType.RESISTANCE, durationTicks, 0);
        caster.getWorld().spawnParticle(Particle.WAX_ON, caster.getLocation().add(0.0, 1.0, 0.0),
                18, 0.35, 0.45, 0.35, 0.01);
        caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.95f, 0.9f);

        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> ACTIVE_GUARDS.remove(caster.getUniqueId()),
                durationTicks);
    }

    public static double applyIncomingDamage(Player player, double incomingDamage) {
        if (player == null || incomingDamage <= 0.0) {
            return Math.max(0.0, incomingDamage);
        }
        GuardState state = ACTIVE_GUARDS.get(player.getUniqueId());
        if (state == null) {
            return incomingDamage;
        }
        if (System.currentTimeMillis() > state.expiresAtMs) {
            ACTIVE_GUARDS.remove(player.getUniqueId());
            return incomingDamage;
        }

        double adjusted = incomingDamage * state.multiplier;
        if (state.absorbRemaining > 0.0) {
            double absorbed = Math.min(state.absorbRemaining, adjusted);
            adjusted -= absorbed;
            state.absorbRemaining -= absorbed;
            if (state.absorbRemaining <= 0.0) {
                state.absorbRemaining = 0.0;
            }
        }
        return Math.max(0.0, adjusted);
    }

    private static final class GuardState {
        private final long expiresAtMs;
        private final double multiplier;
        private double absorbRemaining;

        private GuardState(long expiresAtMs, double multiplier, double absorbRemaining) {
            this.expiresAtMs = expiresAtMs;
            this.multiplier = multiplier;
            this.absorbRemaining = Math.max(0.0, absorbRemaining);
        }
    }
}
