package me.nakilex.levelplugin.quests.listeners;

import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.mob.utils.MobNameUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class QuestKillListener implements Listener {
    private final QuestManager questManager;

    public QuestKillListener(QuestManager questManager) {
        this.questManager = questManager;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player)) return;
        Player killer = event.getEntity().getKiller();
        String customId = MobNameUtil.resolveCustomMobId(event.getEntity()).orElse(null);
        String typeName = customId != null ? customId : event.getEntityType().name();
        questManager.handleKill(killer, typeName, customId != null);
    }
}
