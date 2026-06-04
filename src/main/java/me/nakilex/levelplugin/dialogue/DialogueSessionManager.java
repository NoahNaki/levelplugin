package me.nakilex.levelplugin.dialogue;

import me.nakilex.levelplugin.dialogue.render.ChatDialogueRenderer;
import me.nakilex.levelplugin.npc.system.NpcApi;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class DialogueSessionManager implements Listener {
    private final JavaPlugin plugin;
    private final DialogueRenderer renderer;
    private final DialoguePlaceholderFormatter formatter;
    private final DialogueConditionEvaluator conditionEvaluator;
    private final DialogueActionExecutor actionExecutor;
    private final Map<UUID, DialogueSession> sessions = new HashMap<>();
    private final BukkitTask tickTask;

    public DialogueSessionManager(JavaPlugin plugin) {
        this(plugin, new ChatDialogueRenderer(), new DialoguePlaceholderFormatter(), new DialogueConditionEvaluator());
    }

    public DialogueSessionManager(JavaPlugin plugin, DialogueRenderer renderer,
                                  DialoguePlaceholderFormatter formatter,
                                  DialogueConditionEvaluator conditionEvaluator) {
        this.plugin = plugin;
        this.renderer = renderer;
        this.formatter = formatter;
        this.conditionEvaluator = conditionEvaluator;
        this.actionExecutor = new DialogueActionExecutor(plugin, formatter);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public DialogueSession startDialogue(Player player, DialogueDefinition definition, Integer npcId, Location origin,
                                         Runnable onComplete) {
        return startDialogue(player, definition, npcId, origin, onComplete, null);
    }

    public DialogueSession startDialogue(Player player, DialogueDefinition definition, Integer npcId, Location origin,
                                         Runnable onComplete, Consumer<DialogueAnswer> onAnswer) {
        if (player == null || definition == null) return null;
        endDialogue(player, DialogueEndReason.REPLACED);
        DialogueSession session = new DialogueSession(player, definition, npcId, origin, onComplete, onAnswer);
        sessions.put(player.getUniqueId(), session);
        if (definition.settings().lockPlayer()) lockPlayer(player);
        renderer.begin(player, session);
        enterPage(session, definition.startPageId());
        return session;
    }

    public boolean hasSession(Player player) {
        return player != null && sessions.containsKey(player.getUniqueId());
    }

    public DialogueSession getSession(Player player) {
        return player == null ? null : sessions.get(player.getUniqueId());
    }

    public Integer getSessionNpcId(Player player) {
        DialogueSession session = getSession(player);
        return session == null ? null : session.npcId();
    }

    public boolean isSessionNpc(Player player, int npcId) {
        Integer sessionNpcId = getSessionNpcId(player);
        return sessionNpcId != null && sessionNpcId == npcId;
    }

    public boolean nextOrSkip(Player player, int npcId) {
        DialogueSession session = getSession(player);
        if (session == null || session.npcId() == null || session.npcId() != npcId) return false;
        if (session.state() == DialogueSession.State.ANSWERING) {
            confirmAnswer(player);
        } else if (session.state() == DialogueSession.State.TYPING) {
            skipTyping(player);
        } else {
            advancePage(player);
        }
        return true;
    }

    public void skipTyping(Player player) {
        DialogueSession session = getSession(player);
        if (session == null || session.state() != DialogueSession.State.TYPING) return;
        session.enterWaiting(System.currentTimeMillis());
        render(session);
    }

    public void advancePage(Player player) {
        DialogueSession session = getSession(player);
        if (session == null) return;
        DialoguePage page = session.definition().page(session.pageId());
        leavePage(session, page);
        if (page.gotoPageId() == null || page.gotoPageId().isBlank()) {
            endDialogue(player, DialogueEndReason.COMPLETE);
        } else {
            enterPage(session, page.gotoPageId());
        }
    }

    public void selectAnswer(Player player, int direction) {
        DialogueSession session = getSession(player);
        if (session == null || session.visibleAnswers().isEmpty()) return;
        session.select(session.selectedAnswerIndex() + direction);
        DialogueSound.UI_SELECT.play(player);
        render(session);
    }

    public void confirmAnswer(Player player) {
        DialogueSession session = getSession(player);
        if (session == null || session.visibleAnswers().isEmpty()) return;
        DialogueAnswer answer = session.visibleAnswers().get(session.selectedAnswerIndex());
        if (answer.sound() != null) answer.sound().play(player);
        session.replyLines(answer.replyLines().stream().map(line -> formatter.component(player, line)).toList());
        render(session);
        answer.actions().forEach(action -> actionExecutor.execute(player, session, action));
        session.runAnswer(answer);
        DialoguePage page = session.definition().page(session.pageId());
        leavePage(session, page);
        if (answer.gotoPageId() != null && !answer.gotoPageId().isBlank()) {
            enterPage(session, answer.gotoPageId());
        } else if (page.gotoPageId() != null && !page.gotoPageId().isBlank()) {
            enterPage(session, page.gotoPageId());
        } else {
            endDialogue(player, DialogueEndReason.COMPLETE);
        }
    }

    public void endDialogue(Player player, DialogueEndReason reason) {
        if (player == null) return;
        DialogueSession session = sessions.remove(player.getUniqueId());
        if (session == null) return;
        DialogueEndReason safeReason = reason == null ? DialogueEndReason.RESET : reason;
        DialoguePage page = session.definition().page(session.pageId());
        if (page != null && runsExitActions(safeReason)) {
            page.exitActions().forEach(action -> actionExecutor.execute(player, session, action));
        }
        session.finish();
        unlockPlayer(player);
        renderer.clear(player, session, safeReason);
        if (safeReason == DialogueEndReason.COMPLETE) session.runComplete();
    }

    public void reset(Player player) { endDialogue(player, DialogueEndReason.RESET); }

    public void endOutOfRange(Player player) {
        if (player != null) player.sendMessage(org.bukkit.ChatColor.RED + "You walked away from the NPC. Dialogue cancelled.");
        endDialogue(player, DialogueEndReason.OUT_OF_RANGE);
    }

    public void checkDistance(Player player, double maxDistanceSquared) {
        DialogueSession session = getSession(player);
        if (session == null || session.origin() == null) return;
        double allowed = maxDistanceSquared > 0 ? maxDistanceSquared : session.definition().settings().maxRangeSquared();
        Location origin = session.origin();
        if (origin == null) return;
        if (player.getWorld() != origin.getWorld() || player.getLocation().distanceSquared(origin) > allowed) endOutOfRange(player);
    }

    public void shutdown() {
        tickTask.cancel();
        for (Player player : Bukkit.getOnlinePlayers()) {
            endDialogue(player, DialogueEndReason.RESET);
        }
        sessions.clear();
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (DialogueSession session : List.copyOf(sessions.values())) {
            if (!session.player().isOnline()) {
                endDialogue(session.player(), DialogueEndReason.QUIT);
                continue;
            }
            if (session.state() == DialogueSession.State.TYPING && session.typingComplete(now)) {
                session.enterWaiting(now);
            } else if (session.state() == DialogueSession.State.WAITING && session.waitComplete(now)) {
                advancePage(session.player());
                continue;
            }
            render(session);
        }
    }

    private void enterPage(DialogueSession session, String pageId) {
        DialoguePage page = session.definition().page(pageId);
        if (page == null) {
            endDialogue(session.player(), DialogueEndReason.COMPLETE);
            return;
        }
        page.preActions().forEach(action -> actionExecutor.execute(session.player(), session, action));
        session.enterPage(pageId, System.currentTimeMillis(), formatter, conditionEvaluator);
        DialogueSound.UI_CLICK.play(session.player());
        render(session);
    }

    private void leavePage(DialogueSession session, DialoguePage page) {
        if (page != null) page.postActions().forEach(action -> actionExecutor.execute(session.player(), session, action));
    }

    private void render(DialogueSession session) {
        DialoguePage page = session.definition().page(session.pageId());
        renderer.render(session.player(), session, page, formatter.component(session.player(), session.line().speakerName()),
                session.visibleText(System.currentTimeMillis()), session.lineNumber(), session.lineCount(),
                session.visibleAnswers(), session.selectedAnswerIndex(), session.replyLines());
    }

    private boolean runsExitActions(DialogueEndReason reason) {
        return reason == DialogueEndReason.MANUAL_EXIT || reason == DialogueEndReason.OUT_OF_RANGE
                || reason == DialogueEndReason.QUIT || reason == DialogueEndReason.RESET
                || reason == DialogueEndReason.REPLACED;
    }

    private void lockPlayer(Player player) {
        player.setInvulnerable(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 60, 4, false, false, false));
    }

    private void unlockPlayer(Player player) {
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.setInvulnerable(false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onScroll(PlayerItemHeldEvent event) {
        DialogueSession session = getSession(event.getPlayer());
        if (session == null || session.visibleAnswers().isEmpty()) return;
        event.setCancelled(true);
        selectAnswer(event.getPlayer(), event.getNewSlot() > event.getPreviousSlot() ? 1 : -1);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onNpcClick(PlayerInteractEntityEvent event) {
        DialogueSession session = getSession(event.getPlayer());
        if (session == null || session.npcId() == null) return;
        me.nakilex.levelplugin.npc.system.NPC npc = NpcApi.getRegistry().getNPC(event.getRightClicked());
        net.citizensnpcs.api.npc.NPC citizens = net.citizensnpcs.api.CitizensAPI.getNPCRegistry().getNPC(event.getRightClicked());
        int clickedId = npc != null ? npc.getId() : citizens != null ? citizens.getId() : Integer.MIN_VALUE;
        if (clickedId != session.npcId()) return;
        event.setCancelled(true);
        nextOrSkip(event.getPlayer(), clickedId);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        endDialogue(event.getPlayer(), DialogueEndReason.QUIT);
    }
}
