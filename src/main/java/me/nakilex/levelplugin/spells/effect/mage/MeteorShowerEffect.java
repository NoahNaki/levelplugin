package me.nakilex.levelplugin.spells.effect.mage;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * A “shower” of meteors: schedules multiple MeteorEffect casts
 * spaced out in time. Counts and interval driven by rune params.
 */
public class MeteorShowerEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        // Base + extra projectiles
        int extra = 0;
        Object extraProjParam = ctx.getExtraParam("extraProjectiles");
        if (extraProjParam instanceof Number) {
            extra = ((Number) extraProjParam).intValue();
        }
        int total = 1 + extra;

        // Delay between meteors (in ticks); default 10 (~0.5s)
        long delayBetween = 10L;
        Object intervalParam = ctx.getExtraParam("showerInterval");
        if (intervalParam instanceof Number) {
            delayBetween = ((Number) intervalParam).longValue();
        }

        for (int i = 0; i < total; i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    // Delegate to MeteorEffect, which itself respects rune params
                    new MeteorEffect().apply(ctx);
                }
            }.runTaskLater(Main.getInstance(), i * delayBetween);
        }
    }
}
