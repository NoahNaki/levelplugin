package me.nakilex.levelplugin.quests.tasks;

import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import me.nakilex.levelplugin.Main;
import org.bukkit.Particle;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.Location;
import org.bukkit.Color;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class QuestNPCEffectTask extends BukkitRunnable {
    private final QuestManager questManager;
    private final Map<UUID, Map<Integer, TextDisplay>> glyphs = new HashMap<>();

    public QuestNPCEffectTask(QuestManager questManager) {
        this.questManager = questManager;
    }

    @Override
    public void run() {
        // Clean up glyphs for offline players
        glyphs.entrySet().removeIf(entry -> {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null) {
                for (TextDisplay td : entry.getValue().values()) {
                    if (td != null && !td.isDead()) td.remove();
                }
                return true;
            }
            return false;
        });

        for (Player player : Bukkit.getOnlinePlayers()) {
            Map<Integer, TextDisplay> map = glyphs.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());

            for (var entry : questManager.getNpcQuestMap().entrySet()) {
                int npcId = entry.getKey();
                NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);
                TextDisplay disp = map.get(npcId);

                if (npc == null || !npc.isSpawned() || !npc.getEntity().getWorld().equals(player.getWorld())
                        || player.getLocation().distanceSquared(npc.getEntity().getLocation()) > 100) {
                    if (disp != null) {
                        disp.remove();
                        map.remove(npcId);
                    }
                    continue;
                }

                var quest = questManager.getQuest(entry.getValue());
                QuestState state = questManager.getQuestState(player, quest);
                if (state == QuestState.AVAILABLE || state == QuestState.TURN_IN_READY) {
                    player.spawnParticle(Particle.HAPPY_VILLAGER, npc.getEntity().getLocation().add(0, 2, 0), 1, 0, 0, 0, 0);

                    Location loc = npc.getEntity().getLocation().add(0, 2.4, 0);
                    if (disp == null || disp.isDead()) {
                        disp = (TextDisplay) npc.getEntity().getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
                        disp.setBillboard(Display.Billboard.CENTER);
                        disp.setShadowRadius(0f);
                        disp.setShadowStrength(0f);
                        disp.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
                        disp.setText("<glyph:info>");
                        map.put(npcId, disp);
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            if (!p.equals(player)) {
                                p.hideEntity(Main.getInstance(), disp);
                            }
                        }
                    } else {
                        disp.teleport(loc);
                        if (!player.canSee(disp)) {
                            player.showEntity(Main.getInstance(), disp);
                        }
                    }
                } else if (disp != null) {
                    disp.remove();
                    map.remove(npcId);
                }
            }

            // Remove glyphs for NPCs no longer tracked
            map.entrySet().removeIf(e -> {
                if (!questManager.getNpcQuestMap().containsKey(e.getKey())) {
                    TextDisplay td = e.getValue();
                    if (td != null && !td.isDead()) td.remove();
                    return true;
                }
                return false;
            });
        }
    }
}
