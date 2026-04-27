package me.nakilex.levelplugin.stronghold.run;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.mercenary.MercenaryExpeditionManager;
import me.nakilex.levelplugin.storage.PersonalStorage;
import me.nakilex.levelplugin.storage.events.StorageEvents;
import me.nakilex.levelplugin.storage.gui.StorageGUI;
import me.nakilex.levelplugin.utils.GuiUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Stronghold results implementation that reuses StorageGUI sorting/filter UX and storage slot behavior.
 */
public class StrongholdResultsStorageGUI extends StorageGUI {
    private static final int SUMMARY_SLOT = 49;
    private static final int CLAIM_ALL_SLOT = 46;
    private static final int SEND_TO_STORAGE_SLOT = 52;
    private final ItemStack summaryItem;

    public StrongholdResultsStorageGUI(String ownerKey,
                                       StorageEvents storageEvents,
                                       ItemStack summaryItem,
                                       List<ItemStack> stashedItems) {
        super(ownerKey, "stronghold_results", "run_", ChatColor.DARK_PURPLE + "Stronghold Results", storageEvents, true, 1);
        this.summaryItem = summaryItem == null ? createFallbackSummary() : summaryItem.clone();
        seedItems(stashedItems);
    }

    @Override
    protected ItemStack createInfoItem() {
        return summaryItem == null ? createFallbackSummary() : summaryItem.clone();
    }

    @Override
    public void open(Player player) {
        super.open(player);
        if (player == null || !player.isOnline() || player.getOpenInventory() == null) {
            return;
        }
        Inventory top = player.getOpenInventory().getTopInventory();
        if (top == null) {
            return;
        }
        top.setItem(CLAIM_ALL_SLOT, createClaimAllItem());
        top.setItem(SEND_TO_STORAGE_SLOT, createSendToStorageItem(player));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event == null || !(event.getWhoClicked() instanceof Player player)) {
            super.handleClick(event);
            return;
        }
        int slot = event.getRawSlot();
        if (slot == CLAIM_ALL_SLOT) {
            event.setCancelled(true);
            claimAllToInventory(player);
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> open(player));
            return;
        }
        if (slot == SEND_TO_STORAGE_SLOT) {
            event.setCancelled(true);
            sendAllToStorage(player);
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> open(player));
            return;
        }
        super.handleClick(event);
    }

    @Override
    public void saveToDisk() {
        // Stronghold result GUI is ephemeral for the current run only.
    }

    @Override
    public void loadFromDisk() {
        // Stronghold result GUI is ephemeral for the current run only.
    }

    public boolean hasRemainingItems() {
        for (ItemStack stack : collectRemainingItems()) {
            if (stack != null && !stack.getType().isAir()) {
                return true;
            }
        }
        return false;
    }

    public void salvageRemaining(Player player) {
        List<ItemStack> leftovers = collectRemainingItems();
        clearStoredItems();
        if (player == null || leftovers.isEmpty()) {
            return;
        }
        MercenaryExpeditionManager expeditionManager = Main.getInstance().getMercenaryExpeditionManager();
        if (expeditionManager != null) {
            expeditionManager.salvageRemaining(player, leftovers);
        }
    }

    private void seedItems(List<ItemStack> stashedItems) {
        Inventory page = getPages().isEmpty() ? null : getPages().get(0);
        if (page == null || stashedItems == null || stashedItems.isEmpty()) {
            return;
        }
        int idx = 0;
        for (int slot = 0; slot < page.getSize(); slot++) {
            if (!isStorageSlot(slot) || slot == SUMMARY_SLOT) {
                continue;
            }
            while (idx < stashedItems.size()) {
                ItemStack next = stashedItems.get(idx++);
                if (next == null || next.getType().isAir()) {
                    continue;
                }
                page.setItem(slot, next.clone());
                break;
            }
            if (idx >= stashedItems.size()) {
                break;
            }
        }
    }

    private List<ItemStack> collectRemainingItems() {
        List<ItemStack> items = new ArrayList<>();
        for (Inventory page : getPages()) {
            if (page == null) {
                continue;
            }
            for (int slot = 0; slot < page.getSize(); slot++) {
                if (!isStorageSlot(slot) || slot == SUMMARY_SLOT) {
                    continue;
                }
                ItemStack stack = page.getItem(slot);
                if (stack != null && !stack.getType().isAir()) {
                    items.add(stack.clone());
                }
            }
        }
        return items;
    }

    private void clearStoredItems() {
        for (Inventory page : getPages()) {
            if (page == null) {
                continue;
            }
            for (int slot = 0; slot < page.getSize(); slot++) {
                if (!isStorageSlot(slot) || slot == SUMMARY_SLOT) {
                    continue;
                }
                page.setItem(slot, null);
            }
        }
    }

    private void claimAllToInventory(Player player) {
        if (player == null) {
            return;
        }
        int movedStacks = 0;
        int leftoverStacks = 0;
        for (Inventory page : getPages()) {
            if (page == null) {
                continue;
            }
            for (int slot = 0; slot < page.getSize(); slot++) {
                if (!isStorageSlot(slot) || slot == SUMMARY_SLOT) {
                    continue;
                }
                ItemStack stack = page.getItem(slot);
                if (stack == null || stack.getType().isAir()) {
                    continue;
                }
                ItemStack toMove = stack.clone();
                var overflow = player.getInventory().addItem(toMove);
                if (overflow.isEmpty()) {
                    page.setItem(slot, null);
                    movedStacks++;
                } else {
                    ItemStack remaining = overflow.values().iterator().next();
                    page.setItem(slot, remaining);
                    leftoverStacks++;
                }
            }
        }
        if (leftoverStacks > 0) {
            me.nakilex.levelplugin.utils.ChatMessageUtil.send(player,
                    me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType.WARNING,
                    "Inventory full. Claimed " + ChatColor.WHITE + movedStacks + ChatColor.GRAY
                            + " stack(s); " + ChatColor.WHITE + leftoverStacks + ChatColor.GRAY + " stack(s) remain.");
            return;
        }
        me.nakilex.levelplugin.utils.ChatMessageUtil.send(player,
                me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType.SUCCESS,
                "Claimed " + ChatColor.WHITE + movedStacks + ChatColor.GRAY + " stack(s) from Stronghold results.");
    }

    private void sendAllToStorage(Player player) {
        if (player == null || Main.getInstance() == null || Main.getInstance().getStorageManager() == null) {
            return;
        }
        Main.getInstance().getStorageManager().createStorage(player.getUniqueId());
        PersonalStorage personalStorage = Main.getInstance().getStorageManager().getStorage(player.getUniqueId());
        if (personalStorage == null || personalStorage.getStorageGUI() == null) {
            me.nakilex.levelplugin.utils.ChatMessageUtil.send(player,
                    me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType.ERROR,
                    "Personal storage is unavailable.");
            return;
        }
        personalStorage.load();
        StorageGUI storage = personalStorage.getStorageGUI();

        int movedStacks = 0;
        int leftoverStacks = 0;
        for (Inventory page : getPages()) {
            if (page == null) {
                continue;
            }
            for (int slot = 0; slot < page.getSize(); slot++) {
                if (!isStorageSlot(slot) || slot == SUMMARY_SLOT) {
                    continue;
                }
                ItemStack stack = page.getItem(slot);
                if (stack == null || stack.getType().isAir()) {
                    continue;
                }
                ItemStack remaining = storage.addItemToStorage(stack.clone());
                if (remaining == null || remaining.getType().isAir() || remaining.getAmount() <= 0) {
                    page.setItem(slot, null);
                    movedStacks++;
                } else {
                    page.setItem(slot, remaining);
                    leftoverStacks++;
                }
            }
        }
        storage.saveToDisk();
        if (leftoverStacks > 0) {
            me.nakilex.levelplugin.utils.ChatMessageUtil.send(player,
                    me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType.WARNING,
                    "Storage full. Sent " + ChatColor.WHITE + movedStacks + ChatColor.GRAY + " stack(s); "
                            + ChatColor.WHITE + leftoverStacks + ChatColor.GRAY + " stack(s) remain in results.");
            return;
        }
        me.nakilex.levelplugin.utils.ChatMessageUtil.send(player,
                me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType.SUCCESS,
                "Sent " + ChatColor.WHITE + movedStacks + ChatColor.GRAY + " stack(s) to personal storage.");
    }

    private ItemStack createClaimAllItem() {
        return GuiUtil.createGuiItem(Material.HOPPER_MINECART, ChatColor.GREEN + "Claim All",
                List.of(
                        ChatColor.GRAY + "Move all possible result items to inventory.",
                        ChatColor.GRAY + "If inventory is full, leftovers stay here.",
                        " ",
                        ChatColor.WHITE + "Left-click " + ChatColor.GRAY + "to claim all"
                ));
    }

    private ItemStack createSendToStorageItem(Player player) {
        int freeSlots = 0;
        if (Main.getInstance() != null
                && Main.getInstance().getStorageManager() != null
                && Main.getInstance().getStorageManager().hasStorage(player.getUniqueId())) {
            PersonalStorage storage = Main.getInstance().getStorageManager().getStorage(player.getUniqueId());
            if (storage != null && storage.getStorageGUI() != null) {
                storage.load();
                freeSlots = storage.getStorageGUI().countFreeStorageSlots();
            }
        }
        return GuiUtil.createGuiItem(Material.CHEST_MINECART, ChatColor.AQUA + "Send to Storage",
                List.of(
                        ChatColor.GRAY + "Send as many result items as possible",
                        ChatColor.GRAY + "into your personal storage.",
                        ChatColor.GRAY + "Estimated free storage slots: " + ChatColor.WHITE + freeSlots,
                        " ",
                        ChatColor.WHITE + "Left-click " + ChatColor.GRAY + "to send items"
                ));
    }

    private ItemStack createFallbackSummary() {
        ItemStack summary = GuiUtil.createGuiItem(Material.BOOK, ChatColor.GOLD + "Run Summary",
                List.of(ChatColor.GRAY + "Stronghold run results."));
        ItemMeta meta = summary.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Run Summary");
            summary.setItemMeta(meta);
        }
        return summary;
    }
}
