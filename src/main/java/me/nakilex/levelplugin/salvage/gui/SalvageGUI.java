
package me.nakilex.levelplugin.salvage.gui;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
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
        ItemStack info = getNexoItem("info", ChatColor.YELLOW + "Information");
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
                ChatColor.GOLD + "Deposit Buttons:",
                ChatColor.GRAY + "  Move all items of a chosen rarity",
                ChatColor.GRAY + "  from your inventory into this menu."
            ));
            info.setItemMeta(infoMeta);
        }

        gui.setItem(8, info);


        // Bottom-left close button
        gui.setItem(45, getNexoItem("cross", ChatColor.RED + "Cancel"));

        // Bottom-right confirm button
        gui.setItem(53, getNexoItem("check", ChatColor.GREEN + "Confirm Salvage"));

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
            gui.setItem(actualSlot, createRarityDepositButton(rarities[i]));
        }

        // Deposit/Return buttons
        gui.setItem(46, getNexoItem("arrow_down", ChatColor.YELLOW + "Return All"));
        gui.setItem(52, getNexoItem("arrow_up", ChatColor.YELLOW + "Deposit All"));
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

    private static ItemStack getNexoItem(String id, String name) {
        ItemBuilder builder = NexoItems.itemFromId(id);
        if (builder == null) return new ItemStack(Material.BARRIER);
        ItemStack item = builder.build();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createRarityDepositButton(ItemRarity rarity) {
        String id;
        switch (rarity) {
            case COMMON: id = "arrow_common"; break;
            case UNCOMMON: id = "arrow_uncommon"; break;
            case RARE: id = "arrow_rare"; break;
            case EPIC: id = "arrow_epic"; break;
            case LEGENDARY: id = "arrow_legendary"; break;
            default: id = "arrow_common"; break;
        }

        String rarityName = rarity.name().charAt(0) + rarity.name().substring(1).toLowerCase();
        return getNexoItem(id, rarity.getColor() + "Deposit " + rarityName + " Items");
    }
}
