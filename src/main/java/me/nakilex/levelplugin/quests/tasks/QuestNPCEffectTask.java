package me.nakilex.levelplugin.quests.tasks;

import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.QuestObjective;
import me.nakilex.levelplugin.quests.data.QuestObjectiveType;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class QuestNPCEffectTask extends BukkitRunnable {
    private final QuestManager questManager;
    private final Map<UUID, Map<Integer, TextDisplay>> glyphs = new HashMap<>();

    private static final Map<String, String> NAME_GLYPHS = Map.of(
            "blacksmith", "<glyph:anvil>",
            "enchanter", "<glyph:enchanter>",
            "storage manager", "<glyph:banker>",
            "auction house", "<glyph:auctionhouse>",
            "stable keeper", "<glyph:horse>",
            "salvager", "<glyph:scrapper>");

    /** Vertical offset for NPC glyph displays. */
    private static final double GLYPH_Y_OFFSET = 2.8;

    /** Horizontal offsets for specific glyphs to fine-tune centering. */
    private static final Map<String, Double> GLYPH_X_OFFSETS = Map.of(
            "<glyph:alert>", 0.15
    );

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

        Set<NPC> relevant = new HashSet<>();
        for (int npcId : questManager.getNpcQuestMap().keySet()) {
            NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);
            if (npc != null) relevant.add(npc);
        }
        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            if (getServiceGlyph(npc.getName().toLowerCase()) != null) {
                relevant.add(npc);
            }
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            Map<Integer, TextDisplay> map = glyphs.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
            Set<Integer> processed = new HashSet<>();

            for (NPC npc : relevant) {
                int npcId = npc.getId();
                TextDisplay disp = map.get(npcId);

                if (npc == null || !npc.isSpawned() || !npc.getEntity().getWorld().equals(player.getWorld())
                        || player.getLocation().distanceSquared(npc.getEntity().getLocation()) > 100) {
                    if (disp != null) {
                        disp.remove();
                        map.remove(npcId);
                    }
                    continue;
                }

                String glyph = getGlyph(player, npc);

                if (glyph != null) {
                    updateDisplay(player, npc, npcId, glyph, disp, map);
                    processed.add(npcId);
                } else if (disp != null) {
                    disp.remove();
                    map.remove(npcId);
                }
            }

            map.entrySet().removeIf(e -> {
                if (!processed.contains(e.getKey())) {
                    TextDisplay td = e.getValue();
                    if (td != null && !td.isDead()) td.remove();
                    return true;
                }
                return false;
            });
        }
    }

    private String getGlyph(Player player, NPC npc) {
        Quest quest = questManager.getQuestByNpc(npc);
        if (quest != null) {
            QuestState state = questManager.getQuestState(player, quest);
            if (state == QuestState.AVAILABLE) {
                return "<glyph:info>";
            } else if (state == QuestState.ACCEPTED || state == QuestState.IN_PROGRESS || state == QuestState.TURN_IN_READY) {
                PlayerQuestProgress prog = questManager.getProgress(player.getUniqueId(), quest.getId());
                int idx = 0;
                if (prog != null) {
                    for (int i = 0; i < quest.getObjectives().size(); i++) {
                        if (prog.getProgress(i) < quest.getObjectives().get(i).getAmount()) {
                            idx = i;
                            break;
                        }
                    }
                }
                QuestObjective obj = quest.getObjectives().get(idx);
                if (questManager.isTalkObjectiveForNpc(obj, npc)) {
                    boolean last = idx == quest.getObjectives().size() - 1;
                    return last ? "<glyph:check>" : "<glyph:alert>";
                } else if (state == QuestState.TURN_IN_READY) {
                    return "<glyph:check>";
                }
            }
        }
        return getServiceGlyph(npc.getName().toLowerCase());
    }

    private void updateDisplay(Player player, NPC npc, int npcId, String glyph, TextDisplay disp, Map<Integer, TextDisplay> map) {
        if (getServiceGlyph(npc.getName().toLowerCase()) == null) {
            player.spawnParticle(Particle.HAPPY_VILLAGER, npc.getEntity().getLocation().add(0, 2, 0), 1, 0, 0, 0, 0);
        }

        double xOffset = GLYPH_X_OFFSETS.getOrDefault(glyph, 0.0);
        Location loc = npc.getEntity().getLocation().add(xOffset, GLYPH_Y_OFFSET, 0);
        if (disp == null || disp.isDead()) {
            disp = (TextDisplay) npc.getEntity().getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
            disp.setBillboard(Display.Billboard.CENTER);
            disp.setShadowRadius(0f);
            disp.setShadowStrength(0f);
            disp.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            disp.setText(glyph);
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
            if (!glyph.equals(disp.getText())) {
                disp.setText(glyph);
            }
        }
    }

    private static String getServiceGlyph(String lowerName) {
        String glyph = NAME_GLYPHS.get(lowerName);
        if (glyph != null) return glyph;
        return lowerName.contains("merchant") ? " \n<glyph:market>" : null;
    }

    public void clearGlyphs() {
        for (Map<Integer, TextDisplay> map : glyphs.values()) {
            for (TextDisplay td : map.values()) {
                if (td != null && !td.isDead()) {
                    td.remove();
                }
            }
        }
        glyphs.clear();
    }
}
