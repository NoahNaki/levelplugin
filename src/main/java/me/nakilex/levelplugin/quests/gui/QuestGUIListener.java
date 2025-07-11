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
        if (view.getTitle().equals(QuestGUI.CONFIRM_TITLE)) {
            handleConfirmClick(event);
            return;
        }
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

        var quest = questManager.getQuest(id);
        QuestState state = questManager.getQuestState(player, quest);

        if (event.getClick() == ClickType.RIGHT) {
            if (state == QuestState.AVAILABLE) {
                questManager.startQuest(player, id);
                player.closeInventory();
            } else if (state == QuestState.ACCEPTED || state == QuestState.IN_PROGRESS || state == QuestState.TURN_IN_READY) {
                if (!quest.isMainQuest()) {
                    QuestGUI.openConfirmAbandon(player, quest);
                }
            }
        } else if (event.getClick() == ClickType.LEFT) {
            if (state == QuestState.ACCEPTED || state == QuestState.IN_PROGRESS || state == QuestState.TURN_IN_READY) {
                questManager.setTrackedQuest(player, id);
                player.sendMessage(ChatColor.GREEN + "Tracking quest: " + ChatColor.WHITE + quest.getName());
                QuestGUI.openQuestGUI(player, questManager, QuestGUI.pageMap.getOrDefault(player.getUniqueId(),0));
            }
        } else if (event.getClick() == ClickType.SHIFT_LEFT) {
            questManager.setTrackedQuest(player, id);
            player.sendMessage(ChatColor.GREEN + "Now tracking " + id);
        }
    }

    private void handleConfirmClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);
        var inv = QuestGUI.getConfirmInventory(player.getUniqueId());
        if (inv == null || !event.getView().getTopInventory().equals(inv)) return;
        if (event.getRawSlot() == QuestGUI.CONFIRM_YES_SLOT) {
            String qId = QuestGUI.getPendingQuest(player.getUniqueId());
            if (qId != null) {
                var quest = questManager.getQuest(qId);
                questManager.resetQuest(player.getUniqueId(), qId);
                player.sendMessage(ChatColor.RED + "Abandoned quest: " + ChatColor.WHITE + quest.getName());
            }
            QuestGUI.clearPending(player.getUniqueId());
            QuestGUI.openQuestGUI(player, questManager, QuestGUI.pageMap.getOrDefault(player.getUniqueId(),0));
        } else if (event.getRawSlot() == QuestGUI.CONFIRM_NO_SLOT) {
            QuestGUI.clearPending(player.getUniqueId());
            QuestGUI.openQuestGUI(player, questManager, QuestGUI.pageMap.getOrDefault(player.getUniqueId(),0));
        }
    }

    @EventHandler
    public void onClose(org.bukkit.event.inventory.InventoryCloseEvent e) {
        if (!e.getView().getTitle().equals(QuestGUI.CONFIRM_TITLE)) return;
        Player p = (Player) e.getPlayer();
        QuestGUI.clearPending(p.getUniqueId());
        QuestGUI.openQuestGUI(p, questManager, QuestGUI.pageMap.getOrDefault(p.getUniqueId(), 0));
    }
}
