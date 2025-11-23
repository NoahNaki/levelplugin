package me.nakilex.levelplugin.quests.listeners;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.nakilex.levelplugin.quests.managers.QuestManager;
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
        ActiveMob mob = MythicBukkit.inst().getAPIHelper().getMythicMobInstance(event.getEntity());
        String typeName = mob != null ? mob.getMobType() : event.getEntityType().name();
        questManager.handleKill(killer, typeName, mob != null);
    }
}
