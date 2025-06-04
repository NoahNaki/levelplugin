package me.nakilex.levelplugin.quests.tasks;

import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class QuestNPCEffectTask extends BukkitRunnable {
    private final QuestManager questManager;

    public QuestNPCEffectTask(QuestManager questManager) {
        this.questManager = questManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (var entry : questManager.getNpcQuestMap().entrySet()) {
                NPC npc = CitizensAPI.getNPCRegistry().getById(entry.getKey());
                if (npc == null || !npc.isSpawned()) continue;
                if (!npc.getEntity().getWorld().equals(player.getWorld())) continue;
                if (player.getLocation().distanceSquared(npc.getEntity().getLocation()) > 100) continue;
                var quest = questManager.getQuest(entry.getValue());
                QuestState state = questManager.getQuestState(player, quest);
                if (state == QuestState.AVAILABLE || state == QuestState.TURN_IN_READY) {
                    player.spawnParticle(Particle.VILLAGER_HAPPY, npc.getEntity().getLocation().add(0,2,0), 1, 0,0,0,0);
                }
            }
        }
    }
}
