package me.nakilex.levelplugin.dungeon.rift.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dungeon.rift.FrontierRiftDefinition;
import me.nakilex.levelplugin.dungeon.rift.FrontierRiftMutator;
import me.nakilex.levelplugin.dungeon.rift.FrontierRiftState;
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

/** GUI for the frontier rift board. */
public final class FrontierRiftBoard {
    public static final String TITLE = ChatColor.DARK_AQUA + "Frontier Rift Board";
    public static final NamespacedKey KEY_ACTION = new NamespacedKey(Main.getInstance(), "frontier_action");
    public static final String ACTION_START = "start";

    private FrontierRiftBoard() {}

    public static Inventory create(FrontierRiftDefinition next,
                                   FrontierRiftMutator mutator,
                                   FrontierRiftState state) {
        GuiBuilder builder = GuiBuilder.create(54, TITLE)
                .filler(Material.BLUE_STAINED_GLASS_PANE)
                .border();

        builder.setItem(4, mutatorItem(mutator));
        builder.setItem(22, definitionItem(next, state));
        builder.setItem(40, startItem(next));
        return builder.build();
    }

    private static ItemStack mutatorItem(FrontierRiftMutator mutator) {
        ItemStack item = new ItemStack(Material.HEART_OF_THE_SEA);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Daily Mutator: " + mutator.getDisplayName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + mutator.getDescription());
            lore.add(" ");
            lore.add(ChatColor.YELLOW + "Reward Multiplier: "
                    + String.format("%.0f%%", mutator.getRewardMultiplier() * 100));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack definitionItem(FrontierRiftDefinition def, FrontierRiftState state) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Stage " + def.getStage() + ": " + def.getDisplayName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + def.getDescription());
            lore.add(" ");
            lore.add(ChatColor.GRAY + "Recommended: " + def.getRecommendedPower());
            lore.add(ChatColor.GRAY + "Time Limit: " + def.getTimeLimitMinutes() + "m");
            lore.add(" ");
            lore.add(ChatColor.WHITE + "Current Stage: " + ChatColor.GREEN + state.getCurrentStage());
            lore.add(ChatColor.WHITE + "Best Stage: " + ChatColor.GOLD + state.getBestStage());
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack startItem(FrontierRiftDefinition def) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Launch Stage " + def.getStage());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + def.getDescription());
            lore.add(" ");
            lore.add(ChatColor.YELLOW + "Guild Coin Reward: " + def.getBaseGuildCoins());
            lore.add(ChatColor.AQUA + "Guild XP Reward: " + def.getBaseGuildExp());
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to begin the expedition", null));
            meta.setLore(lore);
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(KEY_ACTION, PersistentDataType.STRING, ACTION_START);
            item.setItemMeta(meta);
        }
        return item;
    }
}

