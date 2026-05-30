package me.nakilex.levelplugin.npc.dialog;

import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.npc.system.NPC;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.npc.dialog.entry.DialogueEntry;
import me.nakilex.levelplugin.npc.dialog.entry.MessageDialogueEntry;
import me.nakilex.levelplugin.npc.dialog.entry.OptionDialogueEntry;
import me.nakilex.levelplugin.npc.dialog.messenger.DialogueMessenger;
import me.nakilex.levelplugin.npc.dialog.messenger.OptionMessenger;
import me.nakilex.levelplugin.npc.dialog.model.DialogNpcRef;
import me.nakilex.levelplugin.npc.dialog.model.InteractionContext;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import java.util.function.Consumer;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles simple dialog sequences with NPCs.
 */
public class NPCDialogManager implements Listener {

    private final Main plugin;
    private final BukkitTask messengerTickTask;

    public NPCDialogManager(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.messengerTickTask = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, this::tickMessengers, 1L, 1L);
    }

    private static class DialogSession {
        final Quest quest;
        final List<DialogueEntry> entries;
        final DialogNpcRef npc;
        final Runnable finish;
        final InteractionContext context;
        DialogueMessenger currentMessenger;
        int index = 0;
        boolean paused = false;

        DialogSession(Main plugin, Player player, Quest quest, List<DialogueEntry> entries,
                      DialogNpcRef npc, Runnable finish) {
            this.quest = quest;
            this.entries = entries;
            this.npc = npc;
            this.finish = finish;
            this.context = new InteractionContext(plugin, player, npc, quest, finish);
        }
    }

    private final Map<UUID, DialogSession> sessions = new HashMap<>();
    private final Map<UUID, String> lastLines = new HashMap<>();
    private final Map<UUID, BukkitTask> resumeTasks = new HashMap<>();
    private final Map<UUID, Long> dialogCooldowns = new HashMap<>();

    private static final long SKILL_DELAY_MS = 500L;

    public NPC getSessionNpc(Player player) {
        DialogSession s = sessions.get(player.getUniqueId());
        return s != null && s.npc != null ? s.npc.npc() : null;
    }

    public Integer getSessionNpcId(Player player) {
        DialogSession s = sessions.get(player.getUniqueId());
        if (s == null) {
            return null;
        }
        return s.npc != null ? s.npc.id() : null;
    }

    public boolean isSessionNpc(Player player, int npcId) {
        Integer sessionNpcId = getSessionNpcId(player);
        return sessionNpcId != null && sessionNpcId == npcId;
    }

    private static class ChoiceSession {
        final DialogNpcRef npc;
        final OptionMessenger messenger;

        ChoiceSession(DialogNpcRef npc, OptionMessenger messenger) {
            this.npc = npc;
            this.messenger = messenger;
        }
    }

    private final Map<UUID, ChoiceSession> choiceSessions = new HashMap<>();

    private static class PendingChoice {
        final DialogNpcRef npc;
        final List<String> options;
        final Consumer<Integer> callback;
        final String questId;
        final String flagBase;
        final String resumeLine;

        PendingChoice(DialogNpcRef npc, List<String> options, Consumer<Integer> callback,
                      String questId, String flagBase, String resumeLine) {
            this.npc = npc;
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
        if (hasSession(player) || hasChoiceSession(player)) {
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

    private void tickMessengers() {
        Duration tick = Duration.ofMillis(50L);
        for (DialogSession session : List.copyOf(sessions.values())) {
            if (session.currentMessenger != null && !session.currentMessenger.isFinished()
                    && !session.currentMessenger.isCancelled()) {
                session.currentMessenger.tick(tick);
            }
        }
        for (ChoiceSession session : List.copyOf(choiceSessions.values())) {
            if (session.messenger != null && !session.messenger.isFinished()
                    && !session.messenger.isCancelled()) {
                session.messenger.tick(tick);
            }
        }
    }

    /** Start a dialog sequence for a quest. */
    public void startDialog(Player player, Quest quest, NPC npc) {
        List<String> lines = quest.getDialogLines();
        if (lines == null || lines.isEmpty()) return;
        if (plugin.getQuestManager().isDebug()) {
            plugin.getLogger().info("[DialogDebug] startDialog quest=" + quest.getId() +
                    " player=" + player.getName());
        }
        startDialogSession(player, quest, lines, DialogNpcRef.of(npc), null);
    }

    public void startDialog(Player player, Quest quest, net.citizensnpcs.api.npc.NPC npc) {
        List<String> lines = quest.getDialogLines();
        if (lines == null || lines.isEmpty()) return;
        if (plugin.getQuestManager().isDebug()) {
            plugin.getLogger().info("[DialogDebug] startDialog quest=" + quest.getId() +
                    " player=" + player.getName());
        }
        startDialogSession(player, quest, lines, DialogNpcRef.of(npc), null);
    }

    /** Start a dialog sequence with custom lines and finish callback. */
    public void startDialog(Player player, List<String> lines, NPC npc, Runnable finish) {
        if (lines == null || lines.isEmpty()) return;
        if (plugin.getQuestManager().isDebug()) {
            plugin.getLogger().info("[DialogDebug] startDialog custom player=" + player.getName());
        }
        startDialogSession(player, null, lines, DialogNpcRef.of(npc), finish);
    }

    public void startDialog(Player player, List<String> lines, net.citizensnpcs.api.npc.NPC npc, Runnable finish) {
        if (lines == null || lines.isEmpty()) return;
        if (plugin.getQuestManager().isDebug()) {
            plugin.getLogger().info("[DialogDebug] startDialog custom player=" + player.getName());
        }
        startDialogSession(player, null, lines, DialogNpcRef.of(npc), finish);
    }

    /**
     * Present a choice dialog to the player using the scroll wheel.
     *
     * @param questId   optional quest identifier for flag tracking
     * @param flagBase  if questId is supplied, choice index will be appended to this and stored as a flag
     */
    public void startChoiceDialog(Player player, NPC npc, List<String> options, String questId, String flagBase, Consumer<Integer> callback) {
        startChoiceDialog(player, DialogNpcRef.of(npc), options, questId, flagBase, callback);
    }

    public void startChoiceDialog(Player player, net.citizensnpcs.api.npc.NPC npc, List<String> options,
                                  String questId, String flagBase, Consumer<Integer> callback) {
        startChoiceDialog(player, DialogNpcRef.of(npc), options, questId, flagBase, callback);
    }

    private void startChoiceDialog(Player player, DialogNpcRef npc, List<String> options,
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

        lockPlayer(player);
        List<OptionDialogueEntry.Option> optionEntries = options.stream()
                .map(OptionDialogueEntry.Option::new)
                .toList();
        Consumer<Integer> wrappedCallback = selected -> finishChoiceSelection(player, selected, questId, flagBase, callback);
        OptionDialogueEntry entry = new OptionDialogueEntry("choice:" + player.getUniqueId(), "Choose your answer",
                "Choose your answer:", optionEntries, wrappedCallback, "choice_index");
        InteractionContext context = new InteractionContext(plugin, player, npc, null, null);
        OptionMessenger messenger = (OptionMessenger) entry.createMessenger(player, context);
        choiceSessions.put(player.getUniqueId(), new ChoiceSession(npc, messenger));
        messenger.init();
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
            lockPlayer(player);
        }

        if (session.currentMessenger != null && !session.currentMessenger.isFinished()
                && !session.currentMessenger.isCancelled()) {
            session.currentMessenger.requestNextOrSkip();
            return;
        }

        if (session.index >= session.entries.size()) {
            endDialog(player);
            if (session.quest != null) {
                questManager.startQuest(player, session.quest.getId());
            }
            if (session.npc != null) {
                Main.getInstance().getCodexManager().recordNpc(player,
                        org.bukkit.ChatColor.stripColor(session.npc.name()));
            }
            if (session.finish != null) {
                session.finish.run();
            }
            return;
        }

        sendNextEntry(player, session);
    }

    private void startDialogSession(Player player, Quest quest, List<String> lines, DialogNpcRef npc, Runnable finish) {
        if (lines == null || lines.isEmpty()) return;
        lockPlayer(player);
        DialogSession session = new DialogSession(plugin, player, quest, createMessageEntries(lines), npc, finish);
        sessions.put(player.getUniqueId(), session);
        sendNextEntry(player, session);
    }

    private List<DialogueEntry> createMessageEntries(List<String> lines) {
        int total = lines.size();
        return java.util.stream.IntStream.range(0, total)
                .mapToObj(index -> new MessageDialogueEntry("message:" + index, "Message " + (index + 1),
                        lines.get(index), index, total))
                .map(DialogueEntry.class::cast)
                .toList();
    }

    private void sendNextEntry(Player player, DialogSession session) {
        if (session.index >= session.entries.size()) {
            return;
        }
        DialogueEntry entry = session.entries.get(session.index);
        session.currentMessenger = entry.createMessenger(player, session.context);
        if (entry instanceof MessageDialogueEntry messageEntry) {
            lastLines.put(player.getUniqueId(), messageEntry.rawLine());
        }
        session.currentMessenger.init();
        session.index++;
    }

    private void lockPlayer(Player player) {
        player.setInvulnerable(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 60, 4, false, false, false));
    }

    private void unlockPlayer(Player player) {
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.setInvulnerable(false);
    }

    private void endDialog(Player player) {
        DialogSession session = sessions.remove(player.getUniqueId());
        if (session != null && session.currentMessenger != null) {
            session.currentMessenger.dispose();
        }
        unlockPlayer(player);
        recordDialogCooldown(player);
    }

    private void pauseDialog(Player player) {
        DialogSession session = sessions.get(player.getUniqueId());
        if (session == null) return;
        unlockPlayer(player);
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
        DialogSession session = sessions.remove(player.getUniqueId());
        if (session != null && session.currentMessenger != null) {
            session.currentMessenger.cancel();
        }
        unlockPlayer(player);
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
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() != plugin) return;
        messengerTickTask.cancel();
        for (UUID playerId : List.copyOf(sessions.keySet())) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null) resetDialog(player);
        }
        for (UUID playerId : List.copyOf(choiceSessions.keySet())) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null) resetDialog(player);
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
        if (cs != null && cs.messenger != null) {
            cs.messenger.cancel();
        }
    }

    private void finishChoiceSelection(Player player, int selectedIndex, String questId, String flagBase, Consumer<Integer> callback) {
        choiceSessions.remove(player.getUniqueId());
        unlockPlayer(player);
        recordDialogCooldown(player);
        BukkitTask pending = resumeTasks.remove(player.getUniqueId());
        if (pending != null) pending.cancel();
        if (plugin.getQuestManager().isDebug()) {
            plugin.getLogger().info("[DialogDebug] finishChoice player=" + player.getName() +
                    " quest=" + questId + " choice=" + selectedIndex);
        }
        if (questId != null && flagBase != null) {
            QuestManager qm = plugin.getQuestManager();
            qm.removeFlag(player.getUniqueId(), questId, flagBase + "pending");
            qm.setFlag(player.getUniqueId(), questId, flagBase + selectedIndex);
            pendingChoices.remove(player.getUniqueId());
        }
        if (callback != null) {
            callback.accept(selectedIndex);
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
        if (npc != null && pc.npc != null && pc.npc.id() != npc.getId()) {
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
        if (npc != null && pc.npc != null && pc.npc.id() != npc.getId()) {
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
            Location location = session.npc != null ? session.npc.location() : null;
            if (location != null && player.getLocation().distanceSquared(location) > maxDistanceSquared) {
                player.sendMessage(ChatColor.RED + "You walked away from the NPC. Dialogue cancelled.");
                resetDialog(player);
                return;
            }
        }

        ChoiceSession cs = choiceSessions.get(player.getUniqueId());
        if (cs != null) {
            Location location = cs.npc != null ? cs.npc.location() : null;
            if (location != null && player.getLocation().distanceSquared(location) > maxDistanceSquared) {
                player.sendMessage(ChatColor.RED + "You walked away from the NPC. Dialogue cancelled.");
                resetDialog(player);
            }
        }
    }

}
