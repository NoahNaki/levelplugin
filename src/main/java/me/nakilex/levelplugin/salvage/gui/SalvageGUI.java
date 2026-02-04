
package me.nakilex.levelplugin.salvage.gui;

import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.salvage.managers.SalvageManager;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import me.nakilex.levelplugin.utils.gui.widgets.SlotWidget;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class SalvageGUI {

    private static final int GUI_SIZE = 54;
    private static final String GUI_TITLE = "Salvage Items";
    public static final int TOGGLE_SLOT = 9;
    private static final int INFO_SLOT = 8;
    private static final int CANCEL_SLOT = 45;
    private static final int CONFIRM_SLOT = 53;
    private static final int RETURN_ALL_SLOT = 46;
    private static final int DEPOSIT_ALL_SLOT = 52;
    private static final int RARITY_START_SLOT = 47;

    public static void openMerchantGUI(Player player) {
        Inventory gui = GuiBuilder.create(GUI_SIZE, GUI_TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .fillEmptySlots(false)
                .border()
                .build();

        renderWidgets(gui, player);
        player.openInventory(gui);
    }


    public static ItemStack createLowerToggle(boolean enabled) {
        return GuiUtil.createToggleItem(enabled,
                ChatColor.YELLOW + "Include Lower Rarities",
                ChatColor.GRAY + "Deposit buttons also move",
                ChatColor.GRAY + "lower rarity gear when enabled.");
    }

    public static ItemStack createRarityDepositButton(ItemRarity rarity) {
        String rarityName = rarity.name().charAt(0) + rarity.name().substring(1).toLowerCase();
        return GuiUtil.getRarityArrowItem(rarity, rarity.getColor() + "Deposit " + rarityName + " Items");
    }

    public static boolean isInputSlot(int slot) {
        return slot >= 0 && slot < GUI_SIZE && !(slot < 9 || slot >= 45 || slot % 9 == 0 || slot % 9 == 8);
    }

    private static void renderWidgets(Inventory inventory, Player player) {
        GuiLayout layout = new GuiLayout(inventory);
        GuiContext context = new GuiContext(player, inventory);
        for (GuiWidget widget : buildWidgets(player)) {
            widget.contribute(layout, context);
        }
    }

    private static List<GuiWidget> buildWidgets(Player player) {
        List<GuiWidget> widgets = new java.util.ArrayList<>();
        widgets.add(new InfoWidget());
        widgets.add(new ToggleWidget());
        widgets.add(new CancelWidget());
        widgets.add(new ConfirmWidget());
        widgets.add(new ReturnAllWidget());
        widgets.add(new DepositAllWidget());
        ItemRarity[] rarities = {
                ItemRarity.COMMON,
                ItemRarity.UNCOMMON,
                ItemRarity.RARE,
                ItemRarity.EPIC,
                ItemRarity.LEGENDARY
        };
        for (int i = 0; i < rarities.length; i++) {
            widgets.add(new RarityDepositWidget(RARITY_START_SLOT + i, rarities[i]));
        }
        return widgets;
    }

    private static class InfoWidget extends SlotWidget {
        private InfoWidget() {
            super(INFO_SLOT);
        }

        @Override
        protected ItemStack render(GuiContext context) {
            ItemStack info = GuiUtil.getNexoItem("info", ChatColor.YELLOW + "Information");
            ItemMeta infoMeta = info.getItemMeta();
            if (infoMeta != null) {
                infoMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "  Place ꐗ unwanted items into the center.",
                    ChatColor.GRAY + "  Custom items, tools, potions, and essences can be salvaged.",
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
            return info;
        }
    }

    private static class ToggleWidget extends SlotWidget {
        private ToggleWidget() {
            super(TOGGLE_SLOT);
        }

        @Override
        protected ItemStack render(GuiContext context) {
            boolean includeLower = SalvageManager.getInstance().isIncludingLower(context.player().getUniqueId());
            return createLowerToggle(includeLower);
        }
    }

    private static class CancelWidget extends SlotWidget {
        private CancelWidget() {
            super(CANCEL_SLOT);
        }

        @Override
        protected ItemStack render(GuiContext context) {
            return GuiUtil.getNexoItem("cross", ChatColor.RED + "Cancel");
        }
    }

    private static class ConfirmWidget extends SlotWidget {
        private ConfirmWidget() {
            super(CONFIRM_SLOT);
        }

        @Override
        protected ItemStack render(GuiContext context) {
            return GuiUtil.getNexoItem("check", ChatColor.GREEN + "Confirm Salvage");
        }
    }

    private static class ReturnAllWidget extends SlotWidget {
        private ReturnAllWidget() {
            super(RETURN_ALL_SLOT);
        }

        @Override
        protected ItemStack render(GuiContext context) {
            return GuiUtil.getNexoItem("arrow_down", ChatColor.YELLOW + "Return All");
        }
    }

    private static class DepositAllWidget extends SlotWidget {
        private DepositAllWidget() {
            super(DEPOSIT_ALL_SLOT);
        }

        @Override
        protected ItemStack render(GuiContext context) {
            return GuiUtil.getNexoItem("arrow_up", ChatColor.YELLOW + "Deposit All");
        }
    }

    private static class RarityDepositWidget extends SlotWidget {
        private final ItemRarity rarity;

        private RarityDepositWidget(int slot, ItemRarity rarity) {
            super(slot);
            this.rarity = rarity;
        }

        @Override
        protected ItemStack render(GuiContext context) {
            return createRarityDepositButton(rarity);
        }
    }
}
