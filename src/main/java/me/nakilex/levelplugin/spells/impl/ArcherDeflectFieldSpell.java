package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.utils.PotionEffectUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ArcherDeflectFieldSpell implements SpellHandler, Listener {
    private static final Map<UUID, TurretState> ACTIVE_TURRETS = new ConcurrentHashMap<>();
    private static boolean listenerRegistered;

    private final Main plugin;
    private final int stanceTicks;
    private final int dodgeBonus;
    private final double attackSpeedMultiplier;

    public ArcherDeflectFieldSpell(Main plugin,
                                   int stanceTicks,
                                   int dodgeBonus,
                                   double attackSpeedMultiplier) {
        this.plugin = plugin;
        this.stanceTicks = Math.max(20, stanceTicks);
        this.dodgeBonus = Math.max(0, dodgeBonus);
        this.attackSpeedMultiplier = Math.max(1.0, attackSpeedMultiplier);
        if (!listenerRegistered) {
            this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
            listenerRegistered = true;
        }
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        PotionEffectUtil.applyHiddenEffect(caster, PotionEffectType.SLOWNESS, stanceTicks, 2);
        ACTIVE_TURRETS.put(caster.getUniqueId(),
                new TurretState(caster.getLocation().clone(), System.currentTimeMillis() + (stanceTicks * 50L),
                        dodgeBonus, attackSpeedMultiplier));
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> ACTIVE_TURRETS.remove(caster.getUniqueId()),
                stanceTicks);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        TurretState state = ACTIVE_TURRETS.get(player.getUniqueId());
        if (state == null) {
            return;
        }
        if (System.currentTimeMillis() > state.expiresAt()) {
            ACTIVE_TURRETS.remove(player.getUniqueId());
            return;
        }
        if (event.getFrom() == null || event.getTo() == null) {
            return;
        }
        double moved = event.getFrom().distanceSquared(event.getTo());
        if (moved <= 0.0009) {
            return;
        }
        ACTIVE_TURRETS.remove(player.getUniqueId());
    }

    public static int getTurretDodgeBonus(Player player) {
        if (player == null) {
            return 0;
        }
        TurretState state = ACTIVE_TURRETS.get(player.getUniqueId());
        if (state == null || System.currentTimeMillis() > state.expiresAt()) {
            ACTIVE_TURRETS.remove(player.getUniqueId());
            return 0;
        }
        return state.dodgeBonus();
    }

    public static double getTurretAttackSpeedMultiplier(Player player) {
        if (player == null) {
            return 1.0;
        }
        TurretState state = ACTIVE_TURRETS.get(player.getUniqueId());
        if (state == null || System.currentTimeMillis() > state.expiresAt()) {
            ACTIVE_TURRETS.remove(player.getUniqueId());
            return 1.0;
        }
        return state.attackSpeedMultiplier();
    }

    private record TurretState(org.bukkit.Location anchor,
                               long expiresAt,
                               int dodgeBonus,
                               double attackSpeedMultiplier) {
    }
}
