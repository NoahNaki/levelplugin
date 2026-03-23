package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.spells.ArcSlashCombatUtil;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
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
        int hits = ArcSlashCombatUtil.applyConeDamage(caster, caster.getLocation().clone().add(0.0, 1.0, 0.0),
                caster.getLocation().getDirection(), 3.8, 72.0, 5.0, 3.4);
        if (hits == 0 && caster.getTargetEntity(5) instanceof LivingEntity target && !target.equals(caster)) {
            SpellEffectUtil.applyDirectSpellDamage(context.plugin(), caster, target, 3.4, true);
        }
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.65f, 1.35f);
    }
}
