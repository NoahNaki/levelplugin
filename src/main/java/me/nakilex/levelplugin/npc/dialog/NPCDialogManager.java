package me.nakilex.levelplugin.npc.dialog;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dialogue.DialogueAnswer;
import me.nakilex.levelplugin.dialogue.DialogueDefinition;
import me.nakilex.levelplugin.dialogue.DialogueEndReason;
import me.nakilex.levelplugin.dialogue.DialoguePage;
import me.nakilex.levelplugin.dialogue.DialogueSessionManager;
import me.nakilex.levelplugin.npc.system.NPC;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/** Compatibility adapter for NPC dialogue APIs backed by DialogueSessionManager. */
public class NPCDialogManager implements Listener {
    private final Main plugin;
    private final DialogueSessionManager sessions;
    private final Map<UUID, String> lastLines = new java.util.HashMap<>();
    private final Map<UUID, Long> dialogCooldowns = new java.util.HashMap<>();
    private final Map<UUID, PendingChoice> pendingChoices = new java.util.HashMap<>();
    private final Map<UUID, Boolean> activeChoices = new java.util.HashMap<>();

    private static final long SKILL_DELAY_MS = 500L;

    public NPCDialogManager(Main plugin, DialogueSessionManager sessions) {
        this.plugin = plugin;
        this.sessions = sessions;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public NPC getSessionNpc(Player player) { return null; }

    public Integer getSessionNpcId(Player player) {
        return sessions.getSessionNpcId(player);
    }

    public boolean isSessionNpc(Player player, int npcId) {
        return sessions.isSessionNpc(player, npcId);
    }

    public boolean hasSession(Player player) {
        return sessions.hasSession(player);
    }

    public boolean hasChoiceSession(Player player) {
        return player != null && Boolean.TRUE.equals(activeChoices.get(player.getUniqueId())) && sessions.hasSession(player);
    }

    public boolean isDialogLockActive(Player player) {
        if (hasSession(player)) return true;
        Long last = dialogCooldowns.get(player.getUniqueId());
        if (last == null) return false;
        if (System.currentTimeMillis() - last < SKILL_DELAY_MS) return true;
        dialogCooldowns.remove(player.getUniqueId());
        return false;
    }

    public void startDialog(Player player, Quest quest, NPC npc) {
        if (quest == null) return;
        startDefinition(player, DialogueDefinition.fromLegacyQuest(quest, npcName(npc, null)), npcId(npc, null), npcLocation(npc, null), () -> {
            plugin.getQuestManager().startQuest(player, quest.getId());
            recordNpc(player, npcName(npc, null));
        });
    }

    public void startDialog(Player player, Quest quest, net.citizensnpcs.api.npc.NPC npc) {
        if (quest == null) return;
        startDefinition(player, DialogueDefinition.fromLegacyQuest(quest, npcName(null, npc)), npcId(null, npc), npcLocation(null, npc), () -> {
            plugin.getQuestManager().startQuest(player, quest.getId());
            recordNpc(player, npcName(null, npc));
        });
    }

    public void startDialog(Player player, List<String> lines, NPC npc, Runnable finish) {
        startLegacyLines(player, lines, npc, null, finish);
    }

    public void startDialog(Player player, List<String> lines, net.citizensnpcs.api.npc.NPC npc, Runnable finish) {
        startLegacyLines(player, lines, null, npc, finish);
    }

    private void startLegacyLines(Player player, List<String> lines, NPC npc, net.citizensnpcs.api.npc.NPC citizensNpc,
                                  Runnable finish) {
        if (player == null || lines == null || lines.isEmpty()) return;
        lines.forEach(line -> lastLines.put(player.getUniqueId(), line));
        DialogueDefinition definition = DialogueDefinition.fromLegacyLines("npc_" + npcId(npc, citizensNpc),
                npcName(npc, citizensNpc), lines);
        startDefinition(player, definition, npcId(npc, citizensNpc), npcLocation(npc, citizensNpc), finish);
    }

    private void startDefinition(Player player, DialogueDefinition definition, int npcId, Location origin, Runnable finish) {
        if (plugin.getQuestManager().isDebug()) {
            plugin.getLogger().info("[DialogDebug] start unified dialogue=" + definition.id() + " player=" + player.getName());
        }
        sessions.startDialogue(player, definition, npcId, origin, () -> {
            activeChoices.remove(player.getUniqueId());
            recordDialogCooldown(player);
            if (finish != null) finish.run();
        });
    }

    public void startChoiceDialog(Player player, NPC npc, List<String> options, String questId, String flagBase,
                                  Consumer<Integer> callback) {
        startChoiceDialog(player, npc, null, options, questId, flagBase, callback);
    }

    public void startChoiceDialog(Player player, net.citizensnpcs.api.npc.NPC npc, List<String> options,
                                  String questId, String flagBase, Consumer<Integer> callback) {
        startChoiceDialog(player, null, npc, options, questId, flagBase, callback);
    }

    public void startChoiceDialog(Player player, NPC npc, List<String> options, Consumer<Integer> callback) {
        startChoiceDialog(player, npc, null, options, null, null, callback);
    }

    public void startChoiceDialog(Player player, net.citizensnpcs.api.npc.NPC npc, List<String> options,
                                  Consumer<Integer> callback) {
        startChoiceDialog(player, null, npc, options, null, null, callback);
    }

    private void startChoiceDialog(Player player, NPC npc, net.citizensnpcs.api.npc.NPC citizensNpc, List<String> options,
                                   String questId, String flagBase, Consumer<Integer> callback) {
        if (player == null || options == null || options.isEmpty() || hasChoiceSession(player)) return;
        if (questId != null && flagBase != null) {
            plugin.getQuestManager().setFlag(player.getUniqueId(), questId, flagBase + "pending");
            pendingChoices.put(player.getUniqueId(), new PendingChoice(npc, citizensNpc, options, callback,
                    questId, flagBase, lastLines.get(player.getUniqueId())));
        }
        List<DialogueAnswer> answers = new java.util.ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            answers.add(new DialogueAnswer(String.valueOf(i), options.get(i), null, List.of(), null, null,
                    List.of("callback:" + i)));
        }
        DialoguePage page = new DialoguePage("choice", List.of(npcName(npc, citizensNpc) + "|Choose your answer:"),
                null, answers, List.of(), List.of(), List.of());
        Map<String, DialoguePage> pages = new LinkedHashMap<>();
        pages.put(page.id(), page);
        DialogueDefinition definition = new DialogueDefinition("choice_" + npcId(npc, citizensNpc), null, List.of(),
                pages, page.id(), npcName(npc, citizensNpc));
        activeChoices.put(player.getUniqueId(), true);
        sessions.startDialogue(player, definition, npcId(npc, citizensNpc), npcLocation(npc, citizensNpc), () -> {
            activeChoices.remove(player.getUniqueId());
            recordDialogCooldown(player);
        }, answer -> finishChoice(player, Integer.parseInt(answer.id()), questId, flagBase, callback));
    }

    public void nextOrSkipDialog(Player player, QuestManager questManager) {
        Integer npcId = getSessionNpcId(player);
        if (npcId != null) sessions.nextOrSkip(player, npcId);
    }

    public void advanceDialog(Player player, QuestManager questManager) {
        nextOrSkipDialog(player, questManager);
    }

    public void resetDialog(Player player) {
        activeChoices.remove(player.getUniqueId());
        pendingChoices.remove(player.getUniqueId());
        lastLines.remove(player.getUniqueId());
        sessions.endDialogue(player, DialogueEndReason.RESET);
        recordDialogCooldown(player);
        if (plugin.getQuestManager().isDebug()) plugin.getLogger().info("[DialogDebug] reset dialog for " + player.getName());
    }

    public void recordDialogCooldown(Player player) {
        if (player != null) dialogCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public boolean resumePendingChoice(Player player, NPC npc) {
        return resumePendingChoice(player, npc, null);
    }

    public boolean resumePendingChoice(Player player, net.citizensnpcs.api.npc.NPC npc) {
        return resumePendingChoice(player, null, npc);
    }

    private boolean resumePendingChoice(Player player, NPC npc, net.citizensnpcs.api.npc.NPC citizensNpc) {
        PendingChoice pc = pendingChoices.get(player.getUniqueId());
        if (pc == null || !matches(pc, npc, citizensNpc)) return false;
        if (pc.questId != null && !plugin.getQuestManager().hasFlag(player.getUniqueId(), pc.questId, pc.flagBase + "pending")) {
            pendingChoices.remove(player.getUniqueId());
            return false;
        }
        Runnable openChoice = () -> startChoiceDialog(player, pc.npc, pc.citizensNpc, pc.options, pc.questId, pc.flagBase, pc.callback);
        if (pc.resumeLine != null) {
            startLegacyLines(player, List.of(pc.resumeLine), npc, citizensNpc, openChoice);
        } else {
            openChoice.run();
        }
        return true;
    }

    public void checkDistance(Player player, double maxDistanceSquared) {
        sessions.checkDistance(player, maxDistanceSquared);
    }

    private void finishChoice(Player player, int index, String questId, String flagBase, Consumer<Integer> callback) {
        activeChoices.remove(player.getUniqueId());
        if (questId != null && flagBase != null) {
            QuestManager qm = plugin.getQuestManager();
            qm.removeFlag(player.getUniqueId(), questId, flagBase + "pending");
            qm.setFlag(player.getUniqueId(), questId, flagBase + index);
            pendingChoices.remove(player.getUniqueId());
        }
        recordDialogCooldown(player);
        if (callback != null) Bukkit.getScheduler().runTask(plugin, () -> callback.accept(index));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sessions.endDialogue(event.getPlayer(), DialogueEndReason.QUIT);
        activeChoices.remove(event.getPlayer().getUniqueId());
    }

    private boolean matches(PendingChoice pc, NPC npc, net.citizensnpcs.api.npc.NPC citizensNpc) {
        int clicked = npcId(npc, citizensNpc);
        return clicked == npcId(pc.npc, pc.citizensNpc);
    }

    private int npcId(NPC npc, net.citizensnpcs.api.npc.NPC citizensNpc) {
        if (npc != null) return npc.getId();
        return citizensNpc != null ? citizensNpc.getId() : -1;
    }

    private String npcName(NPC npc, net.citizensnpcs.api.npc.NPC citizensNpc) {
        if (npc != null) return npc.getName();
        return citizensNpc != null ? citizensNpc.getName() : "NPC";
    }

    private Location npcLocation(NPC npc, net.citizensnpcs.api.npc.NPC citizensNpc) {
        if (npc != null) {
            if (npc.isSpawned() && npc.getEntity() != null) return npc.getEntity().getLocation();
            return npc.getStoredLocation();
        }
        if (citizensNpc != null) {
            if (citizensNpc.isSpawned() && citizensNpc.getEntity() != null) return citizensNpc.getEntity().getLocation();
            return citizensNpc.getStoredLocation();
        }
        return null;
    }

    private void recordNpc(Player player, String npcName) {
        if (plugin.getCodexManager() != null) {
            plugin.getCodexManager().recordNpc(player, org.bukkit.ChatColor.stripColor(npcName));
        }
    }

    private record PendingChoice(NPC npc, net.citizensnpcs.api.npc.NPC citizensNpc, List<String> options,
                                 Consumer<Integer> callback, String questId, String flagBase, String resumeLine) { }
}
