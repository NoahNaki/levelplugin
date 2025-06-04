package me.nakilex.levelplugin.quests.listeners;

import me.nakilex.levelplugin.quests.managers.QuestManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;

public class QuestCraftListener implements Listener {
    private final QuestManager questManager;

    public QuestCraftListener(QuestManager questManager) {
        this.questManager = questManager;
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String type = event.getRecipe().getResult().getType().name();
        questManager.handleCraft(player, type);
    }
}
