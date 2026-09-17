package me.nakilex.levelplugin.xprison;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.GuiUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;

/** Replaces X-Prison's vanilla currency tabs with the matching Nexo currency items. */
public final class XPrisonCurrencyMenuIconListener implements Listener {
    private static final String PICKAXE_MENU_TITLE = "Pickaxe Menu";
    private static final int TOKENS_SLOT = 48;
    private static final int GEMS_SLOT = 49;

    private final Main plugin;

    public XPrisonCurrencyMenuIconListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player && isPickaxeMenu(event.getView().getTitle())) {
            refreshNextTick(player, event.getInventory());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player
                && isPickaxeMenu(event.getView().getTitle())
                && (event.getRawSlot() == TOKENS_SLOT || event.getRawSlot() == GEMS_SLOT)) {
            refreshNextTick(player, event.getView().getTopInventory());
        }
    }

    private void refreshNextTick(Player player, Inventory inventory) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || !player.getOpenInventory().getTopInventory().equals(inventory)) {
                return;
            }
            replaceCurrencyTab(inventory, TOKENS_SLOT, Material.SUNFLOWER,
                    "Token Enchants", "levelplugin_tokens", ChatColor.GREEN + "Token Enchants");
            replaceCurrencyTab(inventory, GEMS_SLOT, Material.EMERALD,
                    "Gem Enchants", "levelplugin_gems", ChatColor.LIGHT_PURPLE + "Gem Enchants");
        });
    }

    private void replaceCurrencyTab(Inventory inventory, int slot, Material expectedMaterial,
                                    String expectedName, String nexoId, String name) {
        ItemStack current = inventory.getItem(slot);
        // X-Prison owns the menu. Only replace the exact vanilla tab item it configured;
        // never overwrite an enchant icon, filler, arrow, or another menu item that happens
        // to occupy this slot in a different render state.
        if (current == null || current.getType() != expectedMaterial || !hasExactName(current, expectedName)) {
            return;
        }

        List<String> lore = new ArrayList<>();
        ItemMeta currentMeta = current.getItemMeta();
        if (currentMeta != null && currentMeta.hasLore() && currentMeta.getLore() != null) {
            lore.addAll(currentMeta.getLore());
        }
        ItemStack replacement = GuiUtil.getNexoItemIfPresent(nexoId, name, lore);
        if (replacement != null) {
            inventory.setItem(slot, replacement);
        }
    }

    private boolean hasExactName(ItemStack item, String expectedName) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        String plainName = meta.displayName() == null
                ? ""
                : PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        return expectedName.equals(plainName.trim());
    }

    private boolean isPickaxeMenu(String title) {
        return GuiUtil.titleMatches(title, PICKAXE_MENU_TITLE);
    }
}
