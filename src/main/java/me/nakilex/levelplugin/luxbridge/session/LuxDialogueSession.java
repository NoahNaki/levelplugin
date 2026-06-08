package me.nakilex.levelplugin.luxbridge.session;

import me.nakilex.levelplugin.luxbridge.LuxBridgeManager;
import me.nakilex.levelplugin.luxbridge.model.LuxAnswer;
import me.nakilex.levelplugin.luxbridge.model.LuxDialogue;
import me.nakilex.levelplugin.luxbridge.model.LuxPage;
import me.nakilex.levelplugin.luxbridge.model.LuxSoundSpec;
import me.nakilex.levelplugin.luxbridge.render.LuxBridgeRenderer;
import me.nakilex.levelplugin.luxbridge.util.LuxBridgeFormat;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class LuxDialogueSession {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final JavaPlugin plugin;
    private final LuxBridgeManager manager;
    private final Player player;
    private final LuxDialogue dialogue;
    private final LuxBridgeRenderer renderer;

    private LuxPage page;
    private int typedCharacters;
    private int selectedAnswer = 1;
    private BukkitTask task;

    public LuxDialogueSession(JavaPlugin plugin, LuxBridgeManager manager, Player player, LuxDialogue dialogue) {
        this.plugin = plugin;
        this.manager = manager;
        this.player = player;
        this.dialogue = dialogue;
        this.renderer = new LuxBridgeRenderer(manager.resourceManager());
        this.page = dialogue.firstPage();
    }

    public void start() {
        if (page == null) {
            stop(false);
            return;
        }
        runActions(page.preActions());
        task = new BukkitRunnable() {
            private int ticks;
            @Override public void run() {
                if (!player.isOnline()) {
                    stop(false);
                    return;
                }
                int total = renderer.totalLineLength(page);
                int speed = Math.max(1, dialogue.typingSpeed());
                if (typedCharacters < total && ticks++ % speed == 0) {
                    typedCharacters++;
                    playSound(dialogue.typingSound());
                }
                player.sendActionBar(MINI_MESSAGE.deserialize(renderer.render(player, dialogue, page, typedCharacters, selectedAnswer)));
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void stop(boolean runExit) {
        if (task != null) task.cancel();
        if (runExit && page != null) runActions(page.exitActions());
        player.sendActionBar(net.kyori.adventure.text.Component.empty());
        manager.removeSession(player.getUniqueId());
    }

    public void skipOrNext() {
        int total = renderer.totalLineLength(page);
        if (typedCharacters < total) {
            typedCharacters = total;
            return;
        }
        if (!page.gotoPage().isBlank()) {
            goTo(page.gotoPage());
        }
    }

    public void selectNext() {
        int count = Math.min(page.answers().size(), manager.resourceManager().lines().answerLineCount());
        if (count <= 0) return;
        selectedAnswer = selectedAnswer >= count ? 1 : selectedAnswer + 1;
        playSound(dialogue.selectionSound());
    }

    public void acceptAnswer() {
        List<LuxAnswer> answers = new ArrayList<>(page.answers().values());
        if (answers.isEmpty() || selectedAnswer < 1 || selectedAnswer > answers.size()) return;
        LuxAnswer answer = answers.get(selectedAnswer - 1);
        playSound(answer.sound());
        for (String reply : answer.replies()) player.sendMessage(MINI_MESSAGE.deserialize(LuxBridgeFormat.miniMessageText(reply)));
        runActions(answer.actions());
        if (!answer.gotoPage().isBlank()) {
            goTo(answer.gotoPage());
        } else {
            stop(true);
        }
    }

    private void goTo(String pageId) {
        LuxPage next = dialogue.pages().get(pageId);
        if (next == null) {
            stop(true);
            return;
        }
        runActions(page.postActions());
        page = next;
        typedCharacters = 0;
        selectedAnswer = 1;
        runActions(page.preActions());
    }

    private void playSound(LuxSoundSpec sound) {
        if (sound == null || sound.id() == null || sound.id().isBlank()) return;
        try {
            player.playSound(player.getLocation(), sound.id().replace("luxdialogues:", "levelplugin_dialogue:"), SoundCategory.valueOf(sound.source().toUpperCase()), sound.volume(), sound.pitch());
        } catch (IllegalArgumentException ignored) {
            player.playSound(player.getLocation(), sound.id().replace("luxdialogues:", "levelplugin_dialogue:"), SoundCategory.MASTER, sound.volume(), sound.pitch());
        }
    }

    private void runActions(List<String> actions) {
        for (String action : actions) {
            if (action == null || action.isBlank()) continue;
            String command = action.replace("%player%", player.getName()).replace("{player}", player.getName());
            if (command.startsWith("[console]")) {
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command.substring(9).trim());
            } else if (command.startsWith("[player]")) {
                player.performCommand(command.substring(8).trim());
            }
        }
    }
}
