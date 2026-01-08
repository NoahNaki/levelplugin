package me.nakilex.levelplugin.quests.listeners;

import me.nakilex.levelplugin.quests.managers.QuestManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class QuestKillListener implements Listener {
    private static final String MYTHIC_PLUGIN = "MythicMobs";
    private static final String MYTHIC_BUKKIT_CLASS = "io.lumine.mythic.bukkit.MythicBukkit";
    private final QuestManager questManager;

    public QuestKillListener(QuestManager questManager) {
        this.questManager = questManager;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player)) return;
        Player killer = event.getEntity().getKiller();
        String mythicType = resolveMythicMobType(event.getEntity());
        String typeName = mythicType != null ? mythicType : event.getEntityType().name();
        questManager.handleKill(killer, typeName, mythicType != null);
    }

    private String resolveMythicMobType(Entity entity) {
        if (!Bukkit.getPluginManager().isPluginEnabled(MYTHIC_PLUGIN)) {
            return null;
        }
        try {
            Class<?> mythicClass = Class.forName(MYTHIC_BUKKIT_CLASS);
            Object mythic = mythicClass.getMethod("inst").invoke(null);
            Object apiHelper = mythic.getClass().getMethod("getAPIHelper").invoke(mythic);
            Object activeMob = apiHelper.getClass()
                    .getMethod("getMythicMobInstance", Entity.class)
                    .invoke(apiHelper, entity);
            if (activeMob == null) {
                return null;
            }
            Object mobType = activeMob.getClass().getMethod("getMobType").invoke(activeMob);
            return mobType != null ? mobType.toString() : null;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }
}
