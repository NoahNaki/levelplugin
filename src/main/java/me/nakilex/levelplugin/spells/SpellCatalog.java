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
import me.nakilex.levelplugin.spells.impl.DeckFireballSpell;
import me.nakilex.levelplugin.spells.impl.RogueArcBasicAttackSpell;
import me.nakilex.levelplugin.spells.impl.RogueRazorDashSpell;
import me.nakilex.levelplugin.spells.impl.RogueNightfallLungeSpell;
import me.nakilex.levelplugin.spells.impl.RogueShadowFlurrySpell;
import me.nakilex.levelplugin.spells.impl.RogueSmokeBombSpell;
import me.nakilex.levelplugin.spells.impl.WarriorExecutionArcSpell;
import me.nakilex.levelplugin.spells.impl.WarriorEarthquakeSpell;
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
        SpellDefinition mageBasicChain = new SpellDefinition("mage_fireball_chainlightning", "Mage Fireball: Chain Lightning", 0, false);
        registry.registerSpell(mageBasicAttack, new MageFireballBasicAttackSpell(plugin, 1, 0.0, 3.2, 0.48, 0.0, 0.0, 0));
        registry.registerSpell(mageBasicBarrage, new MageFireballBasicAttackSpell(plugin, 3, 36.0, 4.6, 0.68, 2.2, 0.46, 50));
        registry.registerSpell(mageBasicInferno, new MageFireballBasicAttackSpell(plugin, 3, 42.0, 6.6, 0.92, 4.1, 0.95, 110));
        registry.registerSpell(mageBasicChain, new MageFireballBasicAttackSpell(plugin, 3, 0.0, 7.4, 1.05, 4.0, 0.90, 140, 4));
        registry.registerProgression(new SpellProgression(mageBasicAttack.id(), java.util.List.of(
                mageBasicBarrage.id(), mageBasicInferno.id(), mageBasicChain.id())));
        registry.registerBinding(SpellBinding.forInputType(mageBasicAttack.id(), ClassUtil::isMageFamily,
                SpellInputType.BASIC_ATTACK));

        SpellDefinition meteor = new SpellDefinition("meteor", "Meteor", 18, false);
        SpellDefinition meteorDouble = new SpellDefinition("meteor_double", "Meteor: Emberfall", 18, false);
        SpellDefinition meteorBig = new SpellDefinition("meteor_big", "Meteor: Cataclysm", 18, false);
        registry.registerSpell(meteor, new MeteorSpell(plugin, particleService, 18.0, 14.5, 6.2, 3.8, 2.4, 5.5, 6,
                0, 0.0, 0.0));
        registry.registerSpell(meteorDouble, new MeteorSpell(plugin, particleService, 22.0, 22.5, 8.7, 5.8, 4.3, 8.2, 8,
                3, 4.8, 0.33));
        registry.registerSpell(meteorBig, new MeteorSpell(plugin, particleService, 27.0, 31.5, 11.8, 7.4, 6.1, 10.8, 10,
                5, 6.8, 0.44));
        registry.registerProgression(new SpellProgression(meteor.id(), java.util.List.of(meteorDouble.id(), meteorBig.id())));

        SpellDefinition blackhole = new SpellDefinition("blackhole", "Blackhole", 22, false);
        SpellDefinition blackholeGravity = new SpellDefinition("blackhole_gravitywell", "Blackhole: Gravity Well", 22, false);
        SpellDefinition blackholeSingularity = new SpellDefinition("blackhole_singularity", "Blackhole: Singularity", 22, false);
        registry.registerSpell(blackhole, new BlackholeSpell(plugin, 4.2, 1.7, 0.24, 1.2, 60, 0.0,
                0, 0.0, 0.0));
        registry.registerSpell(blackholeGravity, new BlackholeSpell(plugin, 6.8, 2.9, 0.42, 2.9, 90, 6.0,
                2, 1.6, 0.12));
        registry.registerSpell(blackholeSingularity, new BlackholeSpell(plugin, 9.0, 4.0, 0.56, 4.8, 120, 18.0,
                4, 2.8, 0.20));
        registry.registerProgression(new SpellProgression(blackhole.id(), java.util.List.of(
                blackholeGravity.id(), blackholeSingularity.id())));
        registry.registerBinding(SpellBinding.forInputType(blackhole.id(), ClassUtil::isMageFamily, SpellInputType.SPELL_1));

        SpellDefinition heal = new SpellDefinition("mage_heal", "Arcane Mend", 16, false);
        SpellDefinition healRegen = new SpellDefinition("mage_heal_rejuvenation", "Arcane Mend: Rejuvenation", 16, false);
        SpellDefinition healParty = new SpellDefinition("mage_heal_party", "Arcane Mend: Party Pulse", 16, false);
        registry.registerSpell(heal, new MageHealSpell(plugin, 8.0, false, false, 8, 0));
        registry.registerSpell(healRegen, new MageHealSpell(plugin, 14.0, false, true, 22, 2));
        registry.registerSpell(healParty, new MageHealSpell(plugin, 12.0, true, true, 24, 2));
        registry.registerProgression(new SpellProgression(heal.id(), java.util.List.of(healRegen.id(), healParty.id())));
        registry.registerBinding(SpellBinding.forInputType(heal.id(), ClassUtil::isMageFamily, SpellInputType.SPELL_4));

        SpellDefinition blink = new SpellDefinition("mage_blink", "Blink", 0, true);
        SpellDefinition blinkPhase = new SpellDefinition("mage_blink_phase", "Blink: Phase Step", 0, true);
        SpellDefinition blinkRift = new SpellDefinition("mage_blink_rift", "Blink: Riftstride", 0, true);
        registry.registerSpell(blink, new MageBlinkSpell(plugin, 8.0, 0.52, 0.45));
        registry.registerSpell(blinkPhase, new MageBlinkSpell(plugin, 11.0, 0.58, 0.55));
        registry.registerSpell(blinkRift, new MageBlinkSpell(plugin, 14.0, 0.66, 0.65, true));
        registry.registerProgression(new SpellProgression(blink.id(), java.util.List.of(blinkPhase.id(), blinkRift.id())));
        // Mobility spell archived for rework.
        // registry.registerBinding(SpellBinding.forInputType(blink.id(), ClassUtil::isMageFamily, SpellInputType.SPELL_3));

        registry.registerBinding(SpellBinding.forInputType(meteor.id(), ClassUtil::isMageFamily, SpellInputType.SPELL_2));
        registerStandardSequenceBindings(registry, blackhole.id(), meteor.id(), null, heal.id(), ClassUtil::isMageFamily);

        SpellDefinition archerBasic = new SpellDefinition("archer_quickshot_basic", "Quickshot", 0, false);
        SpellDefinition archerBasicSeeker = new SpellDefinition("archer_quickshot_seeker", "Quickshot: Seeker Tip", 0, false);
        SpellDefinition archerBasicPayload = new SpellDefinition("archer_quickshot_payload", "Quickshot: Payload Arrow", 0, false);
        SpellDefinition archerBarrage = new SpellDefinition("archer_homing_barrage", "Seeker Barrage", 16, false);
        SpellDefinition archerWindguard = new SpellDefinition("archer_windguard", "Windguard", 18, false);
        SpellDefinition archerSkybound = new SpellDefinition("archer_skybound", "Skybound Vault", 14, true);
        SpellDefinition archerArrowRain = new SpellDefinition("archer_arrow_rain", "Arrow Rain", 18, false);

        registry.registerSpell(archerBasic, new ArcherBasicAttackSpell(plugin, 0.0, 0.0, 0.0));
        registry.registerSpell(archerBasicSeeker, new ArcherBasicAttackSpell(plugin, 0.33, 0.0, 0.0));
        registry.registerSpell(archerBasicPayload, new ArcherBasicAttackSpell(plugin, 0.38, 3.8, 0.72));
        registry.registerSpell(archerBarrage, new ArcherHomingBarrageSpell(plugin, 9, 2L, 3.3, 0.31, 4.8, 0.42));
        registry.registerSpell(archerSkybound, new ArcherSkyboundSpell(plugin, 0.82, 80, 3.2, 5.4, 0.62));
        registry.registerSpell(archerWindguard, new ArcherWindguardSpell(plugin, 100, 1, 30.0));
        registry.registerSpell(archerArrowRain, new ArcherArrowRainSpell(plugin, 8, 11, 7, 8.2, 15.5, 4.4, 0.36));

        registry.registerBinding(SpellBinding.forInputType(archerBasic.id(), ClassUtil::isArcherFamily, SpellInputType.BASIC_ATTACK));
        registry.registerProgression(new SpellProgression(archerBasic.id(), java.util.List.of(
                archerBasicSeeker.id(), archerBasicPayload.id())));
        registry.registerBinding(SpellBinding.forInputType(archerBarrage.id(), ClassUtil::isArcherFamily, SpellInputType.SPELL_1));
        registry.registerBinding(SpellBinding.forInputType(archerArrowRain.id(), ClassUtil::isArcherFamily, SpellInputType.SPELL_2));
        // Mobility spell archived for rework.
        // registry.registerBinding(SpellBinding.forInputType(archerSkybound.id(), ClassUtil::isArcherFamily, SpellInputType.SPELL_3));
        registry.registerBinding(SpellBinding.forInputType(archerWindguard.id(), ClassUtil::isArcherFamily, SpellInputType.SPELL_4));
        registerReversedSequenceBindings(registry, archerBarrage.id(), archerArrowRain.id(), null, archerWindguard.id(), ClassUtil::isArcherFamily);

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
        SpellDefinition rogueArcBasicTempest = new SpellDefinition("rogue_arc_basic_tempest", "Rogue Arc Slash: Tempest Arc", 0, false);
        SpellDefinition rogueArcBasicReaper = new SpellDefinition("rogue_arc_basic_reaper", "Rogue Arc Slash: Reaper Crescent", 0, false);

        registry.registerSpell(rogueShadowFlurry, new RogueShadowFlurrySpell(plugin, 4, 6.0, 0.8, 80, 2.6, 7.4));
        registry.registerSpell(rogueShadowFlurryTempest, new RogueShadowFlurrySpell(plugin, 6, 7.6, 1.2, 120, 3.8, 13.0));
        registry.registerSpell(rogueShadowFlurryExecution, new RogueShadowFlurrySpell(plugin, 8, 8.9, 1.5, 150, 4.8, 17.6));

        registry.registerSpell(rogueSmokeBomb, new RogueSmokeBombSpell(plugin, 100, 30.0, 2.0, true, 16));
        registry.registerSpell(rogueSmokeBombObscure, new RogueSmokeBombSpell(plugin, 120, 34.0, 2.3, true, 22));
        registry.registerSpell(rogueSmokeBombDread, new RogueSmokeBombSpell(plugin, 140, 40.0, 2.8, true, 30));

        registry.registerSpell(rogueRazorDash, new RogueRazorDashSpell(plugin, 1.28));
        registry.registerSpell(rogueRazorDashRift, new RogueRazorDashSpell(plugin, 1.40));
        registry.registerSpell(rogueRazorDashShade, new RogueRazorDashSpell(plugin, 1.52));

        registry.registerSpell(rogueNightfallLunge, new RogueNightfallLungeSpell(plugin, 4, 6.0, 0.55, 7.2, 0.6, 11.8));
        registry.registerSpell(rogueNightfallLungeCyclone, new RogueNightfallLungeSpell(plugin, 6, 7.5, 0.88, 8.8, 1.0, 17.0));
        registry.registerSpell(rogueNightfallLungeJudgement, new RogueNightfallLungeSpell(plugin, 8, 8.6, 1.2, 10.6, 1.25, 23.0));

        registry.registerSpell(rogueArcBasic, new RogueArcBasicAttackSpell(1.2, 1.0, 1.55, 3.4,
                3.8, 72.0, 5.0, 1.0, 1.0));
        registry.registerSpell(rogueArcBasicTempest, new RogueArcBasicAttackSpell(1.25, 1.0, 1.7, 3.9,
                4.6, 76.0, 5.8, 1.22, 1.18));
        registry.registerSpell(rogueArcBasicReaper, new RogueArcBasicAttackSpell(1.3, 1.05, 1.85, 4.5,
                5.6, 80.0, 6.9, 1.46, 1.34));

        registry.registerProgression(new SpellProgression(rogueShadowFlurry.id(), java.util.List.of(
                rogueShadowFlurryTempest.id(), rogueShadowFlurryExecution.id())));
        registry.registerProgression(new SpellProgression(rogueArcBasic.id(), java.util.List.of(
                rogueArcBasicTempest.id(), rogueArcBasicReaper.id())));
        registry.registerProgression(new SpellProgression(rogueSmokeBomb.id(), java.util.List.of(
                rogueSmokeBombObscure.id(), rogueSmokeBombDread.id())));
        registry.registerProgression(new SpellProgression(rogueRazorDash.id(), java.util.List.of(
                rogueRazorDashRift.id(), rogueRazorDashShade.id())));
        registry.registerProgression(new SpellProgression(rogueNightfallLunge.id(), java.util.List.of(
                rogueNightfallLungeCyclone.id(), rogueNightfallLungeJudgement.id())));

        registry.registerBinding(SpellBinding.forInputType(rogueArcBasic.id(), ClassUtil::isRogueFamily, SpellInputType.BASIC_ATTACK));
        registry.registerBinding(SpellBinding.forInputType(rogueShadowFlurry.id(), ClassUtil::isRogueFamily, SpellInputType.SPELL_1));
        registry.registerBinding(SpellBinding.forInputType(rogueNightfallLunge.id(), ClassUtil::isRogueFamily, SpellInputType.SPELL_2));
        // Mobility spell archived for rework.
        // registry.registerBinding(SpellBinding.forInputType(rogueRazorDash.id(), ClassUtil::isRogueFamily, SpellInputType.SPELL_3));
        registry.registerBinding(SpellBinding.forInputType(rogueSmokeBomb.id(), ClassUtil::isRogueFamily, SpellInputType.SPELL_4));
        registerStandardSequenceBindings(registry, rogueShadowFlurry.id(), rogueNightfallLunge.id(), null, rogueSmokeBomb.id(), ClassUtil::isRogueFamily);

        SpellDefinition warriorExecutionArc = new SpellDefinition("warrior_execution_arc", "Cyclone Brand", 16, false);
        SpellDefinition warriorEarthquake = new SpellDefinition("warrior_earthquake", "Earthquake", 16, false);
        SpellDefinition warriorEarthquakeTremor = new SpellDefinition("warrior_earthquake_tremor", "Earthquake: Tremor", 16, false);
        SpellDefinition warriorEarthquakeCataclysm = new SpellDefinition("warrior_earthquake_cataclysm", "Earthquake: Cataclysm", 16, false);
        SpellDefinition warriorRuptureCyclone = new SpellDefinition("warrior_rupture_cyclone", "Rupture Cyclone", 18, false);
        SpellDefinition warriorTitanVault = new SpellDefinition("warrior_titan_vault", "Titan Vault", 14, true);
        SpellDefinition warriorGuardedResolve = new SpellDefinition("warrior_guarded_resolve", "Aegis Bastion", 16, false);

        registry.registerSpell(warriorExecutionArc, new WarriorExecutionArcSpell(plugin, 120, 2.0, 6.4));
        registry.registerSpell(warriorEarthquake, new WarriorEarthquakeSpell(plugin, 3.8, 6.2, 0.55));
        registry.registerSpell(warriorEarthquakeTremor, new WarriorEarthquakeSpell(plugin, 6.3, 10.8, 0.82));
        registry.registerSpell(warriorEarthquakeCataclysm, new WarriorEarthquakeSpell(plugin, 8.8, 14.2, 1.08));
        registry.registerSpell(warriorRuptureCyclone, new WarriorRuptureCycloneSpell(plugin, 9, 2L, 0.9, 1.0, 3.8, 1.0, 0.58));
        registry.registerSpell(warriorTitanVault, new WarriorTitanVaultSpell(plugin, 1.18, 0.72, 3.0, 7.2));
        registry.registerSpell(warriorGuardedResolve, new WarriorGuardedResolveSpell(plugin, 130, 5, 34.0));
        registry.registerProgression(new SpellProgression(warriorEarthquake.id(), java.util.List.of(
                warriorEarthquakeTremor.id(), warriorEarthquakeCataclysm.id())));

        registry.registerBinding(SpellBinding.forInputType(warriorEarthquake.id(), ClassUtil::isWarriorFamily, SpellInputType.SPELL_1));
        registry.registerBinding(SpellBinding.forInputType(warriorRuptureCyclone.id(), ClassUtil::isWarriorFamily, SpellInputType.SPELL_2));
        // Mobility spell archived for rework.
        // registry.registerBinding(SpellBinding.forInputType(warriorTitanVault.id(), ClassUtil::isWarriorFamily, SpellInputType.SPELL_3));
        registry.registerBinding(SpellBinding.forInputType(warriorGuardedResolve.id(), ClassUtil::isWarriorFamily, SpellInputType.SPELL_4));
        registerStandardSequenceBindings(registry, warriorEarthquake.id(), warriorRuptureCyclone.id(), null, warriorGuardedResolve.id(), ClassUtil::isWarriorFamily);


        registerDeckFireballs(registry, plugin);

        configureCooldowns();
    }

    private static void registerDeckFireballs(SpellRegistry registry, Main plugin) {
        registerDeckFireball(registry, plugin, "deck_fireball_common", "Fireball", 20,
                new DeckFireballSpell.Config(0, 100, 20, 6, 2.5, 3, 10, 1.2,
                        0, 0, 0,
                        false, 0, 0, 0, 0,
                        0, 0, 0, 0,
                        false, 0, 0, 0, 0, 0,
                        0, 0,
                        false, 0, 0, 0,
                        0, 0, 1.15f));
        registerDeckFireball(registry, plugin, "deck_fireball_uncommon", "Enhanced Fireball", 20,
                new DeckFireballSpell.Config(1, 120, 20, 7, 3.5, 3, 10, 1.2,
                        4, 15, 3,
                        false, 0, 0, 0, 0,
                        0, 0, 0, 0,
                        false, 0, 0, 0, 0, 0,
                        0, 0,
                        false, 0, 0, 0,
                        0, 0, 1.05f));
        registerDeckFireball(registry, plugin, "deck_fireball_rare", "Infernal Fireball", 20,
                new DeckFireballSpell.Config(2, 150, 20, 6, 3.0, 5, 16, 1.2,
                        0, 0, 0,
                        true, 2, 3, 8, 2,
                        0, 0, 0, 0,
                        false, 0, 0, 0, 0, 0,
                        0, 0,
                        false, 0, 0, 0,
                        0, 0, 0.95f));
        registerDeckFireball(registry, plugin, "deck_fireball_epic", "Cataclysm Fireball", 35,
                new DeckFireballSpell.Config(3, 180, 35, 9, 4.0, 6, 18, 1.2,
                        0, 0, 0,
                        false, 0, 0, 0, 0,
                        3, 14, 2.5, 90,
                        false, 0, 0, 0, 0, 0,
                        0, 0,
                        false, 0, 0, 0,
                        0, 0, 0.85f));
        registerDeckFireball(registry, plugin, "deck_fireball_legendary", "Dragonfire Orb", 30,
                new DeckFireballSpell.Config(4, 170, 30, 9, 3.5, 6, 18, 1.0,
                        0, 0, 0,
                        false, 0, 0, 0, 0,
                        0, 14, 2.5, 120,
                        true, 350, 7, 8, 24, 5,
                        0.85, 20,
                        false, 0, 0, 0,
                        0, 0, 0.7f));
        registerDeckFireball(registry, plugin, "deck_fireball_mythic", "Worldfire", 75,
                new DeckFireballSpell.Config(5, 250, 75, 14, 5.0, 10, 30, 0.95,
                        0, 0, 0,
                        false, 0, 0, 0, 0,
                        0, 0, 0, 0,
                        false, 0, 0, 0, 0, 0,
                        0.45, 0,
                        true, 4, 140, 10,
                        1.0, 0.10, 0.55f));
    }

    private static void registerDeckFireball(SpellRegistry registry,
                                             Main plugin,
                                             String id,
                                             String displayName,
                                             int manaCost,
                                             DeckFireballSpell.Config config) {
        registry.registerSpell(new SpellDefinition(id, displayName, manaCost, false), new DeckFireballSpell(plugin, config));
        SpellCastManager.setSpellCooldownMs(id, config.cooldownSeconds() * 1000L);
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
        if (mobilitySpellId != null && !mobilitySpellId.isBlank()) {
            registry.registerBinding(SpellBinding.forSequence(mobilitySpellId, classPredicate,
                    SpellInputMode.MOUSE_COMBO, "RRR"));
        }

        registry.registerBinding(SpellBinding.forSequence(offensivePrimarySpellId, classPredicate,
                SpellInputMode.MOUSE_AND_KEYBOARD, "Sneak+Right"));
        registry.registerBinding(SpellBinding.forSequence(offensiveSecondarySpellId, classPredicate,
                SpellInputMode.MOUSE_AND_KEYBOARD, "Sneak+Left"));
        registry.registerBinding(SpellBinding.forSequence(defensiveSpellId, classPredicate,
                SpellInputMode.MOUSE_AND_KEYBOARD, "Sneak+Sneak"));
        if (mobilitySpellId != null && !mobilitySpellId.isBlank()) {
            registry.registerBinding(SpellBinding.forSequence(mobilitySpellId, classPredicate,
                    SpellInputMode.MOUSE_AND_KEYBOARD, "Right"));
        }
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
        if (mobilitySpellId != null && !mobilitySpellId.isBlank()) {
            registry.registerBinding(SpellBinding.forSequence(mobilitySpellId, classPredicate,
                    SpellInputMode.MOUSE_COMBO, "LLL"));
        }

        registry.registerBinding(SpellBinding.forSequence(offensivePrimarySpellId, classPredicate,
                SpellInputMode.MOUSE_AND_KEYBOARD, "Sneak+Left"));
        registry.registerBinding(SpellBinding.forSequence(offensiveSecondarySpellId, classPredicate,
                SpellInputMode.MOUSE_AND_KEYBOARD, "Sneak+Right"));
        registry.registerBinding(SpellBinding.forSequence(defensiveSpellId, classPredicate,
                SpellInputMode.MOUSE_AND_KEYBOARD, "Sneak+Sneak"));
        if (mobilitySpellId != null && !mobilitySpellId.isBlank()) {
            registry.registerBinding(SpellBinding.forSequence(mobilitySpellId, classPredicate,
                    SpellInputMode.MOUSE_AND_KEYBOARD, "Left"));
        }
    }

    private static void configureCooldowns() {

        SpellCastManager.setSpellCooldownMs("deck_fireball_common", 6000L);
        SpellCastManager.setSpellCooldownMs("deck_fireball_uncommon", 7000L);
        SpellCastManager.setSpellCooldownMs("deck_fireball_rare", 6000L);
        SpellCastManager.setSpellCooldownMs("deck_fireball_epic", 9000L);
        SpellCastManager.setSpellCooldownMs("deck_fireball_legendary", 9000L);
        SpellCastManager.setSpellCooldownMs("deck_fireball_mythic", 14000L);
        SpellCastManager.setSpellCooldownMs("mage_fireball_basic", 0L);
        SpellCastManager.setSpellCooldownMs("mage_fireball_barrage", 0L);
        SpellCastManager.setSpellCooldownMs("mage_fireball_inferno", 0L);
        SpellCastManager.setSpellCooldownMs("mage_fireball_chainlightning", 0L);
        SpellCastManager.setSpellCooldownMs("meteor", 7500L);
        SpellCastManager.setSpellCooldownMs("meteor_double", 7300L);
        SpellCastManager.setSpellCooldownMs("meteor_big", 6500L);
        SpellCastManager.setSpellCooldownMs("blackhole", 6200L);
        SpellCastManager.setSpellCooldownMs("blackhole_gravitywell", 5900L);
        SpellCastManager.setSpellCooldownMs("blackhole_singularity", 5200L);
        SpellCastManager.setSpellCooldownMs("mage_heal", 6200L);
        SpellCastManager.setSpellCooldownMs("mage_heal_rejuvenation", 5600L);
        SpellCastManager.setSpellCooldownMs("mage_heal_party", 5200L);
        // Mobility spell archived for rework.
        // SpellCastManager.setSpellCooldownMs("mage_blink", 0L);
        // SpellCastManager.setSpellCooldownMs("mage_blink_phase", 0L);
        // SpellCastManager.setSpellCooldownMs("mage_blink_rift", 0L);

        SpellCastManager.setSpellCooldownMs("archer_quickshot_basic", 0L);
        SpellCastManager.setSpellCooldownMs("archer_quickshot_seeker", 0L);
        SpellCastManager.setSpellCooldownMs("archer_quickshot_payload", 0L);
        SpellCastManager.setSpellCooldownMs("archer_homing_barrage", 6200L);
        SpellCastManager.setSpellCooldownMs("archer_windguard", 9000L);
        // Mobility spell archived for rework.
        // SpellCastManager.setSpellCooldownMs("archer_skybound", 0L);
        SpellCastManager.setSpellCooldownMs("archer_arrow_rain", 9800L);

        SpellCastManager.setSpellCooldownMs("rogue_arc_basic", 450L);
        SpellCastManager.setSpellCooldownMs("rogue_arc_basic_tempest", 420L);
        SpellCastManager.setSpellCooldownMs("rogue_arc_basic_reaper", 380L);
        SpellCastManager.setSpellCooldownMs("rogue_sky_ripper", 5600L);
        SpellCastManager.setSpellCooldownMs("rogue_sky_ripper_tempest", 5200L);
        SpellCastManager.setSpellCooldownMs("rogue_sky_ripper_execution", 4700L);
        SpellCastManager.setSpellCooldownMs("rogue_veil_counter", 30000L);
        SpellCastManager.setSpellCooldownMs("rogue_veil_counter_obscure", 30000L);
        SpellCastManager.setSpellCooldownMs("rogue_veil_counter_dread", 30000L);
        // Mobility spell archived for rework.
        // SpellCastManager.setSpellCooldownMs("rogue_razor_dash", 0L);
        // SpellCastManager.setSpellCooldownMs("rogue_razor_dash_rift", 0L);
        // SpellCastManager.setSpellCooldownMs("rogue_razor_dash_shade", 0L);
        SpellCastManager.setSpellCooldownMs("rogue_phantom_cross", 6900L);
        SpellCastManager.setSpellCooldownMs("rogue_phantom_cross_cyclone", 6200L);
        SpellCastManager.setSpellCooldownMs("rogue_phantom_cross_judgement", 5700L);

        SpellCastManager.setSpellCooldownMs("warrior_execution_arc", 5900L);
        SpellCastManager.setSpellCooldownMs("warrior_earthquake", 5900L);
        SpellCastManager.setSpellCooldownMs("warrior_earthquake_tremor", 5200L);
        SpellCastManager.setSpellCooldownMs("warrior_earthquake_cataclysm", 4700L);
        SpellCastManager.setSpellCooldownMs("warrior_rupture_cyclone", 7600L);
        // Mobility spell archived for rework.
        // SpellCastManager.setSpellCooldownMs("warrior_titan_vault", 0L);
        SpellCastManager.setSpellCooldownMs("warrior_guarded_resolve", 11000L);
    }
}
