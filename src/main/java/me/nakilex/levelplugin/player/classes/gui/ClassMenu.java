package me.nakilex.levelplugin.player.classes.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class ClassMenu {

    public static Inventory getClassSelectionMenu() {
        // Create an inventory with 27 slots
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_GREEN + "Choose Your Class");

        // Warrior (using wooden shovel as icon)
        inv.setItem(10, createMenuItem(Material.WOODEN_SHOVEL, ChatColor.GREEN + "" + ChatColor.BOLD + "Start As A Warrior!",
            Arrays.asList(
                "",
                ChatColor.GRAY + "Choosing this will start you out as a ",
                ChatColor.GREEN + "WARRIOR" + ChatColor.GRAY + "! Your starting item will be a sword.",
                "",
                ChatColor.RED + "" + ChatColor.BOLD + "ABILITY 1:",
                ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Iron Fortress " + ChatColor.GRAY +
                    "(Combo: RLR, Cost: 15, Cooldown: 3s)",
                ChatColor.YELLOW + "" + ChatColor.BOLD + "ABILITY 2:",
                ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Heroic Leap " + ChatColor.GRAY +
                    "(Combo: RRR, Cost: 10, Cooldown: 15s, Multiplier: 1.2)",
                ChatColor.BLUE + "" + ChatColor.BOLD + "ABILITY 3:",
                ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Uppercut " + ChatColor.GRAY +
                    "(Combo: RRL, Cost: 15, Extra: 4, Cooldown: 10s, Multiplier: 1.3)",
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "ABILITY 4:",
                ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Ground Slam " + ChatColor.GRAY +
                    "(Combo: RLL, Cost: 14, Cooldown: 8s, Multiplier: 1.5)",
                "",
                ChatColor.GOLD + "" + ChatColor.BOLD + "Notice! " + ChatColor.GOLD + "You can switch your class at any time.",
                "",
                ChatColor.WHITE + "" + ChatColor.BOLD + "Click To Begin Your Adventure!"
            )));

        // Archer (using bow as icon)
        inv.setItem(12, createMenuItem(Material.BOW, ChatColor.YELLOW + "" + ChatColor.BOLD + "Start As An Archer!",
            Arrays.asList(
                "",
                ChatColor.GRAY + "Choosing this will start you out as an ",
                ChatColor.YELLOW + "ARCHER" + ChatColor.GRAY + "! Your starting item will be a bow.",
                "",
                ChatColor.RED + "" + ChatColor.BOLD + "ABILITY 1:",
                ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Power Shot " + ChatColor.GRAY +
                    "(Combo: LLR, Cost: 12, Cooldown: 3s, Multiplier: 2.0)",
                ChatColor.YELLOW + "" + ChatColor.BOLD + "ABILITY 2:",
                ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Explosive Arrow " + ChatColor.GRAY +
                    "(Combo: LRR, Cost: 15, Cooldown: 8s, Multiplier: 1.5)",
                ChatColor.BLUE + "" + ChatColor.BOLD + "ABILITY 3:",
                ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Grapple Hook " + ChatColor.GRAY +
                    "(Combo: LLL, Cost: 8, Cooldown: 10s)",
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "ABILITY 4:",
                ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Arrow Storm " + ChatColor.GRAY +
                    "(Combo: LRL, Cost: 20, Cooldown: 15s, Multiplier: 0.5)",
                "",
                ChatColor.GOLD + "" + ChatColor.BOLD + "Notice! " + ChatColor.GOLD + "You can switch your class at any time.",
                "",
                ChatColor.WHITE + "" + ChatColor.BOLD + "Click To Begin Your Adventure!"
            )));

        // Mage (using stick as icon)
        inv.setItem(14, createMenuItem(Material.STICK, ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Start As A Mage!",
            Arrays.asList(
                "",
                ChatColor.GRAY + "Choosing this will start you out as a ",
                ChatColor.LIGHT_PURPLE + "MAGE" + ChatColor.GRAY + "! Your starting item will be a magical staff.",
                "",
                ChatColor.RED + "" + ChatColor.BOLD + "ABILITY 1:",
                ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Meteor " + ChatColor.GRAY +
                    "(Combo: RLL, Cost: 20, Cooldown: 3s, Multiplier: 2.5)",
                ChatColor.YELLOW + "" + ChatColor.BOLD + "ABILITY 2:",
                ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Blackhole " + ChatColor.GRAY +
                    "(Combo: RRL, Cost: 18, Cooldown: 15s)",
                ChatColor.BLUE + "" + ChatColor.BOLD + "ABILITY 3:",
                ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Heal " + ChatColor.GRAY +
                    "(Combo: RLR, Cost: 15, Cooldown: 8s)",
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "ABILITY 4:",
                ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Teleport " + ChatColor.GRAY +
                    "(Combo: RRR, Cost: 10, Cooldown: 10s)",
                "",
                ChatColor.GOLD + "" + ChatColor.BOLD + "Notice! " + ChatColor.GOLD + "You can switch your class at any time.",
                "",
                ChatColor.WHITE + "" + ChatColor.BOLD + "Click To Begin Your Adventure!"
            )));

        // Rogue (using iron sword as icon)
        inv.setItem(16, createMenuItem(Material.IRON_SWORD, ChatColor.RED + "" + ChatColor.BOLD + "Start As A Rogue!",
            Arrays.asList(
                "",
                ChatColor.GRAY + "Choosing this will start you out as a ",
                ChatColor.RED + "ROGUE" + ChatColor.GRAY + "! Your starting item will be a dagger.",
                "",
                ChatColor.RED + "" + ChatColor.BOLD + "ABILITY 1:",
                ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Execute " + ChatColor.GRAY +
                    "(Combo: RLL, Cost: 12, Cooldown: 3s, Multiplier: 1.3)",
                ChatColor.YELLOW + "" + ChatColor.BOLD + "ABILITY 2:",
                ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Blade Fury " + ChatColor.GRAY +
                    "(Combo: RRL, Cost: 15, Cooldown: 8s, Multiplier: 1.5)",
                ChatColor.BLUE + "" + ChatColor.BOLD + "ABILITY 3:",
                ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Dagger Throw " + ChatColor.GRAY +
                    "(Combo: RLR, Cost: 10, Cooldown: 10s)",
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "ABILITY 4:",
                ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Vanish " + ChatColor.GRAY +
                    "(Combo: RRR, Cost: 8, Cooldown: 15s)",
                "",
                ChatColor.GOLD + "" + ChatColor.BOLD + "Notice! " + ChatColor.GOLD + "You can switch your class at any time.",
                "",
                ChatColor.WHITE + "" + ChatColor.BOLD + "Click To Begin Your Adventure!"
            )));

        // Fill borders with gray stained glass panes
        ItemStack filler = createFillerItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (i < 9 || i >= 18) { // Top and bottom rows
                inv.setItem(i, filler);
            } else if (inv.getItem(i) == null) { // Empty slots in the middle row
                inv.setItem(i, filler);
            }
        }

        return inv;
    }

    private static ItemStack createMenuItem(Material mat, String displayName, java.util.List<String> lore) {
        ItemStack item = new ItemStack(mat, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(lore); // Set abilities as lore
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES); // Hide item attributes like attack damage
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createFillerItem(Material material, String space) {
        ItemStack filler = new ItemStack(material);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" "); // Blank display name
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES); // Hide attributes
            filler.setItemMeta(meta);
        }
        return filler;
    }
}
