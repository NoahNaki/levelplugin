package me.nakilex.levelplugin.dialogue;

import me.nakilex.levelplugin.dialogue.model.DialogueDefinition;
import me.nakilex.levelplugin.dialogue.model.DialoguePage;
import me.nakilex.levelplugin.dialogue.render.ActionBarDialogueRenderer;
import me.nakilex.levelplugin.dialogue.render.DialogueActionBarSender;
import me.nakilex.levelplugin.dialogue.render.DialogueGlyphs;
import me.nakilex.levelplugin.dialogue.render.DialogueOffsetGlyphs;
import me.nakilex.levelplugin.dialogue.render.DialogueRenderContext;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
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
    private static final long RENDER_PERIOD_TICKS = 1L;
    private static final int RENDER_DURATION_TICKS = 20 * 10;
    private static final List<String> TUNING_KEYS = List.of(
            DialogueRenderContext.TUNE_DIALOGUE_BACKGROUND_OFFSET,
            DialogueRenderContext.TUNE_DIALOGUE_TEXT_OFFSET,
            DialogueRenderContext.TUNE_CHARACTER_OFFSET,
            DialogueRenderContext.TUNE_NAME_BACKGROUND_OFFSET,
            DialogueRenderContext.TUNE_NAME_TEXT_OFFSET,
            DialogueRenderContext.TUNE_INFO_TEXT_OFFSET
    );

    private final JavaPlugin plugin;
    private final DialogueManager dialogueManager;
    private final ActionBarDialogueRenderer renderer;
    private final DialogueActionBarSender actionBarSender;
    private final Map<UUID, BukkitTask> activeRenderTasks = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> tuningByPlayer = new HashMap<>();

    public DialogueDebugCommand(JavaPlugin plugin, DialogueManager dialogueManager) {
        this(plugin, dialogueManager, new DialogueActionBarSender());
    }

    public DialogueDebugCommand(JavaPlugin plugin, DialogueManager dialogueManager, DialogueActionBarSender actionBarSender) {
        this.plugin = plugin;
        this.dialogueManager = dialogueManager;
        this.actionBarSender = actionBarSender == null ? new DialogueActionBarSender() : actionBarSender;
        this.renderer = new ActionBarDialogueRenderer(this.actionBarSender);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> reload(sender);
            case "render" -> render(sender, label, args, true);
            case "renderonce" -> render(sender, label, args, false);
            case "inspect" -> inspect(sender, label, args);
            case "fonttest" -> fontTest(sender);
            case "tune" -> tune(sender, label, args);
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
            actionBarSender.clear(player);
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Stopped dialogue preview.");
        } else {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "No active dialogue preview.");
        }
    }

    private void render(CommandSender sender, String label, String[] args, boolean repeat) {
        if (!(sender instanceof Player player)) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                    "Only players can render dialogue previews.");
            return;
        }
        if (args.length < 3) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Usage: /" + label + " " + args[0].toLowerCase(Locale.ROOT) + " <dialogueId> <pageId>");
            return;
        }

        cancelRenderTask(player.getUniqueId());
        PageLookup lookup = resolvePage(player, args[1], args[2]);
        if (lookup == null) {
            return;
        }

        if (repeat) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                    "Rendering dialogue " + ChatColor.WHITE + lookup.dialogue().id() + ChatColor.GREEN
                            + " page " + ChatColor.WHITE + lookup.page().id() + ChatColor.GREEN + " for 10 seconds.");
            startRenderTask(player, lookup.dialogue(), lookup.page());
        } else {
            renderer.render(player, renderContext(player, lookup.dialogue(), lookup.page()));
        }
    }

    private void inspect(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                    "Usage: /" + label + " inspect <dialogueId> <pageId>");
            return;
        }

        PageLookup lookup = resolvePage(sender, args[1], args[2]);
        if (lookup == null) {
            return;
        }

        DialogueRenderContext context = sender instanceof Player player
                ? renderContext(player, lookup.dialogue(), lookup.page())
                : DialogueRenderContext.of(lookup.dialogue(), lookup.page());
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                ChatColor.YELLOW + "Dialogue HUD Inspect");
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "Dialogue: " + ChatColor.WHITE + lookup.dialogue().id());
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "Page: " + ChatColor.WHITE + lookup.page().id());
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "Offsets: " + ChatColor.WHITE
                        + "dialogueBackground=" + context.dialogueBackgroundOffsetPixels()
                        + ", dialogueText=" + context.dialogueTextOffsetPixels()
                        + ", character=" + context.characterOffsetPixels()
                        + ", nameBackground=" + context.nameBackgroundOffsetPixels()
                        + ", nameText=" + context.nameTextOffsetPixels()
                        + ", infoText=" + context.infoTextOffsetPixels());
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "Glyph widths: " + ChatColor.WHITE
                        + "dialogue=" + DialogueGlyphs.DIALOGUE_WIDTH
                        + ", answer=" + DialogueGlyphs.ANSWER_WIDTH
                        + ", character=" + DialogueGlyphs.CHARACTER_WIDTH
                        + ", arrow=" + DialogueGlyphs.ARROW_WIDTH
                        + ", nameStart=" + DialogueGlyphs.NAME_START_WIDTH
                        + ", nameMid=" + DialogueGlyphs.NAME_MID_WIDTH
                        + ", nameEnd=" + DialogueGlyphs.NAME_END_WIDTH
                        + ", fog=" + DialogueGlyphs.FOG_WIDTH);
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "Flags: " + ChatColor.WHITE
                        + "fog=" + context.fogEnabled()
                        + ", characterBox=" + context.characterBoxEnabled()
                        + ", nameBox=" + context.nameBoxEnabled());
    }

    private void fontTest(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                    "Only players can run dialogue font tests.");
            return;
        }

        String miniMessage = "<font:" + DialogueGlyphs.OFFSET_FONT_TAG + ">"
                + DialogueOffsetGlyphs.POSITIVE_ONE_PIXEL.repeat(8)
                + "</font>"
                + "<font:" + DialogueGlyphs.DEFAULT_TEXT_FONT + ">offset test</font> "
                + "<font:" + DialogueGlyphs.DIALOGUE_FONT_TAG + ">" + DialogueGlyphs.DIALOGUE_BACKGROUND + "</font>"
                + "<font:" + DialogueGlyphs.DEFAULT_TEXT_FONT + "> dialogue glyph test </font>"
                + "<font:" + DialogueGlyphs.LINE_FONT_PREFIX + "1>line_1 font test</font> "
                + "<font:" + DialogueGlyphs.LINE_FONT_PREFIX + "2>line_2 font test</font>";
        actionBarSender.sendMiniMessage(player, miniMessage);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Sent dialogue font test. If offset glyphs are visible boxes, fix the resource pack fonts first.");
    }

    private void tune(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                    "Only players can tune dialogue preview offsets.");
            return;
        }
        if (args.length < 3) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Usage: /" + label + " tune <key> <value>");
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Keys: " + ChatColor.WHITE + String.join(", ", TUNING_KEYS));
            return;
        }

        String key = findTuningKey(args[1]);
        if (key == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Unknown tuning key '" + args[1] + "'.");
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Keys: " + ChatColor.WHITE + String.join(", ", TUNING_KEYS));
            return;
        }

        int value;
        try {
            value = Integer.parseInt(args[2]);
        } catch (NumberFormatException exception) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Tuning value must be a whole number.");
            return;
        }

        tuningByPlayer.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>()).put(key, value);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Set " + ChatColor.WHITE + key + ChatColor.GREEN + " to " + ChatColor.WHITE + value + ChatColor.GREEN + ".");
    }

    private DialogueRenderContext renderContext(Player player, DialogueDefinition dialogue, DialoguePage page) {
        return DialogueRenderContext.of(dialogue, page)
                .withTuning(tuningByPlayer.get(player.getUniqueId()));
    }

    private void startRenderTask(Player player, DialogueDefinition dialogue, DialoguePage page) {
        UUID playerId = player.getUniqueId();
        DialogueRenderContext context = renderContext(player, dialogue, page);
        BukkitTask task = new BukkitRunnable() {
            private int ticksRemaining = RENDER_DURATION_TICKS;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancelActiveTask(playerId, this, false);
                    return;
                }

                renderer.render(player, context);
                ticksRemaining -= RENDER_PERIOD_TICKS;
                if (ticksRemaining <= 0) {
                    cancelActiveTask(playerId, this, true);
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

    private void cancelActiveTask(UUID playerId, BukkitRunnable runnable, boolean clearActionBar) {
        runnable.cancel();
        activeRenderTasks.remove(playerId);
        if (clearActionBar) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null && player.isOnline()) {
                actionBarSender.clear(player);
            }
        }
    }

    private PageLookup resolvePage(CommandSender sender, String dialogueId, String pageId) {
        DialogueDefinition dialogue = dialogueManager.getDialogue(dialogueId);
        if (dialogue == null) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                    "Dialogue '" + dialogueId + "' does not exist.");
            return null;
        }

        DialoguePage page = dialogue.pages().get(pageId);
        if (page == null) {
            page = findPageIgnoreCase(dialogue, pageId);
        }
        if (page == null) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                    "Page '" + pageId + "' does not exist in dialogue '" + dialogue.id() + "'.");
            return null;
        }
        return new PageLookup(dialogue, page);
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
                "/" + label + " inspect <dialogueId> <pageId>" + ChatColor.GRAY
                        + " - Print dialogue HUD diagnostics.");
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "/" + label + " fonttest" + ChatColor.GRAY
                        + " - Send dialogue font and offset diagnostics.");
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "/" + label + " render <dialogueId> <pageId>" + ChatColor.GRAY
                        + " - Preview a static dialogue page for 10 seconds.");
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "/" + label + " renderonce <dialogueId> <pageId>" + ChatColor.GRAY
                        + " - Send a static dialogue page once.");
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "/" + label + " tune <key> <value>" + ChatColor.GRAY
                        + " - Tune your preview offsets in memory.");
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "/" + label + " stop" + ChatColor.GRAY + " - Stop your active dialogue preview.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return matching(List.of("render", "renderonce", "inspect", "fonttest", "reload", "stop", "tune"), args[0]);
        }
        if (args.length == 2 && "tune".equalsIgnoreCase(args[0])) {
            return matching(TUNING_KEYS, args[1]);
        }
        if (args.length == 2 && usesDialoguePage(args[0])) {
            return matching(dialogueManager.getDialogues().stream().map(DialogueDefinition::id).toList(), args[1]);
        }
        if (args.length == 3 && usesDialoguePage(args[0])) {
            DialogueDefinition dialogue = dialogueManager.getDialogue(args[1]);
            if (dialogue == null) {
                return List.of();
            }
            return matching(dialogue.pages().keySet().stream().toList(), args[2]);
        }
        return List.of();
    }

    private String findTuningKey(String input) {
        for (String key : TUNING_KEYS) {
            if (key.equalsIgnoreCase(input)) {
                return key;
            }
        }
        return null;
    }

    private boolean usesDialoguePage(String subcommand) {
        return "render".equalsIgnoreCase(subcommand)
                || "renderonce".equalsIgnoreCase(subcommand)
                || "inspect".equalsIgnoreCase(subcommand);
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

    private record PageLookup(DialogueDefinition dialogue, DialoguePage page) {
    }
}
