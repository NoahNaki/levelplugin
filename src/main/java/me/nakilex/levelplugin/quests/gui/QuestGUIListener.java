package me.nakilex.levelplugin.quests.gui;

import me.nakilex.levelplugin.quests.managers.QuestManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;

public class QuestGUIListener implements Listener {

    private final QuestManager questManager;

    public QuestGUIListener(QuestManager questManager) {
        this.questManager = questManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        if (view.getTitle().equals(QuestGUI.CONFIRM_TITLE)) {
            if (event.getClickedInventory() != view.getTopInventory()) {
                return;
            }
            QuestGUI.handleConfirmWidgetClick(event, (Player) event.getWhoClicked());
            return;
        }
        if (!view.getTitle().equals(QuestGUI.GUI_TITLE)) return;
        if (event.getClickedInventory() != view.getTopInventory()) return;
        QuestGUI.handleWidgetClick(event, (Player) event.getWhoClicked(), questManager);
    }

    @EventHandler
    public void onClose(org.bukkit.event.inventory.InventoryCloseEvent e) {
        if (!e.getView().getTitle().equals(QuestGUI.CONFIRM_TITLE)) return;
        Player p = (Player) e.getPlayer();
        if (QuestGUI.hasPending(p.getUniqueId())) {
            QuestGUI.clearPending(p.getUniqueId());
            org.bukkit.Bukkit.getScheduler().runTask(
                    me.nakilex.levelplugin.Main.getInstance(),
                    () -> QuestGUI.openQuestGUI(p, questManager,
                            QuestGUI.pageMap.getOrDefault(p.getUniqueId(), 0)));
        }
    }
}
