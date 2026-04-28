package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.spells.SpellPartyUtil;
import me.nakilex.levelplugin.utils.ModelEngineUtil;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RogueSmokeBombSpell implements SpellHandler {
    private static final Map<UUID, BuffState> ACTIVE_BUFFS = new ConcurrentHashMap<>();
    private static final String MODEL_ID = "rogue_smokebomb";

    private final Main plugin;
    private final int durationTicks;
    private final double partyRadius;
    private final double damageMultiplier;
    private final boolean guaranteeCrit;
    private final int buffParticleCount;

    public RogueSmokeBombSpell(Main plugin, int durationTicks, double partyRadius, int ignored) {
        this(plugin, durationTicks, partyRadius, 2.0, true, 16);
    }

    public RogueSmokeBombSpell(Main plugin,
                               int durationTicks,
                               double partyRadius,
                               double damageMultiplier,
                               boolean guaranteeCrit,
                               int buffParticleCount) {
        this.plugin = plugin;
        this.durationTicks = Math.max(20, durationTicks);
        this.partyRadius = Math.max(1.0, partyRadius);
        this.damageMultiplier = Math.max(1.0, damageMultiplier);
        this.guaranteeCrit = guaranteeCrit;
        this.buffParticleCount = Math.max(6, buffParticleCount);
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        long expiresAt = System.currentTimeMillis() + (durationTicks * 50L);
        for (Player ally : SpellPartyUtil.resolvePartyPlayersInRange(plugin, caster, partyRadius, true)) {
            ACTIVE_BUFFS.put(ally.getUniqueId(), new BuffState(expiresAt, damageMultiplier, guaranteeCrit));
            ally.getWorld().spawnParticle(Particle.CRIT, ally.getLocation().add(0.0, 1.0, 0.0),
                    buffParticleCount, 0.35, 0.35, 0.35, 0.02);
            ally.getWorld().playSound(ally.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.75f);
        }
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.45f, 1.8f);
        spawnSmokeBombModel(caster);
    }

    public static double getOutgoingDamageMultiplier(Player player) {
        BuffState state = getActiveState(player);
        return state == null ? 1.0 : state.damageMultiplier();
    }

    public static boolean hasGuaranteedCrit(Player player) {
        BuffState state = getActiveState(player);
        return state != null && state.forceCrit();
    }

    private static BuffState getActiveState(Player player) {
        if (player == null) {
            return null;
        }
        BuffState state = ACTIVE_BUFFS.get(player.getUniqueId());
        if (state == null) {
            return null;
        }
        if (System.currentTimeMillis() > state.expiresAtMs()) {
            ACTIVE_BUFFS.remove(player.getUniqueId());
            return null;
        }
        return state;
    }

    private void spawnSmokeBombModel(Player caster) {
        if (caster == null || !caster.isOnline() || !org.bukkit.Bukkit.getPluginManager().isPluginEnabled("ModelEngine")) {
            return;
        }
        ArmorStand anchor = caster.getWorld().spawn(caster.getLocation().clone().add(0.0, 0.25, 0.0), ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.setSilent(true);
            stand.setInvulnerable(true);
        });
        ModelEngineUtil.applyFirstAvailableModel(anchor, ModelEngineUtil.buildModelCandidates(MODEL_ID), plugin);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (anchor.isValid()) {
                anchor.remove();
            }
        }, Math.max(20L, durationTicks));
    }

    private record BuffState(long expiresAtMs, double damageMultiplier, boolean forceCrit) {
    }
}
