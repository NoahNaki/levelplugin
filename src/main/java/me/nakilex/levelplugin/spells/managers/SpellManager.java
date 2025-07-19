package me.nakilex.levelplugin.spells.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.WeaponType;
import org.bukkit.Material;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.spells.Spell;
import me.nakilex.levelplugin.spells.registry.EffectRegistry;
import me.nakilex.levelplugin.spells.utils.MythicSkillConfig;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class SpellManager {
    private static SpellManager instance;

    private final EffectRegistry effectRegistry;
    private final Map<String, Map<String, Spell>> spellsByClass = new HashMap<>();
    private Main plugin;

    private static final List<Material> WARRIOR_WEAPONS = new ArrayList<>();
    static {
        WARRIOR_WEAPONS.addAll(WeaponType.SWORD.getMaterials());
        WARRIOR_WEAPONS.addAll(WeaponType.AXE.getMaterials());
        WARRIOR_WEAPONS.addAll(WeaponType.SHOVEL.getMaterials());
    }

    public static SpellManager getInstance() {
        if (instance == null) throw new IllegalStateException("SpellManager not init’d!");
        return instance;
    }

    /**
     * Convenience constructor for demo utilities that don’t have access to the
     * plugin instance. This simply delegates to the main constructor using the
     * singleton {@link Main} instance.
     */
    public SpellManager() {
        this(Main.getInstance());
    }

    public SpellManager(Main plugin) {
        instance = this;
        this.plugin = plugin;
        this.effectRegistry = EffectRegistry.getInstance();
        loadSpells();
    }

    public Spell getSpell(String className, String combo) {
        Map<String, Spell> classMap = spellsByClass.get(className.toLowerCase());
        if (classMap == null) return null;
        return classMap.get(combo);
    }

    public Map<String, Spell> getSpellsByClass(String className) {
        return spellsByClass.getOrDefault(className.toLowerCase(), Collections.emptyMap());
    }


    private void loadSpells() {

        // — ARCHER CLASS —
        Map<String, Spell> archerMap = new HashMap<>();
        archerMap.put("BASIC_ATTACK", new Spell(
            "quick_shot",
            "Quick Shot",
            "BASIC_ATTACK",
            0.0,
            MythicSkillConfig.getCooldownSeconds("Quick_Shot"),
            1,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_QUICK_SHOT",
            0.0
        ));
        archerMap.put("LRL", new Spell(
            "backstep", "Backstep", "LRL",
            5.0,
            MythicSkillConfig.getCooldownSeconds("Backstep"), 3,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_BACKSTEP", 0.0
        ));
        archerMap.put("LRR", new Spell(
            "windrazor", "Windrazor", "LRR",
            8.0,
            MythicSkillConfig.getCooldownSeconds("Windrazor"), 5,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_WINDRAZOR", 0.0
        ));
        archerMap.put("LLR", new Spell(
            "arrow_barrage", "Arrow Barrage", "LLR",
            12.0,
            MythicSkillConfig.getCooldownSeconds("Arrow_Barrage"), 10,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_ARROW_BARRAGE", 0.0
        ));
        archerMap.put("RRR", new Spell(
            "bow_drone", "Bow Drone", "RRR",
            10.0,
            MythicSkillConfig.getCooldownSeconds("Deadly_Javelin"), 10,
            WeaponType.BOW.getMaterials(),
            "BOW_DRONE", 0.0
        ));
        archerMap.put("LLL", new Spell(
            "dragon_piercer", "Dragon Piercer", "LLL",
            15.0,
            MythicSkillConfig.getCooldownSeconds("Dragon_Piercer"), 10,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_DRAGON_PIERCER", 0.0
        ));
        spellsByClass.put("archer", Collections.unmodifiableMap(archerMap));
        plugin.getLogger().info("[SPELLS] Archer combos: " + archerMap.keySet());

        // — PHOENIXHUNTER CLASS —
        Map<String, Spell> phoenixMap = new HashMap<>();
        phoenixMap.put("BASIC_ATTACK", new Spell(
            "blazing_feathers",
            "Blazing Feathers",
            "BASIC_ATTACK",
            0.0,
            1,
            1,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_BLAZING_FEATHERS",
            0.0
        ));
        phoenixMap.put("LRL", new Spell(
            "ashdance", "Ashdance", "LRL",
            6.0,
            MythicSkillConfig.getCooldownSeconds("Ashdance"), 3,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_ASHDANCE", 0.0
        ));
        phoenixMap.put("LRR", new Spell(
            "flameburst_convergence", "Flameburst Convergence", "LRR",
            8.0,
            MythicSkillConfig.getCooldownSeconds("Flameburst_Convergence"), 5,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_FLAMEBURST_CONVERGENCE", 0.0
        ));
        phoenixMap.put("LLR", new Spell(
            "phoenix_totem", "Phoenix Totem", "LLR",
            0.0,
            MythicSkillConfig.getCooldownSeconds("Phoenix_Totem"), 0,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_PHOENIX_TOTEM", 0.0,
            true
        ));
        phoenixMap.put("LLL", new Spell(
            "pyroclasmic_barrage", "Pyroclasmic Barrage", "LLL",
            12.0,
            MythicSkillConfig.getCooldownSeconds("Pyroclasmic_Barrage"), 10,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_PYROCLASMIC_BARRAGE", 0.0
        ));
        phoenixMap.put("RRR", new Spell(
            "phoenix_rebirth", "Phoenix Rebirth", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("Phoenix_Rebirth"), 10,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_PHOENIX_REBIRTH", 0.0
        ));
        spellsByClass.put("phoenixhunter", Collections.unmodifiableMap(phoenixMap));
        plugin.getLogger().info("[SPELLS] PhoenixHunter combos: " + phoenixMap.keySet());

        // — DEADEYE CLASS —
        Map<String, Spell> deadeyeMap = new HashMap<>();
        deadeyeMap.put("BASIC_ATTACK", new Spell(
            "pistol_shot", "Pistol Shot", "BASIC_ATTACK",
            0.0,
            MythicSkillConfig.getCooldownSeconds("Pistol_Shot"), 1,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_PISTOL_SHOT", 0.0
        ));
        deadeyeMap.put("LRL", new Spell(
            "shotgun_blast", "Shotgun Blast", "LRL",
            6.0,
            MythicSkillConfig.getCooldownSeconds("Shotgun_Blast"), 3,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_SHOTGUN_BLAST", 0.0
        ));
        deadeyeMap.put("LRR", new Spell(
            "sniper_backup", "Sniper Backup", "LRR",
            10.0,
            MythicSkillConfig.getCooldownSeconds("Sniper_Backup"), 5,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_SNIPER_BACKUP", 0.0
        ));
        deadeyeMap.put("LLR", new Spell(
            "deathfire", "Deathfire", "LLR",
            12.0,
            MythicSkillConfig.getCooldownSeconds("Deathfire"), 5,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_DEATHFIRE", 0.0
        ));
        deadeyeMap.put("LLL", new Spell(
            "focus_shot", "Focus Shot", "LLL",
            15.0,
            MythicSkillConfig.getCooldownSeconds("Focus_Shot"), 10,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_FOCUS_SHOT", 0.0
        ));
        deadeyeMap.put("RRR", new Spell(
            "air_strike", "Air Strike", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("Air_Strike"), 10,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_AIR_STRIKE", 0.0
        ));
        spellsByClass.put("deadeye", Collections.unmodifiableMap(deadeyeMap));
        plugin.getLogger().info("[SPELLS] Deadeye combos: " + deadeyeMap.keySet());

        // — WARRIOR CLASS —
        Map<String, Spell> warriorMap = new HashMap<>();
        warriorMap.put("BASIC_ATTACK", new Spell(
            "brutal_strike", "Brutal Strike", "BASIC_ATTACK",
            0.0,
            1,
            1,
            WARRIOR_WEAPONS,
            "MYTHIC_BRUTAL_STRIKE", 0.0
        ));
        warriorMap.put("LRL", new Spell(
            "charge", "Charge", "LRL",
            5.0,
            MythicSkillConfig.getCooldownSeconds("Charge"), 3,
            WARRIOR_WEAPONS,
            "MYTHIC_CHARGE", 0.0
        ));
        warriorMap.put("LRR", new Spell(
            "chain_hook", "Chain Hook", "LRR",
            6.0,
            MythicSkillConfig.getCooldownSeconds("Chain_Hook"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_CHAIN_HOOK", 0.0
        ));
        warriorMap.put("LLR", new Spell(
            "shield_barrier", "Shield Barrier", "LLR",
            0.0,
            MythicSkillConfig.getCooldownSeconds("Shield_Barrier"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_SHIELD_BARRIER", 0.0
        ));
        warriorMap.put("RLL", new Spell(
            "shockwave", "Shockwave", "RLL",
            14.0,
            MythicSkillConfig.getCooldownSeconds("Whirlwind"), 3,
            WARRIOR_WEAPONS,
            "SHOCKWAVE", 1.5
        ));
        warriorMap.put("LLL", new Spell(
            "judgement", "Judgement", "LLL",
            15.0,
            MythicSkillConfig.getCooldownSeconds("Judgement"), 10,
            WARRIOR_WEAPONS,
            "MYTHIC_JUDGEMENT", 0.0
        ));
        warriorMap.put("RRR", new Spell(
            "rampage", "Rampage", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("Rampage"), 10,
            WARRIOR_WEAPONS,
            "MYTHIC_RAMPAGE", 0.0
        ));
        spellsByClass.put("warrior", Collections.unmodifiableMap(warriorMap));
        plugin.getLogger().info("[SPELLS] Warrior combos: " + warriorMap.keySet());

        // — BARBARIAN CLASS —
        Map<String, Spell> barbarianMap = new HashMap<>();
        barbarianMap.put("BASIC_ATTACK", new Spell(
            "rageblade", "Rageblade", "BASIC_ATTACK",
            0.0,
            MythicSkillConfig.getCooldownSeconds("Rageblade"),
            1,
            WARRIOR_WEAPONS,
            "MYTHIC_RAGEBLADE", 0.0
        ));
        barbarianMap.put("LRL", new Spell(
            "primal_axe", "Primal Axe", "LRL",
            8.0,
            MythicSkillConfig.getCooldownSeconds("Primal_Axe"), 3,
            WARRIOR_WEAPONS,
            "MYTHIC_PRIMAL_AXE", 0.0
        ));
        barbarianMap.put("LLR", new Spell(
            "war_cry", "War Cry", "LLR",
            10.0,
            MythicSkillConfig.getCooldownSeconds("War_Cry"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_WAR_CRY", 0.0
        ));
        barbarianMap.put("LLL", new Spell(
            "double_edge", "Double Edge", "LLL",
            12.0,
            MythicSkillConfig.getCooldownSeconds("Double_Edge"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_DOUBLE_EDGE", 0.0
        ));
        barbarianMap.put("RLL", new Spell(
            "relentless_leap", "Relentless Leap", "RLL",
            6.0,
            MythicSkillConfig.getCooldownSeconds("Relentless_Leap"), 3,
            WARRIOR_WEAPONS,
            "MYTHIC_RELENTLESS_LEAP", 0.0
        ));
        barbarianMap.put("RRR", new Spell(
            "eternal_fury", "Eternal Fury", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("Eternal_Fury"), 10,
            WARRIOR_WEAPONS,
            "MYTHIC_ETERNAL_FURY", 0.0
        ));
        spellsByClass.put("barbarian", Collections.unmodifiableMap(barbarianMap));
        plugin.getLogger().info("[SPELLS] Barbarian combos: " + barbarianMap.keySet());

        // — PALADIN CLASS —
        Map<String, Spell> paladinMap = new HashMap<>();
        paladinMap.put("BASIC_ATTACK", new Spell(
            "holy_strike", "Holy Strike", "BASIC_ATTACK",
            0.0,
            MythicSkillConfig.getCooldownSeconds("Holy_Strike"),
            1,
            WARRIOR_WEAPONS,
            "MYTHIC_HOLY_STRIKE", 0.0
        ));
        paladinMap.put("LRL", new Spell(
            "bound_seal", "Bound Seal", "LRL",
            8.0,
            MythicSkillConfig.getCooldownSeconds("Bound_Seal"), 3,
            WARRIOR_WEAPONS,
            "MYTHIC_BOUND_SEAL", 0.0
        ));
        paladinMap.put("LRR", new Spell(
            "hammer_of_justice", "Hammer Of Justice", "LRR",
            10.0,
            MythicSkillConfig.getCooldownSeconds("Hammer_Of_Justice"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_HAMMER_OF_JUSTICE", 0.0
        ));
        paladinMap.put("LLL", new Spell(
            "heavenly_shield", "Heavenly Shield", "LLL",
            12.0,
            MythicSkillConfig.getCooldownSeconds("Heavenly_Shield"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_HEAVENLY_SHIELD", 0.0
        ));
        paladinMap.put("RLL", new Spell(
            "unbreakable_will", "Unbreakable Will", "RLL",
            6.0,
            MythicSkillConfig.getCooldownSeconds("Unbreakable_Will"), 3,
            WARRIOR_WEAPONS,
            "MYTHIC_UNBREAKABLE_WILL", 0.0
        ));
        paladinMap.put("RRR", new Spell(
            "last_stand", "Last Stand", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("Last_Stand"), 10,
            WARRIOR_WEAPONS,
            "MYTHIC_LAST_STAND", 0.0
        ));
        spellsByClass.put("paladin", Collections.unmodifiableMap(paladinMap));
        plugin.getLogger().info("[SPELLS] Paladin combos: " + paladinMap.keySet());

        // — DEATH KNIGHT CLASS —
        Map<String, Spell> deathMap = new HashMap<>();
        deathMap.put("BASIC_ATTACK", new Spell(
            "death_strike", "Death Strike", "BASIC_ATTACK",
            0.0,
            MythicSkillConfig.getCooldownSeconds("Death_Strike_ST"),
            1,
            WARRIOR_WEAPONS,
            "MYTHIC_DEATH_STRIKE", 0.0
        ));
        deathMap.put("LRL", new Spell(
            "phantom_charge", "Phantom Charge", "LRL",
            6.0,
            MythicSkillConfig.getCooldownSeconds("Phantom_Charge"), 3,
            WARRIOR_WEAPONS,
            "MYTHIC_PHANTOM_CHARGE", 0.0
        ));
        deathMap.put("LRR", new Spell(
            "wraithbound_chains", "Wraithbound Chains", "LRR",
            6.0,
            MythicSkillConfig.getCooldownSeconds("Wraithbound_Chains"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_WRAITHBOUND_CHAINS", 0.0
        ));
        deathMap.put("LLR", new Spell(
            "soul_barrier", "Soul Barrier", "LLR",
            0.0,
            MythicSkillConfig.getCooldownSeconds("Soul_Barrier"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_SOUL_BARRIER", 0.0
        ));
        deathMap.put("LLL", new Spell(
            "necrotic_whirlwind", "Necrotic Whirlwind", "LLL",
            10.0,
            MythicSkillConfig.getCooldownSeconds("Necrotic_Whirlwind"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_NECROTIC_WHIRLWIND", 0.0
        ));
        deathMap.put("RRR", new Spell(
            "death_sentence", "Death Sentence", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("Death_Sentence"), 10,
            WARRIOR_WEAPONS,
            "MYTHIC_DEATH_SENTENCE", 0.0
        ));
        spellsByClass.put("deathknight", Collections.unmodifiableMap(deathMap));
        plugin.getLogger().info("[SPELLS] DeathKnight combos: " + deathMap.keySet());

        // — ABYSSION CLASS —
        Map<String, Spell> abyssionMap = new HashMap<>();
        abyssionMap.put("BASIC_ATTACK", new Spell(
            "aqua_slash", "Aqua Slash", "BASIC_ATTACK",
            0.0,
            MythicSkillConfig.getCooldownSeconds("Aqua_Slash"),
            1,
            WARRIOR_WEAPONS,
            "MYTHIC_AQUA_SLASH", 0.0
        ));
        abyssionMap.put("LRL", new Spell(
            "abyssal_dash", "Abyssal Dash", "LRL",
            6.0,
            MythicSkillConfig.getCooldownSeconds("Abyssal_Dash"), 3,
            WARRIOR_WEAPONS,
            "MYTHIC_ABYSSAL_DASH", 0.0
        ));
        abyssionMap.put("LLL", new Spell(
            "tidal_wave", "Tidal Wave", "LLL",
            8.0,
            MythicSkillConfig.getCooldownSeconds("Tidal_Wave"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_TIDAL_WAVE", 0.0
        ));
        abyssionMap.put("RLL", new Spell(
            "aqua_aura", "Aqua Aura", "RLL",
            10.0,
            MythicSkillConfig.getCooldownSeconds("Aqua_Aura"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_AQUA_AURA", 0.0
        ));
        abyssionMap.put("RRR", new Spell(
            "abyssal_smash", "Abyssal Smash", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("Abyssal_Smash"), 10,
            WARRIOR_WEAPONS,
            "MYTHIC_ABYSSAL_SMASH", 0.0
        ));
        spellsByClass.put("abyssion", Collections.unmodifiableMap(abyssionMap));
        plugin.getLogger().info("[SPELLS] Abyssion combos: " + abyssionMap.keySet());

        // — MAGE CLASS —
        Map<String, Spell> mageMap = new HashMap<>();
        mageMap.put("BASIC_ATTACK", new Spell(
            "fireball", "Fireball", "BASIC_ATTACK",
            0.0,
            MythicSkillConfig.getCooldownSeconds("Fireball"),
            1,
            WeaponType.WAND.getMaterials(),
            "MYTHIC_FIREBALL", 0.0
        ));
        mageMap.put("LRL", new Spell(
            "blink", "Blink", "LRL",
            6.0,
            1,
            3,
            WeaponType.WAND.getMaterials(),
            "MYTHIC_BLINK", 0.0
        ));
        mageMap.put("LLL", new Spell(
            "meteor", "Meteor", "LLL",
            12.0,
            MythicSkillConfig.getCooldownSeconds("Meteor"), 5,
            WeaponType.WAND.getMaterials(),
            "MYTHIC_METEOR", 0.0
        ));
        mageMap.put("LRR", new Spell(
            "frost_nova", "Frost Nova", "LRR",
            8.0,
            MythicSkillConfig.getCooldownSeconds("Frost_Nova"), 5,
            WeaponType.WAND.getMaterials(),
            "MYTHIC_FROST_NOVA", 0.0
        ));
        mageMap.put("RRR", new Spell(
            "inferno_chains", "Inferno Chains", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("Inferno_Chains"), 10,
            WeaponType.WAND.getMaterials(),
            "MYTHIC_INFERNO_CHAINS", 0.0
        ));
        spellsByClass.put("mage", Collections.unmodifiableMap(mageMap));
        plugin.getLogger().info("[SPELLS] Mage combos: " + mageMap.keySet());

        // — DRAGONIAN CLASS —
        Map<String, Spell> dragonianMap = new HashMap<>();
        dragonianMap.put("BASIC_ATTACK", new Spell(
            "dragonian_slash", "Dragonian Slash", "BASIC_ATTACK",
            0.0,
            MythicSkillConfig.getCooldownSeconds("dragonian_l_t"),
            1,
            WARRIOR_WEAPONS,
            "MYTHIC_DRAGONIAN_L_T", 0.0
        ));
        dragonianMap.put("LRL", new Spell(
            "dragonian_lunge", "Dragonian Lunge", "LRL",
            6.0,
            MythicSkillConfig.getCooldownSeconds("dragonian_r_t"), 3,
            WARRIOR_WEAPONS,
            "MYTHIC_DRAGONIAN_R_T", 0.0
        ));
        dragonianMap.put("LLL", new Spell(
            "dragonian_rs", "Dragonian RS", "LLL",
            8.0,
            MythicSkillConfig.getCooldownSeconds("dragonian_rs_t"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_DRAGONIAN_RS_T", 0.0
        ));
        dragonianMap.put("LLR", new Spell(
            "dragonian_ss", "Dragonian Stance", "LLR",
            10.0,
            MythicSkillConfig.getCooldownSeconds("dragonian_ss_t"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_DRAGONIAN_SS_T", 0.0
        ));
        dragonianMap.put("RRR", new Spell(
            "taotie_dragon", "Taotie Dragon", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("dragonian_ls_t"), 10,
            WARRIOR_WEAPONS,
            "MYTHIC_DRAGONIAN_LS_T", 0.0
        ));
        spellsByClass.put("dragonian", Collections.unmodifiableMap(dragonianMap));
        plugin.getLogger().info("[SPELLS] Dragonian combos: " + dragonianMap.keySet());

        // — DRAGON WARRIOR CLASS —
        Map<String, Spell> dragonwarriorMap = new HashMap<>();
        dragonwarriorMap.put("BASIC_ATTACK", new Spell(
            "dragon_slash", "Dragon Slash", "BASIC_ATTACK",
            0.0,
            MythicSkillConfig.getCooldownSeconds("Dragon_Slash"),
            1,
            WARRIOR_WEAPONS,
            "MYTHIC_DRAGON_SLASH", 0.0
        ));
        dragonwarriorMap.put("LRL", new Spell(
            "dragon_dash", "Dragon Dash", "LRL",
            6.0,
            MythicSkillConfig.getCooldownSeconds("Dragon_Dash"), 3,
            WARRIOR_WEAPONS,
            "MYTHIC_DRAGON_DASH", 0.0
        ));
        dragonwarriorMap.put("LLL", new Spell(
            "dragon_breath", "Dragon Breath", "LLL",
            8.0,
            MythicSkillConfig.getCooldownSeconds("Dragon_Breath"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_DRAGON_BREATH", 0.0
        ));
        dragonwarriorMap.put("LLR", new Spell(
            "dragon_zone", "Dragon Zone", "LLR",
            10.0,
            MythicSkillConfig.getCooldownSeconds("Dragon_Zone"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_DRAGON_ZONE", 0.0
        ));
        dragonwarriorMap.put("RRR", new Spell(
            "dragonborn", "Dragonborn", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("Dragonborn"), 10,
            WARRIOR_WEAPONS,
            "MYTHIC_DRAGONBORN", 0.0
        ));
        spellsByClass.put("dragonwarrior", Collections.unmodifiableMap(dragonwarriorMap));
        plugin.getLogger().info("[SPELLS] DragonWarrior combos: " + dragonwarriorMap.keySet());

        // — WINDRUNE CLASS —
        Map<String, Spell> windruneMap = new HashMap<>();
        windruneMap.put("BASIC_ATTACK", new Spell(
            "gale_slash", "Gale Slash", "BASIC_ATTACK",
            0.0,
            MythicSkillConfig.getCooldownSeconds("Gale_Slash"),
            1,
            WARRIOR_WEAPONS,
            "MYTHIC_GALE_SLASH", 0.0
        ));
        windruneMap.put("LRL", new Spell(
            "vault", "Vault", "LRL",
            6.0,
            MythicSkillConfig.getCooldownSeconds("Vault"), 3,
            WARRIOR_WEAPONS,
            "MYTHIC_VAULT", 0.0
        ));
        windruneMap.put("LLL", new Spell(
            "dancing_blade", "Dancing Blade", "LLL",
            8.0,
            MythicSkillConfig.getCooldownSeconds("Dancing_Blade"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_DANCING_BLADE", 0.0
        ));
        windruneMap.put("LRR", new Spell(
            "cloudpiercer", "Cloudpiercer", "LRR",
            8.0,
            MythicSkillConfig.getCooldownSeconds("Cloudpiercer"), 3,
            WARRIOR_WEAPONS,
            "MYTHIC_CLOUDPIERCER", 0.0
        ));
        windruneMap.put("RLL", new Spell(
            "torrent", "Torrent", "RLL",
            10.0,
            MythicSkillConfig.getCooldownSeconds("Torrent"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_TORRENT", 0.0
        ));
        windruneMap.put("RRR", new Spell(
            "windbound_fury", "Windbound Fury", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("Windbound_Fury"), 10,
            WARRIOR_WEAPONS,
            "MYTHIC_WINDBOUND_FURY", 0.0
        ));
        spellsByClass.put("windrune", Collections.unmodifiableMap(windruneMap));
        plugin.getLogger().info("[SPELLS] Windrune combos: " + windruneMap.keySet());

        // — ARCTIC KNIGHT CLASS —
        Map<String, Spell> arcticMap = new HashMap<>();
        arcticMap.put("BASIC_ATTACK", new Spell(
            "frost_strike", "Frost Strike", "BASIC_ATTACK",
            0.0,
            MythicSkillConfig.getCooldownSeconds("Frost_Strike"),
            1,
            WARRIOR_WEAPONS,
            "MYTHIC_FROST_STRIKE", 0.0
        ));
        arcticMap.put("LRL", new Spell(
            "glacial_impalement", "Glacial Impalement", "LRL",
            6.0,
            MythicSkillConfig.getCooldownSeconds("Glacial_Impalement"), 3,
            WARRIOR_WEAPONS,
            "MYTHIC_GLACIAL_IMPALEMENT", 0.0
        ));
        arcticMap.put("LLL", new Spell(
            "frozen_shield", "Frozen Shield", "LLL",
            8.0,
            MythicSkillConfig.getCooldownSeconds("Frozen_Shield"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_FROZEN_SHIELD", 0.0
        ));
        arcticMap.put("LLR", new Spell(
            "arctic_charge", "Arctic Charge", "LLR",
            10.0,
            MythicSkillConfig.getCooldownSeconds("Arctic_Charge"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_ARCTIC_CHARGE", 0.0
        ));
        arcticMap.put("RLL", new Spell(
            "glacier_smash", "Glacier Smash", "RLL",
            8.0,
            MythicSkillConfig.getCooldownSeconds("Glacier_Smash"), 3,
            WARRIOR_WEAPONS,
            "MYTHIC_GLACIER_SMASH", 0.0
        ));
        arcticMap.put("RRR", new Spell(
            "permafrost_lance", "Permafrost Lance", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("Permafrost_Lance"), 10,
            WARRIOR_WEAPONS,
            "MYTHIC_PERMAFROST_LANCE", 0.0
        ));
        spellsByClass.put("arctic", Collections.unmodifiableMap(arcticMap));
        plugin.getLogger().info("[SPELLS] Arctic combos: " + arcticMap.keySet());

        // — OVERLORD CLASS —
        Map<String, Spell> overlordMap = new HashMap<>();
        overlordMap.put("BASIC_ATTACK", new Spell(
                "dark_bolt", "Dark Bolt", "BASIC_ATTACK",
                0.0,
                MythicSkillConfig.getCooldownSeconds("mf_class_overlord_normalattack"),
                1,
                WeaponType.WAND.getMaterials(),
                "MYTHIC_OVERLORD_DARK_BOLT", 0.0
        ));
        overlordMap.put("LRL", new Spell(
                "summon_minions", "Summon Minions", "LRL",
                10.0,
                MythicSkillConfig.getCooldownSeconds("mf_class_overlord_summon_minions"), 5,
                WeaponType.WAND.getMaterials(),
                "MYTHIC_OVERLORD_SUMMON_MINIONS", 0.0
        ));
        overlordMap.put("RRR", new Spell(
                "dark_ascension", "Dark Ascension", "RRR",
                20.0,
                MythicSkillConfig.getCooldownSeconds("mf_class_overlord_ultimate"), 10,
                WeaponType.WAND.getMaterials(),
                "MYTHIC_OVERLORD_ULTIMATE", 0.0
        ));
        spellsByClass.put("overlord", Collections.unmodifiableMap(overlordMap));
        plugin.getLogger().info("[SPELLS] Overlord combos: " + overlordMap.keySet());


    }

}