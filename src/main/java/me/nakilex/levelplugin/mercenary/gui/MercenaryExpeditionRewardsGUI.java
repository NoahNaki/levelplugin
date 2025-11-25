package me.nakilex.levelplugin.mercenary.gui;

import me.nakilex.levelplugin.mercenary.ExpeditionRewards;
import me.nakilex.levelplugin.mercenary.MercenaryExpeditionManager;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.NumberUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.*;

/** GUI for inspecting and claiming expedition loot. */
public class MercenaryExpeditionRewardsGUI implements Listener {
    public enum RewardView { EXPEDITIONS }

    private static final int SIZE = 54;
    private static final String TITLE = ChatColor.DARK_AQUA + "Expedition Rewards";
    private static final int[] LOOT_SLOTS = GuiUtil.PAGED_SLOTS;

    private final MercenaryExpeditionManager expeditionManager;
    private final Map<UUID, Set<Integer>> lootTrackedSlots = new HashMap<>();

    public MercenaryExpeditionRewardsGUI(Plugin plugin, MercenaryExpeditionManager expeditionManager) {
        this.expeditionManager = expeditionManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player, RewardView view) {
        ExpeditionRewards rewards = expeditionManager.getPendingRewards(player.getUniqueId());
        if (rewards == null) {
            player.sendMessage(ChatColor.RED + "You have no pending expedition rewards.");
            return;
        }
        Inventory inv = GuiBuilder.create(SIZE, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .fillEmptySlots(false)
                .border()
                .build();

        Set<Integer> tracked = new HashSet<>();
        int idx = 0;
        for (ItemStack loot : rewards.loot()) {
            if (idx >= LOOT_SLOTS.length) {
                break;
            }
            int slot = LOOT_SLOTS[idx++];
            inv.setItem(slot, loot);
            tracked.add(slot);
        }
        lootTrackedSlots.put(player.getUniqueId(), tracked);

        ItemStack info = GuiUtil.getNexoItem("info", ChatColor.YELLOW + "Expedition Earnings");
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Coins: " + ChatColor.GOLD + NumberUtil.formatCommas(rewards.coins()));
            lore.add(ChatColor.GRAY + "Take the pieces you want; the rest are salvaged.");
            meta.setLore(lore);
            info.setItemMeta(meta);
        }
        inv.setItem(49, info);
        player.openInventory(inv);

        // Credit coins immediately so they aren't lost.
        if (rewards.coins() > 0) {
            player.sendMessage(ChatColor.GOLD + "You received " + ChatColor.YELLOW + NumberUtil.formatCommas(rewards.coins())
                    + ChatColor.GOLD + " coins from your expedition.");
            ExpeditionRewards remaining = new ExpeditionRewards();
            rewards.loot().forEach(remaining::addLoot);
            expeditionManager.setPendingRewards(player.getUniqueId(), remaining);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!TITLE.equals(event.getView().getTitle())) {
            return;
        }
        int raw = event.getRawSlot();
        // Bottom inventory interactions should remain untouched.
        if (raw >= event.getView().getTopInventory().getSize()) {
            return;
        }
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.GRAY_STAINED_GLASS_PANE) {
            event.setCancelled(true);
            return;
        }
        // Allow players to take loot items from tracked slots; keep all other GUI controls locked.
        Set<Integer> tracked = lootTrackedSlots.getOrDefault(player.getUniqueId(), Collections.emptySet());
        if (!tracked.contains(event.getSlot())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!TITLE.equals(event.getView().getTitle())) {
            return;
        }
        Set<Integer> tracked = lootTrackedSlots.remove(player.getUniqueId());
        if (tracked == null) {
            return;
        }
        List<ItemStack> leftovers = new ArrayList<>();
        for (int slot : tracked) {
            ItemStack stack = event.getInventory().getItem(slot);
            if (stack != null) {
                leftovers.add(stack);
                event.getInventory().setItem(slot, null);
            }
        }
        expeditionManager.salvageRemaining(player, leftovers);
        expeditionManager.clearPending(player.getUniqueId());
    }
}
