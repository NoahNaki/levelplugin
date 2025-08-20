package me.nakilex.levelplugin.guild.quests;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import me.nakilex.levelplugin.guild.Guild;
import me.nakilex.levelplugin.guild.GuildManager;
import me.nakilex.levelplugin.guild.quests.GuildQuestManager;

/**
 * Simple click handler for the guild quest menu to prevent players from
 * moving items out of the GUI. More advanced interactions (e.g. rerolling
 * quests) can be layered on top later.
 */
public class GuildQuestGUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GuildQuestGUI.TITLE)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        event.setCancelled(true);

        int slot = event.getRawSlot();
        int idx = GuildQuestGUI.indexFromSlot(slot);
        if (idx == -1) return;

        Player player = (Player) event.getWhoClicked();
        Guild g = GuildManager.getInstance().getGuild(player.getUniqueId());
        if (g == null) return;
        String key = String.valueOf(idx);
        me.nakilex.levelplugin.guild.quests.GuildQuest quest = g.getQuests().get(key);
        if (quest == null || quest.isCompleted()) return;

        if (event.getClick() == ClickType.LEFT) {
            if (!quest.isAccepted()) {
                quest.setAccepted(true);
                player.sendMessage(ChatColor.GREEN + "Accepted guild quest: " + quest.getName());
            } else {
                boolean tracked = GuildQuestManager.getInstance().toggleTracking(player, quest);
                if (tracked) {
                    player.sendMessage(ChatColor.GREEN + "Tracking guild quest: " + quest.getName());
                } else {
                    player.sendMessage(ChatColor.YELLOW + "Stopped tracking guild quest: " + quest.getName());
                }
            }
        } else if (event.getClick() == ClickType.RIGHT) {
            if (!quest.isAccepted() && !quest.isRerolled()) {
                GuildQuestManager.getInstance().rerollQuest(g, key);
                player.sendMessage(ChatColor.YELLOW + "Guild quest rerolled.");
            }
        }

        player.openInventory(GuildQuestGUI.create(player, g.getQuests()));
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!event.getView().getTitle().equals(GuildQuestGUI.TITLE)) return;
        event.setCancelled(true);
    }
}
