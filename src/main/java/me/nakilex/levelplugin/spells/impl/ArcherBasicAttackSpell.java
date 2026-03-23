package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.ArcherArrowUtil;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class ArcherBasicAttackSpell implements SpellHandler {
    private static final double ARROW_SPEED = 3.2;
    private static final double BASE_DAMAGE = 3.4;
    private static final double DEX_SCALE = 0.30;
    private static final double TECHNIQUE_SCALE = 0.001;

    private final Main plugin;

    public ArcherBasicAttackSpell(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        double damage = SpellEffectUtil.computeDexTecScaledDamage(caster, BASE_DAMAGE, DEX_SCALE, TECHNIQUE_SCALE);
        ArcherArrowUtil.launchClassArrow(plugin, caster, caster.getEyeLocation().getDirection(), ARROW_SPEED, damage);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.75f, 1.22f);
    }
}
