package me.nakilex.levelplugin.npc.dialog;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.npc.dialog.engine.DialogueActionExecutor;
import me.nakilex.levelplugin.npc.dialog.engine.DialogueAnswer;
import me.nakilex.levelplugin.npc.dialog.engine.DialogueConditionEvaluator;
import me.nakilex.levelplugin.npc.dialog.engine.DialogueDefinition;
import me.nakilex.levelplugin.npc.dialog.engine.DialogueDefinitionLoader;
import me.nakilex.levelplugin.npc.dialog.render.ResourcePackScaffolder;
import me.nakilex.levelplugin.npc.dialog.engine.DialoguePage;
import me.nakilex.levelplugin.npc.dialog.engine.DialogueSession;
import me.nakilex.levelplugin.npc.dialog.engine.DialogueSessionManager;
import me.nakilex.levelplugin.npc.dialog.render.ActionBarDialogueRenderer;
import me.nakilex.levelplugin.npc.system.NPC;
import me.nakilex.levelplugin.npc.system.NpcApi;
import net.citizensnpcs.api.CitizensAPI;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Backwards-compatible facade for the page-based dialogue engine.
 */
public class NPCDialogManager implements Listener {
    private static final long SKILL_DELAY_MS = 500L;
    private final Main plugin;
    private final DialogueSessionManager sessions;
    private final DialogueDefinitionLoader dialogueLoader;
    private final Map<UUID, Long> dialogCooldowns = new HashMap<>();
    private final Map<UUID, PendingChoice> pendingChoices = new HashMap<>();

    public NPCDialogManager(Main plugin) {
        this.plugin = plugin;
        DialogueConditionEvaluator conditions = new DialogueConditionEvaluator(plugin);
        DialogueActionExecutor actions = new DialogueActionExecutor(plugin);
        this.sessions = new DialogueSessionManager(plugin, new ActionBarDialogueRenderer(conditions), conditions, actions,
                this::recordDialogCooldown);
        this.dialogueLoader = new DialogueDefinitionLoader(plugin);
        new ResourcePackScaffolder(plugin).ensureDirectories();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void startDialog(Player player, Quest quest, NPC npc) {
        start(player, quest.getDialogLines(), npc, null, quest, null);
    }

    public void startDialog(Player player, Quest quest, net.citizensnpcs.api.npc.NPC npc) {
        start(player, quest.getDialogLines(), null, npc, quest, null);
    }

    public void startDialog(Player player, List<String> lines, NPC npc, Runnable finish) {
        start(player, lines, npc, null, null, finish);
    }

    public void startDialog(Player player, List<String> lines, net.citizensnpcs.api.npc.NPC npc, Runnable finish) {
        start(player, lines, null, npc, null, finish);
    }

    /** Start a native page dialogue. Legacy line-based overloads adapt to this method. */
    public void startDialog(Player player, DialogueDefinition dialogue, NPC npc, Runnable finish) {
        sessions.start(player, dialogue, npc, null, null, finish);
    }

    public void startDialog(Player player, DialogueDefinition dialogue, net.citizensnpcs.api.npc.NPC npc, Runnable finish) {
        sessions.start(player, dialogue, null, npc, null, finish);
    }

    public DialogueDefinition getDialogue(String id) {
        return dialogueLoader.get(id);
    }

    public void reloadDialogues() {
        dialogueLoader.reload();
    }

    public boolean startDialog(Player player, String dialogueId, NPC npc, Runnable finish) {
        DialogueDefinition dialogue = getDialogue(dialogueId);
        if (dialogue == null) return false;
        startDialog(player, dialogue, npc, finish);
        return true;
    }

    public boolean startDialog(Player player, String dialogueId, net.citizensnpcs.api.npc.NPC npc, Runnable finish) {
        DialogueDefinition dialogue = getDialogue(dialogueId);
        if (dialogue == null) return false;
        startDialog(player, dialogue, npc, finish);
        return true;
    }

    public void startChoiceDialog(Player player, NPC npc, List<String> options, String questId,
                                  String flagBase, Consumer<Integer> callback) {
        startChoice(player, npc, null, options, questId, flagBase, callback);
    }

    public void startChoiceDialog(Player player, net.citizensnpcs.api.npc.NPC npc, List<String> options,
                                  String questId, String flagBase, Consumer<Integer> callback) {
        startChoice(player, null, npc, options, questId, flagBase, callback);
    }

    public void startChoiceDialog(Player player, NPC npc, List<String> options, Consumer<Integer> callback) {
        startChoiceDialog(player, npc, options, null, null, callback);
    }

    public void startChoiceDialog(Player player, net.citizensnpcs.api.npc.NPC npc, List<String> options,
                                  Consumer<Integer> callback) {
        startChoiceDialog(player, npc, options, null, null, callback);
    }

    public boolean resumePendingChoice(Player player, NPC npc) {
        return resumePendingChoice(player, npc, null);
    }

    public boolean resumePendingChoice(Player player, net.citizensnpcs.api.npc.NPC npc) {
        return resumePendingChoice(player, null, npc);
    }

    /** Universal right-click input: reveal, answer, navigate, or finish. */
    public void handlePrimaryInput(Player player) {
        if (hasSession(player)) sessions.handlePrimaryInput(player);
    }

    /** @deprecated use {@link #handlePrimaryInput(Player)}. */
    @Deprecated
    public void advanceDialog(Player player, QuestManager ignored) {
        handlePrimaryInput(player);
    }

    public boolean hasSession(Player player) {
        return sessions.hasSession(player);
    }

    public boolean hasChoiceSession(Player player) {
        return sessions.hasAnswers(player);
    }

    public NPC getSessionNpc(Player player) {
        DialogueSession session = sessions.getSession(player);
        return session == null ? null : session.npc;
    }

    public Integer getSessionNpcId(Player player) {
        DialogueSession session = sessions.getSession(player);
        if (session == null) return null;
        if (session.npc != null) return session.npc.getId();
        return session.citizensNpc != null ? session.citizensNpc.getId() : null;
    }

    public boolean isSessionNpc(Player player, int npcId) {
        Integer sessionNpcId = getSessionNpcId(player);
        return sessionNpcId != null && sessionNpcId == npcId;
    }

    public boolean isDialogLockActive(Player player) {
        if (hasSession(player)) return true;
        Long last = dialogCooldowns.get(player.getUniqueId());
        if (last == null) return false;
        if (System.currentTimeMillis() - last < SKILL_DELAY_MS) return true;
        dialogCooldowns.remove(player.getUniqueId());
        return false;
    }

    public void checkDistance(Player player, double maxDistanceSquared) {
        sessions.checkDistance(player, maxDistanceSquared);
    }

    public void resetDialog(Player player) {
        sessions.reset(player, true);
        recordDialogCooldown(player);
    }

    public void recordDialogCooldown(Player player) {
        dialogCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public DialogueActionExecutor actions() {
        return sessions.actions();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!hasChoiceSession(event.getPlayer())) return;
        NPC npc = NpcApi.getRegistry().getNPC(event.getRightClicked());
        net.citizensnpcs.api.npc.NPC citizensNpc = CitizensAPI.getNPCRegistry().getNPC(event.getRightClicked());
        Integer clickedId = npc != null ? npc.getId() : citizensNpc != null ? citizensNpc.getId() : null;
        Integer sessionNpcId = getSessionNpcId(event.getPlayer());
        if (clickedId == null || !clickedId.equals(sessionNpcId)) return;
        event.setCancelled(true);
        handlePrimaryInput(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onScroll(PlayerItemHeldEvent event) {
        if (!hasChoiceSession(event.getPlayer())) return;
        event.setCancelled(true);
        sessions.selectAnswer(event.getPlayer(), getScrollDirection(event.getPreviousSlot(), event.getNewSlot()));
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) sessions.exit(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sessions.close(event.getPlayer(), me.nakilex.levelplugin.npc.dialog.engine.DialogueEndReason.QUIT, true);
    }

    public void shutdown() {
        sessions.shutdown();
    }

    private int getScrollDirection(int oldSlot, int newSlot) {
        if (oldSlot == 8 && newSlot == 0) return 1;
        if (oldSlot == 0 && newSlot == 8) return -1;
        return newSlot > oldSlot ? 1 : -1;
    }

    private void start(Player player, List<String> lines, NPC npc, net.citizensnpcs.api.npc.NPC citizensNpc,
                       Quest quest, Runnable finish) {
        if (lines == null || lines.isEmpty()) return;
        String id = quest != null ? quest.getId() : "legacy-" + player.getUniqueId();
        sessions.start(player, DialogueDefinition.linear(id, lines), npc, citizensNpc, quest, finish);
    }

    private void startChoice(Player player, NPC npc, net.citizensnpcs.api.npc.NPC citizensNpc,
                             List<String> options, String questId, String flagBase, Consumer<Integer> callback) {
        if (options == null || options.isEmpty()) return;
        if (questId != null && flagBase != null) {
            plugin.getQuestManager().setFlag(player.getUniqueId(), questId, flagBase + "pending");
            pendingChoices.put(player.getUniqueId(), new PendingChoice(npc, citizensNpc, options, questId, flagBase, callback));
        }
        List<DialogueAnswer> answers = options.stream().map(DialogueAnswer::new).toList();
        DialoguePage page = new DialoguePage("choice", List.of(), List.of(), List.of(), List.of(), List.of(), answers);
        Consumer<Integer> wrapped = selected -> {
            if (questId != null && flagBase != null) {
                plugin.getQuestManager().removeFlag(player.getUniqueId(), questId, flagBase + "pending");
                plugin.getQuestManager().setFlag(player.getUniqueId(), questId, flagBase + selected);
                pendingChoices.remove(player.getUniqueId());
            }
            if (callback != null) callback.accept(selected);
        };
        sessions.start(player, new DialogueDefinition("legacy-choice", "choice", Map.of("choice", page)),
                npc, citizensNpc, null, null, wrapped, questId, flagBase);
    }

    private boolean resumePendingChoice(Player player, NPC npc, net.citizensnpcs.api.npc.NPC citizensNpc) {
        PendingChoice pending = pendingChoices.get(player.getUniqueId());
        if (pending == null || !pending.matches(npc, citizensNpc)) return false;
        if (pending.questId != null && !plugin.getQuestManager().hasFlag(player.getUniqueId(), pending.questId,
                pending.flagBase + "pending")) {
            pendingChoices.remove(player.getUniqueId());
            return false;
        }
        startChoice(player, npc, citizensNpc, pending.options, pending.questId, pending.flagBase, pending.callback);
        return true;
    }

    private record PendingChoice(NPC npc, net.citizensnpcs.api.npc.NPC citizensNpc, List<String> options,
                                 String questId, String flagBase, Consumer<Integer> callback) {
        private boolean matches(NPC clickedNpc, net.citizensnpcs.api.npc.NPC clickedCitizensNpc) {
            Integer expected = npc != null ? npc.getId() : citizensNpc != null ? citizensNpc.getId() : null;
            Integer clicked = clickedNpc != null ? clickedNpc.getId() : clickedCitizensNpc != null ? clickedCitizensNpc.getId() : null;
            return expected == null || expected.equals(clicked);
        }
    }
}
