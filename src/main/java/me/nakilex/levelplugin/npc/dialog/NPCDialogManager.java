package me.nakilex.levelplugin.npc.dialog;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.npc.dialog.entry.DialogueEntry;
import me.nakilex.levelplugin.npc.dialog.entry.MessageDialogueEntry;
import me.nakilex.levelplugin.npc.dialog.entry.OptionDialogueEntry;
import me.nakilex.levelplugin.npc.dialog.messenger.OptionMessenger;
import me.nakilex.levelplugin.npc.dialog.model.DialogNpcRef;
import me.nakilex.levelplugin.npc.system.NPC;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.managers.QuestManager;
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

/** Routes NPC dialogue events into interaction-owned dialogue flows. */
public class NPCDialogManager implements Listener {
    private static final long SKILL_DELAY_MS = 500L;
    private final Main plugin;
    private final DialogueRegistry registry;
    private final BukkitTask messengerTickTask;
    private final Map<UUID, DialogueInteraction> interactions = new HashMap<>();
    private final Map<UUID, String> lastLines = new HashMap<>();
    private final Map<UUID, BukkitTask> resumeTasks = new HashMap<>();
    private final Map<UUID, Long> dialogCooldowns = new HashMap<>();
    private final Map<UUID, PendingChoice> pendingChoices = new HashMap<>();

    private record PendingChoice(DialogNpcRef npc, List<String> options, Consumer<Integer> callback,
                                 String questId, String flagBase, String resumeLine) { }

    public NPCDialogManager(Main plugin) {
        this.plugin = plugin;
        this.registry = new DialogueRegistry(plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        messengerTickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickMessengers, 1L, 1L);
    }

    public DialogueRegistry getRegistry() { return registry; }

    public NPC getSessionNpc(Player player) {
        DialogueInteraction interaction = interactions.get(player.getUniqueId());
        return interaction == null || interaction.npc() == null ? null : interaction.npc().npc();
    }

    public Integer getSessionNpcId(Player player) {
        DialogueInteraction interaction = interactions.get(player.getUniqueId());
        return interaction == null ? null : interaction.getNpcId();
    }

    public boolean isSessionNpc(Player player, int npcId) {
        Integer activeNpcId = getSessionNpcId(player);
        return activeNpcId != null && activeNpcId == npcId;
    }

    public boolean hasSession(Player player) {
        DialogueInteraction interaction = interactions.get(player.getUniqueId());
        return interaction != null && interaction.isActive();
    }

    public boolean hasChoiceSession(Player player) {
        DialogueInteraction interaction = interactions.get(player.getUniqueId());
        return interaction != null && interaction.isActive() && interaction.currentMessenger() instanceof OptionMessenger;
    }

    public boolean isDialogLockActive(Player player) {
        if (hasSession(player)) return true;
        Long last = dialogCooldowns.get(player.getUniqueId());
        if (last == null) return false;
        if (System.currentTimeMillis() - last < SKILL_DELAY_MS) return true;
        dialogCooldowns.remove(player.getUniqueId());
        return false;
    }

    public void nextOrSkip(Player player) { advanceDialog(player, plugin.getQuestManager()); }

    public void startInteraction(Player player, DialogueDefinition definition) {
        startInteraction(player, definition, definition.npc());
    }

    public void startInteraction(Player player, DialogueDefinition definition, DialogNpcRef clickedNpc) {
        startInteraction(player, null, clickedNpc, definition.entries(), null);
    }

    public void startDialog(Player player, Quest quest, NPC npc) {
        if (quest != null) startDialog(player, quest, DialogNpcRef.of(npc));
    }

    public void startDialog(Player player, Quest quest, net.citizensnpcs.api.npc.NPC npc) {
        if (quest != null) startDialog(player, quest, DialogNpcRef.of(npc));
    }

    private void startDialog(Player player, Quest quest, DialogNpcRef npc) {
        List<String> lines = quest.getDialogLines();
        if (lines == null || lines.isEmpty()) return;
        debug("startDialog quest=" + quest.getId() + " player=" + player.getName());
        startInteraction(player, quest, npc, createMessageEntries(lines), null);
    }

    public void startDialog(Player player, List<String> lines, NPC npc, Runnable finish) {
        startDialog(player, lines, DialogNpcRef.of(npc), finish);
    }

    public void startDialog(Player player, List<String> lines, net.citizensnpcs.api.npc.NPC npc, Runnable finish) {
        startDialog(player, lines, DialogNpcRef.of(npc), finish);
    }

    private void startDialog(Player player, List<String> lines, DialogNpcRef npc, Runnable finish) {
        if (lines == null || lines.isEmpty()) return;
        debug("startDialog custom player=" + player.getName());
        startInteraction(player, null, npc, createMessageEntries(lines), finish);
    }

    private void startInteraction(Player player, Quest quest, DialogNpcRef npc,
                                  List<DialogueEntry> entries, Runnable finish) {
        DialogueInteraction old = interactions.remove(player.getUniqueId());
        if (old != null) old.cancel();
        lockPlayer(player);
        Runnable completion = () -> {
            if (quest != null) plugin.getQuestManager().startQuest(player, quest.getId());
            if (npc != null) plugin.getCodexManager().recordNpc(player, ChatColor.stripColor(npc.name()));
            if (finish != null) finish.run();
        };
        DialogueInteraction interaction = new DialogueInteraction(plugin, player, npc, quest, entries, completion,
                entry -> { if (entry instanceof MessageDialogueEntry message) lastLines.put(player.getUniqueId(), message.line()); });
        interactions.put(player.getUniqueId(), interaction);
        interaction.start();
        cleanupIfClosed(player, interaction);
    }

    public void startChoiceDialog(Player player, NPC npc, List<String> options, String questId,
                                  String flagBase, Consumer<Integer> callback) {
        startChoiceDialog(player, DialogNpcRef.of(npc), options, questId, flagBase, callback);
    }

    public void startChoiceDialog(Player player, net.citizensnpcs.api.npc.NPC npc, List<String> options,
                                  String questId, String flagBase, Consumer<Integer> callback) {
        startChoiceDialog(player, DialogNpcRef.of(npc), options, questId, flagBase, callback);
    }

    private void startChoiceDialog(Player player, DialogNpcRef npc, List<String> options, String questId,
                                   String flagBase, Consumer<Integer> callback) {
        if (options == null || options.isEmpty()) return;
        debug("startChoiceDialog player=" + player.getName() + " quest=" + questId + " flagBase=" + flagBase);
        if (questId != null && flagBase != null) {
            plugin.getQuestManager().setFlag(player.getUniqueId(), questId, flagBase + "pending");
            pendingChoices.put(player.getUniqueId(), new PendingChoice(npc, List.copyOf(options), callback,
                    questId, flagBase, lastLines.get(player.getUniqueId())));
        }
        List<OptionDialogueEntry.Option> optionEntries = options.stream().map(OptionDialogueEntry.Option::new).toList();
        OptionDialogueEntry entry = new OptionDialogueEntry("choice:" + player.getUniqueId(), "Choose your answer",
                npc, "Choose your answer:", "choice_index", optionEntries,
                selected -> finishChoiceSelection(player, selected, questId, flagBase, callback));
        startInteraction(player, null, npc, List.of(entry), null);
    }

    public void startChoiceDialog(Player player, NPC npc, List<String> options, Consumer<Integer> callback) {
        startChoiceDialog(player, npc, options, null, null, callback);
    }

    public void startChoiceDialog(Player player, net.citizensnpcs.api.npc.NPC npc, List<String> options,
                                  Consumer<Integer> callback) {
        startChoiceDialog(player, npc, options, null, null, callback);
    }

    public void advanceDialog(Player player, QuestManager questManager) {
        DialogueInteraction interaction = interactions.get(player.getUniqueId());
        if (interaction == null) return;
        if (!interaction.isActive()) { cleanupIfClosed(player, interaction); return; }
        interaction.nextOrSkip();
        cleanupIfClosed(player, interaction);
    }

    private List<DialogueEntry> createMessageEntries(List<String> lines) {
        return IntStream.range(0, lines.size())
                .mapToObj(index -> (DialogueEntry) new MessageDialogueEntry("message:" + index,
                        "Message " + (index + 1), lines.get(index), index, lines.size()))
                .toList();
    }

    private void tickMessengers() {
        for (Map.Entry<UUID, DialogueInteraction> entry : List.copyOf(interactions.entrySet())) {
            DialogueInteraction interaction = entry.getValue();
            interaction.tick(Duration.ofMillis(50L));
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null) cleanupIfClosed(player, interaction);
        }
    }

    private void cleanupIfClosed(Player player, DialogueInteraction interaction) {
        if (interaction.isActive()) return;
        if (!interactions.remove(player.getUniqueId(), interaction)) return;
        unlockPlayer(player);
        recordDialogCooldown(player);
    }

    private void finishChoiceSelection(Player player, int selectedIndex, String questId, String flagBase,
                                       Consumer<Integer> callback) {
        recordDialogCooldown(player);
        BukkitTask task = resumeTasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
        if (questId != null && flagBase != null) {
            QuestManager manager = plugin.getQuestManager();
            manager.removeFlag(player.getUniqueId(), questId, flagBase + "pending");
            manager.setFlag(player.getUniqueId(), questId, flagBase + selectedIndex);
            pendingChoices.remove(player.getUniqueId());
        }
        if (callback != null) callback.accept(selectedIndex);
    }

    public void recordDialogCooldown(Player player) { dialogCooldowns.put(player.getUniqueId(), System.currentTimeMillis()); }

    public boolean resumePendingChoice(Player player, NPC npc) { return resumePendingChoice(player, DialogNpcRef.of(npc)); }
    public boolean resumePendingChoice(Player player, net.citizensnpcs.api.npc.NPC npc) { return resumePendingChoice(player, DialogNpcRef.of(npc)); }

    private boolean resumePendingChoice(Player player, DialogNpcRef npc) {
        PendingChoice pending = pendingChoices.get(player.getUniqueId());
        if (pending == null || (npc != null && pending.npc() != null && !pending.npc().matches(npc))) return false;
        if (pending.questId() != null && !plugin.getQuestManager().hasFlag(player.getUniqueId(), pending.questId(), pending.flagBase() + "pending")) {
            pendingChoices.remove(player.getUniqueId());
            return false;
        }
        if (pending.resumeLine() == null) {
            startChoiceDialog(player, npc, pending.options(), pending.questId(), pending.flagBase(), pending.callback());
            return true;
        }
        startDialog(player, List.of(pending.resumeLine()), npc, null);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            advanceDialog(player, plugin.getQuestManager());
            if (!hasSession(player)) startChoiceDialog(player, npc, pending.options(), pending.questId(), pending.flagBase(), pending.callback());
            resumeTasks.remove(player.getUniqueId());
        }, 1L);
        BukkitTask old = resumeTasks.put(player.getUniqueId(), task);
        if (old != null) old.cancel();
        return true;
    }

    public void checkDistance(Player player, double maxDistanceSquared) {
        DialogueInteraction interaction = interactions.get(player.getUniqueId());
        if (interaction == null) return;
        Location location = interaction.getNpcLocation();
        if (location != null && player.getLocation().distanceSquared(location) > maxDistanceSquared) {
            player.sendMessage(ChatColor.RED + "You walked away from the NPC. Dialogue cancelled.");
            resetDialog(player);
        }
    }

    private void lockPlayer(Player player) {
        player.setInvulnerable(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 60, 4, false, false, false));
    }

    private void unlockPlayer(Player player) { player.removePotionEffect(PotionEffectType.SLOWNESS); player.setInvulnerable(false); }

    private void pauseDialog(Player player) {
        DialogueInteraction interaction = interactions.get(player.getUniqueId());
        if (interaction == null) return;
        if (interaction.currentMessenger() instanceof OptionMessenger) {
            interactions.remove(player.getUniqueId(), interaction);
            interaction.cancel();
        } else interaction.pause();
        unlockPlayer(player);
        BukkitTask task = resumeTasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

    public void resetDialog(Player player) {
        DialogueInteraction interaction = interactions.remove(player.getUniqueId());
        if (interaction != null) interaction.cancel();
        unlockPlayer(player);
        recordDialogCooldown(player);
        lastLines.remove(player.getUniqueId());
        pendingChoices.remove(player.getUniqueId());
        BukkitTask task = resumeTasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) { pauseDialog(event.getPlayer()); }

    @EventHandler public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() != plugin) return;
        messengerTickTask.cancel();
        for (UUID id : List.copyOf(interactions.keySet())) {
            Player player = plugin.getServer().getPlayer(id);
            if (player != null) resetDialog(player);
        }
    }

    private void debug(String message) { if (plugin.getQuestManager().isDebug()) plugin.getLogger().info("[DialogDebug] " + message); }
}
