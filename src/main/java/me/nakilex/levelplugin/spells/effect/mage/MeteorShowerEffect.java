package me.nakilex.levelplugin.spells.effect.mage;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * A “shower” of meteors: schedules multiple MeteorEffect casts
 * spaced out in time. Counts are driven by the rune’s extraProjectiles.
 */
public class MeteorShowerEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        // how many total meteors? 1 (base) + any extra from the rune
        int extra = (int) ctx.getExtraParams().getOrDefault("extraProjectiles", 0);
        int total = 1 + extra;

        long delayBetween = 10L; // ticks between each meteor (~0.5s)
        for (int i = 0; i < total; i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    // just delegate back to your normal MeteorEffect
                    new MeteorEffect().apply(ctx);
                }
            }.runTaskLater(Main.getInstance(), i * delayBetween);
        }
    }
}
