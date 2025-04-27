package me.nakilex.levelplugin.spells.gui;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.spells.Spell;
import me.nakilex.levelplugin.spells.managers.SpellManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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

        SPELL_DESCRIPTIONS.put("endless_assault",   "Assault your target with a barrage of attacks");
        SPELL_DESCRIPTIONS.put("blade_fury",    "Spin furiously, slicing nearby enemies.");
        SPELL_DESCRIPTIONS.put("shadow_clone",  "Spawn a shadow clone that you can swap places with.");
        SPELL_DESCRIPTIONS.put("vanish",        "Disappear in the shadows temporarily.");

        SPELL_DESCRIPTIONS.put("power_shot",        "Charge up a powerful arrow shot.");
        SPELL_DESCRIPTIONS.put("explosive_arrow",   "Fire an arrow that explodes on impact.");
        SPELL_DESCRIPTIONS.put("grapple_hook",      "Grapple to surfaces for mobility.");
        SPELL_DESCRIPTIONS.put("arrow_storm",       "Rain down a storm of arrows.");
    }

    // The slots where we will place the spells in a 27-slot inventory.
    private static final int[] SPELL_SLOTS = { 10, 12, 14, 16 };

    /**
     * Opens the Spell GUI for the given player. It fills all slots with filler and places the class spells
     * in slots 10, 12, 14, and 16 (sorted by level requirement).
     */
    public static void openSpellGUI(Player player) {
        // Get the player's class from StatsManager
        PlayerClass playerClass = getPlayerClass(player);
        Bukkit.getLogger().info("[SpellGUI] " + player.getName() + " is detected as: " + playerClass);

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

        // If the player is a VILLAGER (or has no real class), do not place any spells.
        if (playerClass == PlayerClass.VILLAGER) {
            Bukkit.getLogger().info("[SpellGUI] " + player.getName() + " is a VILLAGER, so no spells will be displayed.");
            player.openInventory(gui);
            return;
        }

        // Convert playerClass to lowercase to match SpellManager keys.
        String classKey = playerClass.name().toLowerCase();
        Bukkit.getLogger().info("[SpellGUI] Looking up spells for class key: " + classKey);

        // Retrieve spells for that class.
        Map<String, Spell> classSpells = SpellManager.getInstance().getSpellsByClass(classKey);
        if (classSpells == null || classSpells.isEmpty()) {
            Bukkit.getLogger().warning("[SpellGUI] No spells found for class: " + classKey);
            player.sendMessage(ChatColor.RED + "No spells available for your class!");
            player.openInventory(gui);
            return;
        }
        Bukkit.getLogger().info("[SpellGUI] Found " + classSpells.size() + " spells for " + playerClass);

        // Create a list and sort the spells by their level requirement (lowest to highest)
        List<Spell> spells = new ArrayList<>(classSpells.values());
        spells.sort(Comparator.comparingInt(Spell::getLevelReq));

        // Get player's level from LevelManager
        int playerLevel = LevelManager.getInstance().getLevel(player);

        // Place up to 4 spells in the designated slots.
        for (int i = 0; i < SPELL_SLOTS.length && i < spells.size(); i++) {
            Spell spell = spells.get(i);
            ItemStack spellItem = createSpellItem(spell, playerLevel);
            gui.setItem(SPELL_SLOTS[i], spellItem);
            Bukkit.getLogger().info("[SpellGUI] Placed spell '" + spell.getDisplayName() + "' in slot " + SPELL_SLOTS[i]);
        }

        // Finally, open the GUI for the player.
        player.openInventory(gui);
    }

    /**
     * Retrieves the player's class from StatsManager.
     */
    private static PlayerClass getPlayerClass(Player player) {
        PlayerClass pClass = StatsManager.getInstance().getPlayerStats(player.getUniqueId()).playerClass;
        // Log the retrieved class
        Bukkit.getLogger().info("[SpellGUI] Retrieved class for " + player.getName() + ": " + pClass);
        return pClass;
    }

    /**
     * Creates the ItemStack that represents a spell in the GUI.
     * If the player’s level is lower than the spell's requirement, the spell is considered locked.
     */
    private static ItemStack createSpellItem(Spell spell, int playerLevel) {
        boolean unlocked = (playerLevel >= spell.getLevelReq());
        Material material = unlocked ? Material.SLIME_BALL : Material.FIREWORK_STAR;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        // Set the display name with color based on locked/unlocked.
        String spellName = spell.getDisplayName();
        meta.setDisplayName(unlocked ? ChatColor.GREEN + spellName : ChatColor.RED + spellName);

        // Build the lore with combo, mana cost, and level requirement.
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Combo: " + ChatColor.YELLOW + spell.getCombo());
        lore.add(ChatColor.GRAY + "Mana Cost: " + ChatColor.YELLOW + spell.getManaCost());
        lore.add(ChatColor.GRAY + "Requires Level: " + ChatColor.YELLOW + spell.getLevelReq());

        if (unlocked) {
            lore.add(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "--------------------");
            String description = SPELL_DESCRIPTIONS.getOrDefault(spell.getId(), "No description available.");
            lore.add(ChatColor.GRAY + "Description: " + ChatColor.WHITE + description);
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
