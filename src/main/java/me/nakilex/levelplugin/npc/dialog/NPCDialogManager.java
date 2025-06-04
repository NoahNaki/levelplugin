package me.nakilex.levelplugin.npc.dialog;

import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles simple dialog sequences with NPCs.
 */
public class NPCDialogManager {

    private static class DialogSession {
        final Quest quest;
        final List<String> lines;
        final NPC npc;
        int index = 0;

        DialogSession(Quest quest, List<String> lines, NPC npc) {
            this.quest = quest;
            this.lines = lines;
            this.npc = npc;
        }
    }

    private final Map<UUID, DialogSession> sessions = new HashMap<>();

    public boolean hasSession(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    /** Start a dialog sequence for a quest. */
    public void startDialog(Player player, Quest quest, NPC npc) {
        List<String> lines = quest.getDialogLines();
        if (lines == null || lines.isEmpty()) return;
        player.setInvulnerable(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 60, 4, false, false, false));
        DialogSession session = new DialogSession(quest, lines, npc);
        sessions.put(player.getUniqueId(), session);
        sendLine(player, session);
    }

    /** Advance the dialog or accept the quest if finished. */
    public void advanceDialog(Player player, QuestManager questManager) {
        DialogSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        if (session.index >= session.lines.size()) {
            endDialog(player);
            questManager.startQuest(player, session.quest.getId());
            return;
        }

        sendLine(player, session);
    }

    private void sendLine(Player player, DialogSession session) {
        String line = session.lines.get(session.index);
        player.sendMessage(ChatColor.GOLD + "[" + (session.index + 1) + "/" + session.lines.size() + "] " + line);
        session.index++;
    }

    private void endDialog(Player player) {
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.setInvulnerable(false);
        sessions.remove(player.getUniqueId());
    }

    /** Cancel dialog if player walks too far from the NPC. */
    public void checkDistance(Player player, double maxDistanceSquared) {
        DialogSession session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (session.npc == null || !session.npc.isSpawned()) return;
        if (player.getLocation().distanceSquared(session.npc.getEntity().getLocation()) > maxDistanceSquared) {
            player.sendMessage(ChatColor.RED + "You walked away from the NPC.");
            endDialog(player);
        }
    }
}
