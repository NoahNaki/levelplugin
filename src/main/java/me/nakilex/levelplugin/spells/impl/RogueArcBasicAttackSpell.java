package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.spells.ArcSlashCombatUtil;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellHandler;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RogueArcBasicAttackSpell implements SpellHandler {
    private static final long ATTACK_COOLDOWN_MS = 140L;

    private final Map<UUID, Long> lastCastAt = new HashMap<>();

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        long now = System.currentTimeMillis();
        Long last = lastCastAt.get(caster.getUniqueId());
        if (last != null && now - last < ATTACK_COOLDOWN_MS) {
            return;
        }
        lastCastAt.put(caster.getUniqueId(), now);

        ArcSlashCombatUtil.strikeForward(caster, 1.2, 1.0, 3.4, 1.55);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.65f, 1.35f);
    }
}
