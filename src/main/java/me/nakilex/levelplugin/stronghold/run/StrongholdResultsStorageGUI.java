package me.nakilex.levelplugin.stronghold.run;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.mercenary.MercenaryExpeditionManager;
import me.nakilex.levelplugin.storage.events.StorageEvents;
import me.nakilex.levelplugin.storage.gui.StorageGUI;
import me.nakilex.levelplugin.utils.GuiUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
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
