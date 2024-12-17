package me.nakilex.levelplugin.ui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.ChatPaginator;

import java.util.Arrays;
import java.util.Collections;

public class ClassMenu {

    public static Inventory getClassSelectionMenu() {
        // Create an inventory with 27 slots
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_GREEN + "Choose Your Class");

        // Add class selection items with lore
        inv.setItem(10, createMenuItem(Material.WOODEN_SHOVEL, ChatColor.GREEN + "" + ChatColor.BOLD + "Start As A Warrior!",
            Arrays.asList(
                "",
                ChatColor.GRAY + "Choosing this will start you out as a ",
                ChatColor.GREEN + "WARRIOR" + ChatColor.GRAY + "! Your starting item will be a sword.",
                "",
                ChatColor.RED + "" + ChatColor.BOLD + "ABILITY 1:",
                ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Taunt " + ChatColor.GRAY + "Force enemies to focus their attention on you,",
                ChatColor.GRAY + "drawing their attacks away from your allies.",
                ChatColor.YELLOW + "" + ChatColor.BOLD + "ABILITY 2:",
                ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Berserk " + ChatColor.GRAY + "Enter a frenzied state, enhancing your attack",
                ChatColor.GRAY + "speed and damage output for a limited time.",
                ChatColor.BLUE + "" + ChatColor.BOLD + "ABILITY 3:",
                ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Shield Bash " + ChatColor.GRAY + "Stun and knock back enemies with a forceful",
                ChatColor.GRAY + "shield bash, creating space in the heat of battle.",
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "ABILITY 4:",
                ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Piercing Strike " + ChatColor.GRAY + "Unleash a powerful, armor-piercing blow,",
                ChatColor.GRAY + "dealing massive damage to a single target.",
                "",
                ChatColor.GOLD + "" + ChatColor.BOLD + "Notice! " + ChatColor.GOLD + "You can switch your class at any time.",
                "",
                ChatColor.WHITE + "" + ChatColor.BOLD + "Click To Begin Your Adventure!"
            )));
        inv.setItem(12, createMenuItem(Material.BOW, ChatColor.YELLOW + "" + ChatColor.BOLD + "Start As An Archer!",
            Arrays.asList(
                "",
                ChatColor.GRAY + "Choosing this will start you out as an ",
                ChatColor.YELLOW + "ARCHER" + ChatColor.GRAY + "! Your starting item will be a bow.",
                "",
                ChatColor.RED + "" + ChatColor.BOLD + "ABILITY 1:",
                ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Volley " + ChatColor.GRAY + "Rain a volley of arrows on a designated area,",
                ChatColor.GRAY + "hitting multiple targets with a spread of projectiles.",
                ChatColor.YELLOW + "" + ChatColor.BOLD + "ABILITY 2:",
                ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Hawk's Eye " + ChatColor.GRAY + "Enhance your vision, allowing you to spot",
                ChatColor.GRAY + "hidden enemies and weak points in their armor.",
                ChatColor.BLUE + "" + ChatColor.BOLD + "ABILITY 3:",
                ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Camouflage " + ChatColor.GRAY + "Blend into the surroundings, becoming",
                ChatColor.GRAY + "temporarily invisible to enemies.",
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "ABILITY 4:",
                ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Precision Shot " + ChatColor.GRAY + "Take careful aim and unleash a highly",
                ChatColor.GRAY + "accurate shot, dealing critical damage.",
                "",
                ChatColor.GOLD + "" + ChatColor.BOLD + "Notice! " + ChatColor.GOLD + "You can switch your class at any time.",
                "",
                ChatColor.WHITE + "" + ChatColor.BOLD + "Click To Begin Your Adventure!"
            )));
        inv.setItem(14, createMenuItem(Material.STICK, ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Start As A Mage!",
            Arrays.asList(
                "",
                ChatColor.GRAY + "Choosing this will start you out as a ",
                ChatColor.LIGHT_PURPLE + "MAGE" + ChatColor.GRAY + "! Your starting item will be a magical staff.",
                "",
                ChatColor.RED + "" + ChatColor.BOLD + "ABILITY 1:",
                ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Fireball Barrage " + ChatColor.GRAY + "Unleash a rapid series of fireballs,",
                ChatColor.GRAY + "scorching enemies in your path.",
                ChatColor.YELLOW + "" + ChatColor.BOLD + "ABILITY 2:",
                ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Black Hole " + ChatColor.GRAY + "Create a gravitational well that pulls in",
                ChatColor.GRAY + "enemies, dealing damage and disrupting their positions.",
                ChatColor.BLUE + "" + ChatColor.BOLD + "ABILITY 3:",
                ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Teleportation " + ChatColor.GRAY + "Blink to a nearby location, evading danger",
                ChatColor.GRAY + "or repositioning strategically.",
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "ABILITY 4:",
                ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Thunderstorm " + ChatColor.GRAY + "Call down a powerful lightning storm,",
                ChatColor.GRAY + "dealing area-of-effect damage to all nearby foes.",
                "",
                ChatColor.GOLD + "" + ChatColor.BOLD + "Notice! " + ChatColor.GOLD + "You can switch your class at any time.",
                "",
                ChatColor.WHITE + "" + ChatColor.BOLD + "Click To Begin Your Adventure!"
            )));
        inv.setItem(16, createMenuItem(Material.IRON_SWORD, ChatColor.RED + "" + ChatColor.BOLD + "Start As A Rogue!",
            Arrays.asList(
                "",
                ChatColor.GRAY + "Choosing this will start you out as a ",
                ChatColor.RED + "ROGUE" + ChatColor.GRAY + "! Your starting item will be a dagger.",
                "",
                ChatColor.RED + "" + ChatColor.BOLD + "ABILITY 1:",
                ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Backstab " + ChatColor.GRAY + "Deliver a devastating strike from behind,",
                ChatColor.GRAY + "dealing bonus critical damage.",
                ChatColor.YELLOW + "" + ChatColor.BOLD + "ABILITY 2:",
                ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Shadow Dash " + ChatColor.GRAY + "Dash forward at incredible speed,",
                ChatColor.GRAY + "closing the distance to your target instantly.",
                ChatColor.BLUE + "" + ChatColor.BOLD + "ABILITY 3:",
                ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Vanish " + ChatColor.GRAY + "Disappear into the shadows, becoming",
                ChatColor.GRAY + "invisible to enemies for a short time.",
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "ABILITY 4:",
                ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Poisoned Blade " + ChatColor.GRAY + "Coat your weapon with poison,",
                ChatColor.GRAY + "dealing damage over time to struck enemies.",
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
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" "); // Blank display name
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES); // Hide attributes
            filler.setItemMeta(meta);
        }
        return filler;
    }
}
