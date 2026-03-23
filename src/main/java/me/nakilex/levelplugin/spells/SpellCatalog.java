package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.particles.ParticleService;
import me.nakilex.levelplugin.player.classes.data.ClassUtil;
import me.nakilex.levelplugin.spells.impl.MageFireballBasicAttackSpell;
import me.nakilex.levelplugin.spells.impl.MageHealSpell;
import me.nakilex.levelplugin.spells.impl.MageBlinkSpell;
import me.nakilex.levelplugin.spells.impl.MeteorSpell;
import me.nakilex.levelplugin.spells.impl.BlackholeSpell;
import me.nakilex.levelplugin.spells.impl.RogueArcBasicAttackSpell;
import me.nakilex.levelplugin.spells.impl.RogueRazorDashSpell;
import me.nakilex.levelplugin.spells.impl.RogueNightfallLungeSpell;
import me.nakilex.levelplugin.spells.impl.RogueShadowFlurrySpell;
import me.nakilex.levelplugin.spells.impl.RogueSmokeBombSpell;
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
        SpellDefinition mageBasicBarrage = new SpellDefinition("mage_fireball_barrage", "Mage Fireball: Arc Barrage", 0, false);
        SpellDefinition mageBasicInferno = new SpellDefinition("mage_fireball_inferno", "Mage Fireball: Inferno Volley", 0, false);
        registry.registerSpell(mageBasicAttack, new MageFireballBasicAttackSpell(plugin, 1, 0.0, 3.2, 0.48, 0.0, 0.0, 0));
        registry.registerSpell(mageBasicBarrage, new MageFireballBasicAttackSpell(plugin, 3, 28.0, 3.8, 0.58, 1.4, 0.35, 30));
        registry.registerSpell(mageBasicInferno, new MageFireballBasicAttackSpell(plugin, 3, 34.0, 5.0, 0.72, 2.9, 0.80, 70));
        registry.registerProgression(new SpellProgression(mageBasicAttack.id(), java.util.List.of(
                mageBasicBarrage.id(), mageBasicInferno.id())));
        registry.registerBinding(SpellBinding.forInputType(mageBasicAttack.id(), ClassUtil::isMageFamily,
                SpellInputType.BASIC_ATTACK));

        SpellDefinition meteor = new SpellDefinition("meteor", "Meteor", 18, false);
        SpellDefinition meteorDouble = new SpellDefinition("meteor_double", "Meteor: Emberfall", 18, false);
        SpellDefinition meteorBig = new SpellDefinition("meteor_big", "Meteor: Cataclysm", 18, false);
        registry.registerSpell(meteor, new MeteorSpell(plugin, particleService, 18.0, 14.5, 6.2, 3.8, 2.4, 5.5, 6));
        registry.registerSpell(meteorDouble, new MeteorSpell(plugin, particleService, 20.0, 18.0, 7.6, 4.6, 3.1, 6.8, 7));
        registry.registerSpell(meteorBig, new MeteorSpell(plugin, particleService, 24.0, 23.0, 9.2, 5.4, 4.1, 8.2, 8));
        registry.registerProgression(new SpellProgression(meteor.id(), java.util.List.of(meteorDouble.id(), meteorBig.id())));
        registry.registerBinding(SpellBinding.forSequence(meteor.id(), ClassUtil::isMageFamily,
                SpellInputMode.MOUSE_COMBO, "RLL"));
        registry.registerBinding(SpellBinding.forSequence(meteor.id(), ClassUtil::isMageFamily,
                SpellInputMode.MOUSE_AND_KEYBOARD, "Sneak+Left"));

        SpellDefinition blackhole = new SpellDefinition("blackhole", "Blackhole", 22, false);
        SpellDefinition blackholeGravity = new SpellDefinition("blackhole_gravitywell", "Blackhole: Gravity Well", 22, false);
        SpellDefinition blackholeSingularity = new SpellDefinition("blackhole_singularity", "Blackhole: Singularity", 22, false);
        registry.registerSpell(blackhole, new BlackholeSpell(plugin, 4.2, 1.7, 0.24, 1.2, 60, 0.0));
        registry.registerSpell(blackholeGravity, new BlackholeSpell(plugin, 5.4, 2.2, 0.31, 1.8, 70, 0.0));
        registry.registerSpell(blackholeSingularity, new BlackholeSpell(plugin, 6.5, 2.8, 0.37, 2.5, 80, 9.5));
        registry.registerProgression(new SpellProgression(blackhole.id(), java.util.List.of(
                blackholeGravity.id(), blackholeSingularity.id())));
        registry.registerBinding(SpellBinding.forInputType(blackhole.id(), ClassUtil::isMageFamily, SpellInputType.SPELL_1));

        SpellDefinition heal = new SpellDefinition("mage_heal", "Arcane Mend", 16, false);
        SpellDefinition healRegen = new SpellDefinition("mage_heal_rejuvenation", "Arcane Mend: Rejuvenation", 16, false);
        SpellDefinition healParty = new SpellDefinition("mage_heal_party", "Arcane Mend: Party Pulse", 16, false);
        registry.registerSpell(heal, new MageHealSpell(plugin, 8.0, false, false, 8, 0));
        registry.registerSpell(healRegen, new MageHealSpell(plugin, 11.0, false, true, 16, 1));
        registry.registerSpell(healParty, new MageHealSpell(plugin, 9.0, true, true, 12, 1));
        registry.registerProgression(new SpellProgression(heal.id(), java.util.List.of(healRegen.id(), healParty.id())));
        registry.registerBinding(SpellBinding.forInputType(heal.id(), ClassUtil::isMageFamily, SpellInputType.SPELL_2));

        SpellDefinition blink = new SpellDefinition("mage_blink", "Blink", 14, true);
        SpellDefinition blinkPhase = new SpellDefinition("mage_blink_phase", "Blink: Phase Step", 14, true);
        SpellDefinition blinkRift = new SpellDefinition("mage_blink_rift", "Blink: Riftstride", 14, true);
        registry.registerSpell(blink, new MageBlinkSpell(plugin, 8.0));
        registry.registerSpell(blinkPhase, new MageBlinkSpell(plugin, 11.0));
        registry.registerSpell(blinkRift, new MageBlinkSpell(plugin, 14.0));
        registry.registerProgression(new SpellProgression(blink.id(), java.util.List.of(blinkPhase.id(), blinkRift.id())));
        registry.registerBinding(SpellBinding.forInputType(blink.id(), ClassUtil::isMageFamily, SpellInputType.SPELL_3));

        registry.registerBinding(SpellBinding.forInputType(meteor.id(), ClassUtil::isMageFamily, SpellInputType.SPELL_4));

        SpellDefinition rogueShadowFlurry = new SpellDefinition("rogue_sky_ripper", "Shadow Flurry", 14, false);
        SpellDefinition rogueSmokeBomb = new SpellDefinition("rogue_veil_counter", "Smoke Bomb", 16, false);
        SpellDefinition rogueRazorDash = new SpellDefinition("rogue_razor_dash", "Razor Dash", 12, true);
        SpellDefinition rogueNightfallLunge = new SpellDefinition("rogue_phantom_cross", "Nightfall Lunge", 18, false);
        SpellDefinition rogueArcBasic = new SpellDefinition("rogue_arc_basic", "Rogue Arc Slash", 0, false);

        registry.registerSpell(rogueShadowFlurry, new RogueShadowFlurrySpell(plugin));
        registry.registerSpell(rogueSmokeBomb, new RogueSmokeBombSpell(plugin));
        registry.registerSpell(rogueRazorDash, new RogueRazorDashSpell(plugin, 1.28));
        registry.registerSpell(rogueNightfallLunge, new RogueNightfallLungeSpell(plugin));
        registry.registerSpell(rogueArcBasic, new RogueArcBasicAttackSpell());

        registry.registerBinding(SpellBinding.forInputType(rogueArcBasic.id(), ClassUtil::isRogueFamily, SpellInputType.BASIC_ATTACK));
        registry.registerBinding(SpellBinding.forInputType(rogueShadowFlurry.id(), ClassUtil::isRogueFamily, SpellInputType.SPELL_1));
        registry.registerBinding(SpellBinding.forInputType(rogueSmokeBomb.id(), ClassUtil::isRogueFamily, SpellInputType.SPELL_2));
        registry.registerBinding(SpellBinding.forInputType(rogueRazorDash.id(), ClassUtil::isRogueFamily, SpellInputType.SPELL_3));
        registry.registerBinding(SpellBinding.forInputType(rogueNightfallLunge.id(), ClassUtil::isRogueFamily, SpellInputType.SPELL_4));
    }
}
