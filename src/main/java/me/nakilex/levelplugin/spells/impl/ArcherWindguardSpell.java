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

public class ArcherWindguardSpell implements SpellHandler {
    private static final Map<UUID, GuardState> ACTIVE_GUARDS = new ConcurrentHashMap<>();

    private final Main plugin;
    private final int durationTicks;
    private final double incomingDamageMultiplier;

    public ArcherWindguardSpell(Main plugin, int durationTicks, double incomingDamageMultiplier) {
        this.plugin = plugin;
        this.durationTicks = Math.max(20, durationTicks);
        this.incomingDamageMultiplier = Math.max(0.1, Math.min(1.0, incomingDamageMultiplier));
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        ACTIVE_GUARDS.put(caster.getUniqueId(),
                new GuardState(System.currentTimeMillis() + (durationTicks * 50L), incomingDamageMultiplier));
        PotionEffectUtil.applyHiddenEffect(caster, PotionEffectType.SPEED, durationTicks, 0);
        caster.getWorld().spawnParticle(Particle.CLOUD, caster.getLocation().add(0.0, 1.0, 0.0),
                20, 0.35, 0.45, 0.35, 0.01);
        caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_2, 0.65f, 1.55f);
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> ACTIVE_GUARDS.remove(caster.getUniqueId()),
                durationTicks);
    }

    public static double getIncomingDamageMultiplier(Player player) {
        if (player == null) {
            return 1.0;
        }
        GuardState state = ACTIVE_GUARDS.get(player.getUniqueId());
        if (state == null) {
            return 1.0;
        }
        if (System.currentTimeMillis() > state.expiresAt()) {
            ACTIVE_GUARDS.remove(player.getUniqueId());
            return 1.0;
        }
        return state.incomingDamageMultiplier();
    }

    private record GuardState(long expiresAt, double incomingDamageMultiplier) {
    }
}
