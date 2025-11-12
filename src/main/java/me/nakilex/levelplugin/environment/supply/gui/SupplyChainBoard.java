package me.nakilex.levelplugin.environment.supply.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.environment.supply.SupplyChainDefinition;
import me.nakilex.levelplugin.environment.supply.SupplyChainStage;
import me.nakilex.levelplugin.environment.supply.SupplyChainState;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** GUI for supply chain management. */
public final class SupplyChainBoard {
    public static final String TITLE = ChatColor.DARK_GREEN + "Supply Chain Log";
    public static final NamespacedKey KEY_ACTION = new NamespacedKey(Main.getInstance(), "supply_action");
    public static final String ACTION_DEPOSIT = "deposit";

    private SupplyChainBoard() {}

    public static Inventory create(SupplyChainDefinition def,
                                   SupplyChainStage stage,
                                   SupplyChainState state) {
        GuiBuilder builder = GuiBuilder.create(54, TITLE)
                .filler(Material.GREEN_STAINED_GLASS_PANE)
                .border();

        builder.setItem(4, definitionItem(def));
        builder.setItem(22, stageItem(stage, state));
        builder.setItem(40, depositItem());
        return builder.build();
    }

    private static ItemStack definitionItem(SupplyChainDefinition def) {
        ItemStack stack = new ItemStack(Material.BOOK);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + def.getDisplayName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + def.getDescription());
            lore.add(" ");
            lore.add(ChatColor.GRAY + "Requires " + ChatColor.YELLOW + def.getRequiredBuilding()
                    + ChatColor.GRAY + " stage " + ChatColor.YELLOW + def.getRequiredStage());
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static ItemStack stageItem(SupplyChainStage stage, SupplyChainState state) {
        ItemStack stack = new ItemStack(Material.CRAFTING_TABLE);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + stage.getName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Production Time: " + ChatColor.YELLOW + stage.getProductionSeconds() / 60 + "m");
            lore.add(" ");
            for (Map.Entry<Material, Integer> req : stage.getRequirements().entrySet()) {
                int delivered = state.getContributions().getOrDefault(req.getKey().name(), 0);
                String bar = TooltipUtil.progressBar(delivered, req.getValue(), 20);
                lore.add(ChatColor.WHITE + req.getKey().name().toLowerCase() + ChatColor.GRAY + ": "
                        + ChatColor.GREEN + delivered + ChatColor.GRAY + "/" + ChatColor.YELLOW + req.getValue());
                lore.add(ChatColor.DARK_GRAY + bar);
            }
            lore.add(" ");
            lore.add(ChatColor.YELLOW + "+" + stage.getGuildCoins() + " Guild Coins");
            lore.add(ChatColor.AQUA + "+" + stage.getGuildExp() + " Guild XP");
            if (!stage.getRewardDescription().isEmpty()) {
                lore.add(" ");
                lore.add(ChatColor.GRAY + stage.getRewardDescription());
            }
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static ItemStack depositItem() {
        ItemStack stack = new ItemStack(Material.CHEST);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Deposit Materials");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Hold a required item in your hand");
            lore.add(ChatColor.GRAY + "and click to contribute it.");
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to deposit from your hand", null));
            meta.setLore(lore);
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(KEY_ACTION, PersistentDataType.STRING, ACTION_DEPOSIT);
            stack.setItemMeta(meta);
        }
        return stack;
    }
}

