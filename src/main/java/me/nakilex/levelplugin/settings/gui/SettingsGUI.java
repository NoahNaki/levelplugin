package me.nakilex.levelplugin.settings.gui;

import me.nakilex.levelplugin.settings.managers.SettingsManager;
import me.nakilex.levelplugin.settings.data.PlayerSettings;
import me.nakilex.levelplugin.settings.data.PlayerVisibility;
import me.nakilex.levelplugin.leaderboards.LeaderboardType;
import me.nakilex.levelplugin.player.attributes.gui.StatsInventory;
import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
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

        // Back button
        gui.setItem(0, getNexoItem("arrow_left2", "§7Back"));

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

        // Drop Details (hologram) toggle
        gui.setItem(12, createSettingItem(
            playerSettings.isDropDetailsEnabled(),
            "§bDrop Details",
            "/toggle dropdetails"
        ));

        // Drop Details Chat toggle
        gui.setItem(13, createSettingItem(
            playerSettings.isDropDetailsChatEnabled(),
            "§bDrop Details Chat",
            "/toggle dropdetailschat"
        ));

        // Party Glow toggle
        gui.setItem(14, createSettingItem(
            playerSettings.isPartyGlowEnabled(),
            "§bParty Glow",
            "/partyglow"
        ));

        // Friend Glow toggle
        gui.setItem(15, createSettingItem(
            playerSettings.isFriendGlowEnabled(),
            "§bFriend Glow",
            "/friendglow"
        ));

        // Balance visibility toggle
        gui.setItem(16, createSettingItem(
            playerSettings.isBalancePublic(),
            "§ePublic Balance",
            "/toggle balancepublic"
        ));

        // Player visibility mode
        gui.setItem(17, createVisibilityItem(playerSettings.getPlayerVisibility()));

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

    private ItemStack createVisibilityItem(PlayerVisibility vis) {
        Material mat;
        String status;
        switch (vis) {
            case SHOW_ALL -> { mat = Material.LIME_DYE; status = "All"; }
            case FRIENDS_ONLY -> { mat = Material.GREEN_DYE; status = "Friends"; }
            case HIDE_ALL -> { mat = Material.GRAY_DYE; status = "None"; }
            default -> { mat = Material.GRAY_DYE; status = "Unknown"; }
        }
        return createItem(mat, "§bPlayer Visibility",
                "",
                "§7Status: §f" + status,
                "",
                "§eClick to cycle");
    }

    private ItemStack getNexoItem(String id, String name) {
        ItemBuilder builder = NexoItems.itemFromId(id);
        if (builder == null) return new ItemStack(Material.BARRIER);
        ItemStack item = builder.build();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void updateSettingItem(Inventory inventory, int slot, boolean enabled, String name, String command) {
        inventory.setItem(slot, createSettingItem(enabled, name, command));
    }

    private void updateVisibilityItem(Inventory inv, PlayerVisibility vis) {
        inv.setItem(17, createVisibilityItem(vis));
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

        if (slot == 0) {
            player.openInventory(StatsInventory.getStatsMenu(player));
            return;
        }

        if (slot == 10) {
            settings.toggleDmgChat();
            Bukkit.dispatchCommand(player, "dmgchat");
            updateSettingItem(event.getInventory(), 10,
                settings.isDmgChatEnabled(), "§bDamage Chat", "/dmgchat");

        } else if (slot == 11) {
            settings.toggleDmgNumber();
            Bukkit.dispatchCommand(player, "dmgnumber");
            updateSettingItem(event.getInventory(), 11,
                settings.isDmgNumberEnabled(), "§bDamage Numbers", "/dmgnumber");

        } else if (slot == 12) {
            settings.toggleDropDetails();
            Bukkit.dispatchCommand(player, "toggle dropdetails");
            updateSettingItem(event.getInventory(), 12,
                settings.isDropDetailsEnabled(), "§bDrop Details", "/toggle dropdetails");

        } else if (slot == 13) {
            // new drop-details-chat toggle
            settings.toggleDropDetailsChat();
            Bukkit.dispatchCommand(player, "toggle dropdetailschat");
            updateSettingItem(event.getInventory(), 13,
                settings.isDropDetailsChatEnabled(), "§bDrop Details Chat", "/toggle dropdetailschat");
        } else if (slot == 14) {
            settings.togglePartyGlow();
            Bukkit.dispatchCommand(player, "partyglow");
            updateSettingItem(event.getInventory(), 14,
                settings.isPartyGlowEnabled(), "§bParty Glow", "/partyglow");
        } else if (slot == 15) {
            settings.toggleFriendGlow();
            Bukkit.dispatchCommand(player, "friendglow");
            updateSettingItem(event.getInventory(), 15,
                settings.isFriendGlowEnabled(), "§bFriend Glow", "/friendglow");
        } else if (slot == 16) {
            settings.toggleBalancePublic();
            updateSettingItem(event.getInventory(), 16,
                settings.isBalancePublic(), "§ePublic Balance", "/toggle balancepublic");
            me.nakilex.levelplugin.Main main = me.nakilex.levelplugin.Main.getInstance();
            if (main != null && main.getLeaderboardManager() != null) {
                main.getLeaderboardManager().updateType(LeaderboardType.BALANCE);
            }
        } else if (slot == 17) {
            settings.cyclePlayerVisibility();
            updateVisibilityItem(event.getInventory(), settings.getPlayerVisibility());
            me.nakilex.levelplugin.Main.getInstance()
                .getPlayerVisibilityManager().updatePlayer(player);
        }
    }
}
