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
    private static final long CONTEXT_TTL_MS = 4000L;

    public static void setPending(UUID playerId, String spellName, boolean isCrit, boolean basicAttack) {
        Main.getInstance().getLogger();
           // .info("[SpellContext] setPending for " + playerId + " -> " + spellName + " crit=" + isCrit);
        pending.put(playerId, new Context(spellName, isCrit, basicAttack, System.currentTimeMillis()));
    }

    public static Context consume(UUID playerId) {
        Context ctx = getFresh(playerId);
        pending.remove(playerId);
        Main.getInstance().getLogger();
            //.info("[SpellContext] consume for " + playerId + " -> " + (ctx == null ? "null" : ctx.spellName));
        return ctx;
    }

    /** Returns true if the player has a pending spell damage context. */
    public static boolean hasPending(UUID playerId) {
        return getFresh(playerId) != null;
    }

    /** Returns the pending context without consuming it, or {@code null}. */
    public static Context peek(UUID playerId) {
        return getFresh(playerId);
    }

    public static void applySpellDamage(Player caster,
                                        LivingEntity target,
                                        double damage,
                                        String spellName,
                                        boolean isCrit,
                                        boolean basicAttack) {
        // 1) mark context
        setPending(caster.getUniqueId(), spellName, isCrit, basicAttack);
        // 2) actually deal damage
        target.damage(damage, caster);
    }


    private static Context getFresh(UUID playerId) {
        Context ctx = pending.get(playerId);
        if (ctx == null) return null;

        if (ctx.isExpired()) {
            pending.remove(playerId);
            return null;
        }

        return ctx;
    }

    public static class Context {
        public final String spellName;
        public final boolean isCrit;
        public final boolean basicAttack;
        private final long createdAt;

        public Context(String spellName, boolean isCrit, boolean basicAttack, long createdAt) {
            this.spellName  = spellName;
            this.isCrit     = isCrit;
            this.basicAttack = basicAttack;
            this.createdAt = createdAt;
        }

        private boolean isExpired() {
            return System.currentTimeMillis() - createdAt > CONTEXT_TTL_MS;
        }
    }
}
