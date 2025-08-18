package me.nakilex.levelplugin.spells.gui;

import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.spells.Spell;
import me.nakilex.levelplugin.spells.managers.SpellManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;

import java.util.*;

public class SpellGUI {

    // Example descriptions keyed by Spell ID.
    private static final Map<String, String> SPELL_DESCRIPTIONS = new HashMap<>();
    static {
        SPELL_DESCRIPTIONS.put("iron_fortress", "Fortify yourself with an iron barrier.");
        SPELL_DESCRIPTIONS.put("heroic_leap",   "Leap heroically to close distance.");
        SPELL_DESCRIPTIONS.put("uppercut",      "Strike upwards, launching your enemy.");
        SPELL_DESCRIPTIONS.put("ground_slam",   "Slam the ground to damage nearby foes.");

        SPELL_DESCRIPTIONS.put("meteor",        "Call down a meteor from the sky.");
        SPELL_DESCRIPTIONS.put("blackhole",     "Create a singularity that pulls in enemies.");
        SPELL_DESCRIPTIONS.put("heal",          "Heal yourself or allies.");
        SPELL_DESCRIPTIONS.put("teleport",      "Teleport forward a short distance.");

        SPELL_DESCRIPTIONS.put("crescent_slash", "Unleash a crescent wave that cuts enemies in front of you.");
        SPELL_DESCRIPTIONS.put("vanish",        "Disappear in the shadows temporarily.");
        SPELL_DESCRIPTIONS.put("multihit",      "Dash around an enemy and hit many times.");
        SPELL_DESCRIPTIONS.put("smoke_bomb",    "Throw a bomb that slows and damages foes.");

        SPELL_DESCRIPTIONS.put("power_shot",        "Charge up a powerful arrow shot.");
        SPELL_DESCRIPTIONS.put("bow_drone",   "Summon a sentry that shoots enemies.");
        SPELL_DESCRIPTIONS.put("grapple_hook",      "Grapple to surfaces for mobility.");
        SPELL_DESCRIPTIONS.put("arrow_storm",       "Rain down a storm of arrows.");

        SPELL_DESCRIPTIONS.put("quick_shot",      "Fire a quick empowered arrow.");
        SPELL_DESCRIPTIONS.put("backstep",        "Leap backwards to evade foes.");
        SPELL_DESCRIPTIONS.put("arrow_barrage",   "Rapidly fire a volley of arrows.");
        SPELL_DESCRIPTIONS.put("dragon_piercer",  "Launch a devastating dragon arrow.");

        SPELL_DESCRIPTIONS.put("blazing_feathers", "Fire fiery feathers at your foes.");
        SPELL_DESCRIPTIONS.put("ashdance", "Dash in flames leaving fire in your wake.");
        SPELL_DESCRIPTIONS.put("flameburst_convergence", "Unleash converging fire bolts.");
        SPELL_DESCRIPTIONS.put("phoenix_totem", "Summon a blazing totem that burns enemies.");
        SPELL_DESCRIPTIONS.put("pyroclasmic_barrage", "Launch a barrage of burning feathers.");
        SPELL_DESCRIPTIONS.put("phoenix_rebirth", "Transform into a phoenix to scorch foes.");

        SPELL_DESCRIPTIONS.put("brutal_strike", "Swing your axe in a brutal strike.");
        SPELL_DESCRIPTIONS.put("charge", "Rush forward, knocking enemies aside.");
        SPELL_DESCRIPTIONS.put("chain_hook", "Throw a chain to pull foes to you.");
        SPELL_DESCRIPTIONS.put("shield_barrier", "Raise a temporary blocking shield.");
        SPELL_DESCRIPTIONS.put("whirlwind", "Spin and damage nearby foes.");
        SPELL_DESCRIPTIONS.put("judgement", "Leap and smash the ground mightily.");
        SPELL_DESCRIPTIONS.put("rampage", "Gain buffs when near death.");

        SPELL_DESCRIPTIONS.put("rageblade", "Strike rapidly with your axe.");
        SPELL_DESCRIPTIONS.put("primal_axe", "Hurl your axe forward with force.");
        SPELL_DESCRIPTIONS.put("war_cry", "Shout to debuff nearby enemies.");
        SPELL_DESCRIPTIONS.put("double_edge", "Spin with deadly slashes.");
        SPELL_DESCRIPTIONS.put("relentless_leap", "Leap and pull foes together.");
        SPELL_DESCRIPTIONS.put("eternal_fury", "Enter an empowered rage.");

        SPELL_DESCRIPTIONS.put("holy_strike", "Strike with righteous power.");
        SPELL_DESCRIPTIONS.put("bound_seal", "Bind foes in holy chains.");
        SPELL_DESCRIPTIONS.put("hammer_of_justice", "Smash down a massive hammer.");
        SPELL_DESCRIPTIONS.put("heavenly_shield", "Grant a protective barrier.");
        SPELL_DESCRIPTIONS.put("unbreakable_will", "Dash forward with steadfast will.");
        SPELL_DESCRIPTIONS.put("last_stand", "Unleash a devastating holy assault.");

        SPELL_DESCRIPTIONS.put("death_strike", "Slash enemies with dark power.");
        SPELL_DESCRIPTIONS.put("phantom_charge", "Dash forward in a spectral charge.");
        SPELL_DESCRIPTIONS.put("wraithbound_chains", "Throw chains that bind foes.");
        SPELL_DESCRIPTIONS.put("soul_barrier", "Summon souls to shield yourself.");
        SPELL_DESCRIPTIONS.put("necrotic_whirlwind", "Spin with necrotic energy.");
        SPELL_DESCRIPTIONS.put("death_sentence", "Leap and crush targets with deathly force.");
        SPELL_DESCRIPTIONS.put("aqua_slash", "Slash enemies with a burst of water.");
        SPELL_DESCRIPTIONS.put("abyssal_dash", "Dash forward leaving waves behind.");
        SPELL_DESCRIPTIONS.put("tidal_wave", "Summon a wave that knocks foes back.");
        SPELL_DESCRIPTIONS.put("aqua_aura", "Emit an aura that empowers allies.");
        SPELL_DESCRIPTIONS.put("abyssal_smash", "Leap high and smash down with tidal force.");
        // Assassin
        SPELL_DESCRIPTIONS.put("blade_slash", "Slash enemies with your blade.");
        SPELL_DESCRIPTIONS.put("assassin_dash", "Dash swiftly forward.");
        SPELL_DESCRIPTIONS.put("dagger_throw", "Hurl daggers at distant foes.");
        SPELL_DESCRIPTIONS.put("blade_dance", "Teleport between enemies with deadly strikes.");
        SPELL_DESCRIPTIONS.put("shadow_walk", "Vanish into the shadows to gain invisibility.");
        // Awakened Assassin
        SPELL_DESCRIPTIONS.put("lethal_combo", "Chain blades into a lethal combo.");
        SPELL_DESCRIPTIONS.put("ravaging_dash", "Dash forward leaving slashes in your wake.");
        SPELL_DESCRIPTIONS.put("death_bloom", "Vanish then cut nearby enemies repeatedly.");
        SPELL_DESCRIPTIONS.put("shadowquake", "Disappear and erupt with a shadowquake.");
        SPELL_DESCRIPTIONS.put("crimson_arc", "Hurl returning crimson shuriken.");
        SPELL_DESCRIPTIONS.put("last_dance", "Unleash a devastating dance of blades.");
        SPELL_DESCRIPTIONS.put("deadly_calm", "Remain still to empower your next attack.");
        // Awakened Warrior
        SPELL_DESCRIPTIONS.put("bulwark_instinct", "Tap into instinct to gain resilience.");
        SPELL_DESCRIPTIONS.put("brutal_combo", "Perform a brutal multi-hit combo.");
        SPELL_DESCRIPTIONS.put("berserkers_leap", "Leap toward enemies with ferocity.");
        SPELL_DESCRIPTIONS.put("relentless_whirlwind", "Spin in a relentless whirlwind of blades.");
        SPELL_DESCRIPTIONS.put("bloodbound_barrier", "Conjure a barrier fueled by blood.");
        SPELL_DESCRIPTIONS.put("vicious_strike", "Wind up a devastating strike.");
        SPELL_DESCRIPTIONS.put("strike_of_fury", "Unleash a furious ultimate assault.");
        // Awakened Archer
        SPELL_DESCRIPTIONS.put("blasting_combo", "Roll and fire a bursting arrow combo.");
        SPELL_DESCRIPTIONS.put("evasive_shot", "Leap back and shoot in mid-air.");
        SPELL_DESCRIPTIONS.put("piercing_skyfall", "Rain piercing arrows from above.");
        SPELL_DESCRIPTIONS.put("rapid_arrows", "Loose a flurry of rapid arrows.");
        SPELL_DESCRIPTIONS.put("shot_of_destruction", "Charge an arrow that devastates targets.");
        SPELL_DESCRIPTIONS.put("volley_of_arrows", "Call down a volley of arrows around you.");
        SPELL_DESCRIPTIONS.put("ambush", "Prepare to bleed targets with your next strike.");

        SPELL_DESCRIPTIONS.put("fireball", "Launch a blazing projectile.");
        SPELL_DESCRIPTIONS.put("blink", "Teleport a short distance.");
        SPELL_DESCRIPTIONS.put("frost_nova", "Freeze and damage nearby foes.");
        SPELL_DESCRIPTIONS.put("inferno_chains", "Bind enemies with burning chains.");

        // Dragonian
        SPELL_DESCRIPTIONS.put("dragonian_slash", "Slash foes with blazing speed.");
        SPELL_DESCRIPTIONS.put("dragonian_lunge", "Lunge forward leaving flames.");
        SPELL_DESCRIPTIONS.put("dragonian_rs", "Cleave enemies in a fiery sweep.");
        SPELL_DESCRIPTIONS.put("dragonian_ss", "Enter a stance of draconic power.");
        SPELL_DESCRIPTIONS.put("taotie_dragon", "Summon the fearsome Taotie dragon.");

        // Windrune
        SPELL_DESCRIPTIONS.put("gale_slash", "Slash enemies with slicing wind.");
        SPELL_DESCRIPTIONS.put("vault", "Leap skyward with the wind's aid.");
        SPELL_DESCRIPTIONS.put("dancing_blade", "Dash and cut surrounding foes.");
        SPELL_DESCRIPTIONS.put("torrent", "Spin rapidly drawing enemies in.");
        SPELL_DESCRIPTIONS.put("cloudpiercer", "Launch a piercing spear of air.");
        SPELL_DESCRIPTIONS.put("windbound_fury", "Unleash a raging windstorm.");

        // Arctic Knight
        SPELL_DESCRIPTIONS.put("frost_strike", "Strike with chilling power.");
        SPELL_DESCRIPTIONS.put("glacial_impalement", "Impale foes on icy spikes.");
        SPELL_DESCRIPTIONS.put("glacier_smash", "Leap and smash the ground with ice.");
        SPELL_DESCRIPTIONS.put("arctic_charge", "Charge forward leaving frost behind.");
        SPELL_DESCRIPTIONS.put("frozen_shield", "Conjure a protective ice shield.");
        SPELL_DESCRIPTIONS.put("permafrost_lance", "Devastate enemies with frozen might.");
    }

    /** Simple usage hints for non-combo based spells. */
    private static final Map<String, String> SPELL_USAGE = new HashMap<>();
    static {
        SPELL_USAGE.put("quick_shot", "Right Click");
        SPELL_USAGE.put("backstep", "Left Click");
        SPELL_USAGE.put("arrow_barrage", "Sneak");
        SPELL_USAGE.put("bow_drone", "Sneak + Left Click");
        SPELL_USAGE.put("dragon_piercer", "Sneak + Right Click");

        SPELL_USAGE.put("blazing_feathers", "Left Click");
        SPELL_USAGE.put("ashdance", "Right Click");
        SPELL_USAGE.put("flameburst_convergence", "Sneak");
        SPELL_USAGE.put("phoenix_totem", "Passive");
        SPELL_USAGE.put("pyroclasmic_barrage", "Sneak + Right Click");
        SPELL_USAGE.put("phoenix_rebirth", "Sneak + Left Click");

        SPELL_USAGE.put("brutal_strike", "Left Click");
        SPELL_USAGE.put("charge", "Right Click");
        SPELL_USAGE.put("chain_hook", "Sneak + Right Click");
        SPELL_USAGE.put("shield_barrier", "Sneak");
        SPELL_USAGE.put("shockwave", "Sneak + Left Click");
        SPELL_USAGE.put("whirlwind", "Sneak + Right Click");
        SPELL_USAGE.put("judgement", "Sneak + Left Click");
        SPELL_USAGE.put("rampage", "Sneak + Left Click (low HP)");

        SPELL_USAGE.put("rageblade", "Left Click");
        SPELL_USAGE.put("primal_axe", "Right Click");
        SPELL_USAGE.put("war_cry", "Sneak");
        SPELL_USAGE.put("double_edge", "Sneak + Right Click");
        SPELL_USAGE.put("relentless_leap", "Sneak + Toggle");
        SPELL_USAGE.put("eternal_fury", "Sneak + Left Click");

        SPELL_USAGE.put("holy_strike", "Left Click");
        SPELL_USAGE.put("bound_seal", "Right Click");
        SPELL_USAGE.put("hammer_of_justice", "Sneak");
        SPELL_USAGE.put("heavenly_shield", "Sneak + Right Click");
        SPELL_USAGE.put("unbreakable_will", "Sneak + Left Click");
        SPELL_USAGE.put("last_stand", "Sneak + Left Click");

        SPELL_USAGE.put("death_strike", "Left Click");
        SPELL_USAGE.put("phantom_charge", "Right Click");
        SPELL_USAGE.put("wraithbound_chains", "Sneak");
        SPELL_USAGE.put("soul_barrier", "Sneak");
        SPELL_USAGE.put("necrotic_whirlwind", "Sneak + Right Click");
        SPELL_USAGE.put("death_sentence", "Sneak + Left Click");
        SPELL_USAGE.put("aqua_slash", "Left Click");
        SPELL_USAGE.put("abyssal_dash", "Right Click");
        SPELL_USAGE.put("tidal_wave", "Sneak + Right Click");
        SPELL_USAGE.put("aqua_aura", "Sneak");
        SPELL_USAGE.put("abyssal_smash", "Sneak + Left Click");
        SPELL_USAGE.put("blade_slash", "Left Click");
        SPELL_USAGE.put("assassin_dash", "Right Click");
        SPELL_USAGE.put("dagger_throw", "Sneak + Right Click");
        SPELL_USAGE.put("blade_dance", "Sneak + Left Click");
        SPELL_USAGE.put("shadow_walk", "Sneak");
        // Awakened Assassin
        SPELL_USAGE.put("lethal_combo", "Left Click");
        SPELL_USAGE.put("ravaging_dash", "Right Click");
        SPELL_USAGE.put("crimson_arc", "Sneak + Right Click");
        SPELL_USAGE.put("last_dance", "Sneak + Left Click");
        // Shadowquake now uses a click combo rather than a sneak trigger
        SPELL_USAGE.put("shadowquake", "Left + Left + Right");
        SPELL_USAGE.put("death_bloom", "Sneak + Toggle");
        SPELL_USAGE.put("deadly_calm", "Passive");
        // Awakened Warrior
        SPELL_USAGE.put("bulwark_instinct", "Passive");
        SPELL_USAGE.put("brutal_combo", "Left Click");
        SPELL_USAGE.put("berserkers_leap", "Right Click");
        SPELL_USAGE.put("relentless_whirlwind", "Sneak + Right Click");
        SPELL_USAGE.put("bloodbound_barrier", "Sneak");
        SPELL_USAGE.put("vicious_strike", "Sneak + Toggle");
        SPELL_USAGE.put("strike_of_fury", "Sneak + Left Click");
        // Awakened Archer
        SPELL_USAGE.put("blasting_combo", "Left Click");
        SPELL_USAGE.put("evasive_shot", "Left + Right + Left");
        SPELL_USAGE.put("piercing_skyfall", "Left + Left + Left");
        SPELL_USAGE.put("rapid_arrows", "Right + Left + Left");
        SPELL_USAGE.put("shot_of_destruction", "Right + Right + Right");
        SPELL_USAGE.put("volley_of_arrows", "Sneak + Toggle");
        SPELL_USAGE.put("ambush", "Passive");
        SPELL_USAGE.put("fireball", "Left Click");
        SPELL_USAGE.put("blink", "Right Click");
        SPELL_USAGE.put("meteor", "Sneak + Right Click");
        SPELL_USAGE.put("frost_nova", "Sneak");
        SPELL_USAGE.put("inferno_chains", "Sneak + Left Click");

        // Dragonian
        SPELL_USAGE.put("dragonian_slash", "Left Click");
        SPELL_USAGE.put("dragonian_lunge", "Right Click");
        SPELL_USAGE.put("dragonian_rs", "Sneak + Right Click");
        SPELL_USAGE.put("dragonian_ss", "Sneak");
        SPELL_USAGE.put("taotie_dragon", "Sneak + Left Click");

        // Windrune
        SPELL_USAGE.put("gale_slash", "Left Click");
        SPELL_USAGE.put("vault", "Right Click");
        SPELL_USAGE.put("dancing_blade", "Sneak + Right Click");
        SPELL_USAGE.put("torrent", "Sneak + Toggle");
        SPELL_USAGE.put("cloudpiercer", "Sneak");
        SPELL_USAGE.put("windbound_fury", "Sneak + Left Click");

        // Arctic Knight
        SPELL_USAGE.put("frost_strike", "Left Click");
        SPELL_USAGE.put("glacial_impalement", "Right Click");
        SPELL_USAGE.put("frozen_shield", "Sneak + Right Click");
        SPELL_USAGE.put("arctic_charge", "Sneak");
        SPELL_USAGE.put("glacier_smash", "Sneak + Toggle");
        SPELL_USAGE.put("permafrost_lance", "Sneak + Left Click");
    }

    /** Maps spell IDs to Nexo item icons */
    private static final Map<String, String> SPELL_ICONS = Map.ofEntries(
        Map.entry("quick_shot", "icon_quick_shot"),
        Map.entry("backstep", "icon_backstep"),
        Map.entry("arrow_barrage", "icon_windrazor"),
        Map.entry("bow_drone", "icon_deadly_javelin"),
        Map.entry("dragon_piercer", "icon_dragon_piercer"),
        // Phoenix Hunter
        Map.entry("blazing_feathers", "icon_blazing_feathers"),
        Map.entry("ashdance", "icon_ashdance"),
        Map.entry("flameburst_convergence", "icon_flameburst_convergence"),
        Map.entry("phoenix_totem", "icon_phoenix_totem"),
        Map.entry("pyroclasmic_barrage", "icon_pyroclasmic_barrage"),
        Map.entry("phoenix_rebirth", "icon_phoenix_rebirth"),
        Map.entry("flameborn", "icon_flameborn"),
        // Warrior
        Map.entry("brutal_strike", "icon_brutal_strike"),
        Map.entry("charge", "icon_charge"),
        Map.entry("chain_hook", "icon_chain_hook"),
        Map.entry("shield_barrier", "icon_shield_barrier"),
        Map.entry("whirlwind", "icon_rampage"),
        Map.entry("judgement", "icon_judgement"),
        Map.entry("rampage", "icon_rampage"),
        // Barbarian
        Map.entry("bloodlust", "icon_bloodlust"),
        Map.entry("rageblade", "icon_rageblade"),
        Map.entry("primal_axe", "icon_primal_axe"),
        Map.entry("war_cry", "icon_war_cry"),
        Map.entry("double_edge", "icon_double_edge"),
        Map.entry("relentless_leap", "icon_relentless_leap"),
        Map.entry("eternal_fury", "icon_eternal_fury"),
        // Paladin
        Map.entry("radiant_aura", "icon_radiant_aura"),
        Map.entry("holy_strike", "icon_holy_strike"),
        Map.entry("bound_seal", "icon_bound_seal"),
        Map.entry("hammer_of_justice", "icon_hammer_of_justice"),
        Map.entry("heavenly_shield", "icon_heavenly_shield"),
        Map.entry("unbreakable_will", "icon_unbreakable_will"),
        Map.entry("last_stand", "icon_last_stand"),
        // Death Knight
        Map.entry("death_strike", "icon_death_strike"),
        Map.entry("phantom_charge", "icon_phantom_charge"),
        Map.entry("wraithbound_chains", "icon_wraithbound_chains"),
        Map.entry("soul_barrier", "icon_soul_barrier"),
        Map.entry("necrotic_whirlwind", "icon_necrotic_whirlwind"),
        Map.entry("death_sentence", "icon_death_sentence"),
        Map.entry("aqua_slash", "icon_aqua_slash"),
        Map.entry("abyssal_dash", "icon_abyssal_dash"),
        Map.entry("tidal_wave", "icon_tidal_wave"),
        Map.entry("aqua_aura", "icon_aqua_aura"),
        Map.entry("abyssal_smash", "icon_abyssal_smash"),
        Map.entry("blade_slash", "icon_blade_slash"),
        Map.entry("assassin_dash", "icon_assassin_dash"),
        Map.entry("dagger_throw", "icon_dagger_throw"),
        Map.entry("blade_dance", "icon_blade_dance"),
        Map.entry("shadow_walk", "icon_shadow_walk"),
        // Awakened Assassin
        Map.entry("lethal_combo", "icon_lethal_combo"),
        Map.entry("ravaging_dash", "icon_ravaging_dash"),
        Map.entry("death_bloom", "icon_death_bloom"),
        Map.entry("shadowquake", "icon_shadowquake"),
        Map.entry("crimson_arc", "icon_crimson_arc"),
        Map.entry("last_dance", "icon_last_dance"),
        Map.entry("deadly_calm", "icon_deadly_calm"),
        // Awakened Warrior
        Map.entry("bulwark_instinct", "icon_bulwark_instinct"),
        Map.entry("brutal_combo", "icon_brutal_combo"),
        Map.entry("berserkers_leap", "icon_berserkers_leap"),
        Map.entry("relentless_whirlwind", "icon_relentless_whirlwind"),
        Map.entry("bloodbound_barrier", "icon_bloodbound_barrier"),
        Map.entry("vicious_strike", "icon_vicious_strike"),
        Map.entry("strike_of_fury", "icon_strike_of_fury"),
        // Awakened Archer
        Map.entry("blasting_combo", "icon_blasting_combo"),
        Map.entry("evasive_shot", "icon_evasive_shot"),
        Map.entry("piercing_skyfall", "icon_piercing_skyfall"),
        Map.entry("rapid_arrows", "icon_rapid_arrows"),
        Map.entry("shot_of_destruction", "icon_shot_of_destruction"),
        Map.entry("volley_of_arrows", "icon_volley_of_arrows"),
        Map.entry("ambush", "icon_ambush"),
        Map.entry("fireball", "icon_fireball"),
        Map.entry("blink", "icon_blink"),
        Map.entry("meteor", "icon_meteor"),
        Map.entry("frost_nova", "icon_frost_nova"),
        Map.entry("inferno_chains", "icon_inferno_chains")
        ,
        // Dragonian
        Map.entry("dragonian_slash", "icon_dragonian_slash"),
        Map.entry("dragonian_lunge", "icon_dragonian_lunge"),
        Map.entry("dragonian_rs", "icon_dragonian_rs"),
        Map.entry("dragonian_ss", "icon_dragonian_ss"),
        Map.entry("taotie_dragon", "icon_taotie_dragon"),
        // Windrune
        Map.entry("gale_slash", "icon_gale_slash"),
        Map.entry("vault", "icon_vault"),
        Map.entry("dancing_blade", "icon_dancing_blade"),
        Map.entry("torrent", "icon_torrent"),
        Map.entry("cloudpiercer", "icon_cloudpiercer"),
        Map.entry("windbound_fury", "icon_windbound_fury"),
        // Arctic Knight
        Map.entry("frost_strike", "icon_frost_strike"),
        Map.entry("glacial_impalement", "icon_glacial_impalement"),
        Map.entry("glacier_smash", "icon_glacier_smash"),
        Map.entry("arctic_charge", "icon_arctic_charge"),
        Map.entry("frozen_shield", "icon_frozen_shield"),
        Map.entry("permafrost_lance", "icon_permafrost_lance")
    );

    // The slots where we will place the spells in a 27-slot inventory.
    private static final int[] SPELL_SLOTS = { 10, 12, 14, 16, 22 };

    private static ItemStack getNexoItem(String id, String name) {
        ItemBuilder builder = NexoItems.itemFromId(id);
        ItemStack item = builder == null ? new ItemStack(Material.PAPER) : builder.build();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null && !name.isEmpty()) meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Opens the Spell GUI for the given player. It fills all slots with filler and places the class spells
     * in slots 10, 12, 14, and 16 (sorted by level requirement).
     */
    public static void openSpellGUI(Player player) {
        // Determine class based on the player's selected class
        var ps = me.nakilex.levelplugin.player.attributes.managers.StatsManager.getInstance()
                .getPlayerStats(player.getUniqueId());
        PlayerClass pClass = ps.playerClass;
        String classKey = switch (pClass) {
            case ARCHER -> "archer";
            case DEADEYE -> "deadeye";
            case PHOENIXHUNTER -> "phoenixhunter";
            case WARRIOR -> "warrior";
            case BARBARIAN -> "barbarian";
            case PALADIN -> "paladin";
            case DEATHKNIGHT -> "deathknight";
            case ASSASSIN -> "assassin";
            case ABYSSION -> "abyssion";
            case MAGE -> "mage";
            case DRAGONIAN -> "dragonian";
            case GALEGLAIVE -> "windrune";
            case ARCTICKNIGHT -> "arctic";
            case DRAGONWARRIOR -> "dragonwarrior";
            case AWAKASSASSIN -> "awakassassin";
            case AWAKWARRIOR -> "awakwarrior";
            case WITCH -> "witch";
            default -> null;
        };

        if (classKey == null) {
            player.sendMessage(ChatColor.RED + "Select a class to view its spells.");
            Inventory gui = Bukkit.createInventory(null, 27, ChatColor.BLACK + "Spell Book");
            ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta fm = filler.getItemMeta();
            fm.setDisplayName(" ");
            filler.setItemMeta(fm);
            for (int i = 0; i < gui.getSize(); i++) gui.setItem(i, filler);
            player.openInventory(gui);
            return;
        }

        // Create a 27-slot inventory titled "Spell Book"
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.BLACK + "Spell Book");

        // Fill all slots with dark gray stained glass panes as fillers
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, filler);
        }

        Bukkit.getLogger().info("[SpellGUI] Looking up spells for class key: " + classKey);

        // Retrieve spells for that class.
        Map<String, Spell> classSpells = SpellManager.getInstance().getSpellsByClass(classKey);
        if (classSpells == null || classSpells.isEmpty()) {
            Bukkit.getLogger().warning("[SpellGUI] No spells found for class: " + classKey);
            player.sendMessage(ChatColor.RED + "No spells available for your class!");
            player.openInventory(gui);
            return;
        }
        Bukkit.getLogger().info("[SpellGUI] Found " + classSpells.size() + " spells for class " + classKey);

        // Create a list and sort the spells by their level requirement (lowest to highest)
        List<Spell> spells = new ArrayList<>(classSpells.values());
        spells.sort(Comparator.comparingInt(Spell::getLevelReq));

        int playerRank = 0;
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand != null && hand.hasItemMeta()) {
            PersistentDataContainer pdc = hand.getItemMeta().getPersistentDataContainer();
            playerRank = pdc.getOrDefault(ItemUtil.EGO_RANK_KEY, PersistentDataType.INTEGER, 0);
        }

        // Place up to 4 spells in the designated slots.
        for (int i = 0; i < SPELL_SLOTS.length && i < spells.size(); i++) {
            Spell spell = spells.get(i);
            ItemStack spellItem = createSpellItem(player, spell, playerRank);
            gui.setItem(SPELL_SLOTS[i], spellItem);
            Bukkit.getLogger().info("[SpellGUI] Placed spell '" + spell.getDisplayName() + "' in slot " + SPELL_SLOTS[i]);
        }

        // Finally, open the GUI for the player.
        player.openInventory(gui);
    }

    /**
     * Creates the ItemStack that represents a spell in the GUI.
     * If the player’s level is lower than the spell's requirement, the spell is considered locked.
     */
    private static ItemStack createSpellItem(Player player, Spell spell, int playerLevel) {
        boolean unlocked = (playerLevel >= spell.getLevelReq());
        String iconId = SPELL_ICONS.get(spell.getId());
        ItemStack item;
        if (iconId != null && unlocked) {
            item = getNexoItem(iconId, "");
        } else if (iconId != null) {
            item = new ItemStack(Material.FIREWORK_STAR);
        } else {
            item = new ItemStack(unlocked ? Material.SLIME_BALL : Material.FIREWORK_STAR);
        }
        ItemMeta meta = item.getItemMeta();

        // Set the display name with color based on locked/unlocked.
        String spellName = spell.getDisplayName();
        meta.setDisplayName(unlocked ? ChatColor.GREEN + spellName : ChatColor.RED + spellName);

        // Build the lore with usage info and level requirement.
        List<String> lore = new ArrayList<>();

        String usage = SPELL_USAGE.get(spell.getId());
        if (usage == null) {
            switch (spell.getCombo()) {
                case "LEFT" -> usage = "Left Click";
                case "RIGHT" -> usage = "Right Click";
                case "SHIFT_LEFT" -> usage = "Sneak + Left Click";
                case "SHIFT_RIGHT" -> usage = "Sneak + Right Click";
                case "SNEAK" -> usage = "Sneak";
                default -> usage = spell.getCombo();
            }
        }

        lore.add(ChatColor.GRAY + "Usage: " + ChatColor.YELLOW + usage);
        lore.add(ChatColor.GRAY + "Required Rank: " + ChatColor.YELLOW + spell.getLevelReq());

        if (unlocked) {
            lore.add(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "--------------------");
            String desc = SPELL_DESCRIPTIONS.getOrDefault(spell.getId(), "No description available.");
            lore.add(ChatColor.GRAY + "Description: " + ChatColor.WHITE + desc);

            // Additional spell details could be listed here
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
