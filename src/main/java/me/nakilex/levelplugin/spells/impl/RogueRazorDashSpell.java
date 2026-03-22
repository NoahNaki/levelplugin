package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.spells.ArcSlashCombatUtil;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class RogueRazorDashSpell implements SpellHandler {
    private final me.nakilex.levelplugin.Main plugin;
    private final double dashSpeed;

    public RogueRazorDashSpell(me.nakilex.levelplugin.Main plugin, double dashSpeed) {
        this.plugin = plugin;
        this.dashSpeed = dashSpeed;
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        Vector forward = caster.getLocation().getDirection().setY(0.0);
        if (forward.lengthSquared() <= 0.0001) {
            ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.WARNING,
                    "No forward direction for Razor Dash.");
            return;
        }
        Vector dashDirection = forward.normalize();
        caster.setVelocity(dashDirection.clone().multiply(dashSpeed).add(new Vector(0.0, 0.18, 0.0)));
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.45f, 1.85f);

        new BukkitRunnable() {
            private int tick;

            @Override
            public void run() {
                if (!caster.isOnline() || tick > 6) {
                    cancel();
                    return;
                }
                if (tick % 2 == 0 && tick <= 4) {
                    caster.setVelocity(dashDirection.clone().multiply(dashSpeed * 0.72).add(new Vector(0.0, 0.10, 0.0)));
                    var current = caster.getLocation().clone()
                            .add(dashDirection.clone().multiply(1.25))
                            .add(0.0, 1.0, 0.0);
                    var orientation = caster.getLocation().clone();
                    orientation.setDirection(dashDirection.clone());
                    ArcSlashCombatUtil.strike(caster, current, orientation, 4.2, 1.65);
                    caster.getWorld().playSound(current, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.25f + (tick * 0.04f));
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
