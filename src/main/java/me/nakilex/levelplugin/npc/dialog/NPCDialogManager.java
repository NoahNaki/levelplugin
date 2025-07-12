package me.nakilex.levelplugin.npc.dialog;

import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import me.nakilex.levelplugin.utils.ChatFormatter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.HandlerList;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.function.Consumer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles simple dialog sequences with NPCs.
 */
public class NPCDialogManager implements Listener {

    private final Main plugin;

    public NPCDialogManager(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private static class DialogSession {
        final Quest quest;
        final List<String> lines;
        final NPC npc;
        final Runnable finish;
        int index = 0;
        boolean paused = false;

        DialogSession(Quest quest, List<String> lines, NPC npc, Runnable finish) {
            this.quest = quest;
            this.lines = lines;
            this.npc = npc;
            this.finish = finish;
        }
    }

    private final Map<UUID, DialogSession> sessions = new HashMap<>();

    public NPC getSessionNpc(Player player) {
        DialogSession s = sessions.get(player.getUniqueId());
        return s != null ? s.npc : null;
    }

    private static class ChoiceSession {
        final NPC npc;
        final List<String> options;
        int index = 0;
        final Consumer<Integer> callback;
        final Listener listener;

        ChoiceSession(NPC npc, List<String> options, Consumer<Integer> callback, Listener listener) {
            this.npc = npc;
            this.options = options;
            this.callback = callback;
            this.listener = listener;
        }
    }

    private final Map<UUID, ChoiceSession> choiceSessions = new HashMap<>();

    public boolean hasSession(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public boolean hasChoiceSession(Player player) {
        return choiceSessions.containsKey(player.getUniqueId());
    }

    /** Start a dialog sequence for a quest. */
    public void startDialog(Player player, Quest quest, NPC npc) {
        List<String> lines = quest.getDialogLines();
        if (lines == null || lines.isEmpty()) return;
        player.setInvulnerable(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 60, 4, false, false, false));
        DialogSession session = new DialogSession(quest, lines, npc, null);
        sessions.put(player.getUniqueId(), session);
        sendLine(player, session);
    }

    /** Start a dialog sequence with custom lines and finish callback. */
    public void startDialog(Player player, List<String> lines, NPC npc, Runnable finish) {
        if (lines == null || lines.isEmpty()) return;
        player.setInvulnerable(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 60, 4, false, false, false));
        DialogSession session = new DialogSession(null, lines, npc, finish);
        sessions.put(player.getUniqueId(), session);
        sendLine(player, session);
    }

    /** Present a choice dialog to the player using the scroll wheel. */
    public void startChoiceDialog(Player player, NPC npc, List<String> options, Consumer<Integer> callback) {
        if (options == null || options.isEmpty()) return;
        if (hasChoiceSession(player)) return;

        ChoiceSession[] ref = new ChoiceSession[1];
        Listener listener = new Listener() {
            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
            public void onScroll(PlayerItemHeldEvent e) {
                if (!e.getPlayer().equals(player)) return;
                e.setCancelled(true);
                ChoiceSession cs = ref[0];
                if (cs == null) return;
                if (e.getNewSlot() > e.getPreviousSlot()) cs.index++; else cs.index--;
                if (cs.index < 0) cs.index = cs.options.size() - 1;
                if (cs.index >= cs.options.size()) cs.index = 0;
                sendChoice(player, cs);
            }

            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
            public void onInteract(PlayerInteractEntityEvent e) {
                if (!e.getPlayer().equals(player)) return;
                if (!CitizensAPI.getNPCRegistry().isNPC(e.getRightClicked())) return;
                NPC n = CitizensAPI.getNPCRegistry().getNPC(e.getRightClicked());
                if (npc != null && n.getId() != npc.getId()) return;
                e.setCancelled(true);
                finishChoice(player, ref[0]);
            }

            @EventHandler
            public void onQuit(PlayerQuitEvent e) {
                if (e.getPlayer().equals(player)) {
                    // Treat quitting like walking away so the dialog can resume
                    pauseDialog(player);
                }
            }
        };
        ChoiceSession cs = new ChoiceSession(npc, options, callback, listener);
        ref[0] = cs;
        choiceSessions.put(player.getUniqueId(), cs);
        Bukkit.getPluginManager().registerEvents(listener, me.nakilex.levelplugin.Main.getInstance());
        sendChoice(player, cs);
    }

    /** Advance the dialog or accept the quest if finished. */
    public void advanceDialog(Player player, QuestManager questManager) {
        DialogSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        if (session.paused) {
            session.paused = false;
            player.setInvulnerable(true);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 60, 4, false, false, false));
        }

        if (session.index >= session.lines.size()) {
            endDialog(player);
            if (session.quest != null) {
                questManager.startQuest(player, session.quest.getId());
            }
            if (session.finish != null) {
                session.finish.run();
            }
            return;
        }

        sendLine(player, session);
    }

    private void sendLine(Player player, DialogSession session) {
        String raw = session.lines.get(session.index);
        String speaker = session.npc.getName();
        String line = raw;
        int bar = raw.indexOf('|');
        if (bar >= 0) {
            speaker = raw.substring(0, bar);
            line = raw.substring(bar + 1);
            if ("<player>".equalsIgnoreCase(speaker)) {
                speaker = player.getName();
            }
        }
        if (session.index == 0) {
            ChatFormatter.constructDivider(player, " ", 45);
        }
        String msg = ChatColor.DARK_GRAY + "[" + ChatColor.GRAY + (session.index + 1)
                + "/" + session.lines.size() + ChatColor.DARK_GRAY + "] "
                + ChatColor.YELLOW + speaker
                + ChatColor.WHITE + ": " + line;
        player.sendMessage(msg);
        ChatFormatter.constructDivider(player, " ", 45);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        session.index++;
    }

    private void endDialog(Player player) {
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.setInvulnerable(false);
        sessions.remove(player.getUniqueId());
    }

    private void pauseDialog(Player player) {
        DialogSession session = sessions.get(player.getUniqueId());
        if (session == null) return;
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.setInvulnerable(false);
        session.paused = true;
        cancelChoice(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pauseDialog(event.getPlayer());
    }

    /** Remove an active choice dialog without triggering its callback. */
    private void cancelChoice(Player player) {
        ChoiceSession cs = choiceSessions.remove(player.getUniqueId());
        if (cs != null) {
            HandlerList.unregisterAll(cs.listener);
        }
    }

    private void sendChoice(Player player, ChoiceSession cs) {
        ChatFormatter.sendCenteredMessage(player, ChatColor.AQUA + "Choose your answer:");
        ChatFormatter.constructDivider(player, " ", 45);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cs.options.size(); i++) {
            if (i > 0) sb.append(ChatColor.GRAY).append(" / ");
            ChatColor col = i == cs.index ? ChatColor.GREEN : ChatColor.WHITE;
            sb.append(ChatColor.DARK_GRAY).append("[")
                    .append(col).append(i == cs.index ? ChatColor.UNDERLINE : "")
                    .append(cs.options.get(i))
                    .append(ChatColor.DARK_GRAY).append("]");
        }
        ChatFormatter.sendCenteredMessage(player, sb.toString());
        ChatFormatter.sendCenteredMessage(player, ChatColor.GRAY + "(Scroll to cycle)");
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    private void finishChoice(Player player, ChoiceSession cs) {
        if (cs == null) return;
        HandlerList.unregisterAll(cs.listener);
        choiceSessions.remove(player.getUniqueId());
        if (cs.callback != null) {
            cs.callback.accept(cs.index);
        }
    }

    /** Cancel dialog if player walks too far from the NPC. */
    public void checkDistance(Player player, double maxDistanceSquared) {
        DialogSession session = sessions.get(player.getUniqueId());
        if (session != null) {
            if (session.paused) return;
            if (session.npc != null && session.npc.isSpawned() &&
                    player.getLocation().distanceSquared(session.npc.getEntity().getLocation()) > maxDistanceSquared) {
                player.sendMessage(ChatColor.RED + "You walked away from the NPC. Right-click again to continue.");
                pauseDialog(player);
                return;
            }
        }

        ChoiceSession cs = choiceSessions.get(player.getUniqueId());
        if (cs != null && cs.npc != null && cs.npc.isSpawned() &&
                player.getLocation().distanceSquared(cs.npc.getEntity().getLocation()) > maxDistanceSquared) {
            player.sendMessage(ChatColor.RED + "You walked away from the NPC. Right-click again to continue.");
            cancelChoice(player);
        }
    }
}
