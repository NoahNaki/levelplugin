package me.nakilex.levelplugin.quests.gui;

import me.nakilex.levelplugin.quests.managers.QuestManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class QuestGUIListener implements Listener {

    private final QuestManager questManager;

    public QuestGUIListener(QuestManager questManager) {
        this.questManager = questManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        if (!view.getTitle().equals(QuestGUI.GUI_TITLE)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;
        String id = meta.getLocalizedName();
        String name = meta.getDisplayName();
        if (name != null) {
            String stripped = ChatColor.stripColor(name);
            if (stripped.equalsIgnoreCase("Previous")) {
                int page = QuestGUI.pageMap.getOrDefault(player.getUniqueId(), 0);
                QuestGUI.openQuestGUI(player, questManager, Math.max(0, page - 1));
                return;
            }
            if (stripped.equalsIgnoreCase("Next")) {
                int page = QuestGUI.pageMap.getOrDefault(player.getUniqueId(), 0);
                QuestGUI.openQuestGUI(player, questManager, page + 1);
                return;
            }
            if (stripped.startsWith("Filter")) {
                int mode = QuestGUI.filterMap.getOrDefault(player.getUniqueId(), 0);
                mode = (mode + 1) % 4;
                QuestGUI.filterMap.put(player.getUniqueId(), mode);
                QuestGUI.openQuestGUI(player, questManager, QuestGUI.pageMap.getOrDefault(player.getUniqueId(),0));
                return;
            }
            if (stripped.startsWith("Sort")) {
                int mode = QuestGUI.sortMap.getOrDefault(player.getUniqueId(), 0);
                mode = (mode + 1) % 2;
                QuestGUI.sortMap.put(player.getUniqueId(), mode);
                QuestGUI.openQuestGUI(player, questManager, QuestGUI.pageMap.getOrDefault(player.getUniqueId(),0));
                return;
            }
        }
        if (id == null || id.isEmpty()) return;

        QuestState state = questManager.getQuestState(player, questManager.getQuest(id));

        if (event.getClick() == ClickType.RIGHT) {
            if (state == QuestState.AVAILABLE) {
                questManager.startQuest(player, id);
                player.closeInventory();
            }
        }

        if (event.getClick() == ClickType.LEFT) {
            player.sendMessage(ChatColor.AQUA + "Quest: " + id);
        }

        if (event.getClick() == ClickType.SHIFT_LEFT) {
            questManager.setTrackedQuest(player, id);
            player.sendMessage(ChatColor.GREEN + "Now tracking " + id);
        }
    }
}
