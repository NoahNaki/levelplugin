package me.nakilex.levelplugin.spells.gui;

import me.nakilex.levelplugin.player.level.managers.LevelManager;
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
        SPELL_DESCRIPTIONS.put("windrazor",       "Surround yourself with slicing wind.");
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
    }

    /** Simple usage hints for non-combo based spells. */
    private static final Map<String, String> SPELL_USAGE = new HashMap<>();
    static {
        SPELL_USAGE.put("quick_shot", "Left Click");
        SPELL_USAGE.put("backstep", "Right Click");
        SPELL_USAGE.put("windrazor", "Sneak");
        SPELL_USAGE.put("arrow_barrage", "Sneak + Right Click");
        SPELL_USAGE.put("dragon_piercer", "Sneak + Left Click");

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
    }

    /** Maps spell IDs to Nexo item icons */
    private static final Map<String, String> SPELL_ICONS = Map.ofEntries(
        Map.entry("quick_shot", "icon_quick_shot"),
        Map.entry("backstep", "icon_backstep"),
        Map.entry("windrazor", "icon_windrazor"),
        Map.entry("arrow_barrage", "icon_windrazor"),
        Map.entry("deadly_javelin", "icon_deadly_javelin"),
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
        Map.entry("death_sentence", "icon_death_sentence")
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
        // Determine class based on the equipped Ego Weapon
        ItemStack weapon = player.getInventory().getItemInMainHand();
        String classKey = null;
        if (weapon != null && weapon.hasItemMeta()) {
            PersistentDataContainer pdc = weapon.getItemMeta().getPersistentDataContainer();
            if (pdc.has(ItemUtil.EGO_ID_KEY, PersistentDataType.STRING)) {
                String id = pdc.get(ItemUtil.EGO_ID_KEY, PersistentDataType.STRING);
                String prefix = id.split("_")[0];
                if (prefix.equalsIgnoreCase("archer")) classKey = "coolarcher";
                else if (prefix.equalsIgnoreCase("phoenix")) classKey = "phoenixhunter";
                else if (prefix.equalsIgnoreCase("warrior")) classKey = "warrior";
                else if (prefix.equalsIgnoreCase("barbarian")) classKey = "barbarian";
                else if (prefix.equalsIgnoreCase("paladin")) classKey = "paladin";
                else if (prefix.equalsIgnoreCase("death") || prefix.equalsIgnoreCase("deathknight"))
                    classKey = "deathknight";
            }
            if (classKey == null) {
                String name = weapon.getItemMeta().getDisplayName();
                if (name != null) {
                    String lower = name.toLowerCase();
                    if (lower.contains("necroslayer")) classKey = "deathknight";
                }
            }
        }

        if (classKey == null) {
            player.sendMessage(ChatColor.RED + "Hold an Ego Weapon to view its spells.");
            Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_GREEN + "Spell Book");
            ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta fm = filler.getItemMeta();
            fm.setDisplayName(" ");
            filler.setItemMeta(fm);
            for (int i = 0; i < gui.getSize(); i++) gui.setItem(i, filler);
            player.openInventory(gui);
            return;
        }

        // Create a 27-slot inventory titled "Spell Book"
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_GREEN + "Spell Book");

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
            usage = spell.getCombo().replace("L", "Left").replace("R", "Right");
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
