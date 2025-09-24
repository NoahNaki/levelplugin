package me.nakilex.levelplugin.battlepass;

import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Describes a single battle pass reward. Handles icon rendering and
 * executing the reward logic when claimed.
 */
public final class BattlePassReward {

    @FunctionalInterface
    public interface RewardAction {
        void grant(Player player, BattlePassManager manager);
    }

    private final String displayName;
    private final List<String> baseLore;
    private final Supplier<ItemStack> iconSupplier;
    private final RewardAction action;

    public BattlePassReward(String displayName,
                             List<String> baseLore,
                             Supplier<ItemStack> iconSupplier,
                             RewardAction action) {
        this.displayName = displayName;
        this.baseLore = baseLore != null ? List.copyOf(baseLore) : List.of();
        this.iconSupplier = iconSupplier != null ? iconSupplier : () -> new ItemStack(Material.BARRIER);
        this.action = action;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getBaseLore() {
        return baseLore;
    }

    public void grant(Player player, BattlePassManager manager) {
        if (action != null) {
            action.grant(player, manager);
        }
    }

    /**
     * Build a clickable icon for the GUI with contextual lore.
     */
    public ItemStack createDisplay(Player player,
                                   BattlePassManager manager,
                                   BattlePassProgress progress,
                                   int tierIndex,
                                   boolean premiumTrack,
                                   boolean tierUnlocked,
                                   boolean premiumUnlocked,
                                   boolean claimed) {
        ItemStack base = iconSupplier.get();
        if (base == null || base.getType() == Material.AIR) {
            base = new ItemStack(Material.BARRIER);
        }
        ItemStack item = base.clone();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String title = displayName;
        if (claimed) {
            title += ChatColor.DARK_GRAY + " (Claimed)";
        } else if (!tierUnlocked) {
            title += ChatColor.DARK_GRAY + " (Locked)";
        } else if (premiumTrack && !premiumUnlocked) {
            title += ChatColor.DARK_GRAY + " (Premium)";
        }

        meta.setDisplayName(title);
        List<String> lore = new ArrayList<>();
        lore.add(premiumTrack ? ChatColor.LIGHT_PURPLE + "Premium Reward" : ChatColor.GREEN + "Free Reward");
        lore.add(" ");
        lore.addAll(baseLore);
        lore.add(" ");

        if (claimed) {
            lore.add(ChatColor.GREEN + "Already claimed");
        } else if (!tierUnlocked) {
            int needed = manager.getXpPerTier() * tierIndex;
            int have = progress.getXp();
            lore.add(ChatColor.RED + "Reach Tier " + tierIndex + " to claim.");
            int baseXp = manager.getXpPerTier() * (tierIndex - 1);
            int progressInTier = Math.max(0, Math.min(manager.getXpPerTier(), have - baseXp));
            double ratio = manager.getXpPerTier() == 0 ? 0.0 : (double) progressInTier / manager.getXpPerTier();
            lore.add(ChatColor.GRAY + "Progress:" + ChatColor.WHITE + " " + progressInTier + "/" + manager.getXpPerTier());
            lore.add(ChatColor.DARK_GRAY + GuiUtilHolder.progressBar(ratio));
        } else if (premiumTrack && !premiumUnlocked) {
            lore.add(ChatColor.RED + "Unlock the premium track to claim.");
        } else {
            lore.add(ChatColor.YELLOW + "Ready to claim!");
            lore.addAll(TooltipUtil.clickInstructions("to claim", null));
        }

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    /** Simple holder to avoid direct dependency cycle on GuiUtil. */
    private static final class GuiUtilHolder {
        private GuiUtilHolder() {}
        private static String progressBar(double progress) {
            return me.nakilex.levelplugin.utils.GuiUtil.createProgressBar(progress, 20, ChatColor.GREEN, ChatColor.DARK_GRAY, "|");
        }
    }
}
