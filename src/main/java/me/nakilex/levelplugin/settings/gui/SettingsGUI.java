package me.nakilex.levelplugin.settings.gui;

import me.nakilex.levelplugin.settings.managers.SettingsManager;
import me.nakilex.levelplugin.settings.data.PlayerSettings;
import me.nakilex.levelplugin.settings.data.PlayerVisibility;
import me.nakilex.levelplugin.leaderboards.LeaderboardType;
import me.nakilex.levelplugin.player.attributes.gui.StatsInventory;
import me.nakilex.levelplugin.mob.managers.ChatToggleManager;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.ToggleFeedbackUtil;
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

import java.util.*;

public class SettingsGUI implements Listener {

    private enum Filter { ALL, SOCIAL, VISUAL, COMBAT }

    private static final int GUI_SIZE = 45;
    private static final int FILTER_SLOT = 36;

    private final SettingsManager settingsManager;
    private final Map<UUID, Filter> filters = new HashMap<>();

    public SettingsGUI(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    public SettingsManager getSettingsManager() {
        return settingsManager;
    }

    public void openSettingsMenu(Player player) {
        PlayerSettings playerSettings = settingsManager.getSettings(player);
        Filter filter = filters.getOrDefault(player.getUniqueId(), Filter.ALL);

        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, "Settings");

        // Back button
        gui.setItem(0, GuiUtil.getNexoItem("arrow_left2", "§7Back"));

        // Damage Chat toggle
        if (filter == Filter.ALL || filter == Filter.COMBAT) {
            gui.setItem(10, GuiUtil.createToggleItem(
                    playerSettings.isDmgChatEnabled(),
                    "§bDamage Chat",
                    "§eClick to toggle"
            ));
        }

        // Damage Numbers toggle
        if (filter == Filter.ALL || filter == Filter.COMBAT) {
            gui.setItem(11, GuiUtil.createToggleItem(
                    playerSettings.isDmgNumberEnabled(),
                    "§bDamage Numbers",
                    "§eClick to toggle and run /dmgnumber"
            ));
        }

        // Drop Details (hologram) toggle
        if (filter == Filter.ALL || filter == Filter.VISUAL) {
            gui.setItem(12, GuiUtil.createToggleItem(
                    playerSettings.isDropDetailsEnabled(),
                    "§bDrop Details",
                    "§eClick to toggle and run /toggle dropdetails"
            ));
        }

        // Drop Details Chat toggle
        if (filter == Filter.ALL || filter == Filter.VISUAL) {
            gui.setItem(13, GuiUtil.createToggleItem(
                    playerSettings.isDropDetailsChatEnabled(),
                    "§bDrop Details Chat",
                    "§eClick to toggle and run /toggle dropdetailschat"
            ));
        }

        // Party Glow toggle
        if (filter == Filter.ALL || filter == Filter.SOCIAL) {
            gui.setItem(14, GuiUtil.createToggleItem(
                    playerSettings.isPartyGlowEnabled(),
                    "§bParty Glow",
                    "§eClick to toggle and run /partyglow"
            ));
        }

        // Friend Glow toggle
        if (filter == Filter.ALL || filter == Filter.SOCIAL) {
            gui.setItem(15, GuiUtil.createToggleItem(
                    playerSettings.isFriendGlowEnabled(),
                    "§bFriend Glow",
                    "§eClick to toggle and run /friendglow"
            ));
        }

        // Balance visibility toggle
        if (filter == Filter.ALL || filter == Filter.SOCIAL) {
            gui.setItem(16, GuiUtil.createToggleItem(
                    playerSettings.isBalancePublic(),
                    "§ePublic Balance",
                    "§eClick to toggle and run /toggle balancepublic"
            ));
        }

        if (filter == Filter.ALL || filter == Filter.SOCIAL) {
            gui.setItem(21, createVisibilityItem(playerSettings.getPlayerVisibility()));
        }

        // Auto-skip Cutscenes toggle
        if (filter == Filter.ALL || filter == Filter.VISUAL) {
            gui.setItem(22, GuiUtil.createToggleItem(
                    playerSettings.isAutoSkipCutscenes(),
                    "§bAuto Skip Cutscenes",
                    "§eClick to toggle"
            ));
        }

        // Auto-skip Songs toggle
        if (filter == Filter.ALL || filter == Filter.VISUAL) {
            gui.setItem(23, GuiUtil.createToggleItem(
                    playerSettings.isAutoSkipSongs(),
                    "§bAuto Skip Songs",
                    "§eClick to toggle and run /toggle songskip"
            ));
        }

        // Skill Point Reminder toggle
        if (filter == Filter.ALL || filter == Filter.VISUAL) {
            gui.setItem(24, GuiUtil.createToggleItem(
                    playerSettings.isSkillPointReminderEnabled(),
                    "§bSkill Point Reminder",
                    "§eClick to toggle"
            ));
        }

        gui.setItem(FILTER_SLOT, createFilterItem(filter));

        // Filler border
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", " ");
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }

        player.openInventory(gui);
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



    private void updateSettingItem(Inventory inventory, int slot, boolean enabled, String name, String command) {
        String lore = (command != null && !command.isBlank())
                ? "§eClick to toggle and run " + command
                : "§eClick to toggle";
        inventory.setItem(slot, GuiUtil.createToggleItem(enabled, name, lore));
    }

    private void updateVisibilityItem(Inventory inv, PlayerVisibility vis) {
        inv.setItem(21, createVisibilityItem(vis));
    }

    private ItemStack createFilterItem(Filter filter) {
        String name = switch (filter) {
            case ALL -> "§bFilter: All";
            case SOCIAL -> "§bFilter: Social";
            case VISUAL -> "§bFilter: Visual";
            case COMBAT -> "§bFilter: Combat";
        };
        ItemStack it = GuiUtil.getNexoItem("refresh", name);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setLore(Collections.singletonList("§7Click to change filter"));
            it.setItemMeta(meta);
        }
        return it;
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

        if (slot == FILTER_SLOT) {
            Filter next = switch (filters.getOrDefault(player.getUniqueId(), Filter.ALL)) {
                case ALL -> Filter.SOCIAL;
                case SOCIAL -> Filter.VISUAL;
                case VISUAL -> Filter.COMBAT;
                case COMBAT -> Filter.ALL;
            };
            filters.put(player.getUniqueId(), next);
            openSettingsMenu(player);
            return;
        }

        if (slot == 10) {
            settings.toggleDmgChat();
            boolean enabled = settings.isDmgChatEnabled();
            ChatToggleManager.getInstance().setEnabled(player, enabled);
            ToggleFeedbackUtil.sendToggle(player, "Damage chat", enabled);
            updateSettingItem(event.getInventory(), 10,
                enabled, "§bDamage Chat", null);

        } else if (slot == 11) {
            settings.toggleDmgNumber();
            Bukkit.dispatchCommand(player, "dmgnumber");
            updateSettingItem(event.getInventory(), 11,
                settings.isDmgNumberEnabled(), "§bDamage Numbers", "/dmgnumber");

        } else if (slot == 12) {
            Bukkit.dispatchCommand(player, "toggle dropdetails");
            updateSettingItem(event.getInventory(), 12,
                settings.isDropDetailsEnabled(), "§bDrop Details", "/toggle dropdetails");

        } else if (slot == 13) {
            // new drop-details-chat toggle
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
        } else if (slot == 21) {
            settings.cyclePlayerVisibility();
            updateVisibilityItem(event.getInventory(), settings.getPlayerVisibility());
            me.nakilex.levelplugin.Main.getInstance()
                .getPlayerVisibilityManager().updatePlayer(player);
        } else if (slot == 22) {
            settings.toggleAutoSkipCutscenes();
            updateSettingItem(event.getInventory(), 22,
                settings.isAutoSkipCutscenes(), "§bAuto Skip Cutscenes", "");
        } else if (slot == 23) {
            Bukkit.dispatchCommand(player, "toggle songskip");
            updateSettingItem(event.getInventory(), 23,
                settings.isAutoSkipSongs(), "§bAuto Skip Songs", "/toggle songskip");
        } else if (slot == 24) {
            settings.toggleSkillPointReminder();
            updateSettingItem(event.getInventory(), 24,
                settings.isSkillPointReminderEnabled(), "§bSkill Point Reminder", "");
        }
    }
}
