package me.nakilex.levelplugin.settings.gui;

import me.nakilex.levelplugin.settings.managers.SettingsManager;
import me.nakilex.levelplugin.settings.data.PlayerSettings;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class SettingsGUI implements Listener {

    private final SettingsManager settingsManager;

    public SettingsGUI(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    public void openSettingsMenu(Player player) {
        PlayerSettings playerSettings = settingsManager.getSettings(player);

        Inventory gui = Bukkit.createInventory(null, 27, "Settings");

        // Damage Chat toggle
        gui.setItem(10, createSettingItem(
            playerSettings.isDmgChatEnabled(),
            "§bDamage Chat",
            "/dmgchat"
        ));

        // Damage Numbers toggle
        gui.setItem(11, createSettingItem(
            playerSettings.isDmgNumberEnabled(),
            "§bDamage Numbers",
            "/dmgnumber"
        ));

        // Drop Details toggle
        gui.setItem(12, createSettingItem(
            playerSettings.isDropDetailsEnabled(),
            "§bDrop Details",
            "/toggle dropdetails"
        ));

        // Filler border
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", " ");
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }

        player.openInventory(gui);
    }

    private ItemStack createSettingItem(boolean isEnabled, String name, String command) {
        Material mat = isEnabled ? Material.SLIME_BALL : Material.FIREWORK_STAR;
        String status = isEnabled ? "§aEnabled" : "§cDisabled";
        return createItem(mat, name,
            "",
            "§7Status: " + status,
            "",
            "§eClick to toggle and run " + command
        );
    }

    private ItemStack createItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(loreLines));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void updateSettingItem(Inventory inventory, int slot, boolean enabled, String name, String command) {
        inventory.setItem(slot, createSettingItem(enabled, name, command));
    }

    @EventHandler
    public void onSettingsClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().equals("Settings")) return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        PlayerSettings settings = settingsManager.getSettings(player);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        int slot = event.getSlot();

        if (slot == 10) {
            settings.toggleDmgChat();
            Bukkit.dispatchCommand(player, "dmgchat");
            updateSettingItem(event.getInventory(), 10, settings.isDmgChatEnabled(), "§bDamage Chat", "/dmgchat");
        } else if (slot == 11) {
            settings.toggleDmgNumber();
            Bukkit.dispatchCommand(player, "dmgnumber");
            updateSettingItem(event.getInventory(), 11, settings.isDmgNumberEnabled(), "§bDamage Numbers", "/dmgnumber");
        } else if (slot == 12) {
            settings.toggleDropDetails();
            Bukkit.dispatchCommand(player, "toggle dropdetails");
            updateSettingItem(event.getInventory(), 12, settings.isDropDetailsEnabled(), "§bDrop Details", "/toggle dropdetails");
        }
    }
}
