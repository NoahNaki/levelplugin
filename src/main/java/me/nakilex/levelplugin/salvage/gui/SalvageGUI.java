
package me.nakilex.levelplugin.salvage.gui;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import me.nakilex.levelplugin.items.data.ItemRarity;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class SalvageGUI {

    private static final int GUI_SIZE = 54;
    private static final String GUI_TITLE = ChatColor.DARK_GREEN + "Salvage Items";

    public static void openMerchantGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);
        ItemStack filler = createFiller();

        // Fill border with filler
        for (int i = 0; i < GUI_SIZE; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                gui.setItem(i, filler);
            }
        }

        // Top-right info icon
        // Top-right info icon
        ItemStack info = getOraxenItem("info", ChatColor.YELLOW + "Information");
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "  Place ꐗ unwanted items into the center.",
                ChatColor.GRAY + "  Only valid custom items can be salvaged.",
                "",
                ChatColor.GREEN + "✔ Confirm Salvage:",
                ChatColor.GRAY + "  Converts all valid items into coins/gems.",
                "",
                ChatColor.RED + "✖ Cancel:",
                ChatColor.GRAY + "  Closes the salvage menu safely.",
                "",
                ChatColor.GOLD + "Quick-Sell Buttons:",
                ChatColor.GRAY + "  Instantly salvage all items of a given rarity",
                ChatColor.GRAY + "  from both the GUI and your inventory."
            ));
            info.setItemMeta(infoMeta);
        }

        gui.setItem(8, info);


        // Bottom-left close button
        gui.setItem(45, getOraxenItem("cross", ChatColor.RED + "Cancel"));

        // Bottom-right confirm button
        gui.setItem(53, getOraxenItem("check", ChatColor.GREEN + "Confirm Salvage"));

        // Center visually for 5 items (slots 46–50)
        ItemRarity[] rarities = {
            ItemRarity.COMMON,
            ItemRarity.UNCOMMON,
            ItemRarity.RARE,
            ItemRarity.EPIC,
            ItemRarity.LEGENDARY
        };

        int logicalStart = 47;

        for (int i = 0; i < rarities.length; i++) {
            int actualSlot = logicalStart + i; // 46–50
            gui.setItem(actualSlot, createRaritySellButton(rarities[i]));
        }

        // Deposit/Return buttons
        gui.setItem(46, getOraxenItem("arrow_down", ChatColor.YELLOW + "Return All"));
        gui.setItem(52, getOraxenItem("arrow_up", ChatColor.YELLOW + "Deposit All"));
        player.openInventory(gui);
    }

    private static ItemStack createFiller() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            glass.setItemMeta(meta);
        }
        return glass;
    }

    private static ItemStack getOraxenItem(String id, String name) {
        ItemBuilder builder = OraxenItems.getItemById(id);
        if (builder == null) return new ItemStack(Material.BARRIER);
        ItemStack item = builder.build();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createRaritySellButton(ItemRarity rarity) {
        Material material;
        switch (rarity) {
            case COMMON: material = Material.LIGHT_GRAY_CONCRETE; break;
            case UNCOMMON: material = Material.LIME_CONCRETE; break;
            case RARE: material = Material.CYAN_CONCRETE; break;
            case EPIC: material = Material.MAGENTA_CONCRETE; break;
            case LEGENDARY: material = Material.ORANGE_CONCRETE; break;
            default: material = Material.BARRIER; break;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String rarityName = rarity.name().charAt(0) + rarity.name().substring(1).toLowerCase();
            meta.setDisplayName(rarity.getColor() + "Salvage " + rarityName + " Items");
            item.setItemMeta(meta);
        }

        return item;
    }
}
