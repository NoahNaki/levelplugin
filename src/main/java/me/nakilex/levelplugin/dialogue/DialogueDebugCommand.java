package me.nakilex.levelplugin.dialogue;

import me.nakilex.levelplugin.dialogue.model.DialogueDefinition;
import me.nakilex.levelplugin.dialogue.model.DialoguePage;
import me.nakilex.levelplugin.dialogue.render.ActionBarDialogueRenderer;
import me.nakilex.levelplugin.dialogue.render.DialogueRenderContext;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Temporary developer command for manually previewing static Lux-style dialogue pages.
 */
public class DialogueDebugCommand implements TabExecutor {
    private static final long RENDER_PERIOD_TICKS = 2L;
    private static final int RENDER_DURATION_TICKS = 20 * 10;

    private final JavaPlugin plugin;
    private final DialogueManager dialogueManager;
    private final ActionBarDialogueRenderer renderer;
    private final Map<UUID, BukkitTask> activeRenderTasks = new HashMap<>();

    public DialogueDebugCommand(JavaPlugin plugin, DialogueManager dialogueManager) {
        this(plugin, dialogueManager, new ActionBarDialogueRenderer());
    }

    public DialogueDebugCommand(JavaPlugin plugin, DialogueManager dialogueManager, ActionBarDialogueRenderer renderer) {
        this.plugin = plugin;
        this.dialogueManager = dialogueManager;
        this.renderer = renderer == null ? new ActionBarDialogueRenderer() : renderer;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> reload(sender);
            case "render" -> render(sender, label, args);
            case "stop" -> stop(sender);
            default -> sendUsage(sender, label);
        }
        return true;
    }

    private void reload(CommandSender sender) {
        dialogueManager.reload();
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.SUCCESS,
                "Reloaded dialogues (" + dialogueManager.getDialogues().size() + " loaded).");
    }

    private void stop(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                    "Only players can stop dialogue previews.");
            return;
        }

        if (cancelRenderTask(player.getUniqueId())) {
            player.sendActionBar(Component.empty());
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Stopped dialogue preview.");
        } else {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "No active dialogue preview.");
        }
    }

    private void render(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                    "Only players can render dialogue previews.");
            return;
        }
        if (args.length < 3) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Usage: /" + label + " render <dialogueId> <pageId>");
            return;
        }

        cancelRenderTask(player.getUniqueId());

        String dialogueId = args[1];
        String pageId = args[2];
        DialogueDefinition dialogue = dialogueManager.getDialogue(dialogueId);
        if (dialogue == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Dialogue '" + dialogueId + "' does not exist.");
            return;
        }

        DialoguePage page = dialogue.pages().get(pageId);
        if (page == null) {
            page = findPageIgnoreCase(dialogue, pageId);
        }
        if (page == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Page '" + pageId + "' does not exist in dialogue '" + dialogue.id() + "'.");
            return;
        }

        startRenderTask(player, dialogue, page);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Rendering dialogue " + ChatColor.WHITE + dialogue.id() + ChatColor.GREEN
                        + " page " + ChatColor.WHITE + page.id() + ChatColor.GREEN + " for 10 seconds.");
    }

    private void startRenderTask(Player player, DialogueDefinition dialogue, DialoguePage page) {
        UUID playerId = player.getUniqueId();
        DialogueRenderContext context = DialogueRenderContext.of(dialogue, page);
        BukkitTask task = new BukkitRunnable() {
            private int ticksRemaining = RENDER_DURATION_TICKS;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancelActiveTask(playerId, this);
                    return;
                }

                renderer.render(player, context);
                ticksRemaining -= RENDER_PERIOD_TICKS;
                if (ticksRemaining <= 0) {
                    cancelActiveTask(playerId, this);
                }
            }
        }.runTaskTimer(plugin, 0L, RENDER_PERIOD_TICKS);
        activeRenderTasks.put(playerId, task);
    }

    private boolean cancelRenderTask(UUID playerId) {
        BukkitTask task = activeRenderTasks.remove(playerId);
        if (task == null) {
            return false;
        }
        task.cancel();
        return true;
    }

    private void cancelActiveTask(UUID playerId, BukkitRunnable runnable) {
        runnable.cancel();
        activeRenderTasks.remove(playerId);
    }

    private DialoguePage findPageIgnoreCase(DialogueDefinition dialogue, String pageId) {
        for (DialoguePage page : dialogue.pages().values()) {
            if (page.id().equalsIgnoreCase(pageId)) {
                return page;
            }
        }
        return null;
    }

    private void sendUsage(CommandSender sender, String label) {
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "/" + label + " reload" + ChatColor.GRAY + " - Reload Lux-style dialogue YAML.");
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "/" + label + " render <dialogueId> <pageId>" + ChatColor.GRAY
                        + " - Preview a static dialogue page for 10 seconds.");
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "/" + label + " stop" + ChatColor.GRAY + " - Stop your active dialogue preview.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return matching(List.of("render", "reload", "stop"), args[0]);
        }
        if (args.length == 2 && "render".equalsIgnoreCase(args[0])) {
            return matching(dialogueManager.getDialogues().stream().map(DialogueDefinition::id).toList(), args[1]);
        }
        if (args.length == 3 && "render".equalsIgnoreCase(args[0])) {
            DialogueDefinition dialogue = dialogueManager.getDialogue(args[1]);
            if (dialogue == null) {
                return List.of();
            }
            return matching(dialogue.pages().keySet().stream().toList(), args[2]);
        }
        return List.of();
    }

    private List<String> matching(List<String> values, String input) {
        String prefix = input == null ? "" : input.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String value : values) {
            if (value != null && value.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                matches.add(value);
            }
        }
        return matches;
    }
}
