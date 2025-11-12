package me.nakilex.levelplugin.dungeon.trial.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dungeon.trial.ArcaneTrialDefinition;
import me.nakilex.levelplugin.dungeon.trial.ArcaneTrialState;
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

/** GUI display for arcane trials. */
public final class ArcaneTrialBoard {
    public static final String TITLE = ChatColor.DARK_PURPLE + "Arcane Trials";
    public static final NamespacedKey KEY_ACTION = new NamespacedKey(Main.getInstance(), "trial_action");
    public static final String ACTION_START = "start";
    public static final String ACTION_PRESTIGE = "prestige";

    private ArcaneTrialBoard() {}

    public static Inventory create(ArcaneTrialState state,
                                   ArcaneTrialDefinition next,
                                   Map<Integer, ArcaneTrialDefinition> defs) {
        GuiBuilder builder = GuiBuilder.create(54, TITLE)
                .filler(Material.PURPLE_STAINED_GLASS_PANE)
                .border();

        builder.setItem(4, statsItem(state));
        if (next != null) {
            builder.setItem(22, nextTrialItem(next));
        }
        builder.setItem(40, prestigeItem());
        return builder.build();
    }

    private static ItemStack statsItem(ArcaneTrialState state) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Trial Progress");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Highest Tier: " + ChatColor.GOLD + state.getHighestTier());
            lore.add(ChatColor.GRAY + "Arcane Marks: " + ChatColor.AQUA + state.getMarks());
            lore.add(ChatColor.GRAY + "Prestige: " + ChatColor.DARK_PURPLE + state.getPrestige());
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack nextTrialItem(ArcaneTrialDefinition def) {
        ItemStack item = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Tier " + def.getTier() + ": " + def.getDisplayName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + def.getDescription());
            lore.add(" ");
            lore.add(ChatColor.GRAY + "Recommended Level: " + ChatColor.YELLOW + def.getRecommendedLevel());
            lore.add(ChatColor.GRAY + "Time Limit: " + ChatColor.YELLOW + def.getTimeLimitMinutes() + "m");
            lore.add(" ");
            lore.add(ChatColor.AQUA + "Marks Reward: " + def.getMarkReward());
            lore.add(ChatColor.GRAY + "Battle Pass: " + def.getBattlePassProgress());
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to start this trial", null));
            meta.setLore(lore);
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(KEY_ACTION, PersistentDataType.STRING, ACTION_START + ":" + def.getTier());
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack prestigeItem() {
        ItemStack item = new ItemStack(Material.END_CRYSTAL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Prestige");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Spend accumulated marks to reset the ladder");
            lore.add(ChatColor.GRAY + "and gain a permanent arcane bonus.");
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to prestige if you have enough marks", null));
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, ACTION_PRESTIGE);
            item.setItemMeta(meta);
        }
        return item;
    }
}

