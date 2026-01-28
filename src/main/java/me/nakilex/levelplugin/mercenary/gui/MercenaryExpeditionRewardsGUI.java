package me.nakilex.levelplugin.mercenary.gui;

import me.nakilex.levelplugin.mercenary.ExpeditionRewards;
import me.nakilex.levelplugin.mercenary.MercenaryExpeditionManager;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.NumberUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
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
    private static final String TITLE = "Expedition Rewards";
    private static final int[] LOOT_SLOTS = GuiUtil.PAGED_SLOTS;
    private static final int INFO_SLOT = 49;

    private final MercenaryExpeditionManager expeditionManager;
    private final Map<UUID, Set<Integer>> lootTrackedSlots = new HashMap<>();
    private final Map<UUID, List<GuiWidget>> widgetsByPlayer = new HashMap<>();

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

        List<GuiWidget> widgets = buildWidgets(rewards);
        widgetsByPlayer.put(player.getUniqueId(), widgets);
        renderWidgets(inv, player, widgets);
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
        if (handleWidgetClick(event, player)) {
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
        widgetsByPlayer.remove(player.getUniqueId());
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

    private List<GuiWidget> buildWidgets(ExpeditionRewards rewards) {
        List<GuiWidget> widgets = new ArrayList<>();
        widgets.add(new ActionWidget(INFO_SLOT,
                context -> createInfoItem(rewards),
                null));
        return widgets;
    }

    private ItemStack createInfoItem(ExpeditionRewards rewards) {
        ItemStack info = GuiUtil.getNexoItem("info", ChatColor.YELLOW + "Expedition Earnings");
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Coins: " + ChatColor.GOLD + NumberUtil.formatCommas(rewards.coins()));
            lore.add(ChatColor.GRAY + "Take the pieces you want; the rest are salvaged.");
            meta.setLore(lore);
            info.setItemMeta(meta);
        }
        return info;
    }

    private boolean handleWidgetClick(InventoryClickEvent event, Player player) {
        List<GuiWidget> widgets = widgetsByPlayer.get(player.getUniqueId());
        if (widgets == null) {
            return false;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return false;
        }
        GuiWidget widget = widgets.stream()
                .filter(w -> w.handlesSlot(slot))
                .findFirst()
                .orElse(null);
        if (widget == null) {
            return false;
        }
        event.setCancelled(true);
        widget.onClick(slot, event.getClick(), new GuiContext(player, event.getView().getTopInventory()));
        return true;
    }

    private void renderWidgets(Inventory inventory, Player player, List<GuiWidget> widgets) {
        GuiLayout layout = new GuiLayout(inventory);
        GuiContext context = new GuiContext(player, inventory);
        for (GuiWidget widget : widgets) {
            widget.contribute(layout, context);
        }
    }
}
