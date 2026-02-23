package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.particles.ParticleService;
import me.nakilex.levelplugin.player.classes.data.ClassUtil;
import me.nakilex.levelplugin.spells.impl.MageFireballBasicAttackSpell;
import me.nakilex.levelplugin.spells.impl.MageHealSpell;
import me.nakilex.levelplugin.spells.impl.MeteorSpell;
import me.nakilex.levelplugin.spells.impl.BlackholeSpell;
import me.nakilex.levelplugin.spells.impl.BlinkSpell;
import me.nakilex.levelplugin.spells.input.SpellInputMode;
import me.nakilex.levelplugin.spells.input.SpellInputType;

public final class SpellCatalog {
    private SpellCatalog() {
    }

    public static void registerDefaults(Main plugin) {
        if (plugin == null) {
            return;
        }
        SpellRegistry registry = SpellRegistry.getInstance();
        ParticleService particleService = new ParticleService(plugin);

        SpellDefinition mageBasicAttack = new SpellDefinition("mage_fireball_basic", "Mage Fireball", 0, false);
        registry.registerSpell(mageBasicAttack, new MageFireballBasicAttackSpell(plugin));
        registry.registerBinding(SpellBinding.forInputType(mageBasicAttack.id(), ClassUtil::isMageFamily,
                SpellInputType.BASIC_ATTACK));

        SpellDefinition meteor = new SpellDefinition("meteor", "Meteor", 18, false);
        SpellDefinition meteorDouble = new SpellDefinition("meteor_double", "Meteor: Emberfall", 18, false);
        SpellDefinition meteorBig = new SpellDefinition("meteor_big", "Meteor: Cataclysm", 18, false);
        registry.registerSpell(meteor, new MeteorSpell(plugin, particleService, 18.0, 12.0, 4.0, 3.5, 2.0));
        registry.registerSpell(meteorDouble, new MeteorSpell(plugin, particleService, 20.0, 14.0, 4.8, 4.1, 2.4));
        registry.registerSpell(meteorBig, new MeteorSpell(plugin, particleService, 22.0, 17.0, 5.5, 4.8, 3.0));
        registry.registerProgression(new SpellProgression(meteor.id(), java.util.List.of(meteorDouble.id(), meteorBig.id())));
        registry.registerBinding(SpellBinding.forSequence(meteor.id(), ClassUtil::isMageFamily,
                SpellInputMode.MOUSE_COMBO, "RLL"));
        registry.registerBinding(SpellBinding.forSequence(meteor.id(), ClassUtil::isMageFamily,
                SpellInputMode.MOUSE_AND_KEYBOARD, "Sneak+Left"));

        SpellDefinition blackhole = new SpellDefinition("blackhole", "Blackhole", 22, false);
        SpellDefinition blackholeGravity = new SpellDefinition("blackhole_gravitywell", "Blackhole: Gravity Well", 22, false);
        SpellDefinition blackholeSingularity = new SpellDefinition("blackhole_singularity", "Blackhole: Singularity", 22, false);
        registry.registerSpell(blackhole, new BlackholeSpell(plugin, 3.4, 0.22, 1.1, 50, 0.0));
        registry.registerSpell(blackholeGravity, new BlackholeSpell(plugin, 4.3, 0.30, 1.4, 60, 0.0));
        registry.registerSpell(blackholeSingularity, new BlackholeSpell(plugin, 5.0, 0.34, 1.8, 65, 6.0));
        registry.registerProgression(new SpellProgression(blackhole.id(), java.util.List.of(
                blackholeGravity.id(), blackholeSingularity.id())));
        registry.registerBinding(SpellBinding.forInputType(blackhole.id(), ClassUtil::isMageFamily, SpellInputType.SPELL_1));

        SpellDefinition heal = new SpellDefinition("mage_heal", "Arcane Mend", 16, false);
        SpellDefinition healRegen = new SpellDefinition("mage_heal_rejuvenation", "Arcane Mend: Rejuvenation", 16, false);
        SpellDefinition healParty = new SpellDefinition("mage_heal_party", "Arcane Mend: Party Pulse", 16, false);
        registry.registerSpell(heal, new MageHealSpell(plugin, 6.0, false, false));
        registry.registerSpell(healRegen, new MageHealSpell(plugin, 7.5, false, true));
        registry.registerSpell(healParty, new MageHealSpell(plugin, 6.5, true, true));
        registry.registerProgression(new SpellProgression(heal.id(), java.util.List.of(healRegen.id(), healParty.id())));
        registry.registerBinding(SpellBinding.forInputType(heal.id(), ClassUtil::isMageFamily, SpellInputType.SPELL_2));

        SpellDefinition blink = new SpellDefinition("mage_blink", "Blink", 14, true);
        SpellDefinition blinkRift = new SpellDefinition("mage_blink_rift", "Blink: Rift Step", 14, true);
        SpellDefinition blinkAegis = new SpellDefinition("mage_blink_aegis", "Blink: Arcane Aegis", 14, true);
        registry.registerSpell(blink, new BlinkSpell(plugin, 10, false, false));
        registry.registerSpell(blinkRift, new BlinkSpell(plugin, 12, true, false));
        registry.registerSpell(blinkAegis, new BlinkSpell(plugin, 14, true, true));
        registry.registerProgression(new SpellProgression(blink.id(), java.util.List.of(blinkRift.id(), blinkAegis.id())));
        registry.registerBinding(SpellBinding.forInputType(blink.id(), ClassUtil::isMageFamily, SpellInputType.SPELL_3));

        registry.registerBinding(SpellBinding.forInputType(meteor.id(), ClassUtil::isMageFamily, SpellInputType.SPELL_4));
    }
}
