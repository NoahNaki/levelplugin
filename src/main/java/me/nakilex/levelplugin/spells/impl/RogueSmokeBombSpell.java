package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.utils.ModelEngineUtil;
import me.nakilex.levelplugin.utils.PotionEffectUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class RogueSmokeBombSpell implements SpellHandler, Listener {
    private static final List<String> MODEL_CANDIDATES = List.of("rogue_smokebomb", "rogue_smokebomb.bbmodel");
    private static boolean listenerRegistered;

    private static final int PLAYER_STUN_TICKS = 40; // 2.0s
    private static final int MOB_STUN_TICKS = 40;    // 2.0s
    private static final int BUFF_REFRESH_TICKS = 10;

    private static final Map<UUID, SmokeBombState> ACTIVE_BOMBS = new HashMap<>();
    private static final Map<UUID, Set<UUID>> BOMBS_BY_OWNER = new HashMap<>();

    private final Main plugin;
    private final int durationTicks;
    private final double effectRadius;
    private final double dotDamagePerTick;
    private final int dotPeriodTicks;

    public RogueSmokeBombSpell(Main plugin, int durationTicks, double effectRadius, int unusedStunTicks) {
        this(plugin, durationTicks, effectRadius, unusedStunTicks, 1, 0.0, 0.0, 20);
    }

    public RogueSmokeBombSpell(Main plugin,
                               int durationTicks,
                               double effectRadius,
                               int unusedStunTicks,
                               int unusedBombCount,
                               double unusedConeDegrees,
                               double dotDamagePerTick,
                               int dotPeriodTicks) {
        this.plugin = plugin;
        this.durationTicks = Math.max(20, durationTicks);
        this.effectRadius = Math.max(1.0, effectRadius);
        this.dotDamagePerTick = Math.max(0.0, dotDamagePerTick);
        this.dotPeriodTicks = Math.max(1, dotPeriodTicks);
        if (!listenerRegistered) {
            this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
            listenerRegistered = true;
        }
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        UUID casterId = caster.getUniqueId();
        cleanupOwnerBombs(casterId);

        Location center = caster.getLocation().clone().add(0.0, 0.08, 0.0);
        ArmorStand anchor = spawnModelAnchor(center);
        if (anchor == null) {
            return;
        }

        UUID bombId = anchor.getUniqueId();
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            private int elapsed;
            private int dotElapsed;

            @Override
            public void run() {
                if (!anchor.isValid() || anchor.isDead() || elapsed >= durationTicks) {
                    cleanupBomb(casterId, bombId, true);
                    return;
                }

                Location bombCenter = anchor.getLocation().clone().add(0.0, 0.25, 0.0);
                spawnSmokeVisuals(bombCenter);
                applySmokeEffects(caster, bombCenter, dotElapsed <= 0);

                elapsed += 2;
                dotElapsed += 2;
                if (dotElapsed >= dotPeriodTicks) {
                    dotElapsed = 0;
                }
            }
        }, 0L, 2L);

        ACTIVE_BOMBS.put(bombId, new SmokeBombState(anchor, task));
        BOMBS_BY_OWNER.computeIfAbsent(casterId, key -> new HashSet<>()).add(bombId);

        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 0.6f, 0.72f);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.8f, 0.86f);
    }

    private ArmorStand spawnModelAnchor(Location center) {
        if (center == null || center.getWorld() == null) {
            return null;
        }
        ArmorStand anchor = center.getWorld().spawn(center, ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.setSilent(true);
            stand.setInvulnerable(true);
            stand.setCollidable(false);
        });
        ModelEngineUtil.applyFirstAvailableModel(anchor, MODEL_CANDIDATES, plugin);
        return anchor;
    }

    private void spawnSmokeVisuals(Location center) {
        if (center == null || center.getWorld() == null) {
            return;
        }
        center.getWorld().spawnParticle(Particle.CLOUD, center, 14, 0.6, 0.28, 0.6, 0.001);
        center.getWorld().spawnParticle(Particle.WHITE_ASH, center, 22, 0.95, 0.42, 0.95, 0.001);
    }

    private void applySmokeEffects(Player caster, Location center, boolean applyDotThisTick) {
        for (LivingEntity living : SpellEffectUtil.getLivingTargets(center, effectRadius,
                target -> !target.equals(caster))) {
            if (living instanceof Player targetPlayer) {
                if (!DuelManager.getInstance().areInDuel(caster.getUniqueId(), targetPlayer.getUniqueId())) {
                    continue;
                }
                SpellEffectUtil.applyStun(targetPlayer, PLAYER_STUN_TICKS);
            } else {
                SpellEffectUtil.applyStun(living, MOB_STUN_TICKS);
            }

            if (dotDamagePerTick > 0.0 && applyDotThisTick) {
                SpellEffectUtil.applyDirectSpellDamage(plugin, caster, living, dotDamagePerTick, true);
            }
        }

        if (caster.getLocation().distanceSquared(center) <= effectRadius * effectRadius) {
            PotionEffectUtil.applyHiddenEffect(caster, PotionEffectType.INVISIBILITY, BUFF_REFRESH_TICKS + 2, 0);
            PotionEffectUtil.applyHiddenEffect(caster, PotionEffectType.SPEED, BUFF_REFRESH_TICKS + 2, 1);
            PotionEffectUtil.applyHiddenEffect(caster, PotionEffectType.REGENERATION, BUFF_REFRESH_TICKS + 2, 0);
        }
    }

    @EventHandler
    public void onOwnerQuit(PlayerQuitEvent event) {
        cleanupOwnerBombs(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin().equals(plugin)) {
            cleanupAllBombs();
        }
    }

    private void cleanupOwnerBombs(UUID ownerId) {
        Set<UUID> ownerBombs = BOMBS_BY_OWNER.remove(ownerId);
        if (ownerBombs == null || ownerBombs.isEmpty()) {
            return;
        }
        for (UUID bombId : new HashSet<>(ownerBombs)) {
            cleanupBomb(ownerId, bombId, true);
        }
    }

    private void cleanupAllBombs() {
        for (UUID ownerId : new HashSet<>(BOMBS_BY_OWNER.keySet())) {
            cleanupOwnerBombs(ownerId);
        }
        ACTIVE_BOMBS.clear();
        BOMBS_BY_OWNER.clear();
    }

    private void cleanupBomb(UUID ownerId, UUID bombId, boolean removeEntity) {
        SmokeBombState state = ACTIVE_BOMBS.remove(bombId);
        if (state != null) {
            state.task().cancel();
            if (removeEntity && state.anchor().isValid()) {
                state.anchor().remove();
            }
        }

        Set<UUID> ownerBombs = BOMBS_BY_OWNER.get(ownerId);
        if (ownerBombs == null) {
            return;
        }
        ownerBombs.remove(bombId);
        if (ownerBombs.isEmpty()) {
            BOMBS_BY_OWNER.remove(ownerId);
        }
    }

    private record SmokeBombState(ArmorStand anchor, BukkitTask task) {
    }
}
