package me.nakilex.levelplugin.environment;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.economy.managers.CoinDropManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PalaceGUI implements CommandExecutor, Listener {
    private static final String TITLE = "Palace";
    private static final int INFO_SLOT = 13;
    private static final int CLAIM_SLOT = 31;
    private static final int MAX_STORAGE_MINUTES = 8 * 60;
    private static final int BASE_STORAGE_MINUTES = 210;
    private static final int STORAGE_PER_LEVEL_MINUTES = 30;

    private final Main plugin;
    private final EnvironmentAreaInstanceManager areaManager;

    public PalaceGUI(Main plugin, EnvironmentAreaInstanceManager areaManager) {
        this.plugin = plugin;
        this.areaManager = areaManager;
        plugin.getCommand("palace").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        open(player);
        return true;
    }

    private void open(Player player) {
        Inventory inv = GuiBuilder.create(45, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border()
                .build();
        render(player, inv);
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!GuiUtil.titleMatches(event.getView().getTitle(), TITLE)) return;
        event.setCancelled(true);
        if (event.getRawSlot() == CLAIM_SLOT) {
            claim(player);
            render(player, event.getView().getTopInventory());
        }
    }

    private void render(Player player, Inventory inv) {
        int level = Math.max(0, areaManager.getPalaceBuildingLevel(player));
        RewardState state = computeState(player, level);

        ItemStack info = new ItemStack(Material.STONE_BRICKS);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(ChatColor.GOLD + "Palace Level " + level);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Passive generation for Coins and XP.");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Production Rates");
            lore.add(ChatColor.GRAY + "• Coins/min: " + ChatColor.GOLD + state.coinsPerMinute + " <glyph:coins_icon>");
            lore.add(ChatColor.GRAY + "• XP/min: " + ChatColor.WHITE + state.xpPerMinute + " <glyph:experience_orb_icon>");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Storage");
            lore.add(ChatColor.GRAY + "• Max Duration: " + ChatColor.WHITE + formatDuration(state.storageMinutes));
            lore.add(ChatColor.GRAY + "• Collected: " + ChatColor.GOLD + state.coins + " <glyph:coins_icon>" + ChatColor.GRAY + ", "
                    + ChatColor.WHITE + state.xp + " <glyph:experience_orb_icon>");
            lore.add("");
            lore.addAll(TooltipUtil.bulletList(
                    "Base storage is 3h 30m at level 1.",
                    "Each level adds +30m storage up to 8h."));
            infoMeta.setLore(lore);
            info.setItemMeta(infoMeta);
        }
        inv.setItem(INFO_SLOT, info);

        inv.setItem(CLAIM_SLOT, GuiUtil.createGuiItem(Material.CAULDRON, ChatColor.GREEN + "Claim Rewards",
                TooltipUtil.clickInstructions("to collect coins and experience", null)));
    }

    private void claim(Player player) {
        int level = Math.max(0, areaManager.getPalaceBuildingLevel(player));
        RewardState state = computeState(player, level);
        if (state.coins <= 0 && state.xp <= 0) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, "No palace rewards to claim yet.");
            return;
        }
        int droppedCoins = CoinDropManager.dropCoins(plugin, plugin.getEconomyManager(), player, player.getLocation(), state.coins, true);
        plugin.getLevelManager().addXP(player, state.xp);
        setLastClaimAt(player.getUniqueId(), System.currentTimeMillis());
        String expColor = ChatFormatter.experienceColor();
        String expLabel = ChatFormatter.experienceLabel();
        player.sendMessage(ChatColor.GOLD + "You received "
                + expColor + "+" + state.xp + " <glyph:experience_orb_icon> " + expLabel
                + ChatColor.GOLD + ", and "
                + me.nakilex.levelplugin.utils.CurrencyMessageUtil.formatAmount(
                me.nakilex.levelplugin.utils.CurrencyMessageUtil.Currency.COINS, droppedCoins)
                + ChatColor.GOLD + " dropped nearby!");
    }
    private String formatDuration(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        return h + "h " + m + "m";
    }

    private RewardState computeState(Player player, int level) {
        if (level <= 0) {
            return new RewardState(0, 0, 0, 0, 0);
        }
        int effectiveLevel = Math.max(1, level);
        int coinsPerMinute = 12 + ((effectiveLevel - 1) * 5);
        int xpPerMinute = 8 + ((effectiveLevel - 1) * 3);
        int storageMinutes = Math.min(MAX_STORAGE_MINUTES, BASE_STORAGE_MINUTES + ((effectiveLevel - 1) * STORAGE_PER_LEVEL_MINUTES));
        long now = System.currentTimeMillis();
        long lastClaim = getLastClaimAt(player.getUniqueId());
        long elapsedMinutes = Math.max(0L, (now - lastClaim) / 60000L);
        long cappedMinutes = Math.min(storageMinutes, elapsedMinutes);
        int coins = (int) Math.min(Integer.MAX_VALUE, cappedMinutes * coinsPerMinute);
        int xp = (int) Math.min(Integer.MAX_VALUE, cappedMinutes * xpPerMinute);
        return new RewardState(coinsPerMinute, xpPerMinute, storageMinutes, coins, xp);
    }

    private long getLastClaimAt(UUID scoped) {
        return plugin.getPlayerConfig().getConfig().getLong("players." + scoped + ".environment.area.palace.last-claim-at", System.currentTimeMillis());
    }

    private void setLastClaimAt(UUID scoped, long value) {
        plugin.getPlayerConfig().getConfig().set("players." + scoped + ".environment.area.palace.last-claim-at", value);
        plugin.getPlayerConfig().saveConfigFile();
    }

    private record RewardState(int coinsPerMinute, int xpPerMinute, int storageMinutes, int coins, int xp) {}
}
