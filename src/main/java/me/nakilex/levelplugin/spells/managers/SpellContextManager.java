package me.nakilex.levelplugin.spells.managers;

import me.nakilex.levelplugin.Main;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds a one‐time “I’m about to hit with spell X” context per player.
 */
public class SpellContextManager {
    private static final Map<UUID, Context> pending = new ConcurrentHashMap<>();

    public static void setPending(UUID playerId, String spellName, boolean isCrit) {
        Main.getInstance().getLogger()
            .info("[SpellContext] setPending for " + playerId + " -> " + spellName + " crit=" + isCrit);
        pending.put(playerId, new Context(spellName, isCrit));
    }

    public static Context consume(UUID playerId) {
        Context ctx = pending.remove(playerId);
        Main.getInstance().getLogger()
            .info("[SpellContext] consume for " + playerId + " -> " + (ctx == null ? "null" : ctx.spellName));
        return ctx;
    }

    public static void applySpellDamage(Player caster,
                                        LivingEntity target,
                                        double damage,
                                        String spellName,
                                        boolean isCrit) {
        // 1) mark context
        setPending(caster.getUniqueId(), spellName, isCrit);
        // 2) actually deal damage
        target.damage(damage, caster);
    }


    public static class Context {
        public final String spellName;
        public final boolean isCrit;
        public Context(String spellName, boolean isCrit) {
            this.spellName = spellName;
            this.isCrit    = isCrit;
        }
    }
}
