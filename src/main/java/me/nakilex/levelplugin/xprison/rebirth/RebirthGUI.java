package me.nakilex.levelplugin.xprison.rebirth;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Inventory UI for previewing and confirming an X-Prison rebirth. */
public final class RebirthGUI implements Listener {

    private static final String TITLE = "Rebirth";
    private static final String CONFIRM_TITLE = "Confirm Rebirth";
    private static final int REBIRTH_SLOT = 31;
    private static final int CLOSE_SLOT = 40;
    private static final int CONFIRM_SLOT = 11;
    private static final int CANCEL_SLOT = 15;

    private final Main plugin;
    private final XPrisonRebirthManager manager;

    public RebirthGUI(Main plugin, XPrisonRebirthManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void open(Player player) {
        XPrisonRebirthManager.RebirthPreview preview = manager.preview(player);
        Inventory inventory = GuiBuilder.create(45, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border()
                .build();

        inventory.setItem(4, GuiUtil.createGuiItem(Material.NETHER_STAR,
                ChatColor.GOLD + "Rebirth " + preview.rebirths(), List.of(
                        ChatColor.GRAY + "Permanent progression that makes each",
                        ChatColor.GRAY + "new mining run stronger than the last.",
                        "",
                        ChatColor.GRAY + "Next requirement: " + ChatColor.YELLOW + "Level " + preview.requiredLevel(),
                        ChatColor.GRAY + "Current pickaxe: " + levelColor(preview) + "Level " + preview.currentLevel())));

        List<String> progressLore = new ArrayList<>();
        progressLore.add(ChatColor.GRAY + "Pickaxe Level: " + ChatColor.WHITE + preview.currentLevel()
                + ChatColor.DARK_GRAY + "/" + preview.requiredLevel());
        progressLore.add(TooltipUtil.progressBar(preview.currentLevel(), preview.requiredLevel(), 20));
        progressLore.add("");
        progressLore.add(ChatColor.GRAY + "Rebirths: " + ChatColor.LIGHT_PURPLE + preview.rebirths());
        progressLore.add(ChatColor.GRAY + "Money multiplier: " + ChatColor.GREEN + formatMultiplier(preview.currentMultiplier()));
        progressLore.add(ChatColor.GRAY + "Token multiplier: " + ChatColor.AQUA + formatMultiplier(preview.currentMultiplier()));
        inventory.setItem(20, GuiUtil.createGuiItem(Material.EXPERIENCE_BOTTLE,
                ChatColor.YELLOW + "Current Run", progressLore));

        inventory.setItem(22, GuiUtil.createGuiItem(Material.EMERALD,
                ChatColor.GREEN + "Rebirth Reward", List.of(
                        ChatColor.GRAY + "Projected reward: " + ChatColor.AQUA + "+" + preview.gemReward() + " Gems",
                        "",
                        ChatColor.GRAY + "Mining money earned: " + ChatColor.WHITE + compact(preview.runMoneyEarned()),
                        ChatColor.GRAY + "Mining tokens earned: " + ChatColor.WHITE + compact(preview.runTokensEarned()),
                        ChatColor.GRAY + "Extra levels: " + ChatColor.WHITE
                                + Math.max(0, preview.currentLevel() - preview.requiredLevel()),
                        "",
                        ChatColor.DARK_GRAY + "Only mining-generated income counts toward",
                        ChatColor.DARK_GRAY + "the Gem reward; transfers/admin grants do not.")));

        inventory.setItem(24, GuiUtil.createGuiItem(Material.GOLD_INGOT,
                ChatColor.GOLD + "After Rebirth", List.of(
                        ChatColor.RED + "Reset",
                        ChatColor.GRAY + "Pickaxe Level -> 1",
                        ChatColor.GRAY + "Pickaxe XP -> 0",
                        ChatColor.GRAY + "Money -> 0",
                        ChatColor.GRAY + "Tokens -> 0",
                        "",
                        ChatColor.GREEN + "Keep",
                        ChatColor.GRAY + "Gems",
                        ChatColor.GRAY + "Pickaxe enchants/upgrades",
                        "",
                        ChatColor.LIGHT_PURPLE + "Permanent Bonus",
                        ChatColor.GRAY + "Money: " + ChatColor.WHITE + formatMultiplier(preview.currentMultiplier())
                                + ChatColor.DARK_GRAY + " -> " + ChatColor.GREEN + formatMultiplier(preview.nextMultiplier()),
                        ChatColor.GRAY + "Tokens: " + ChatColor.WHITE + formatMultiplier(preview.currentMultiplier())
                                + ChatColor.DARK_GRAY + " -> " + ChatColor.AQUA + formatMultiplier(preview.nextMultiplier()))));

        inventory.setItem(REBIRTH_SLOT, rebirthButton(preview));
        inventory.setItem(CLOSE_SLOT, GuiUtil.createGuiItem(Material.BARRIER,
                ChatColor.RED + "Close", List.of(TooltipUtil.leftClickLine("to close"))));
        player.openInventory(inventory);
    }

    private ItemStack rebirthButton(XPrisonRebirthManager.RebirthPreview preview) {
        List<String> lore = new ArrayList<>();
        if (preview.eligible()) {
            lore.add(ChatColor.GRAY + "Rebirth now for " + ChatColor.AQUA + "+" + preview.gemReward() + " Gems" + ChatColor.GRAY + ".");
            lore.add(ChatColor.GRAY + "Your pickaxe enchants and Gems are permanent.");
            lore.add("");
            lore.add(TooltipUtil.leftClickLine(ChatColor.GREEN, "to review the reset"));
            return GuiUtil.createGuiItem(Material.TOTEM_OF_UNDYING, ChatColor.GREEN + "Rebirth Available!", lore);
        }

        lore.add(ChatColor.GRAY + (preview.blocker() == null ? "You are not ready to rebirth." : preview.blocker()));
        lore.add("");
        lore.add(ChatColor.GRAY + "Required: " + ChatColor.YELLOW + "Level " + preview.requiredLevel());
        lore.add(ChatColor.GRAY + "Current: " + ChatColor.WHITE + "Level " + preview.currentLevel());
        return GuiUtil.createGuiItem(Material.BARRIER, ChatColor.RED + "Rebirth Locked", lore);
    }

    private void openConfirmation(Player player) {
        XPrisonRebirthManager.RebirthPreview preview = manager.preview(player);
        if (!preview.eligible()) {
            open(player);
            return;
        }

        Inventory inventory = GuiBuilder.create(27, CONFIRM_TITLE)
                .filler(Material.BLACK_STAINED_GLASS_PANE)
                .border()
                .build();

        inventory.setItem(4, GuiUtil.createGuiItem(Material.NETHER_STAR,
                ChatColor.YELLOW + "Confirm Rebirth " + (preview.rebirths() + 1), List.of(
                        ChatColor.GRAY + "You will receive " + ChatColor.AQUA + "+" + preview.gemReward() + " Gems",
                        ChatColor.GRAY + "and unlock a permanent " + ChatColor.GREEN
                                + formatMultiplier(preview.nextMultiplier()) + ChatColor.GRAY + " mining multiplier.",
                        "",
                        ChatColor.RED + "Money, Tokens, Pickaxe Level and XP reset.")));

        inventory.setItem(CONFIRM_SLOT, GuiUtil.createGuiItem(Material.LIME_CONCRETE,
                ChatColor.GREEN + "Confirm Rebirth", List.of(
                        ChatColor.GRAY + "This reset cannot be undone.",
                        ChatColor.GRAY + "Gems and pickaxe enchants are kept.",
                        "",
                        TooltipUtil.leftClickLine(ChatColor.GREEN, "to rebirth"))));
        inventory.setItem(CANCEL_SLOT, GuiUtil.createGuiItem(Material.RED_CONCRETE,
                ChatColor.RED + "Cancel", List.of(TooltipUtil.leftClickLine("to go back"))));
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!TITLE.equals(title) && !CONFIRM_TITLE.equals(title)) return;
        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getRawSlot() < 0) return;

        if (TITLE.equals(title)) {
            if (event.getRawSlot() == REBIRTH_SLOT) {
                if (manager.preview(player).eligible()) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.1F);
                    openConfirmation(player);
                }
            } else if (event.getRawSlot() == CLOSE_SLOT) {
                player.closeInventory();
            }
            return;
        }

        if (event.getRawSlot() == CANCEL_SLOT) {
            open(player);
            return;
        }
        if (event.getRawSlot() != CONFIRM_SLOT) return;

        player.closeInventory();
        XPrisonRebirthManager.RebirthResult result = manager.rebirth(player);
        if (!result.success()) {
            player.sendMessage(ChatColor.RED + "REBIRTH >> " + ChatColor.GRAY + result.message());
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            Bukkit.getScheduler().runTask(plugin, () -> open(player));
            return;
        }

        player.sendMessage(ChatColor.LIGHT_PURPLE + "REBIRTH " + ChatColor.DARK_GRAY + "» "
                + ChatColor.WHITE + "You are now " + ChatColor.LIGHT_PURPLE + "Rebirth " + result.newRebirths()
                + ChatColor.WHITE + " and received " + ChatColor.AQUA + result.gemsAwarded() + " Gems" + ChatColor.WHITE + ".");
        player.sendMessage(ChatColor.DARK_GRAY + "» " + ChatColor.GRAY + "Money/Tokens multiplier: "
                + ChatColor.GREEN + formatMultiplier(result.newMultiplier()));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
        Bukkit.getScheduler().runTaskLater(plugin, () -> open(player), 2L);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (TITLE.equals(title) || CONFIRM_TITLE.equals(title)) {
            event.setCancelled(true);
        }
    }

    private static ChatColor levelColor(XPrisonRebirthManager.RebirthPreview preview) {
        return preview.currentLevel() >= preview.requiredLevel() ? ChatColor.GREEN : ChatColor.RED;
    }

    private static String formatMultiplier(double multiplier) {
        return BigDecimal.valueOf(multiplier).stripTrailingZeros().toPlainString() + "x";
    }

    private static String compact(BigDecimal value) {
        if (value == null) return "0";
        BigDecimal abs = value.abs();
        String[] suffixes = {"", "K", "M", "B", "T", "Q"};
        BigDecimal thousand = BigDecimal.valueOf(1000);
        int index = 0;
        while (abs.compareTo(thousand) >= 0 && index < suffixes.length - 1) {
            value = value.divide(thousand, 2, RoundingMode.HALF_UP);
            abs = abs.divide(thousand, 2, RoundingMode.HALF_UP);
            index++;
        }
        return value.stripTrailingZeros().toPlainString() + suffixes[index];
    }
}
