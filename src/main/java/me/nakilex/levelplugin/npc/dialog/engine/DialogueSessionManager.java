package me.nakilex.levelplugin.npc.dialog.engine;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.npc.dialog.render.DialogueRenderer;
import me.nakilex.levelplugin.npc.system.NPC;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class DialogueSessionManager {
    private static final double MAX_DISTANCE_SQUARED = 25.0;
    private final Main plugin;
    private final DialogueRenderer renderer;
    private final DialogueConditionEvaluator conditions;
    private final DialogueActionExecutor actions;
    private final Map<UUID, DialogueSession> sessions = new HashMap<>();

    public DialogueSessionManager(Main plugin, DialogueRenderer renderer,
                                  DialogueConditionEvaluator conditions, DialogueActionExecutor actions) {
        this.plugin = plugin;
        this.renderer = renderer;
        this.conditions = conditions;
        this.actions = actions;
    }

    public void start(Player player, DialogueDefinition definition, NPC npc,
                      net.citizensnpcs.api.npc.NPC citizensNpc, Quest quest, Runnable finish) {
        start(player, definition, npc, citizensNpc, quest, finish, null, null, null);
    }

    public void start(Player player, DialogueDefinition definition, NPC npc,
                      net.citizensnpcs.api.npc.NPC citizensNpc, Quest quest, Runnable finish,
                      Consumer<Integer> answerCallback, String choiceQuestId, String choiceFlagBase) {
        if (definition == null || definition.startPage() == null || definition.page(definition.startPage()) == null) return;
        reset(player, false);
        DialogueSession session = new DialogueSession(player.getUniqueId(), definition, npc, citizensNpc, quest,
                finish, answerCallback, choiceQuestId, choiceFlagBase);
        sessions.put(player.getUniqueId(), session);
        lock(player);
        session.rangeTask = plugin.getServer().getScheduler().runTaskTimer(plugin,
                () -> checkDistance(player, MAX_DISTANCE_SQUARED), 5L, 5L);
        enterPage(player, session);
    }

    public boolean hasSession(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public DialogueSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public boolean hasAnswers(Player player) {
        DialogueSession session = getSession(player);
        return session != null && !session.visibleAnswers(conditions).isEmpty();
    }

    public void handlePrimaryInput(Player player) {
        DialogueSession session = getSession(player);
        if (session == null) return;
        if (session.typing) {
            finishTyping(player, session);
            return;
        }
        List<DialogueAnswer> answers = session.visibleAnswers(conditions);
        if (!answers.isEmpty()) {
            confirmAnswer(player, session, answers);
            return;
        }
        if (!session.currentPage().gotoTargets().isEmpty()) {
            redirect(player, session, session.currentPage().gotoTargets().get(0));
            return;
        }
        end(player, true);
    }

    public void selectAnswer(Player player, int direction) {
        DialogueSession session = getSession(player);
        if (session == null || session.typing) return;
        List<DialogueAnswer> answers = session.visibleAnswers(conditions);
        if (answers.isEmpty()) return;
        session.selectedAnswerIndex = Math.floorMod(session.selectedAnswerIndex + direction, answers.size());
        renderer.render(player, session);
    }

    public void checkDistance(Player player, double maxDistanceSquared) {
        DialogueSession session = getSession(player);
        if (session == null) return;
        Location npcLocation = getNpcLocation(session);
        if (npcLocation != null && (!npcLocation.getWorld().equals(player.getWorld())
                || player.getLocation().distanceSquared(npcLocation) > maxDistanceSquared)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "You walked away from the NPC. Dialogue cancelled.");
            reset(player, true);
        }
    }

    public void reset(Player player, boolean runExitActions) {
        DialogueSession session = sessions.remove(player.getUniqueId());
        if (session == null) return;
        if (runExitActions && session.currentPage() != null) actions.run(player, session.currentPage().exitActions());
        cancelTasks(session);
        unlock(player);
        renderer.clear(player);
    }

    public DialogueActionExecutor actions() {
        return actions;
    }

    private void enterPage(Player player, DialogueSession session) {
        session.visibleCharacterCount = 0;
        session.selectedAnswerIndex = 0;
        session.postActionsRun = false;
        actions.run(player, session.currentPage().preActions());
        startTyping(player, session);
    }

    private void startTyping(Player player, DialogueSession session) {
        session.typing = true;
        int length = visibleLength(String.join(" ", session.currentPage().lines()));
        if (length == 0) {
            finishTyping(player, session);
            return;
        }
        renderer.render(player, session);
        session.typingTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (getSession(player) != session) return;
            session.visibleCharacterCount++;
            renderer.render(player, session);
            if (session.visibleCharacterCount >= length) finishTyping(player, session);
        }, 1L, 1L);
    }

    private void finishTyping(Player player, DialogueSession session) {
        if (session.typingTask != null) {
            session.typingTask.cancel();
            session.typingTask = null;
        }
        session.visibleCharacterCount = visibleLength(String.join(" ", session.currentPage().lines()));
        session.typing = false;
        runPostActions(player, session);
        renderer.render(player, session);
    }

    private void confirmAnswer(Player player, DialogueSession session, List<DialogueAnswer> answers) {
        if (System.currentTimeMillis() - session.openedAt < 400L) {
            renderer.render(player, session);
            return;
        }
        int index = Math.floorMod(session.selectedAnswerIndex, answers.size());
        DialogueAnswer answer = answers.get(index);
        for (String reply : answer.replies()) player.sendMessage(ChatColor.GRAY + reply.replace("<player>", player.getName()));
        actions.run(player, answer.actions());
        if (!answer.gotoTargets().isEmpty()) {
            if (session.answerCallback != null) session.answerCallback.accept(index);
            redirect(player, session, answer.gotoTargets().get(0));
            return;
        }
        end(player, true);
        if (session.answerCallback != null) session.answerCallback.accept(index);
    }

    private void redirect(Player player, DialogueSession session, String pageId) {
        DialoguePage next = session.dialogue.page(pageId);
        if (next == null) {
            plugin.getLogger().warning("Dialogue " + session.dialogue.id() + " points to missing page " + pageId);
            end(player, true);
            return;
        }
        runPostActions(player, session);
        session.currentPageId = pageId;
        enterPage(player, session);
    }

    private void end(Player player, boolean complete) {
        DialogueSession session = getSession(player);
        if (session == null) return;
        runPostActions(player, session);
        actions.run(player, session.currentPage().exitActions());
        reset(player, false);
        if (!complete) return;
        if (session.quest != null) plugin.getQuestManager().startQuest(player, session.quest.getId());
        String npcName = session.npc != null ? session.npc.getName()
                : session.citizensNpc != null ? session.citizensNpc.getName() : null;
        if (npcName != null) plugin.getCodexManager().recordNpc(player, ChatColor.stripColor(npcName));
        if (session.finish != null) session.finish.run();
    }

    private void runPostActions(Player player, DialogueSession session) {
        if (session.postActionsRun) return;
        session.postActionsRun = true;
        actions.run(player, session.currentPage().postActions());
    }

    private void lock(Player player) {
        player.setInvulnerable(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 60, 4, false, false, false));
    }

    private void unlock(Player player) {
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.setInvulnerable(false);
    }

    private void cancelTasks(DialogueSession session) {
        if (session.typingTask != null) session.typingTask.cancel();
        if (session.rangeTask != null) session.rangeTask.cancel();
    }

    private int visibleLength(String text) {
        return ChatColor.stripColor(text).length();
    }

    private Location getNpcLocation(DialogueSession session) {
        if (session.npc != null) return session.npc.isSpawned() && session.npc.getEntity() != null
                ? session.npc.getEntity().getLocation() : session.npc.getStoredLocation();
        if (session.citizensNpc != null) return session.citizensNpc.isSpawned() && session.citizensNpc.getEntity() != null
                ? session.citizensNpc.getEntity().getLocation() : session.citizensNpc.getStoredLocation();
        return null;
    }
}
