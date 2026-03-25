package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.particles.ParticleService;
import me.nakilex.levelplugin.player.classes.data.ClassUtil;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.spells.impl.MageFireballBasicAttackSpell;
import me.nakilex.levelplugin.spells.impl.MageHealSpell;
import me.nakilex.levelplugin.spells.impl.MageBlinkSpell;
import me.nakilex.levelplugin.spells.impl.MeteorSpell;
import me.nakilex.levelplugin.spells.impl.BlackholeSpell;
import me.nakilex.levelplugin.spells.impl.ArcherArrowRainSpell;
import me.nakilex.levelplugin.spells.impl.ArcherBasicAttackSpell;
import me.nakilex.levelplugin.spells.impl.ArcherHomingBarrageSpell;
import me.nakilex.levelplugin.spells.impl.ArcherSkyboundSpell;
import me.nakilex.levelplugin.spells.impl.ArcherWindguardSpell;
import me.nakilex.levelplugin.spells.impl.RogueArcBasicAttackSpell;
import me.nakilex.levelplugin.spells.impl.RogueRazorDashSpell;
import me.nakilex.levelplugin.spells.impl.RogueNightfallLungeSpell;
import me.nakilex.levelplugin.spells.impl.RogueShadowFlurrySpell;
import me.nakilex.levelplugin.spells.impl.RogueSmokeBombSpell;
import me.nakilex.levelplugin.spells.impl.WarriorExecutionArcSpell;
import me.nakilex.levelplugin.spells.impl.WarriorGuardedResolveSpell;
import me.nakilex.levelplugin.spells.impl.WarriorRuptureCycloneSpell;
import me.nakilex.levelplugin.spells.impl.WarriorTitanVaultSpell;
import me.nakilex.levelplugin.spells.input.SpellInputMode;
import me.nakilex.levelplugin.spells.input.SpellInputType;

import java.util.function.Predicate;

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
        registry.registerBinding(SpellBinding.forInputType(heal.id(), ClassUtil::isMageFamily, SpellInputType.SPELL_4));

        SpellDefinition blink = new SpellDefinition("mage_blink", "Blink", 14, true);
        SpellDefinition blinkPhase = new SpellDefinition("mage_blink_phase", "Blink: Phase Step", 14, true);
        SpellDefinition blinkRift = new SpellDefinition("mage_blink_rift", "Blink: Riftstride", 14, true);
        registry.registerSpell(blink, new MageBlinkSpell(plugin, 8.0));
        registry.registerSpell(blinkPhase, new MageBlinkSpell(plugin, 11.0));
        registry.registerSpell(blinkRift, new MageBlinkSpell(plugin, 14.0));
        registry.registerProgression(new SpellProgression(blink.id(), java.util.List.of(blinkPhase.id(), blinkRift.id())));
        registry.registerBinding(SpellBinding.forInputType(blink.id(), ClassUtil::isMageFamily, SpellInputType.SPELL_3));

        registry.registerBinding(SpellBinding.forInputType(meteor.id(), ClassUtil::isMageFamily, SpellInputType.SPELL_2));
        registerStandardSequenceBindings(registry, blackhole.id(), meteor.id(), blink.id(), heal.id(), ClassUtil::isMageFamily);

        SpellDefinition archerBasic = new SpellDefinition("archer_quickshot_basic", "Quickshot", 0, false);
        SpellDefinition archerBasicSeeker = new SpellDefinition("archer_quickshot_seeker", "Quickshot: Seeker Tip", 0, false);
        SpellDefinition archerBasicPayload = new SpellDefinition("archer_quickshot_payload", "Quickshot: Payload Arrow", 0, false);
        SpellDefinition archerBarrage = new SpellDefinition("archer_homing_barrage", "Seeker Barrage", 16, false);
        SpellDefinition archerWindguard = new SpellDefinition("archer_windguard", "Windguard", 18, false);
        SpellDefinition archerSkybound = new SpellDefinition("archer_skybound", "Skybound Vault", 14, true);
        SpellDefinition archerArrowRain = new SpellDefinition("archer_arrow_rain", "Arrow Rain", 18, false);

        registry.registerSpell(archerBasic, new ArcherBasicAttackSpell(plugin, 0.0, 0.0, 0.0));
        registry.registerSpell(archerBasicSeeker, new ArcherBasicAttackSpell(plugin, 0.22, 0.0, 0.0));
        registry.registerSpell(archerBasicPayload, new ArcherBasicAttackSpell(plugin, 0.24, 2.6, 0.52));
        registry.registerSpell(archerBarrage, new ArcherHomingBarrageSpell(plugin, 7, 2L, 3.0, 0.25, 3.8, 0.34));
        registry.registerSpell(archerSkybound, new ArcherSkyboundSpell(plugin, 0.82, 80, 3.2, 5.4, 0.62));
        registry.registerSpell(archerWindguard, new ArcherWindguardSpell(plugin, 100, 1, 30.0));
        registry.registerSpell(archerArrowRain, new ArcherArrowRainSpell(plugin, 6, 9, 8, 6.8, 14.0, 3.4, 0.30));

        registry.registerBinding(SpellBinding.forInputType(archerBasic.id(), ClassUtil::isArcherFamily, SpellInputType.BASIC_ATTACK));
        registry.registerProgression(new SpellProgression(archerBasic.id(), java.util.List.of(
                archerBasicSeeker.id(), archerBasicPayload.id())));
        registry.registerBinding(SpellBinding.forInputType(archerBarrage.id(), ClassUtil::isArcherFamily, SpellInputType.SPELL_1));
        registry.registerBinding(SpellBinding.forInputType(archerArrowRain.id(), ClassUtil::isArcherFamily, SpellInputType.SPELL_2));
        registry.registerBinding(SpellBinding.forInputType(archerSkybound.id(), ClassUtil::isArcherFamily, SpellInputType.SPELL_3));
        registry.registerBinding(SpellBinding.forInputType(archerWindguard.id(), ClassUtil::isArcherFamily, SpellInputType.SPELL_4));
        registerReversedSequenceBindings(registry, archerBarrage.id(), archerArrowRain.id(), archerSkybound.id(), archerWindguard.id(), ClassUtil::isArcherFamily);

        SpellDefinition rogueShadowFlurry = new SpellDefinition("rogue_sky_ripper", "Shadow Flurry", 14, false);
        SpellDefinition rogueShadowFlurryTempest = new SpellDefinition("rogue_sky_ripper_tempest", "Shadow Flurry: Tempest Dive", 14, false);
        SpellDefinition rogueShadowFlurryExecution = new SpellDefinition("rogue_sky_ripper_execution", "Shadow Flurry: Execution Drop", 14, false);

        SpellDefinition rogueSmokeBomb = new SpellDefinition("rogue_veil_counter", "Smoke Bomb", 16, false);
        SpellDefinition rogueSmokeBombObscure = new SpellDefinition("rogue_veil_counter_obscure", "Smoke Bomb: Obscure Field", 16, false);
        SpellDefinition rogueSmokeBombDread = new SpellDefinition("rogue_veil_counter_dread", "Smoke Bomb: Dread Cloud", 16, false);

        SpellDefinition rogueRazorDash = new SpellDefinition("rogue_razor_dash", "Razor Dash", 12, true);
        SpellDefinition rogueRazorDashRift = new SpellDefinition("rogue_razor_dash_rift", "Razor Dash: Rift Cut", 12, true);
        SpellDefinition rogueRazorDashShade = new SpellDefinition("rogue_razor_dash_shade", "Razor Dash: Shade Surge", 12, true);

        SpellDefinition rogueNightfallLunge = new SpellDefinition("rogue_phantom_cross", "Nightfall Lunge", 18, false);
        SpellDefinition rogueNightfallLungeCyclone = new SpellDefinition("rogue_phantom_cross_cyclone", "Nightfall Lunge: Cyclone", 18, false);
        SpellDefinition rogueNightfallLungeJudgement = new SpellDefinition("rogue_phantom_cross_judgement", "Nightfall Lunge: Judgement", 18, false);

        SpellDefinition rogueArcBasic = new SpellDefinition("rogue_arc_basic", "Rogue Arc Slash", 0, false);

        registry.registerSpell(rogueShadowFlurry, new RogueShadowFlurrySpell(plugin, 4, 6.0, 0.8, 80, 2.6, 7.4));
        registry.registerSpell(rogueShadowFlurryTempest, new RogueShadowFlurrySpell(plugin, 5, 6.6, 0.9, 95, 3.0, 9.0));
        registry.registerSpell(rogueShadowFlurryExecution, new RogueShadowFlurrySpell(plugin, 6, 7.2, 1.0, 110, 3.4, 11.2));

        registry.registerSpell(rogueSmokeBomb, new RogueSmokeBombSpell(plugin, 100, 30.0, 12, 1, 0.0, 0.0, 20));
        registry.registerSpell(rogueSmokeBombObscure, new RogueSmokeBombSpell(plugin, 100, 30.0, 14, 3, 26.0, 0.0, 20));
        registry.registerSpell(rogueSmokeBombDread, new RogueSmokeBombSpell(plugin, 100, 30.0, 16, 3, 30.0, 2.1, 18));

        registry.registerSpell(rogueRazorDash, new RogueRazorDashSpell(plugin, 1.28));
        registry.registerSpell(rogueRazorDashRift, new RogueRazorDashSpell(plugin, 1.40));
        registry.registerSpell(rogueRazorDashShade, new RogueRazorDashSpell(plugin, 1.52));

        registry.registerSpell(rogueNightfallLunge, new RogueNightfallLungeSpell(plugin, 4, 6.0, 0.55, 7.2, 0.6, 11.8));
        registry.registerSpell(rogueNightfallLungeCyclone, new RogueNightfallLungeSpell(plugin, 5, 6.6, 0.62, 7.8, 0.7, 13.2));
        registry.registerSpell(rogueNightfallLungeJudgement, new RogueNightfallLungeSpell(plugin, 6, 7.2, 0.68, 8.4, 0.78, 14.8));

        registry.registerSpell(rogueArcBasic, new RogueArcBasicAttackSpell());

        registry.registerProgression(new SpellProgression(rogueShadowFlurry.id(), java.util.List.of(
                rogueShadowFlurryTempest.id(), rogueShadowFlurryExecution.id())));
        registry.registerProgression(new SpellProgression(rogueSmokeBomb.id(), java.util.List.of(
                rogueSmokeBombObscure.id(), rogueSmokeBombDread.id())));
        registry.registerProgression(new SpellProgression(rogueRazorDash.id(), java.util.List.of(
                rogueRazorDashRift.id(), rogueRazorDashShade.id())));
        registry.registerProgression(new SpellProgression(rogueNightfallLunge.id(), java.util.List.of(
                rogueNightfallLungeCyclone.id(), rogueNightfallLungeJudgement.id())));

        registry.registerBinding(SpellBinding.forInputType(rogueArcBasic.id(), ClassUtil::isRogueFamily, SpellInputType.BASIC_ATTACK));
        registry.registerBinding(SpellBinding.forInputType(rogueShadowFlurry.id(), ClassUtil::isRogueFamily, SpellInputType.SPELL_1));
        registry.registerBinding(SpellBinding.forInputType(rogueNightfallLunge.id(), ClassUtil::isRogueFamily, SpellInputType.SPELL_2));
        registry.registerBinding(SpellBinding.forInputType(rogueRazorDash.id(), ClassUtil::isRogueFamily, SpellInputType.SPELL_3));
        registry.registerBinding(SpellBinding.forInputType(rogueSmokeBomb.id(), ClassUtil::isRogueFamily, SpellInputType.SPELL_4));
        registerStandardSequenceBindings(registry, rogueShadowFlurry.id(), rogueNightfallLunge.id(), rogueRazorDash.id(), rogueSmokeBomb.id(), ClassUtil::isRogueFamily);

        SpellDefinition warriorExecutionArc = new SpellDefinition("warrior_execution_arc", "Cyclone Brand", 16, false);
        SpellDefinition warriorRuptureCyclone = new SpellDefinition("warrior_rupture_cyclone", "Rupture Cyclone", 18, false);
        SpellDefinition warriorTitanVault = new SpellDefinition("warrior_titan_vault", "Titan Vault", 14, true);
        SpellDefinition warriorGuardedResolve = new SpellDefinition("warrior_guarded_resolve", "Aegis Bastion", 16, false);

        registry.registerSpell(warriorExecutionArc, new WarriorExecutionArcSpell(plugin, 120, 2.0, 6.4));
        registry.registerSpell(warriorRuptureCyclone, new WarriorRuptureCycloneSpell(plugin, 7, 2L, 0.6, 0.7, 2.8, 0.7, 0.46));
        registry.registerSpell(warriorTitanVault, new WarriorTitanVaultSpell(plugin, 1.18, 0.72, 3.0, 7.2));
        registry.registerSpell(warriorGuardedResolve, new WarriorGuardedResolveSpell(plugin, 100, 3, 30.0));

        registry.registerBinding(SpellBinding.forInputType(warriorExecutionArc.id(), ClassUtil::isWarriorFamily, SpellInputType.SPELL_1));
        registry.registerBinding(SpellBinding.forInputType(warriorRuptureCyclone.id(), ClassUtil::isWarriorFamily, SpellInputType.SPELL_2));
        registry.registerBinding(SpellBinding.forInputType(warriorTitanVault.id(), ClassUtil::isWarriorFamily, SpellInputType.SPELL_3));
        registry.registerBinding(SpellBinding.forInputType(warriorGuardedResolve.id(), ClassUtil::isWarriorFamily, SpellInputType.SPELL_4));
        registerStandardSequenceBindings(registry, warriorExecutionArc.id(), warriorRuptureCyclone.id(), warriorTitanVault.id(), warriorGuardedResolve.id(), ClassUtil::isWarriorFamily);

        configureCooldowns();
    }


    private static void registerStandardSequenceBindings(SpellRegistry registry,
                                                         String offensivePrimarySpellId,
                                                         String offensiveSecondarySpellId,
                                                         String mobilitySpellId,
                                                         String defensiveSpellId,
                                                         Predicate<PlayerClass> classPredicate) {
        registry.registerBinding(SpellBinding.forSequence(offensivePrimarySpellId, classPredicate,
                SpellInputMode.MOUSE_COMBO, "RRL"));
        registry.registerBinding(SpellBinding.forSequence(offensiveSecondarySpellId, classPredicate,
                SpellInputMode.MOUSE_COMBO, "RLL"));
        registry.registerBinding(SpellBinding.forSequence(defensiveSpellId, classPredicate,
                SpellInputMode.MOUSE_COMBO, "RLR"));
        registry.registerBinding(SpellBinding.forSequence(mobilitySpellId, classPredicate,
                SpellInputMode.MOUSE_COMBO, "RRR"));

        registry.registerBinding(SpellBinding.forSequence(offensivePrimarySpellId, classPredicate,
                SpellInputMode.MOUSE_AND_KEYBOARD, "Sneak+Right"));
        registry.registerBinding(SpellBinding.forSequence(offensiveSecondarySpellId, classPredicate,
                SpellInputMode.MOUSE_AND_KEYBOARD, "Sneak+Left"));
        registry.registerBinding(SpellBinding.forSequence(defensiveSpellId, classPredicate,
                SpellInputMode.MOUSE_AND_KEYBOARD, "Sneak+Sneak"));
        registry.registerBinding(SpellBinding.forSequence(mobilitySpellId, classPredicate,
                SpellInputMode.MOUSE_AND_KEYBOARD, "Right"));
    }

    private static void registerReversedSequenceBindings(SpellRegistry registry,
                                                         String offensivePrimarySpellId,
                                                         String offensiveSecondarySpellId,
                                                         String mobilitySpellId,
                                                         String defensiveSpellId,
                                                         Predicate<PlayerClass> classPredicate) {
        registry.registerBinding(SpellBinding.forSequence(offensivePrimarySpellId, classPredicate,
                SpellInputMode.MOUSE_COMBO, "LLR"));
        registry.registerBinding(SpellBinding.forSequence(offensiveSecondarySpellId, classPredicate,
                SpellInputMode.MOUSE_COMBO, "LRR"));
        registry.registerBinding(SpellBinding.forSequence(defensiveSpellId, classPredicate,
                SpellInputMode.MOUSE_COMBO, "LRL"));
        registry.registerBinding(SpellBinding.forSequence(mobilitySpellId, classPredicate,
                SpellInputMode.MOUSE_COMBO, "LLL"));

        registry.registerBinding(SpellBinding.forSequence(offensivePrimarySpellId, classPredicate,
                SpellInputMode.MOUSE_AND_KEYBOARD, "Sneak+Left"));
        registry.registerBinding(SpellBinding.forSequence(offensiveSecondarySpellId, classPredicate,
                SpellInputMode.MOUSE_AND_KEYBOARD, "Sneak+Right"));
        registry.registerBinding(SpellBinding.forSequence(defensiveSpellId, classPredicate,
                SpellInputMode.MOUSE_AND_KEYBOARD, "Sneak+Sneak"));
        registry.registerBinding(SpellBinding.forSequence(mobilitySpellId, classPredicate,
                SpellInputMode.MOUSE_AND_KEYBOARD, "Left"));
    }

    private static void configureCooldowns() {
        SpellCastManager.setSpellCooldownMs("mage_fireball_basic", 0L);
        SpellCastManager.setSpellCooldownMs("mage_fireball_barrage", 0L);
        SpellCastManager.setSpellCooldownMs("mage_fireball_inferno", 0L);
        SpellCastManager.setSpellCooldownMs("meteor", 7500L);
        SpellCastManager.setSpellCooldownMs("meteor_double", 9000L);
        SpellCastManager.setSpellCooldownMs("meteor_big", 11500L);
        SpellCastManager.setSpellCooldownMs("blackhole", 6200L);
        SpellCastManager.setSpellCooldownMs("blackhole_gravitywell", 7600L);
        SpellCastManager.setSpellCooldownMs("blackhole_singularity", 9300L);
        SpellCastManager.setSpellCooldownMs("mage_heal", 6200L);
        SpellCastManager.setSpellCooldownMs("mage_heal_rejuvenation", 7800L);
        SpellCastManager.setSpellCooldownMs("mage_heal_party", 8600L);
        SpellCastManager.setSpellCooldownMs("mage_blink", 0L);
        SpellCastManager.setSpellCooldownMs("mage_blink_phase", 0L);
        SpellCastManager.setSpellCooldownMs("mage_blink_rift", 0L);

        SpellCastManager.setSpellCooldownMs("archer_quickshot_basic", 0L);
        SpellCastManager.setSpellCooldownMs("archer_quickshot_seeker", 0L);
        SpellCastManager.setSpellCooldownMs("archer_quickshot_payload", 0L);
        SpellCastManager.setSpellCooldownMs("archer_homing_barrage", 6200L);
        SpellCastManager.setSpellCooldownMs("archer_windguard", 9000L);
        SpellCastManager.setSpellCooldownMs("archer_skybound", 0L);
        SpellCastManager.setSpellCooldownMs("archer_arrow_rain", 9800L);

        SpellCastManager.setSpellCooldownMs("rogue_arc_basic", 0L);
        SpellCastManager.setSpellCooldownMs("rogue_sky_ripper", 5600L);
        SpellCastManager.setSpellCooldownMs("rogue_sky_ripper_tempest", 6500L);
        SpellCastManager.setSpellCooldownMs("rogue_sky_ripper_execution", 7600L);
        SpellCastManager.setSpellCooldownMs("rogue_veil_counter", 30000L);
        SpellCastManager.setSpellCooldownMs("rogue_veil_counter_obscure", 30000L);
        SpellCastManager.setSpellCooldownMs("rogue_veil_counter_dread", 30000L);
        SpellCastManager.setSpellCooldownMs("rogue_razor_dash", 0L);
        SpellCastManager.setSpellCooldownMs("rogue_razor_dash_rift", 0L);
        SpellCastManager.setSpellCooldownMs("rogue_razor_dash_shade", 0L);
        SpellCastManager.setSpellCooldownMs("rogue_phantom_cross", 6900L);
        SpellCastManager.setSpellCooldownMs("rogue_phantom_cross_cyclone", 7700L);
        SpellCastManager.setSpellCooldownMs("rogue_phantom_cross_judgement", 8600L);

        SpellCastManager.setSpellCooldownMs("warrior_execution_arc", 5900L);
        SpellCastManager.setSpellCooldownMs("warrior_rupture_cyclone", 7600L);
        SpellCastManager.setSpellCooldownMs("warrior_titan_vault", 0L);
        SpellCastManager.setSpellCooldownMs("warrior_guarded_resolve", 11000L);
    }
}
