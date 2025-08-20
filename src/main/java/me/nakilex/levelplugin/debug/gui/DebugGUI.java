package me.nakilex.levelplugin.debug.gui;

import me.nakilex.levelplugin.mob.managers.PlayerToggleManager;
import me.nakilex.levelplugin.scoreboard.PlayerScoreboardManager;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.ToggleFeedbackUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/**
 * Simple GUI to toggle developer debug features like mob kill info
 * and TPS display, mirroring the style of the player settings menu.
 */
public class DebugGUI implements Listener {
    private static final int GUI_SIZE = 27;
    private static final int MOBINFO_SLOT = 11;
    private static final int TPS_SLOT = 15;
    private static final int SIEGE_SLOT = 13;

    private final PlayerToggleManager mobDebugManager;
    private final PlayerScoreboardManager scoreboardManager;

    public DebugGUI(PlayerToggleManager mobDebugManager, PlayerScoreboardManager scoreboardManager) {
        this.mobDebugManager = mobDebugManager;
        this.scoreboardManager = scoreboardManager;
    }

    /** Open the debug tools menu for the player. */
    public void open(Player player) {
        Inventory inv = GuiBuilder.create(GUI_SIZE, "Debug Tools")
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .fillEmptySlots(false)
                .border()
                .build();
        inv.setItem(MOBINFO_SLOT, GuiUtil.createToggleItem(
                mobDebugManager.isEnabled(player),
                "§bMob Info Debug",
                "§7Show MythicMob rewards on kill"));
        inv.setItem(TPS_SLOT, GuiUtil.createToggleItem(
                scoreboardManager.isTpsEnabled(player),
                "§bShow TPS",
                "§7Display TPS on sidebar"));
        boolean fast = me.nakilex.levelplugin.guild.siege.GuildSiegeManager.getInstance().isFastCapture();
        inv.setItem(SIEGE_SLOT, GuiUtil.createToggleItem(
                fast,
                "§bFast Siege",
                "§750% progress per second"));
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals("Debug Tools")) return;
        event.setCancelled(true);

        int slot = event.getRawSlot();
        Inventory inv = event.getInventory();
        if (slot == MOBINFO_SLOT) {
            boolean enabled = mobDebugManager.toggle(player);
            inv.setItem(MOBINFO_SLOT, GuiUtil.createToggleItem(enabled,
                    "§bMob Info Debug",
                    "§7Show MythicMob rewards on kill"));
            ToggleFeedbackUtil.sendToggle(player, "Mob info debug", enabled);
        } else if (slot == TPS_SLOT) {
            boolean enabled = scoreboardManager.toggleTps(player);
            inv.setItem(TPS_SLOT, GuiUtil.createToggleItem(enabled,
                    "§bShow TPS",
                    "§7Display TPS on sidebar"));
            ToggleFeedbackUtil.sendToggle(player, "TPS display", enabled);
        } else if (slot == SIEGE_SLOT) {
            boolean enabled = me.nakilex.levelplugin.guild.siege.GuildSiegeManager.getInstance().toggleFastCapture();
            inv.setItem(SIEGE_SLOT, GuiUtil.createToggleItem(enabled,
                    "§bFast Siege",
                    "§750% progress per second"));
            ToggleFeedbackUtil.sendToggle(player, "Fast siege mode", enabled);
        }
    }
}
