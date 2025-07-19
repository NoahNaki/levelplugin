
package me.nakilex.levelplugin.salvage.gui;

import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.utils.GuiUtil;
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
    private static final String GUI_TITLE = ChatColor.BLACK + "Salvage Items";

    public static void openMerchantGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);
        ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < GUI_SIZE; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                gui.setItem(i, filler);
            }
        }

        ItemStack info = GuiUtil.getNexoItem("info", ChatColor.YELLOW + "Information");
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
        gui.setItem(45, GuiUtil.getNexoItem("cross", ChatColor.RED + "Cancel"));
        gui.setItem(53, GuiUtil.getNexoItem("check", ChatColor.GREEN + "Confirm Salvage"));
        ItemRarity[] rarities = {
            ItemRarity.COMMON,
            ItemRarity.UNCOMMON,
            ItemRarity.RARE,
            ItemRarity.EPIC,
            ItemRarity.LEGENDARY
        };

        int logicalStart = 47;

        for (int i = 0; i < rarities.length; i++) {
            int actualSlot = logicalStart + i;
            gui.setItem(actualSlot, createRarityDepositButton(rarities[i]));
        }
        gui.setItem(46, GuiUtil.getNexoItem("arrow_down", ChatColor.YELLOW + "Return All"));
        gui.setItem(52, GuiUtil.getNexoItem("arrow_up", ChatColor.YELLOW + "Deposit All"));
        player.openInventory(gui);
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
        return GuiUtil.getNexoItem(id, rarity.getColor() + "Deposit " + rarityName + " Items");
    }
}
