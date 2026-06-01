package me.nakilex.levelplugin.npc.dialog;

import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.npc.system.NpcApi;
import me.nakilex.levelplugin.npc.system.NPC;
import me.nakilex.levelplugin.Main;
import net.citizensnpcs.api.CitizensAPI;
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
import org.bukkit.scheduler.BukkitTask;
import java.util.function.Consumer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;

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
        final net.citizensnpcs.api.npc.NPC citizensNpc;
        final Runnable finish;
        int index = 0;
        boolean paused = false;

        DialogSession(Quest quest, List<String> lines, NPC npc, Runnable finish) {
            this(quest, lines, npc, null, finish);
        }

        DialogSession(Quest quest, List<String> lines, net.citizensnpcs.api.npc.NPC citizensNpc, Runnable finish) {
            this(quest, lines, null, citizensNpc, finish);
        }

        DialogSession(Quest quest, List<String> lines, NPC npc, net.citizensnpcs.api.npc.NPC citizensNpc, Runnable finish) {
            this.quest = quest;
            this.lines = lines;
            this.npc = npc;
            this.citizensNpc = citizensNpc;
            this.finish = finish;
        }
    }

    private final Map<UUID, DialogSession> sessions = new HashMap<>();
    private final Map<UUID, String> lastLines = new HashMap<>();
    private final Map<UUID, BukkitTask> resumeTasks = new HashMap<>();
    private final Map<UUID, Long> dialogCooldowns = new HashMap<>();

    private static final long SKILL_DELAY_MS = 500L;

    public NPC getSessionNpc(Player player) {
        DialogSession s = sessions.get(player.getUniqueId());
        return s != null ? s.npc : null;
    }

    public Integer getSessionNpcId(Player player) {
        DialogSession s = sessions.get(player.getUniqueId());
        if (s == null) {
            return null;
        }
        if (s.npc != null) {
            return s.npc.getId();
        }
        if (s.citizensNpc != null) {
            return s.citizensNpc.getId();
        }
        return null;
    }

    public boolean isSessionNpc(Player player, int npcId) {
        Integer sessionNpcId = getSessionNpcId(player);
        return sessionNpcId != null && sessionNpcId == npcId;
    }

    private static class ChoiceSession {
        final NPC npc;
        final net.citizensnpcs.api.npc.NPC citizensNpc;
        final List<String> options;
        int index = 0;
        final Consumer<Integer> callback;
        final Listener listener;
        String questId;
        String flagBase;
        String resumeLine;
        final long openedAt;

        ChoiceSession(NPC npc, List<String> options, Consumer<Integer> callback, Listener listener) {
            this(npc, null, options, callback, listener);
        }

        ChoiceSession(net.citizensnpcs.api.npc.NPC citizensNpc, List<String> options, Consumer<Integer> callback, Listener listener) {
            this(null, citizensNpc, options, callback, listener);
        }

        ChoiceSession(NPC npc, net.citizensnpcs.api.npc.NPC citizensNpc, List<String> options,
                      Consumer<Integer> callback, Listener listener) {
            this.npc = npc;
            this.citizensNpc = citizensNpc;
            this.options = options;
            this.callback = callback;
            this.listener = listener;
            this.openedAt = System.currentTimeMillis();
        }
    }

    private final Map<UUID, ChoiceSession> choiceSessions = new HashMap<>();
    private static class PendingChoice {
        final NPC npc;
        final net.citizensnpcs.api.npc.NPC citizensNpc;
        final List<String> options;
        final Consumer<Integer> callback;
        final String questId;
        final String flagBase;
        final String resumeLine;

        PendingChoice(NPC npc, List<String> options, Consumer<Integer> callback, String questId, String flagBase, String resumeLine) {
            this(npc, null, options, callback, questId, flagBase, resumeLine);
        }

        PendingChoice(net.citizensnpcs.api.npc.NPC citizensNpc, List<String> options, Consumer<Integer> callback,
                      String questId, String flagBase, String resumeLine) {
            this(null, citizensNpc, options, callback, questId, flagBase, resumeLine);
        }

        PendingChoice(NPC npc, net.citizensnpcs.api.npc.NPC citizensNpc, List<String> options, Consumer<Integer> callback,
                      String questId, String flagBase, String resumeLine) {
            this.npc = npc;
            this.citizensNpc = citizensNpc;
            this.options = options;
            this.callback = callback;
            this.questId = questId;
            this.flagBase = flagBase;
            this.resumeLine = resumeLine;
        }
    }

    private final Map<UUID, PendingChoice> pendingChoices = new HashMap<>();

    public boolean hasSession(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    /**
     * True if the player is currently in dialog or within the short cooldown window
     * after interacting with an NPC. Used to prevent accidental skill triggers.
     */
    public boolean isDialogLockActive(Player player) {
        var questDialogueManager = Main.getInstance().getQuestDialogueManager();
        if (hasSession(player) || (questDialogueManager != null && questDialogueManager.hasSession(player))) {
            return true;
        }
        UUID id = player.getUniqueId();
        Long last = dialogCooldowns.get(id);
        if (last == null) {
            return false;
        }
        if (System.currentTimeMillis() - last < SKILL_DELAY_MS) {
            return true;
        }
        dialogCooldowns.remove(id);
        return false;
    }

    public boolean hasChoiceSession(Player player) {
        return choiceSessions.containsKey(player.getUniqueId());
    }

    /** Start a dialog sequence for a quest. */
    public void startDialog(Player player, Quest quest, NPC npc) {
        List<String> lines = quest.getDialogLines();
        if (lines == null || lines.isEmpty()) return;
        if (plugin.getQuestManager().isDebug()) {
            plugin.getLogger().info("[DialogDebug] startDialog quest=" + quest.getId() +
                    " player=" + player.getName());
        }
        player.setInvulnerable(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 60, 4, false, false, false));
        DialogSession session = new DialogSession(quest, lines, npc, null);
        sessions.put(player.getUniqueId(), session);
        sendLine(player, session);
    }

    public void startDialog(Player player, Quest quest, net.citizensnpcs.api.npc.NPC npc) {
        List<String> lines = quest.getDialogLines();
        if (lines == null || lines.isEmpty()) return;
        if (plugin.getQuestManager().isDebug()) {
            plugin.getLogger().info("[DialogDebug] startDialog quest=" + quest.getId() +
                    " player=" + player.getName());
        }
        player.setInvulnerable(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 60, 4, false, false, false));
        DialogSession session = new DialogSession(quest, lines, npc, null);
        sessions.put(player.getUniqueId(), session);
        sendLine(player, session);
    }

    /** Start a dialog sequence with custom lines and finish callback. */
    public void startDialog(Player player, List<String> lines, NPC npc, Runnable finish) {
        if (lines == null || lines.isEmpty()) return;
        if (plugin.getQuestManager().isDebug()) {
            plugin.getLogger().info("[DialogDebug] startDialog custom player=" + player.getName());
        }
        player.setInvulnerable(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 60, 4, false, false, false));
        DialogSession session = new DialogSession(null, lines, npc, finish);
        sessions.put(player.getUniqueId(), session);
        sendLine(player, session);
    }

    public void startDialog(Player player, List<String> lines, net.citizensnpcs.api.npc.NPC npc, Runnable finish) {
        if (lines == null || lines.isEmpty()) return;
        if (plugin.getQuestManager().isDebug()) {
            plugin.getLogger().info("[DialogDebug] startDialog custom player=" + player.getName());
        }
        player.setInvulnerable(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 60, 4, false, false, false));
        DialogSession session = new DialogSession(null, lines, npc, finish);
        sessions.put(player.getUniqueId(), session);
        sendLine(player, session);
    }

    /**
     * Present a choice dialog to the player using the scroll wheel.
     *
     * @param questId   optional quest identifier for flag tracking
     * @param flagBase  if questId is supplied, choice index will be appended to this and stored as a flag
     */
    public void startChoiceDialog(Player player, NPC npc, List<String> options, String questId, String flagBase, Consumer<Integer> callback) {
        if (options == null || options.isEmpty()) return;
        if (hasChoiceSession(player)) return;
        if (plugin.getQuestManager().isDebug()) {
            plugin.getLogger().info("[DialogDebug] startChoiceDialog player=" + player.getName() +
                    " quest=" + questId + " flagBase=" + flagBase);
        }
        if (questId != null && flagBase != null) {
            String pendingFlag = flagBase + "pending";
            plugin.getQuestManager().setFlag(player.getUniqueId(), questId, pendingFlag);
            String last = lastLines.get(player.getUniqueId());
            pendingChoices.put(player.getUniqueId(), new PendingChoice(npc, options, callback, questId, flagBase, last));
        }

        // Keep the player locked in place while making the choice
        player.setInvulnerable(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 60, 4, false, false, false));

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
                NPC n = NpcApi.getRegistry().getNPC(e.getRightClicked());
                net.citizensnpcs.api.npc.NPC citizens = CitizensAPI.getNPCRegistry().getNPC(e.getRightClicked());
                if (npc != null) {
                    if (n != null && n.getId() == npc.getId()) {
                        // matched LevelPlugin NPC
                    } else if (citizens != null && citizens.getId() == npc.getId()) {
                        // matched Citizens NPC with same id
                    } else {
                        return;
                    }
                } else if (citizens != null) {
                    return;
                }
                e.setCancelled(true);
                finishChoice(player, ref[0]);
            }

            @EventHandler
            public void onQuit(PlayerQuitEvent e) {
                if (e.getPlayer().equals(player)) {
                    // Treat quitting like walking away so the dialog can resume
                    if (plugin.getQuestManager().isDebug()) {
                        plugin.getLogger().info("[DialogDebug] player quit during choice " + player.getName());
                    }
                    pauseDialog(player);
                }
            }
        };
        ChoiceSession cs = new ChoiceSession(npc, options, callback, listener);
        cs.questId = questId;
        cs.flagBase = flagBase;
        ref[0] = cs;
        choiceSessions.put(player.getUniqueId(), cs);
        Bukkit.getPluginManager().registerEvents(listener, me.nakilex.levelplugin.Main.getInstance());
        sendChoice(player, cs);
    }

    public void startChoiceDialog(Player player, net.citizensnpcs.api.npc.NPC npc, List<String> options,
                                  String questId, String flagBase, Consumer<Integer> callback) {
        if (options == null || options.isEmpty()) return;
        if (hasChoiceSession(player)) return;
        if (plugin.getQuestManager().isDebug()) {
            plugin.getLogger().info("[DialogDebug] startChoiceDialog player=" + player.getName() +
                    " quest=" + questId + " flagBase=" + flagBase);
        }
        if (questId != null && flagBase != null) {
            String pendingFlag = flagBase + "pending";
            plugin.getQuestManager().setFlag(player.getUniqueId(), questId, pendingFlag);
            String last = lastLines.get(player.getUniqueId());
            pendingChoices.put(player.getUniqueId(), new PendingChoice(npc, options, callback, questId, flagBase, last));
        }

        player.setInvulnerable(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 60, 4, false, false, false));

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
                NPC n = NpcApi.getRegistry().getNPC(e.getRightClicked());
                net.citizensnpcs.api.npc.NPC citizens = CitizensAPI.getNPCRegistry().getNPC(e.getRightClicked());
                if (npc != null) {
                    if (citizens != null && citizens.getId() == npc.getId()) {
                        // matched Citizens NPC
                    } else if (n != null && n.getId() == npc.getId()) {
                        // matched LevelPlugin NPC with same id
                    } else {
                        return;
                    }
                } else if (citizens != null) {
                    return;
                }
                e.setCancelled(true);
                finishChoice(player, ref[0]);
            }

            @EventHandler
            public void onQuit(PlayerQuitEvent e) {
                if (e.getPlayer().equals(player)) {
                    if (plugin.getQuestManager().isDebug()) {
                        plugin.getLogger().info("[DialogDebug] player quit during choice " + player.getName());
                    }
                    pauseDialog(player);
                }
            }
        };
        ChoiceSession cs = new ChoiceSession(npc, options, callback, listener);
        cs.questId = questId;
        cs.flagBase = flagBase;
        ref[0] = cs;
        choiceSessions.put(player.getUniqueId(), cs);
        Bukkit.getPluginManager().registerEvents(listener, me.nakilex.levelplugin.Main.getInstance());
        sendChoice(player, cs);
    }

    /** Convenience overload for backwards compatibility. */
    public void startChoiceDialog(Player player, NPC npc, List<String> options, Consumer<Integer> callback) {
        startChoiceDialog(player, npc, options, null, null, callback);
    }

    public void startChoiceDialog(Player player, net.citizensnpcs.api.npc.NPC npc, List<String> options,
                                  Consumer<Integer> callback) {
        startChoiceDialog(player, npc, options, null, null, callback);
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
            if (session.npc != null) {
                Main.getInstance().getCodexManager().recordNpc(player,
                        org.bukkit.ChatColor.stripColor(session.npc.getName()));
            } else if (session.citizensNpc != null) {
                Main.getInstance().getCodexManager().recordNpc(player,
                        org.bukkit.ChatColor.stripColor(session.citizensNpc.getName()));
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
        lastLines.put(player.getUniqueId(), raw);
        String speaker = session.npc != null ? session.npc.getName()
                : session.citizensNpc != null ? session.citizensNpc.getName()
                : "NPC";
        String line = raw;
        int bar = raw.indexOf('|');
        if (bar >= 0) {
            speaker = raw.substring(0, bar);
            line = raw.substring(bar + 1);
            if ("<player>".equalsIgnoreCase(speaker)) {
                speaker = player.getName();
            }
        }
        line = line.replaceAll("(?i)<player>", Matcher.quoteReplacement(player.getName()));

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
        recordDialogCooldown(player);
        sessions.remove(player.getUniqueId());
    }

    private void pauseDialog(Player player) {
        DialogSession session = sessions.get(player.getUniqueId());
        if (session == null) return;
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.setInvulnerable(false);
        session.paused = true;
        cancelChoice(player);
        BukkitTask task = resumeTasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
        if (plugin.getQuestManager().isDebug()) {
            plugin.getLogger().info("[DialogDebug] paused dialog for " + player.getName());
        }
    }

    /**
     * Remove any active dialog session entirely, discarding progress.
     */
    public void resetDialog(Player player) {
        cancelChoice(player);
        sessions.remove(player.getUniqueId());
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.setInvulnerable(false);
        recordDialogCooldown(player);
        lastLines.remove(player.getUniqueId());
        pendingChoices.remove(player.getUniqueId());
        BukkitTask task = resumeTasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
        if (plugin.getQuestManager().isDebug()) {
            plugin.getLogger().info("[DialogDebug] reset dialog for " + player.getName());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (plugin.getQuestManager().isDebug()) {
            plugin.getLogger().info("[DialogDebug] player quit " + event.getPlayer().getName());
        }
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
        if (System.currentTimeMillis() - cs.openedAt < 400) {
            sendChoice(player, cs);
            return;
        }
        HandlerList.unregisterAll(cs.listener);
        choiceSessions.remove(player.getUniqueId());
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.setInvulnerable(false);
        recordDialogCooldown(player);
        BukkitTask pending = resumeTasks.remove(player.getUniqueId());
        if (pending != null) pending.cancel();
        if (plugin.getQuestManager().isDebug()) {
            plugin.getLogger().info("[DialogDebug] finishChoice player=" + player.getName() +
                    " quest=" + cs.questId + " choice=" + cs.index);
        }
        if (cs.questId != null && cs.flagBase != null) {
            QuestManager qm = plugin.getQuestManager();
            qm.removeFlag(player.getUniqueId(), cs.questId, cs.flagBase + "pending");
            qm.setFlag(player.getUniqueId(), cs.questId, cs.flagBase + cs.index);
            pendingChoices.remove(player.getUniqueId());
        }
        if (cs.callback != null) {
            cs.callback.accept(cs.index);
        }
    }

    /**
     * Apply the short skill delay used to prevent accidental casts while interacting
     * with NPC dialogs or menus.
     */
    public void recordDialogCooldown(Player player) {
        dialogCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * If the player has a pending choice for the given NPC, replay the last line
     * and reopen the choice dialog.
     *
     * @return true if a pending choice was resumed
     */
    public boolean resumePendingChoice(Player player, NPC npc) {
        PendingChoice pc = pendingChoices.get(player.getUniqueId());
        if (pc == null) {
            if (plugin.getQuestManager().isDebug()) {
                plugin.getLogger().info("[DialogDebug] no pending choice stored for " + player.getName());
            }
            return false;
        }
        if (npc != null && pc.npc != null && pc.npc.getId() != npc.getId()) {
            if (plugin.getQuestManager().isDebug()) {
                plugin.getLogger().info("[DialogDebug] pending choice NPC mismatch for " + player.getName());
            }
            return false;
        }
        if (npc != null && pc.npc == null && pc.citizensNpc != null && pc.citizensNpc.getId() != npc.getId()) {
            if (plugin.getQuestManager().isDebug()) {
                plugin.getLogger().info("[DialogDebug] pending choice NPC mismatch for " + player.getName());
            }
            return false;
        }
        if (pc.questId != null) {
            QuestManager qm = plugin.getQuestManager();
            if (!qm.hasFlag(player.getUniqueId(), pc.questId, pc.flagBase + "pending")) {
                pendingChoices.remove(player.getUniqueId());
                if (plugin.getQuestManager().isDebug()) {
                    plugin.getLogger().info("[DialogDebug] pending choice flag missing for " + player.getName());
                }
                return false;
            }
        }

        if (plugin.getQuestManager().isDebug()) {
            plugin.getLogger().info("[DialogDebug] resuming pending choice for " + player.getName());
        }

        String line = pc.resumeLine;
        if (line != null) {
            startDialog(player, java.util.List.of(line), npc, null);
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (hasSession(player)) {
                    advanceDialog(player, plugin.getQuestManager());
                } else {
                    startChoiceDialog(player, npc, pc.options, pc.questId, pc.flagBase, pc.callback);
                }
                resumeTasks.remove(player.getUniqueId());
            }, 1L);
            BukkitTask old = resumeTasks.put(player.getUniqueId(), task);
            if (old != null) old.cancel();
        } else {
            startChoiceDialog(player, npc, pc.options, pc.questId, pc.flagBase, pc.callback);
        }
        return true;
    }

    public boolean resumePendingChoice(Player player, net.citizensnpcs.api.npc.NPC npc) {
        PendingChoice pc = pendingChoices.get(player.getUniqueId());
        if (pc == null) {
            if (plugin.getQuestManager().isDebug()) {
                plugin.getLogger().info("[DialogDebug] no pending choice stored for " + player.getName());
            }
            return false;
        }
        if (npc != null && pc.citizensNpc != null && pc.citizensNpc.getId() != npc.getId()) {
            if (plugin.getQuestManager().isDebug()) {
                plugin.getLogger().info("[DialogDebug] pending choice NPC mismatch for " + player.getName());
            }
            return false;
        }
        if (npc != null && pc.citizensNpc == null && pc.npc != null && pc.npc.getId() != npc.getId()) {
            if (plugin.getQuestManager().isDebug()) {
                plugin.getLogger().info("[DialogDebug] pending choice NPC mismatch for " + player.getName());
            }
            return false;
        }
        if (pc.questId != null) {
            QuestManager qm = plugin.getQuestManager();
            if (!qm.hasFlag(player.getUniqueId(), pc.questId, pc.flagBase + "pending")) {
                pendingChoices.remove(player.getUniqueId());
                if (plugin.getQuestManager().isDebug()) {
                    plugin.getLogger().info("[DialogDebug] pending choice flag missing for " + player.getName());
                }
                return false;
            }
        }

        if (plugin.getQuestManager().isDebug()) {
            plugin.getLogger().info("[DialogDebug] resuming pending choice for " + player.getName());
        }

        String line = pc.resumeLine;
        if (line != null) {
            startDialog(player, java.util.List.of(line), npc, null);
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (hasSession(player)) {
                    advanceDialog(player, plugin.getQuestManager());
                } else {
                    startChoiceDialog(player, npc, pc.options, pc.questId, pc.flagBase, pc.callback);
                }
                resumeTasks.remove(player.getUniqueId());
            }, 1L);
            BukkitTask old = resumeTasks.put(player.getUniqueId(), task);
            if (old != null) old.cancel();
        } else {
            startChoiceDialog(player, npc, pc.options, pc.questId, pc.flagBase, pc.callback);
        }
        return true;
    }

    /** Cancel dialog if player walks too far from the NPC. */
    public void checkDistance(Player player, double maxDistanceSquared) {
        DialogSession session = sessions.get(player.getUniqueId());
        if (session != null) {
            if (session.paused) return;
            Location location = getNpcLocation(session.npc, session.citizensNpc);
            if (location != null && player.getLocation().distanceSquared(location) > maxDistanceSquared) {
                player.sendMessage(ChatColor.RED + "You walked away from the NPC. Dialogue cancelled.");
                resetDialog(player);
                return;
            }
        }

        ChoiceSession cs = choiceSessions.get(player.getUniqueId());
        if (cs != null) {
            Location location = getNpcLocation(cs.npc, cs.citizensNpc);
            if (location != null && player.getLocation().distanceSquared(location) > maxDistanceSquared) {
                player.sendMessage(ChatColor.RED + "You walked away from the NPC. Dialogue cancelled.");
                resetDialog(player);
            }
        }
    }

    private Location getNpcLocation(NPC npc, net.citizensnpcs.api.npc.NPC citizensNpc) {
        if (npc != null) {
            if (npc.isSpawned() && npc.getEntity() != null) {
                return npc.getEntity().getLocation();
            }
            return npc.getStoredLocation();
        }
        if (citizensNpc != null) {
            if (citizensNpc.isSpawned() && citizensNpc.getEntity() != null) {
                return citizensNpc.getEntity().getLocation();
            }
            return citizensNpc.getStoredLocation();
        }
        return null;
    }
}
