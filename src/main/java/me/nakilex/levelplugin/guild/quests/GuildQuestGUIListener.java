package me.nakilex.levelplugin.guild.quests;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import me.nakilex.levelplugin.guild.Guild;
import me.nakilex.levelplugin.guild.GuildManager;
import me.nakilex.levelplugin.utils.GuiUtil;

/**
 * Simple click handler for the guild quest menu to prevent players from
 * moving items out of the GUI. More advanced interactions (e.g. rerolling
 * quests) can be layered on top later.
 */
public class GuildQuestGUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!GuiUtil.titleMatches(event.getView().getTitle(), GuildQuestGUI.TITLE)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        Guild g = GuildManager.getInstance().getGuild(player.getUniqueId());
        if (g == null) return;
        GuildQuestGUI.handleWidgetClick(event, player, g.getQuests());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!GuiUtil.titleMatches(event.getView().getTitle(), GuildQuestGUI.TITLE)) return;
        event.setCancelled(true);
    }
}
